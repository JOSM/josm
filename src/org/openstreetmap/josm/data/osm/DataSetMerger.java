package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.openstreetmap.josm.data.conflict.Conflict;
import org.openstreetmap.josm.data.conflict.ConflictCollection;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * A dataset merger which takes a target and a source dataset and merges the source data set
 * onto the target dataset.
 *
 */
public class DataSetMerger {
    private static Logger logger = Logger.getLogger(DataSetMerger.class.getName());

    /** the collection of conflicts created during merging */
    private final ConflictCollection conflicts;

    /** the target dataset for merging */
    private final DataSet targetDataSet;
    /** the source dataset where primitives are merged from */
    private final DataSet sourceDataSet;

    /**
     * A map of all primitives that got replaced with other primitives.
     * Key is the PrimitiveId in their dataset, the value is the PrimitiveId in my dataset
     */
    private final Map<PrimitiveId, PrimitiveId> mergedMap;
    /** a set of primitive ids for which we have to fix references (to nodes and
     * to relation members) after the first phase of merging
     */
    private final Set<PrimitiveId> objectsWithChildrenToMerge;
    private final Set<OsmPrimitive> deletedObjectsToUnlink;

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
        CheckParameterUtil.ensureParameterNotNull(targetDataSet, "targetDataSet");
        this.targetDataSet = targetDataSet;
        this.sourceDataSet = sourceDataSet;
        conflicts = new ConflictCollection();
        mergedMap = new HashMap<PrimitiveId, PrimitiveId>();
        objectsWithChildrenToMerge = new HashSet<PrimitiveId>();
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
    protected void mergePrimitive(OsmPrimitive source) {
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
            // ignore deleted primitives from source
            if (source.isDeleted()) return;

            // try to merge onto a primitive  which has no id assigned
            // yet but which is equal in its semantic attributes
            //
            Collection<? extends OsmPrimitive> candidates = null;
            switch(source.getType()) {
            case NODE: candidates = targetDataSet.getNodes(); break;
            case WAY: candidates  =targetDataSet.getWays(); break;
            case RELATION: candidates = targetDataSet.getRelations(); break;
            default: throw new AssertionError();
            }
            for (OsmPrimitive target : candidates) {
                if (!target.isNew() || target.isDeleted()) {
                    continue;
                }
                if (target.hasEqualSemanticAttributes(source)) {
                    mergedMap.put(source.getPrimitiveId(), target.getPrimitiveId());
                    // copy the technical attributes from other
                    // version
                    target.setVisible(source.isVisible());
                    target.setUser(source.getUser());
                    target.setTimestamp(source.getTimestamp());
                    target.setModified(source.isModified());
                    objectsWithChildrenToMerge.add(source.getPrimitiveId());
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
        default: throw new AssertionError();
        }
        target.mergeFrom(source);
        targetDataSet.addPrimitive(target);
        mergedMap.put(source.getPrimitiveId(), target.getPrimitiveId());
        objectsWithChildrenToMerge.add(source.getPrimitiveId());
    }

    protected OsmPrimitive getMergeTarget(OsmPrimitive mergeSource) throws IllegalStateException{
        PrimitiveId targetId = mergedMap.get(mergeSource.getPrimitiveId());
        if (targetId == null)
            return null;
        return targetDataSet.getPrimitiveById(targetId);
    }

    protected void fixIncomplete(Way other) {
        Way myWay = (Way)getMergeTarget(other);
        if (myWay == null)
            throw new RuntimeException(tr("Missing merge target for way with id {0}", other.getUniqueId()));
    }

    /**
     * Postprocess the dataset and fix all merged references to point to the actual
     * data.
     */
    public void fixReferences() {
        for (Way w : sourceDataSet.getWays()) {
            if (!conflicts.hasConflictForTheir(w) && objectsWithChildrenToMerge.contains(w.getPrimitiveId())) {
                mergeNodeList(w);
                fixIncomplete(w);
            }
        }
        for (Relation r : sourceDataSet.getRelations()) {
            if (!conflicts.hasConflictForTheir(r) && objectsWithChildrenToMerge.contains(r.getPrimitiveId())) {
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

        List<Node> newNodes = new ArrayList<Node>(source.getNodesCount());
        for (Node sourceNode : source.getNodes()) {
            Node targetNode = (Node)getMergeTarget(sourceNode);
            if (targetNode != null) {
                if (targetNode.isVisible()) {
                    newNodes.add(targetNode);
                    if (targetNode.isDeleted() && !conflicts.hasConflictForMy(targetNode)) {
                        conflicts.add(new Conflict<OsmPrimitive>(targetNode, sourceNode, true));
                        targetNode.setDeleted(false);
                    }
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
                throw new IllegalStateException(tr("Missing merge target of type {0} with id {1}", sourceMember.getType(), sourceMember.getUniqueId()));
            if (targetMember.isVisible()) {
                RelationMember newMember = new RelationMember(sourceMember.getRole(), targetMember);
                newMembers.add(newMember);
                if (targetMember.isDeleted() && !conflicts.hasConflictForMy(targetMember)) {
                    conflicts.add(new Conflict<OsmPrimitive>(targetMember, sourceMember.getMember(), true));
                    targetMember.setDeleted(false);
                }
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
        mergedMap.put(source.getPrimitiveId(), target.getPrimitiveId());

        if (target.getVersion() > source.getVersion())
            // target.version > source.version => keep target version
            return true;
        if (! target.isVisible() && source.isVisible()) {
            // should not happen
            // FIXME: this message does not make sense, source version can not be lower than
            //        target version at this point
            logger.warning(tr("Target object with id {0} and version {1} is visible although "
                    + "source object with lower version {2} is not visible. "
                    + "Cannot deal with this inconsistency. Keeping target object. ",
                    Long.toString(target.getId()),Long.toString(target.getVersion()), Long.toString(source.getVersion())
            ));
        } else if (target.isVisible() && ! source.isVisible()) {
            // this is always a conflict because the user has to decide whether
            // he wants to create a clone of its target primitive or whether he
            // wants to purge the target from the local dataset. He can't keep it unchanged
            // because it was deleted on the server.
            //
            conflicts.add(target,source);
        } else if (target.isIncomplete() && !source.isIncomplete()) {
            // target is incomplete, source completes it
            // => merge source into target
            //
            target.mergeFrom(source);
            objectsWithChildrenToMerge.add(source.getPrimitiveId());
        } else if (!target.isIncomplete() && source.isIncomplete()) {
            // target is complete and source is incomplete
            // => keep target, it has more information already
            //
        } else if (target.isIncomplete() && source.isIncomplete()) {
            // target and source are incomplete. Doesn't matter which one to
            // take. We take target.
            //
        } else if (target.isDeleted() && ! source.isDeleted() && target.getVersion() == source.getVersion()) {
            // same version, but target is deleted. Assume target takes precedence
            // otherwise too many conflicts when refreshing from the server
            // but, if source has a referrer that is not in the target dataset there is a conflict
            // If target dataset refers to the deleted primitive, conflict will be added in fixReferences method
            for (OsmPrimitive referrer: source.getReferrers()) {
                if (targetDataSet.getPrimitiveById(referrer.getPrimitiveId()) == null) {
                    conflicts.add(new Conflict<OsmPrimitive>(target, source, true));
                    target.setDeleted(false);
                    break;
                }
            }
        } else if (target.isDeleted() != source.isDeleted()) {
            // differences in deleted state have to be resolved manually. This can
            // happen if one layer is merged onto another layer
            //
            conflicts.add(target,source);
        } else if (! target.isModified() && source.isModified()) {
            // target not modified. We can assume that source is the most recent version.
            // clone it into target. But check first, whether source is deleted. if so,
            // make sure that target is not referenced any more in myDataSet. If it is there
            // is a conflict
            if (source.isDeleted()) {
                if (!target.getReferrers().isEmpty()) {
                    conflicts.add(target, source);
                }
            } else {
                target.mergeFrom(source);
                objectsWithChildrenToMerge.add(source.getPrimitiveId());
            }
        } else if (! target.isModified() && !source.isModified() && target.getVersion() == source.getVersion()) {
            // both not modified. Merge nevertheless.
            // This helps when updating "empty" relations, see #4295
            target.mergeFrom(source);
            objectsWithChildrenToMerge.add(source.getPrimitiveId());
        } else if (! target.isModified() && !source.isModified() && target.getVersion() < source.getVersion()) {
            // my not modified but other is newer. clone other onto mine.
            //
            target.mergeFrom(source);
            objectsWithChildrenToMerge.add(source.getPrimitiveId());
        } else if (target.isModified() && ! source.isModified() && target.getVersion() == source.getVersion()) {
            // target is same as source but target is modified
            // => keep target and reset modified flag if target and source are semantically equal
            if (target.hasEqualSemanticAttributes(source)) {
                target.setModified(false);
            }
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
            objectsWithChildrenToMerge.add(source.getPrimitiveId());
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
        targetDataSet.beginUpdate();
        try {
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
        } finally {
            targetDataSet.endUpdate();
        }
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
