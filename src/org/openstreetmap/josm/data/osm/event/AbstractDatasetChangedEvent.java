// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public abstract class AbstractDatasetChangedEvent {

    public enum DatasetEventType {DATA_CHANGED, NODE_MOVED, PRIMITIVES_ADDED, PRIMITIVES_REMOVED,
        RELATION_MEMBERS_CHANGED, TAGS_CHANGED, WAY_NODES_CHANGED, CHANGESET_ID_CHANGED}

    protected final DataSet dataSet;

    protected AbstractDatasetChangedEvent(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public abstract void fire(DataSetListener listener);

    /**
     * Returns list of primitives modified by this event.
     * <br>
     * <strong>WARNING</strong> This value might be incorrect in case
     * of {@link DataChangedEvent}. It returns all primitives in the dataset
     * when this method is called (live list), not list of primitives when
     * the event was created
     * @return List of modified primitives
     */
    public abstract Collection<? extends OsmPrimitive> getPrimitives();

    public DataSet getDataset() {
        return dataSet;
    }

    public abstract DatasetEventType getType();

    @Override
    public String toString() {
        return getType().toString();
    }

}
