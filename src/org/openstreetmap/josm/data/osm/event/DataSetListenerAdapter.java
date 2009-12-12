// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

public class DataSetListenerAdapter implements DataSetListener {

    public interface Listener {
        void processDatasetEvent(AbstractDatasetChangedEvent event);
    }

    private final Listener listener;

    public DataSetListenerAdapter(Listener listener) {
        this.listener = listener;
    }

    public void dataChanged(DataChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void nodeMoved(NodeMovedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void primtivesAdded(PrimitivesAddedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void primtivesRemoved(PrimitivesRemovedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void relationMembersChanged(RelationMembersChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void tagsChanged(TagsChangedEvent event) {
        listener.processDatasetEvent(event);
    }

    public void wayNodesChanged(WayNodesChangedEvent event) {
        listener.processDatasetEvent(event);
    }

}
