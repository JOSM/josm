// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A visitor that gets a data set at construction time and merges every visited object
 * into it.
 *
 * @author imi
 * @author Gubaer
 */
public class MergeVisitor extends AbstractVisitor {
    private static Logger logger = Logger.getLogger(MergeVisitor.class.getName());

    /**
     * Map from primitives in the database to visited primitives. (Attention: The other way
     * round than merged)
     */
    private Map<OsmPrimitive, OsmPrimitive> conflicts;

    private final DataSet myDataSet;
    private final DataSet theirDataSet;

    private final HashMap<Long, Node> nodeshash = new HashMap<Long, Node>();
    private final HashMap<Long, Way> wayshash = new HashMap<Long, Way>();
    private final HashMap<Long, Relation> relshash = new HashMap<Long, Relation>();

    /**
     * A list of all primitives that got replaced with other primitives.
     * Key is the primitives in the other's dataset and the value is the one that is now
     * in ds.nodes instead.
     */
    private Map<OsmPrimitive, OsmPrimitive> merged;

    /**
     * constructor
     * 
     * The visitor will merge <code>theirDataSet</code> onto <code>myDataSet</code>
     * 
     * @param myDataSet  dataset with my primitives
     * @param theirDataSet dataset with their primitives.
     */
    public MergeVisitor(DataSet myDataSet, DataSet theirDataSet) {
        this.myDataSet = myDataSet;
        this.theirDataSet = theirDataSet;

        for (Node n : myDataSet.nodes) if (n.id != 0) {
            nodeshash.put(n.id, n);
        }
        for (Way w : myDataSet.ways) if (w.id != 0) {
            wayshash.put(w.id, w);
        }
        for (Relation r : myDataSet.relations) if (r.id != 0) {
            relshash.put(r.id, r);
        }
        conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();
        merged = new HashMap<OsmPrimitive, OsmPrimitive>();
    }

    /**
     * Merges a primitive <code>other</code> of type <P> onto my primitives.
     * 
     * If other.id != 0 it tries to merge it with an corresponding primitive from
     * my dataset with the same id. If this is not possible a conflict is remembered
     * in {@see #conflicts}.
     * 
     * If other.id == 0 it tries to find a primitive in my dataset with id == 0 which
     * is semantically equal. If it finds one it merges its technical attributes onto
     * my primitive.
     * 
     * @param <P>  the type of the other primitive
     * @param other  the other primitive
     * @param myPrimitives the collection of my relevant primitives (i.e. only my
     *    primitives of the same type)
     * @param otherPrimitives  the collection of the other primitives
     * @param primitivesWithDefinedIds the collection of my primitives with an
     *   assigned id (i.e. id != 0)
     */
    protected <P extends OsmPrimitive> void mergePrimitive(P other,
            Collection<P> myPrimitives, Collection<P> otherPrimitives,
            HashMap<Long, P> primitivesWithDefinedIds) {

        if (other.id > 0 ) {
            // try to merge onto a matching primitive with the same
            // defined id
            //
            if (mergeById(myPrimitives, primitivesWithDefinedIds, other))
                return;
        } else {
            // try to merge onto a primitive with which has no id assigned
            // yet but which is equal in its semantic attributes
            //
            for (P my : myPrimitives) {
                if (my.id >0 ) {
                    continue;
                }
                if (my.hasEqualSemanticAttributes(other)) {
                    // copy the technical attributes from their
                    // version
                    if (other.deleted) {
                        myDataSet.unlinkReferencesToPrimitive(my);
                        my.delete(true);
                    }
                    my.visible = other.visible;
                    my.user = other.user;
                    my.setTimestamp(other.getTimestamp());
                    my.modified = other.modified;
                    merged.put(other, my);
                    return;
                }
            }
        }
        // If we get here we didn't find a suitable primitive in
        // my dataset. Just add other to my dataset.
        //
        myPrimitives.add(other);
    }

    public void visit(Node other) {
        mergePrimitive(other, myDataSet.nodes, theirDataSet.nodes, nodeshash);
    }

    public void visit(Way other) {
        fixWay(other);
        mergePrimitive(other, myDataSet.ways, theirDataSet.ways, wayshash);
    }

    public void visit(Relation other) {
        fixRelation(other);
        mergePrimitive(other, myDataSet.relations, theirDataSet.relations, relshash);
    }

    /**
     * Postprocess the dataset and fix all merged references to point to the actual
     * data.
     */
    public void fixReferences() {
        for (Way w : myDataSet.ways) {
            fixWay(w);
        }
        for (Relation r : myDataSet.relations) {
            fixRelation(r);
        }
        for (OsmPrimitive osm : conflicts.values())
            if (osm instanceof Way) {
                fixWay((Way)osm);
            } else if (osm instanceof Relation) {
                fixRelation((Relation) osm);
            }
    }

    private void fixWay(Way w) {
        boolean replacedSomething = false;
        LinkedList<Node> newNodes = new LinkedList<Node>();
        for (Node n : w.nodes) {
            Node otherN = (Node) merged.get(n);
            newNodes.add(otherN == null ? n : otherN);
            if (otherN != null) {
                replacedSomething = true;
            }
        }
        if (replacedSomething) {
            w.nodes.clear();
            w.nodes.addAll(newNodes);
        }
    }

    private void fixRelation(Relation r) {
        boolean replacedSomething = false;
        LinkedList<RelationMember> newMembers = new LinkedList<RelationMember>();
        for (RelationMember m : r.members) {
            OsmPrimitive otherP = merged.get(m.member);
            if (otherP == null) {
                newMembers.add(m);
            } else {
                RelationMember mnew = new RelationMember(m);
                mnew.member = otherP;
                newMembers.add(mnew);
                replacedSomething = true;
            }
        }
        if (replacedSomething) {
            r.members.clear();
            r.members.addAll(newMembers);
        }
    }

    /**
     * Tries to merge a primitive <code>other</code> into an existing primitive with the same id.
     *
     * @param myPrimitives the complete set of my primitives (potential merge targets)
     * @param myPrimitivesWithDefinedIds the map of primitives (potential merge targets) with an id <> 0, for faster lookup
     *    by id. Key is the id, value the primitive with the given value. myPrimitives.valueSet() is a
     *    subset of primitives.
     * @param other  the other primitive which is to be merged onto a primitive in my primitives
     * @return true, if this method was able to merge <code>other</code> with an existing node; false, otherwise
     */
    private <P extends OsmPrimitive> boolean mergeById(
            Collection<P> myPrimitives, HashMap<Long, P> myPrimitivesWithDefinedIds, P other) {

        // merge other into an existing primitive with the same id, if possible
        //
        if (myPrimitivesWithDefinedIds.containsKey(other.id)) {
            P my = myPrimitivesWithDefinedIds.get(other.id);
            if (my.version <= other.version) {
                if (! my.visible && other.visible) {
                    // should not happen
                    //
                    logger.warning(tr("My primitive with id {0} and version {1} is visible although "
                            + "their primitive with lower version {2} is not visible. "
                            + "Can't deal with this inconsistency. Keeping my primitive. ",
                            Long.toString(my.id),Long.toString(my.version), Long.toString(other.version)
                    ));
                    merged.put(other, my);
                } else if (my.visible && ! other.visible) {
                    // this is always a conflict because the user has to decide whether
                    // he wants to create a clone of its local primitive or whether he
                    // wants to purge my from the local dataset. He can't keep it unchanged
                    // because it was deleted on the server.
                    //
                    conflicts.put(my,other);
                } else if (! my.modified && other.modified) {
                    // my not modified. We can assume that other is the most recent version.
                    // clone it onto my. But check first, whether other is deleted. if so,
                    // make sure that my is not references anymore in myDataSet.
                    //
                    if (other.deleted) {
                        myDataSet.unlinkReferencesToPrimitive(my);
                    }
                    my.cloneFrom(other);
                    merged.put(other, my);
                } else if (! my.modified && !other.modified) {
                    // nothing to merge
                    //
                    merged.put(other,my);
                } else if (my.deleted != other.deleted) {
                    // if we get here my is modified. Differences in deleted state
                    // have to be resolved manually
                    //
                    conflicts.put(my,other);
                } else if (! my.hasEqualSemanticAttributes(other)) {
                    // my is modified and is not semantically equal with other. Can't automatically
                    // resolve the differences
                    // =>  create a conflict
                    conflicts.put(my,other);
                } else {
                    // clone from other, but keep the modified flag. Clone will mainly copy
                    // technical attributes like timestamp or user information. Semantic
                    // attributes should already be equal if we get here.
                    //
                    my.cloneFrom(other);
                    my.modified = true;
                    merged.put(other, my);
                }
            } else {
                // my.version > other.version => keep my version
                merged.put(other, my);
            }
            return true;
        }
        return false;
    }


    /**
     * Runs the merge operation. Successfully merged {@see OsmPrimitive}s are in
     * {@see #getMyDataSet()}.
     * 
     * See {@see #getConflicts()} for a map of conflicts after the merge operation.
     */
    public void merge() {
        for (final OsmPrimitive primitive : theirDataSet.allPrimitives()) {
            primitive.visit(this);
        }
        fixReferences();
    }

    /**
     * replies my dataset
     * 
     * @return
     */
    public DataSet getMyDataSet() {
        return myDataSet;
    }


    /**
     * replies the map of conflicts
     * 
     * @return the map of conflicts
     */
    public Map<OsmPrimitive, OsmPrimitive> getConflicts() {
        return conflicts;
    }
}
