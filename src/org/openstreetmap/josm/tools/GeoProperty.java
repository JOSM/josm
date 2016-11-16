// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;

/**
 * A method to look up a property of the earth surface.
 * 
 * User input for the {@link GeoPropertyIndex}.
 * @param <T> the property
 */
public interface GeoProperty<T> {

    /**
     * Look up the property for a point.
     * @param ll the point coordinates
     * @return property value at that point. Must not be null.
     */
    T get(LatLon ll);

    /**
     * Look up the property for a coordinate rectangle.
     * @param box the rectangle
     * @return the property, if it is the same in the entire rectangle;
     * null otherwise
     */
    T get(BBox box);

}
