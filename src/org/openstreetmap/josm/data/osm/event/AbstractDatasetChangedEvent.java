// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import java.util.List;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmPrimitive;


public abstract class AbstractDatasetChangedEvent {

    protected final DataSet dataSet;

    protected AbstractDatasetChangedEvent(DataSet dataSet) {
        this.dataSet = dataSet;
    }

    public abstract void fire(DataSetListener listener);
    public abstract List<? extends OsmPrimitive> getPrimitives();

    public DataSet getDataset() {
        return dataSet;
    }

}
