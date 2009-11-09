package org.openstreetmap.josm.data.osm.visitor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.conflict.ConflictCollection;
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
 */
public class MergeVisitor extends AbstractVisitor {
    private static Logger logger = Logger.getLogger(MergeVisitor.class.getName());

    /** the collection of conflicts created during merging */
    private ConflictCollection conflicts;

    /** the target dataset for merging */
    private final DataSet myDataSet;
    /** the source dataset where primitives are merged from */
    private final DataSet theirDataSet;

    /**
     * A map of all primitives that got replaced with other primitives.
     * Key is the primitive id in their dataset, the value is the id in my dataset
     */
    private Map<Long, Long> merged;

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
        conflicts = new ConflictCollection();
        merged = new HashMap<Long, Long>();
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
     */
    protected <P extends OsmPrimitive> void mergePrimitive(P other) {
        if (!other.isNew() ) {
            // try to merge onto a matching primitive with the same
            // defined id
            //
            if (mergeById(other))
                return;
        } else {
            // try to merge onto a primitive  which has no id assigned
            // yet but which is equal in its semantic attributes
            //
            Collection<? extends OsmPrimitive> candidates = null;
            switch(other.getType()) {
            case NODE: candidates = myDataSet.getNodes(); break;
            case WAY: candidates  =myDataSet.getWays(); break;
            case RELATION: candidates = myDataSet.getRelations(); break;
            }
            for (OsmPrimitive my : candidates) {
                if (!my.isNew()) {
                    continue;
                }
                if (my.hasEqualSemanticAttributes(other)) {
                    if (my.isDeleted() != other.isDeleted()) {
                        // differences in deleted state have to be merged manually
                        //
                        conflicts.add(my, other);
                        merged.put(other.getUniqueId(), my.getUniqueId());
                    } else {
                        // copy the technical attributes from other
                        // version
                        my.setVisible(other.isVisible());
                        my.setUser(other.getUser());
                        my.setTimestamp(other.getTimestamp());
                        my.setModified(other.isModified());
                        merged.put(other.getUniqueId(), my.getUniqueId());
                    }
                    return;
                }
            }
        }
        // If we get here we didn't find a suitable primitive in
        // my dataset. Create a clone and add it to my dataset.
        //
        OsmPrimitive my = null;
        switch(other.getType()) {
        case NODE: my = other.isNew() ? new Node() : new Node(other.getId()); break;
        case WAY: my = other.isNew() ? new Way() : new Way(other.getId()); break;
        case RELATION: my = other.isNew() ? new Relation() : new Relation(other.getId()); break;
        }
        my.mergeFrom(other);
        myDataSet.addPrimitive(my);
        merged.put(other.getUniqueId(), my.getUniqueId());
    }

    public void visit(Node other) {
        mergePrimitive(other);
    }

    public void visit(Way other) {
        mergePrimitive(other);
    }

    public void visit(Relation other) {
        mergePrimitive(other);
    }

    protected OsmPrimitive getMergeTarget(OsmPrimitive mergeSource) {
        Long targetId = merged.get(mergeSource.getUniqueId());
        if (targetId == null)
            throw new RuntimeException("no merge target for merge primitive " + mergeSource.getUniqueId() + " of type " + mergeSource.getType());
        return myDataSet.getPrimitiveById(targetId, mergeSource.getType());
    }

    protected void fixIncomplete(Way other) {
        Way myWay = (Way)getMergeTarget(other);
        if (myWay == null)
            throw new RuntimeException(tr("Missing merge target for way with id {0}", other.getUniqueId()));
        if (!myWay.incomplete)return;
        if (myWay.incomplete && other.getNodesCount() == 0) return;
        for (Node n: myWay.getNodes()) {
            if (n.incomplete) return;
        }
        myWay.incomplete = false;
    }

    /**
     * Postprocess the dataset and fix all merged references to point to the actual
     * data.
     */
    public void fixReferences() {
        for (Way w : theirDataSet.getWays()) {
            if (!conflicts.hasConflictForTheir(w)) {
                mergeNodeList(w);
                fixIncomplete(w);
            }
        }
        for (Relation r : theirDataSet.getRelations()) {
            if (!conflicts.hasConflictForTheir(r)) {
                mergeRelationMembers(r);
            }
        }
    }

    private void mergeNodeList(Way other) {
        Way myWay = (Way)getMergeTarget(other);
        if (myWay == null)
            throw new RuntimeException(tr("Missing merge target for way with id {0}", other.getUniqueId()));

        List<Node> myNodes = new LinkedList<Node>();
        for (Node otherNode : other.getNodes()) {
            Node myNode = (Node)getMergeTarget(otherNode);
            if (myNode != null) {
                if (!myNode.isDeleted()) {
                    myNodes.add(myNode);
                }
            } else
                throw new RuntimeException(tr("Missing merge target for node with id {0}", otherNode.getUniqueId()));
        }

        // check whether the node list has changed. If so, set the modified flag on the way
        //
        if (myWay.getNodes().size() != myNodes.size()) {
            myWay.setModified(true);
        } else {
            for (int i=0; i< myWay.getNodesCount();i++) {
                Node n1 = myWay.getNode(i);
                Node n2 = myNodes.get(i);
                if (n1.isNew() ^ n2.isNew()) {
                    myWay.setModified(true);
                    break;
                } else if (n1.isNew() && n1 != n2) {
                    myWay.setModified(true);
                    break;
                } else if (! n1.isNew() && n1.getId() != n2.getId()) {
                    myWay.setModified(true);
                    break;
                }
            }
        }
        myWay.setNodes(myNodes);
    }

    private void mergeRelationMembers(Relation other) {
        Relation myRelation = (Relation) getMergeTarget(other);
        if (myRelation == null)
            throw new RuntimeException(tr("Missing merge target for relation with id {0}", other.getUniqueId()));
        LinkedList<RelationMember> newMembers = new LinkedList<RelationMember>();
        for (RelationMember otherMember : other.getMembers()) {
            OsmPrimitive mergedMember = getMergeTarget(otherMember.getMember());
            if (mergedMember == null)
                throw new RuntimeException(tr("Missing merge target of type {0} with id {1}", mergedMember.getType(), mergedMember.getUniqueId()));
            if (! mergedMember.isDeleted()) {
                RelationMember newMember = new RelationMember(otherMember.getRole(), mergedMember);
                newMembers.add(newMember);
            }
        }

        // check whether the list of relation members has changed
        //
        if (other.getMembersCount() != newMembers.size()) {
            myRelation.setModified(true);
        } else {
            for (int i=0; i<other.getMembersCount();i++) {
                RelationMember rm1 = other.getMember(i);
                RelationMember rm2 = newMembers.get(i);
                if (!rm1.getRole().equals(rm2.getRole())) {
                    myRelation.setModified(true);
                    break;
                } else if (rm1.getMember().isNew() ^ rm2.getMember().isNew()) {
                    myRelation.setModified(true);
                    break;
                } else if (rm1.getMember().isNew() && rm1.getMember() != rm2.getMember()) {
                    myRelation.setModified(true);
                    break;
                } else if (! rm1.getMember().isNew() && rm1.getMember().getId() != rm2.getMember().getId()) {
                    myRelation.setModified(true);
                    break;
                }
            }
        }
        myRelation.setMembers(newMembers);
    }

    /**
     * Tries to merge a primitive <code>other</code> into an existing primitive with the same id.
     *
     * @param other  the other primitive which is to be merged onto a primitive in my primitives
     * @return true, if this method was able to merge <code>other</code> with an existing node; false, otherwise
     */
    private <P extends OsmPrimitive> boolean mergeById(P other) {
        OsmPrimitive my = myDataSet.getPrimitiveById(other.getId(), other.getType());
        // merge other into an existing primitive with the same id, if possible
        //
        if (my != null) {
            if (my.getVersion() <= other.getVersion()) {
                if (! my.isVisible() && other.isVisible()) {
                    // should not happen
                    //
                    logger.warning(tr("My primitive with id {0} and version {1} is visible although "
                            + "their primitive with lower version {2} is not visible. "
                            + "Can't deal with this inconsistency. Keeping my primitive. ",
                            Long.toString(my.getId()),Long.toString(my.getVersion()), Long.toString(other.getVersion())
                    ));
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (my.isVisible() && ! other.isVisible()) {
                    // this is always a conflict because the user has to decide whether
                    // he wants to create a clone of its local primitive or whether he
                    // wants to purge my from the local dataset. He can't keep it unchanged
                    // because it was deleted on the server.
                    //
                    conflicts.add(my,other);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (my.incomplete && !other.incomplete) {
                    // my is incomplete, other completes it
                    // => merge other onto my
                    //
                    my.mergeFrom(other);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (!my.incomplete && other.incomplete) {
                    // my is complete and the other is incomplete
                    // => keep mine, we have more information already
                    //
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (my.incomplete && other.incomplete) {
                    // my and other are incomplete. Doesn't matter which one to
                    // take. We take mine.
                    //
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (my.isDeleted() && ! other.isDeleted() && my.getVersion() == other.getVersion()) {
                    // same version, but my is deleted. Assume mine takes precedence
                    // otherwise too many conflicts when refreshing from the server
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (my.isDeleted() != other.isDeleted()) {
                    // differences in deleted state have to be resolved manually
                    //
                    conflicts.add(my,other);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (! my.isModified() && other.isModified()) {
                    // my not modified. We can assume that other is the most recent version.
                    // clone it onto my. But check first, whether other is deleted. if so,
                    // make sure that my is not references anymore in myDataSet.
                    //
                    if (other.isDeleted()) {
                        myDataSet.unlinkReferencesToPrimitive(my);
                    }
                    my.mergeFrom(other);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (! my.isModified() && !other.isModified() && my.getVersion() == other.getVersion()) {
                    // both not modified. Keep mine
                    //
                    merged.put(other.getUniqueId(),my.getUniqueId());
                } else if (! my.isModified() && !other.isModified() && my.getVersion() < other.getVersion()) {
                    // my not modified but other is newer. clone other onto mine.
                    //
                    my.mergeFrom(other);
                    merged.put(other.getUniqueId(),my.getUniqueId());
                } else if (my.isModified() && ! other.isModified() && my.getVersion() == other.getVersion()) {
                    // my is same as other but mine is modified
                    // => keep mine
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else if (! my.hasEqualSemanticAttributes(other)) {
                    // my is modified and is not semantically equal with other. Can't automatically
                    // resolve the differences
                    // =>  create a conflict
                    conflicts.add(my,other);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                } else {
                    // clone from other, but keep the modified flag. Clone will mainly copy
                    // technical attributes like timestamp or user information. Semantic
                    // attributes should already be equal if we get here.
                    //
                    my.mergeFrom(other);
                    my.setModified(true);
                    merged.put(other.getUniqueId(), my.getUniqueId());
                }
            } else {
                // my.version > other.version => keep my version
                merged.put(other.getUniqueId(), my.getUniqueId());
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
        for (Node node: theirDataSet.getNodes()) {
            node.visit(this);
        }
        for (Way way: theirDataSet.getWays()) {
            way.visit(this);
        }
        for (Relation relation: theirDataSet.getRelations()) {
            relation.visit(this);
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
    public ConflictCollection getConflicts() {
        return conflicts;
    }
}
