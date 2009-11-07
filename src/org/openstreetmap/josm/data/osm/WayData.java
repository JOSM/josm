// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

public class WayData extends PrimitiveData {

    private final List<Long> nodes = new ArrayList<Long>();

    public WayData() {

    }

    public WayData(WayData data) {
        super(data);
        nodes.addAll(data.getNodes());
    }

    public List<Long> getNodes() {
        return nodes;
    }

    @Override
    public WayData makeCopy() {
        return new WayData(this);
    }

    @Override
    public OsmPrimitive makePrimitive(DataSet dataSet) {
        return new Way(this, dataSet);
    }

    @Override
    public String toString() {
        return super.toString() + " WAY" + nodes.toString();
    }

    public OsmPrimitiveType getType() {
        return OsmPrimitiveType.WAY;
    }

}
