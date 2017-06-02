// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * A combined data change event. It consists of multiple dataset events.
 */
public class DataChangedEvent extends AbstractDatasetChangedEvent {

    private final List<AbstractDatasetChangedEvent> events;

    /**
     * Constructs a new {@code DataChangedEvent}
     * @param dataSet the dataset from which the event comes from
     * @param events list of change events
     */
    public DataChangedEvent(DataSet dataSet, List<AbstractDatasetChangedEvent> events) {
        super(dataSet);
        this.events = events;
    }

    /**
     * Constructs a new {@code DataChangedEvent}
     * @param dataSet data set. Can be null
     */
    public DataChangedEvent(DataSet dataSet) {
        this(dataSet, null);
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.dataChanged(this);
    }

    @Override
    public Collection<OsmPrimitive> getPrimitives() {
        return dataSet == null ? Collections.emptyList() : dataSet.allPrimitives();
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.DATA_CHANGED;
    }

    /**
     * Returns list of events that caused this DataChangedEvent.
     * @return List of events that caused this DataChangedEvent. Might be null
     */
    public List<AbstractDatasetChangedEvent> getEvents() {
        return events;
    }
}
