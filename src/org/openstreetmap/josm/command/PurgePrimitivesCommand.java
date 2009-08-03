// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Physically removes an {@see OsmPrimitive} from the dataset of the edit
 * layer and disconnects any references from {@see Way}s or {@see Relation}s
 * to this primitive.
 *
 * This command is necessary if a local {@see OsmPrimitive} has been deleted on
 * the server by another user and if the local user decides to delete his version
 * too. If he only deleted it "logically" JOSM would try to delete it on the server
 * which would result in an non resolvable conflict.
 *
 */
public class PurgePrimitivesCommand extends ConflictResolveCommand{

    static private final Logger logger = Logger.getLogger(PurgePrimitivesCommand.class.getName());

    /**
     * Represents a pair of {@see OsmPrimitive} where the parent referrs to
     * the child, either because a {@see Way} includes a {@see Node} or
     * because a {@see Relation} refers to any other {@see OsmPrimitive}
     * via a relation member.
     *
     */
    static class OsmParentChildPair {
        private OsmPrimitive parent;
        private OsmPrimitive child;


        public OsmParentChildPair(OsmPrimitive parent, OsmPrimitive child) {
            this.parent = parent;
            this.child = child;
        }

        public OsmPrimitive getParent() {
            return parent;
        }

        public OsmPrimitive getChild() {
            return child;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((child == null) ? 0 : child.hashCode());
            result = prime * result + ((parent == null) ? 0 : parent.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            OsmParentChildPair other = (OsmParentChildPair) obj;
            if (child == null) {
                if (other.child != null)
                    return false;
            } else if (child != other.child)
                return false;
            if (parent == null) {
                if (other.parent != null)
                    return false;
            } else if (parent != other.parent)
                return false;
            return true;
        }
    }

    /**
     * creates a list of all {@see OsmParentChildPair}s for a given {@see OsmPrimitive}
     * as child and given set of parents. We don't use {@see CollectBackReferencesVisitor}
     * because it seems quite inefficient.
     *
     * @param parents  the set of potential parents
     * @param child the child
     * @return the list of {@see OsmParentChildPair}
     */
    protected List<OsmParentChildPair> getParentChildPairs(List<OsmPrimitive> parents, OsmPrimitive child) {
        ArrayList<OsmParentChildPair> pairs = new ArrayList<OsmParentChildPair>();
        for (OsmPrimitive parent : parents) {
            if (parent instanceof Way) {
                Way w = (Way)parent;
                for (OsmPrimitive node : w.getNodes()) {
                    if (node == child) {
                        OsmParentChildPair pair = new OsmParentChildPair(parent, node);
                        if (! pairs.contains(pair)) {
                            pairs.add(pair);
                        }
                    }
                }
            } else if (parent instanceof Relation) {
                Relation r = (Relation)parent;
                for (RelationMember member : r.members) {
                    if (member.member == child) {
                        OsmParentChildPair pair = new OsmParentChildPair(parent, member.member);
                        if (! pairs.contains(pair)) {
                            pairs.add(pair);
                        }
                    }
                }
            }
        }
        return pairs;
    }

    /** the primitive to purge */
    private OsmPrimitive primitive;

    /** the set of primitives to purge as consequence of purging
     * {@see #primitive}, including {@see #primitive}
     */
    private ArrayList<OsmPrimitive> purgedPrimitives;

    /** the set of {@see OsmParentChildPair}. We keep a reference
     * to this set for the {@see #fillModifiedData(Collection, Collection, Collection)} operation
     */
    private ArrayList<OsmParentChildPair> pairs;


    /**
     * constructor
     * @param node  the node to undelete
     */
    public PurgePrimitivesCommand(OsmPrimitive primitive) {
        this.primitive = primitive;
        purgedPrimitives = new ArrayList<OsmPrimitive>();
        pairs = new ArrayList<OsmParentChildPair>();
    }

    @Override
    public MutableTreeNode description() {
        return new DefaultMutableTreeNode(
                new JLabel(
                        tr("Purging 1 primitive"),
                        ImageProvider.get("data", "object"),
                        JLabel.HORIZONTAL
                )
        );
    }

    /**
     * Purges an {@see OsmPrimitive} <code>toPurge</code> from a {@see DataSet}.
     *
     * @param toPurge the primitive to purge
     * @param ds  the dataset to purge from
     * @param hive the hive of {@see OsmPrimitive}s we remember other {@see OsmPrimitive}
     * we have to purge because we purge <code>toPurge</code>.
     *
     */
    protected void purge(OsmPrimitive toPurge, DataSet ds, ArrayList<OsmPrimitive> hive) {
        ArrayList<OsmPrimitive> parents = new ArrayList<OsmPrimitive>();
        parents.addAll(getLayer().data.ways);
        parents.addAll(getLayer().data.relations);
        List<OsmParentChildPair> pairs = getParentChildPairs(parents, primitive);
        hive.remove(toPurge);
        for (OsmParentChildPair pair: pairs) {
            if (pair.getParent() instanceof Way) {
                Way w = (Way)pair.getParent();
                System.out.println(tr("removing reference from way {0}",w.id));
                w.nodes.remove(primitive);
                // if a way ends up with less than two node we
                // remember it on the "hive"
                //
                if (w.getNodesCount() < 2) {
                    System.out.println(tr("Warning: Purging way {0} because number of nodes dropped below 2. Current is {1}",
                            w.id,w.getNodesCount()));
                    if (!hive.contains(w)) {
                        hive.add(w);
                    }
                }
            } else if (pair.getParent() instanceof Relation) {
                Relation r = (Relation)pair.getParent();
                System.out.println(tr("removing reference from relation {0}",r.id));
                r.removeMembersFor(primitive);
            }
        }
    }

    @Override
    public boolean executeCommand() {
        ArrayList<OsmPrimitive> hive = new ArrayList<OsmPrimitive>();

        // iteratively purge the primitive and all primitives
        // which violate invariants after they loose a reference to
        // the primitive (i.e. ways which end up with less than two
        // nodes)
        hive.add(primitive);
        while(! hive.isEmpty()) {
            OsmPrimitive toPurge = hive.get(0);
            purge(toPurge, getLayer().data, hive);
            if (toPurge instanceof Node) {
                getLayer().data.nodes.remove(toPurge);
            } else if (primitive instanceof Way) {
                getLayer().data.ways.remove(toPurge);
            } else if (primitive instanceof Relation) {
                getLayer().data.relations.remove(toPurge);
            }
            purgedPrimitives.add(toPurge);
            ConflictCollection conflicts = getLayer().getConflicts();
            if (conflicts.hasConflictForMy(toPurge)) {
                rememberConflict(conflicts.getConflictForMy(toPurge));
                conflicts.remove(toPurge);
            }
        }
        return super.executeCommand();
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted,
            Collection<OsmPrimitive> added) {
        for (OsmParentChildPair pair : pairs) {
            modified.add(pair.getParent());
        }
        // we don't need pairs anymore
        pairs = null;
    }

    @Override
    public void undoCommand() {
        if (! Main.map.mapView.hasLayer(getLayer())) {
            logger.warning(tr("Can't undo command ''{0}'' because layer ''{1}'' is not present anymore",
                    this.toString(),
                    getLayer().toString()
            ));
            return;
        }
        Main.map.mapView.setActiveLayer(getLayer());

        // restore purged primitives
        //
        for (OsmPrimitive purged : purgedPrimitives) {
            getLayer().data.addPrimitive(purged);
        }
        reconstituteConflicts();
        // will restore the former references to the purged nodes
        //
        super.undoCommand();
    }
}
