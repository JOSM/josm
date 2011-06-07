// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public interface INode extends IPrimitive {

    LatLon getCoor();
    void setCoor(LatLon coor);
    EastNorth getEastNorth();
    void setEastNorth(EastNorth eastNorth);
}
