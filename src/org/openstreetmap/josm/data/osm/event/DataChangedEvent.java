// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

public class DataChangedEvent extends AbstractDatasetChangedEvent {

    private final List<AbstractDatasetChangedEvent> events;

    public DataChangedEvent(DataSet dataSet, List<AbstractDatasetChangedEvent> events) {
        super(dataSet);
        this.events = events;
    }

    public DataChangedEvent(DataSet dataSet) {
        this(dataSet, null);
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.dataChanged(this);
    }

    @Override
    public Collection<OsmPrimitive> getPrimitives() {
        if (dataSet == null)
            return Collections.emptyList();
        else
            return dataSet.allPrimitives();
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.DATA_CHANGED;
    }

    /**
     *
     * @return List of events that caused this DataChangedEvent. Might be null
     */
    public List<AbstractDatasetChangedEvent> getEvents() {
        return events;
    }

}
