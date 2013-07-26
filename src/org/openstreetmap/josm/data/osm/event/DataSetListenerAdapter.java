// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

/**
 * Classes that do not wish to implement all methods of DataSetListener
 * may use this class. Implement DatasetListenerAdapter.Listener and
 * pass this adapter instead of class itself.
 *
 */
public class DataSetListenerAdapter implements DataSetListener {

    public interface Listener {
        void processDatasetEvent(AbstractDatasetChangedEvent event);
    }

    private final Listener listener;

    public DataSetListenerAdapter(Listener listener) {
        this.listener = listener;
    }

    @Override
    public void dataChanged(DataChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void nodeMoved(NodeMovedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void primitivesAdded(PrimitivesAddedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void primitivesRemoved(PrimitivesRemovedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void relationMembersChanged(RelationMembersChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void tagsChanged(TagsChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void wayNodesChanged(WayNodesChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    @Override
    public void otherDatasetChange(AbstractDatasetChangedEvent event) {
        listener.processDatasetEvent(event);
    }

}
