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
import java.util.Objects;
import java.util.Set;

import javax.swing.Icon;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.DataSet;
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

    protected Map<PrimitiveId, PrimitiveData> makeIncompleteDataByPrimId;

    protected final ConflictCollection purgedConflicts = new ConflictCollection();

    /**
     * Constructs a new {@code PurgeCommand} (handles conflicts).
     * This command relies on a number of consistency conditions:
     *  - makeIncomplete must be a subset of toPurge.
     *  - Each primitive, that is in toPurge but not in makeIncomplete, must have all its referrers in toPurge.
     *  - Each element of makeIncomplete must not be new and must have only referrers that are either a relation or included in toPurge.
     * @param layer OSM data layer
     * @param toPurge primitives to purge
     * @param makeIncomplete primitives to make incomplete
     */
    public PurgeCommand(OsmDataLayer layer, Collection<OsmPrimitive> toPurge, Collection<OsmPrimitive> makeIncomplete) {
        super(layer);
        init(toPurge, makeIncomplete);
    }

    /**
     * Constructs a new {@code PurgeCommand} (does not handle conflicts).
     * This command relies on a number of consistency conditions:
     *  - makeIncomplete must be a subset of toPurge.
     *  - Each primitive, that is in toPurge but not in makeIncomplete, must have all its referrers in toPurge.
     *  - Each element of makeIncomplete must not be new and must have only referrers that are either a relation or included in toPurge.
     * @param data OSM data set
     * @param toPurge primitives to purge
     * @param makeIncomplete primitives to make incomplete
     * @since 11240
     */
    public PurgeCommand(DataSet data, Collection<OsmPrimitive> toPurge, Collection<OsmPrimitive> makeIncomplete) {
        super(data);
        init(toPurge, makeIncomplete);
    }

    private void init(Collection<OsmPrimitive> toPurge, Collection<OsmPrimitive> makeIncomplete) {
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

    protected final void saveIncomplete(Collection<OsmPrimitive> makeIncomplete) {
        makeIncompleteData = new Storage<>(new Storage.PrimitiveIdHash());
        makeIncompleteDataByPrimId = makeIncompleteData.foreignKey(new Storage.PrimitiveIdHash());

        for (OsmPrimitive osm : makeIncomplete) {
            makeIncompleteData.add(osm.save());
        }
    }

    @Override
    public boolean executeCommand() {
        getAffectedDataSet().beginUpdate();
        try {
            purgedConflicts.get().clear();
            // unselect primitives in advance to not fire a selection change for every one of them
            getAffectedDataSet().clearSelection(toPurge);
            // Loop from back to front to keep referential integrity.
            for (int i = toPurge.size()-1; i >= 0; --i) {
                OsmPrimitive osm = toPurge.get(i);
                if (makeIncompleteDataByPrimId.containsKey(osm)) {
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
                    getAffectedDataSet().removePrimitive(osm);
                    Conflict<?> conflict = getAffectedDataSet().getConflicts().getConflictForMy(osm);
                    if (conflict != null) {
                        purgedConflicts.add(conflict);
                        getAffectedDataSet().getConflicts().remove(conflict);
                    }
                }
            }
        } finally {
            getAffectedDataSet().endUpdate();
        }
        return true;
    }

    @Override
    public void undoCommand() {
        if (getAffectedDataSet() == null)
            return;

        for (OsmPrimitive osm : toPurge) {
            PrimitiveData data = makeIncompleteDataByPrimId.get(osm);
            if (data != null) {
                if (getAffectedDataSet().getPrimitiveById(osm) != osm)
                    throw new AssertionError(
                            String.format("Primitive %s has been made incomplete when purging, but it cannot be found on undo.", osm));
                osm.load(data);
            } else {
                if (getAffectedDataSet().getPrimitiveById(osm) != null)
                    throw new AssertionError(String.format("Primitive %s was removed when purging, but is still there on undo", osm));
                getAffectedDataSet().addPrimitive(osm);
            }
        }

        for (Conflict<?> conflict : purgedConflicts) {
            getAffectedDataSet().getConflicts().add(conflict);
        }
    }

    /**
     * Sorts a collection of primitives such that for each object
     * its referrers come later in the sorted collection.
     * @param sel collection of primitives to sort
     * @return sorted list
     */
    public static List<OsmPrimitive> topoSort(Collection<OsmPrimitive> sel) {
        Set<OsmPrimitive> in = new HashSet<>(sel);

        List<OsmPrimitive> out = new ArrayList<>(in.size());

        // Nodes not deleted in the first pass
        Set<OsmPrimitive> remainingNodes = new HashSet<>(in.size());

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
                            it.remove();
                            remainingNodes.add(n);
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
        for (Iterator<OsmPrimitive> it = in.iterator(); it.hasNext();) {
            OsmPrimitive u = it.next();
            if (u instanceof Way) {
                Way w = (Way) u;
                it.remove();
                for (Node n : w.getNodes()) {
                    if (remainingNodes.contains(n)) {
                        remainingNodes.remove(n);
                        out.add(n);
                    }
                }
                out.add(w);
            }
        }

        if (!remainingNodes.isEmpty())
            throw new AssertionError("topo sort algorithm failed (nodes remaining)");

        /**
          * Rest are relations. Do topological sorting on a DAG where each
          * arrow points from child to parent. (Because it is faster to
          * loop over getReferrers() than getMembers().)
          */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        Set<Relation> inR = (Set) in;

        Map<Relation, Integer> numChilds = new HashMap<>();

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
                    numChilds.put((Relation) parent, i+1);
                }
            }
        }
        Set<Relation> childlessR = new HashSet<>();
        for (Relation r : inR) {
            if (numChilds.get(r).equals(0)) {
                childlessR.add(r);
            }
        }

        List<Relation> outR = new ArrayList<>(inR.size());
        while (!childlessR.isEmpty()) {
            // Identify one childless Relation and let it virtually die. This makes other relations childless.
            Iterator<Relation> it = childlessR.iterator();
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
    public String getDescriptionText() {
        return trn("Purged {0} object", "Purged {0} objects", toPurge.size(), toPurge.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "purge");
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return toPurge;
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        // Do nothing
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), toPurge, makeIncompleteData, makeIncompleteDataByPrimId, purgedConflicts, getAffectedDataSet());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        PurgeCommand that = (PurgeCommand) obj;
        return Objects.equals(toPurge, that.toPurge) &&
                Objects.equals(makeIncompleteData, that.makeIncompleteData) &&
                Objects.equals(makeIncompleteDataByPrimId, that.makeIncompleteDataByPrimId) &&
                Objects.equals(purgedConflicts, that.purgedConflicts);
    }
}
