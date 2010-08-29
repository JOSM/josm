// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Hash;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeData;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveData;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationData;
import org.openstreetmap.josm.data.osm.Storage;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WayData;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Command, to purge a list of primitives.
 */
public class PurgeCommand extends Command {
    protected List<OsmPrimitive> toPurge;
    protected Storage<PrimitiveData> makeIncompleteData;

    protected Map<PrimitiveId, PrimitiveData> makeIncompleteData_byPrimId;

    final protected DataSet ds;

    /**
     * This command relies on a number of consistency conditions:
     *  - makeIncomplete must be a subset of toPurge.
     *  - Each primitive, that is in toPurge but not in makeIncomplete, must
     *      have all its referrers in toPurge.
     *  - Each element of makeIncomplete must not be new and must have only
     *      referrers that are either a relation or included in toPurge.
     */
    public PurgeCommand(OsmDataLayer layer, Collection<OsmPrimitive> toPurge, Collection<OsmPrimitive> makeIncomplete) {
        super(layer);
        this.ds = layer.data;
        /**
         * The topological sort is to avoid missing way nodes and missing
         * relation members when adding primitives back to the dataset on undo.
         *
         * The same should hold for normal execution, but at time of writing
         * there seem to be no such consistency checks when removing primitives.
         * (It is done in a save manner, anyway.)
         */
        this.toPurge = topoSort(toPurge);
        saveIncomplete(makeIncomplete);
    }

    protected void saveIncomplete(Collection<OsmPrimitive> makeIncomplete) {
        makeIncompleteData = new Storage<PrimitiveData>(new Hash<PrimitiveData,PrimitiveData>() {
            public int getHashCode(PrimitiveData k) {
                return (int) k.getUniqueId();
            }

            public boolean equals(PrimitiveData k, PrimitiveData t) {
                return k.getUniqueId() == t.getUniqueId() && k.getType() == t.getType();
            }
        });
        makeIncompleteData_byPrimId = makeIncompleteData.foreignKey(new Hash<PrimitiveId, PrimitiveData>() {
            public int getHashCode(PrimitiveId k) {
                return (int) k.getUniqueId();
            }

            public boolean equals(PrimitiveId k, PrimitiveData t) {
                return k.getUniqueId() == t.getUniqueId() && k.getType() == t.getType();
            }
        });

        for (OsmPrimitive osm : makeIncomplete) {
            makeIncompleteData.add(osm.save());
        }
    }

    @Override
    public boolean executeCommand() {
        ds.beginUpdate();
        try {
            /**
             * Loop from back to front to keep referential integrity.
             */
            for (int i=toPurge.size()-1; i>=0; --i) {
                OsmPrimitive osm = toPurge.get(i);
                if (makeIncompleteData_byPrimId.containsKey(osm)) {
                    // we could simply set the incomplete flag
                    // but that would not free memory in case the
                    // user clears undo/redo buffer after purge
                    PrimitiveData empty;
                    switch(osm.getType()) {
                    case NODE: empty = new NodeData(); break;
                    case WAY: empty = new WayData(); break;
                    case RELATION: empty = new RelationData(); break;
                    default: throw new AssertionError();
                    }
                    empty.setId(osm.getUniqueId());
                    empty.setIncomplete(true);
                    osm.load(empty);
                } else {
                    ds.removePrimitive(osm);
                }
            }
        } finally {
            ds.endUpdate();
        }
        return true;
    }

    @Override
    public void undoCommand() {
        if (ds == null)
            return;

        for (OsmPrimitive osm : toPurge) {
            PrimitiveData data = makeIncompleteData_byPrimId.get(osm);
            if (data != null) {
                if (ds.getPrimitiveById(osm) != osm)
                    throw new AssertionError(String.format("Primitive %s has been made incomplete when purging, but it cannot be found on undo.", osm));
                osm.load(data);
            } else {
                if (ds.getPrimitiveById(osm) != null)
                    throw new AssertionError(String.format("Primitive %s was removed when purging, but is still there on undo", osm));
                ds.addPrimitive(osm);
            }
        }
    }

    /**
     * Sorts a collection of primitives such that for each object
     * its referrers come later in the sorted collection.
     */
    public static List<OsmPrimitive> topoSort(Collection<OsmPrimitive> sel) {
        Set<OsmPrimitive> in = new HashSet<OsmPrimitive>(sel);

        List<OsmPrimitive> out = new ArrayList<OsmPrimitive>(in.size());

        /**
         *  First add nodes that have no way referrer.
         */
        outer:
            for (Iterator<OsmPrimitive> it = in.iterator(); it.hasNext();) {
                OsmPrimitive u = it.next();
                if (u instanceof Node) {
                    Node n = (Node) u;
                    for (OsmPrimitive ref : n.getReferrers()) {
                        if (ref instanceof Way && in.contains(ref)) {
                            continue outer;
                        }
                    }
                    it.remove();
                    out.add(n);
                }
            }

        /**
         * Then add all ways, each preceded by its (remaining) nodes.
         */
        top:
            while (!in.isEmpty()) {
                for (Iterator<OsmPrimitive> it = in.iterator(); it.hasNext();) {
                    OsmPrimitive u = it.next();
                    if (u instanceof Way) {
                        Way w = (Way) u;
                        it.remove();
                        for (Node n : w.getNodes()) {
                            if (in.contains(n)) {
                                in.remove(n);
                                out.add(n);
                            }
                        }
                        out.add(w);
                        continue top;
                    }
                }
                break; // no more ways left
            }

            /**
             * Rest are relations. Do topological sorting on a DAG where each
             * arrow points from child to parent. (Because it is faster to
             * loop over getReferrers() than getMembers().)
             */
            Set<Relation> inR = (Set) in;
            Set<Relation> childlessR = new HashSet<Relation>();
            List<Relation> outR = new ArrayList<Relation>(inR.size());

            HashMap<Relation, Integer> numChilds = new HashMap<Relation, Integer>();

            // calculate initial number of childs
            for (Relation r : inR) {
                numChilds.put(r, 0);
            }
            for (Relation r : inR) {
                for (OsmPrimitive parent : r.getReferrers()) {
                    if (!(parent instanceof Relation))
                        throw new AssertionError();
                    Integer i = numChilds.get(parent);
                    if (i != null) {
                        numChilds.put((Relation)parent, i+1);
                    }
                }
            }
            for (Relation r : inR) {
                if (numChilds.get(r).equals(0)) {
                    childlessR.add(r);
                }
            }

            while (!childlessR.isEmpty()) {
                // Identify one childless Relation and
                // let it virtually die. This makes other
                // relations childless.
                Iterator<Relation> it  = childlessR.iterator();
                Relation next = it.next();
                it.remove();
                outR.add(next);

                for (OsmPrimitive parentPrim : next.getReferrers()) {
                    Relation parent = (Relation) parentPrim;
                    Integer i = numChilds.get(parent);
                    if (i != null) {
                        numChilds.put(parent, i-1);
                        if (i-1 == 0) {
                            childlessR.add(parent);
                        }
                    }
                }
            }

            if (outR.size() != inR.size())
                throw new AssertionError("topo sort algorithm failed");

            out.addAll(outR);

            return out;
    }

    @Override
    public Object getDescription() {
        return new JLabel(trn("Purged {0} object", "Purged {0} objects", toPurge.size(), toPurge.size()), ImageProvider.get("data", "purge"), JLabel.HORIZONTAL);
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return toPurge;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
    }
}
