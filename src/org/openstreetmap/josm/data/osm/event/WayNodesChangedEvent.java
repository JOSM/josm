// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;

public class WayNodesChangedEvent extends AbstractDatasetChangedEvent {

    private final Way way;

    public WayNodesChangedEvent(DataSet dataSet, Way way) {
        super(dataSet);
        this.way = way;
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.wayNodesChanged(this);
    }

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
