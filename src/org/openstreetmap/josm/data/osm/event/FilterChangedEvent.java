// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Represents a change of primitive filter flags.
 * @since 14206
 */
public class FilterChangedEvent extends AbstractDatasetChangedEvent {

    /**
     * Constructs a new {@code FilterChangedEvent}.
     * @param dataSet the dataset from which the event comes from
     */
    public FilterChangedEvent(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.otherDatasetChange(this);
    }

    @Override
    public Collection<? extends OsmPrimitive> getPrimitives() {
        return Collections.emptyList();
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.FILTERS_CHANGED;
    }
}
