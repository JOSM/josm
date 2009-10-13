// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.LatLon;

public class NodeData extends PrimitiveData {

    private LatLon coor;

    public LatLon getCoor() {
        return coor;
    }

    public void setCoor(LatLon coor) {
        this.coor = coor;
    }

}
