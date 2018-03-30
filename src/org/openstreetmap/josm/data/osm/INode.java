// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * INode captures the common functions of {@link Node} and {@link NodeData}.
 * @since 4098
 */
public interface INode extends IPrimitive, ILatLon {

    /**
     * Returns lat/lon coordinates of this node.
     * @return lat/lon coordinates of this node
     */
    LatLon getCoor();

    /**
     * Sets lat/lon coordinates of this node.
     * @param coor lat/lon coordinates of this node
     */
    void setCoor(LatLon coor);

    /**
     * Sets east/north coordinates of this node.
     * @param eastNorth east/north coordinates of this node
     */
    void setEastNorth(EastNorth eastNorth);

    @Override
    default String getDisplayName(NameFormatter formatter) {
        return formatter.format(this);
    }
}
