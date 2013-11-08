// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

/**
 * A listener listening for all DataSet changes.
 *
 * @see DataSetListenerAdapter
 * @author nenik
 */
public interface DataSetListener {
    /**
     * A bunch of primitives were added into the DataSet, or existing
     * deleted/invisible primitives were resurrected.
     *
     * @param event An event for an collection of newly-visible primitives
     */
    void primitivesAdded(PrimitivesAddedEvent event);

    /**
     * A bunch of primitives were removed from the DataSet, or preexisting
     * primitives were marked as deleted.
     *
     * @param event An event for an collection of newly-invisible primitives
     */
    void primitivesRemoved(PrimitivesRemovedEvent event);

    /**
     * There was some change in the tag set of a primitive. It can have been
     * a tag addition, tag removal or change in tag value.
     *
     * @param event the event for the primitive, whose tags were affected.
     */
    void tagsChanged(TagsChangedEvent event);

    /**
     * A node's coordinates were modified.
     * @param event The event for the node that was moved.
     */
    void nodeMoved(NodeMovedEvent event);

    /**
     * A way's node list was changed.
     * @param event The event for the way that was modified.
     */
    void wayNodesChanged(WayNodesChangedEvent event);

    /**
     * A relation's members have changed.
     * @param event The event for the relation that was modified.
     */
    void relationMembersChanged(RelationMembersChangedEvent event);

    /**
     * Minor dataset change, currently only changeset id changed is supported, but can
     * be extended in future.
     * @param event the event for data modification
     */
    void otherDatasetChange(AbstractDatasetChangedEvent event);

    /**
     * Called after big changes in dataset. Usually other events are stopped using Dataset.beginUpdate() and
     * after operation is completed (Dataset.endUpdate()), {@link #dataChanged(DataChangedEvent event)} is called.
     */
    void dataChanged(DataChangedEvent event);
}
