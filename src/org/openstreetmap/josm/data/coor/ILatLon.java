// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projecting;

/**
 * This interface represents a coordinate in LatLon space.
 * <p>
 * It provides methods to get the coordinates. The coordinates may be unknown.
 * In this case, both {@link #lat()} and {@link #lon()} need to return a NaN value and {@link #isLatLonKnown()} needs to return false.
 * <p>
 * Whether the coordinates are immutable or not is implementation specific.
 *
 * @author Michael Zangl
 * @since 12161
 */
public interface ILatLon {

    /**
     * Returns the longitude, i.e., the east-west position in degrees.
     * @return the longitude or NaN if {@link #isLatLonKnown()} returns false
     */
    double lon();

    /**
     * Returns the latitude, i.e., the north-south position in degrees.
     * @return the latitude or NaN if {@link #isLatLonKnown()} returns false
     */
    double lat();

    /**
     * Determines if this object has valid coordinates.
     * @return {@code true} if this object has valid coordinates
     */
    default boolean isLatLonKnown() {
        return !Double.isNaN(lat()) && !Double.isNaN(lon());
    }

    /**
     * <p>Replies the projected east/north coordinates.</p>
     *
     * <p>Uses the {@link Main#getProjection() global projection} to project the lat/lon-coordinates.</p>
     *
     * @return the east north coordinates or {@code null} if #is
     * @deprecated use {@link #getEastNorth(org.openstreetmap.josm.data.projection.Projecting)}
     */
    @Deprecated
    default EastNorth getEastNorth() {
        return getEastNorth(Main.getProjection());
    }

    /**
     * Replies the projected east/north coordinates.
     * <p>
     * The result of the last conversion may be cached. Null is returned in case this object is invalid.
     * @param projecting The projection to use.
     * @return The projected east/north coordinates
     * @since 10827
     */
    default EastNorth getEastNorth(Projecting projecting) {
        if (!isLatLonKnown()) {
            return null;
        } else {
            return projecting.latlon2eastNorth(this);
        }
    }
}
