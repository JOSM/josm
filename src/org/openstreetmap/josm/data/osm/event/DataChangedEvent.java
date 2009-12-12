// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;


public class DataChangedEvent extends AbstractDatasetChangedEvent {

    public DataChangedEvent(DataSet dataSet) {
        super(dataSet);
    }

    @Override
    public void fire(DataSetListener listener) {
        listener.dataChanged(this);
    }

    @Override
    public List<OsmPrimitive> getPrimitives() {
        return dataSet.allPrimitives();
    }

}
