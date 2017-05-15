// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

/**
 * An event that is triggered when the nodes of a way have been changed (nodes added, removed or the order was changed)
 */
public class WayNodesChangedEvent extends AbstractDatasetChangedEvent {

    private final Way way;

    /**
     * Constructs a new {@code WayNodesChangedEvent}.
     * @param dataSet the dataset from which the event comes from
     * @param way the way affected by the change
     */
    public WayNodesChangedEvent(DataSet dataSet, Way way) {
        super(dataSet);
        this.way = way;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.wayNodesChanged(this);
    }

    /**
     * Returns the way affected by the change.
     * @return the way affected by the change
     */
    public Way getChangedWay() {
        return way;
    }

    @Override
    public List<? extends OsmPrimitive> getPrimitives() {
        return Collections.singletonList(way);
    }

    @Override
    public DatasetEventType getType() {
        return DatasetEventType.WAY_NODES_CHANGED;
    }

}
