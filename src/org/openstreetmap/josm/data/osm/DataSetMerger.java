package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.conflict.ConflictCollection;

/**
 * A dataset merger which takes a target and a source dataset and merges the source data set
 * onto the target dataset.
 * 
 */
public class DataSetMerger {
    private static Logger logger = Logger.getLogger(DataSetMerger.class.getName());

    /** the collection of conflicts created during merging */
    private ConflictCollection conflicts;

    /** the target dataset for merging */
    private final DataSet targetDataSet;
    /** the source dataset where primitives are merged from */
    private final DataSet sourceDataSet;

    /**
     * A map of all primitives that got replaced with other primitives.
     * Key is the primitive id in their dataset, the value is the id in my dataset
     */
    private Map<Long, Long> mergedMap;
    /** a set of primitive ids for which we have to fix references (to nodes and
     * to relation members) after the first phase of merging
     */
    private Set<Long> childrenToMerge;
    private Set<OsmPrimitive> deletedObjectsToUnlink;

    /**
     * constructor
     *
     * The visitor will merge <code>theirDataSet</code> onto <code>myDataSet</code>
     *
     * @param targetDataSet  dataset with my primitives. Must not be null.
     * @param sourceDataSet dataset with their primitives. Ignored, if null.
     * @throws IllegalArgumentException thrown if myDataSet is null
     */
    public DataSetMerger(DataSet targetDataSet, DataSet sourceDataSet) throws IllegalArgumentException {
        if (targetDataSet == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "targetDataSet"));
        this.targetDataSet = targetDataSet;
        this.sourceDataSet = sourceDataSet;
        conflicts = new ConflictCollection();
        mergedMap = new HashMap<Long, Long>();
        childrenToMerge = new HashSet<Long>();
        deletedObjectsToUnlink = new HashSet<OsmPrimitive>();
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
     * @param source  the other primitive
     */
    protected <P extends OsmPrimitive> void mergePrimitive(P source) {
        if (!source.isNew() ) {
            // try to merge onto a matching primitive with the same
            // defined id
            //
            if (mergeById(source))
                return;
            //if (!source.isVisible())
            // ignore it
            //    return;
        } else {
            // try to merge onto a primitive  which has no id assigned
            // yet but which is equal in its semantic attributes
            //
            Collection<? extends OsmPrimitive> candidates = null;
            switch(source.getType()) {
            case NODE: candidates = targetDataSet.getNodes(); break;
            case WAY: candidates  =targetDataSet.getWays(); break;
            case RELATION: candidates = targetDataSet.getRelations(); break;
            }
            for (OsmPrimitive target : candidates) {
                if (!target.isNew()) {
                    continue;
                }
                if (target.hasEqualSemanticAttributes(source)) {
                    mergedMap.put(source.getUniqueId(), target.getUniqueId());
                    if (target.isDeleted() != source.isDeleted()) {
                        // differences in deleted state have to be merged manually
                        //
                        conflicts.add(target, source);
                    } else {
                        // copy the technical attributes from other
                        // version
                        target.setVisible(source.isVisible());
                        target.setUser(source.getUser());
                        target.setTimestamp(source.getTimestamp());
                        target.setModified(source.isModified());
                        childrenToMerge.add(source.getUniqueId());
                    }
                    return;
                }
            }
        }

        // If we get here we didn't find a suitable primitive in
        // the target dataset. Create a clone and add it to the target dataset.
        //
        OsmPrimitive target = null;
        switch(source.getType()) {
        case NODE: target = source.isNew() ? new Node() : new Node(source.getId()); break;
        case WAY: target = source.isNew() ? new Way() : new Way(source.getId()); break;
        case RELATION: target = source.isNew() ? new Relation() : new Relation(source.getId()); break;
        }
        target.mergeFrom(source);
        targetDataSet.addPrimitive(target);
        mergedMap.put(source.getUniqueId(), target.getUniqueId());
        childrenToMerge.add(source.getUniqueId());
    }

    protected OsmPrimitive getMergeTarget(OsmPrimitive mergeSource) throws IllegalStateException{
        Long targetId = mergedMap.get(mergeSource.getUniqueId());
        if (targetId == null)
            return null;
        return targetDataSet.getPrimitiveById(targetId, mergeSource.getType());
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
        for (Way w : sourceDataSet.getWays()) {
            if (!conflicts.hasConflictForTheir(w) && childrenToMerge.contains(w.getUniqueId())) {
                mergeNodeList(w);
                fixIncomplete(w);
            }
        }
        for (Relation r : sourceDataSet.getRelations()) {
            if (!conflicts.hasConflictForTheir(r) && childrenToMerge.contains(r.getUniqueId())) {
                mergeRelationMembers(r);
            }
        }
        for (OsmPrimitive source: deletedObjectsToUnlink) {
            OsmPrimitive target = getMergeTarget(source);
            if (target == null)
                throw new RuntimeException(tr("Missing merge target for object with id {0}", source.getUniqueId()));
            targetDataSet.unlinkReferencesToPrimitive(target);
        }
    }

    /**
     * Merges the node list of a source way onto its target way.
     * 
     * @param source the source way
     * @throws IllegalStateException thrown if no target way can be found for the source way
     * @throws IllegalStateException thrown if there isn't a target node for one of the nodes in the source way
     * 
     */
    private void mergeNodeList(Way source) throws IllegalStateException {
        Way target = (Way)getMergeTarget(source);
        if (target == null)
            throw new IllegalStateException(tr("Missing merge target for way with id {0}", source.getUniqueId()));

        List<Node> newNodes = new LinkedList<Node>();
        for (Node sourceNode : source.getNodes()) {
            Node targetNode = (Node)getMergeTarget(sourceNode);
            if (targetNode != null) {
                if (!targetNode.isDeleted() && targetNode.isVisible()) {
                    newNodes.add(targetNode);
                } else {
                    target.setModified(true);
                }
            } else
                throw new IllegalStateException(tr("Missing merge target for node with id {0}", sourceNode.getUniqueId()));
        }
        target.setNodes(newNodes);
    }


    /**
     * Merges the relation members of a source relation onto the corresponding target relation.
     * @param source the source relation
     * @throws IllegalStateException thrown if there is no corresponding target relation
     * @throws IllegalStateException thrown if there isn't a corresponding target object for one of the relation
     * members in source
     */
    private void mergeRelationMembers(Relation source) throws IllegalStateException {
        Relation target = (Relation) getMergeTarget(source);
        if (target == null)
            throw new IllegalStateException(tr("Missing merge target for relation with id {0}", source.getUniqueId()));
        LinkedList<RelationMember> newMembers = new LinkedList<RelationMember>();
        for (RelationMember sourceMember : source.getMembers()) {
            OsmPrimitive targetMember = getMergeTarget(sourceMember.getMember());
            if (targetMember == null)
                throw new IllegalStateException(tr("Missing merge target of type {0} with id {1}", targetMember.getType(), targetMember.getUniqueId()));
            if (! targetMember.isDeleted() && targetMember.isVisible()) {
                RelationMember newMember = new RelationMember(sourceMember.getRole(), targetMember);
                newMembers.add(newMember);
            } else {
                target.setModified(true);
            }
        }
        target.setMembers(newMembers);
    }

    /**
     * Tries to merge a primitive <code>source</code> into an existing primitive with the same id.
     *
     * @param source  the source primitive which is to be merged into a target primitive
     * @return true, if this method was able to merge <code>source</code> into a target object; false, otherwise
     */
    private boolean mergeById(OsmPrimitive source) {
        OsmPrimitive target = targetDataSet.getPrimitiveById(source.getId(), source.getType());
        // merge other into an existing primitive with the same id, if possible
        //
        if (target == null)
            return false;
        // found a corresponding target, remember it
        mergedMap.put(source.getUniqueId(), target.getUniqueId());

        if (target.getVersion() > source.getVersion())
            // target.version > source.version => keep target version
            return true;
        if (! target.isVisible() && source.isVisible()) {
            // should not happen
            //
            logger.warning(tr("Target object with id {0} and version {1} is visible although "
                    + "source object with lower version {2} is not visible. "
                    + "Can''t deal with this inconsistency. Keeping target object. ",
                    Long.toString(target.getId()),Long.toString(target.getVersion()), Long.toString(source.getVersion())
            ));
        } else if (target.isVisible() && ! source.isVisible()) {
            // this is always a conflict because the user has to decide whether
            // he wants to create a clone of its target primitive or whether he
            // wants to purge the target from the local dataset. He can't keep it unchanged
            // because it was deleted on the server.
            //
            conflicts.add(target,source);
        } else if (target.incomplete && !source.incomplete) {
            // target is incomplete, source completes it
            // => merge source into target
            //
            target.mergeFrom(source);
            childrenToMerge.add(source.getUniqueId());
        } else if (!target.incomplete && source.incomplete) {
            // target is complete and source is incomplete
            // => keep target, it has more information already
            //
        } else if (target.incomplete && source.incomplete) {
            // target and source are incomplete. Doesn't matter which one to
            // take. We take target.
            //
        } else if (target.isDeleted() && ! source.isDeleted() && target.getVersion() == source.getVersion()) {
            // same version, but target is deleted. Assume target takes precedence
            // otherwise too many conflicts when refreshing from the server
        } else if (target.isDeleted() != source.isDeleted()) {
            // differences in deleted state have to be resolved manually
            //
            conflicts.add(target,source);
        } else if (! target.isModified() && source.isModified()) {
            // target not modified. We can assume that source is the most recent version.
            // clone it into target. But check first, whether source is deleted. if so,
            // make sure that target is not referenced anymore in myDataSet.
            //
            if (source.isDeleted()) {
                deletedObjectsToUnlink.add(source);
            }
            target.mergeFrom(source);
            childrenToMerge.add(source.getUniqueId());
        } else if (! target.isModified() && !source.isModified() && target.getVersion() == source.getVersion()) {
            // both not modified. Keep mine
            //
        } else if (! target.isModified() && !source.isModified() && target.getVersion() < source.getVersion()) {
            // my not modified but other is newer. clone other onto mine.
            //
            target.mergeFrom(source);
            childrenToMerge.add(source.getUniqueId());
        } else if (target.isModified() && ! source.isModified() && target.getVersion() == source.getVersion()) {
            // target is same as source but target is modified
            // => keep target
        } else if (! target.hasEqualSemanticAttributes(source)) {
            // target is modified and is not semantically equal with source. Can't automatically
            // resolve the differences
            // =>  create a conflict
            conflicts.add(target,source);
        } else {
            // clone from other, but keep the modified flag. mergeFrom will mainly copy
            // technical attributes like timestamp or user information. Semantic
            // attributes should already be equal if we get here.
            //
            target.mergeFrom(source);
            target.setModified(true);
            childrenToMerge.add(source.getUniqueId());
        }
        return true;
    }

    /**
     * Runs the merge operation. Successfully merged {@see OsmPrimitive}s are in
     * {@see #getMyDataSet()}.
     *
     * See {@see #getConflicts()} for a map of conflicts after the merge operation.
     */
    public void merge() {
        if (sourceDataSet == null)
            return;
        for (Node node: sourceDataSet.getNodes()) {
            mergePrimitive(node);
        }
        for (Way way: sourceDataSet.getWays()) {
            mergePrimitive(way);
        }
        for (Relation relation: sourceDataSet.getRelations()) {
            mergePrimitive(relation);
        }
        fixReferences();
    }

    /**
     * replies my dataset
     *
     * @return
     */
    public DataSet getTargetDataSet() {
        return targetDataSet;
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
