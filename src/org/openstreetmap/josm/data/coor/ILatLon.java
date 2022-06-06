// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

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
     * Minimum difference in location to not be represented as the same position.
     * The API returns 7 decimals.
     */
    double MAX_SERVER_PRECISION = 1e-7;

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

    /**
     * Determines if the other point has almost the same lat/lon values.
     * @param other other lat/lon
     * @return <code>true</code> if the other point has almost the same lat/lon
     * values, only differing by no more than 1 / {@link #MAX_SERVER_PRECISION MAX_SERVER_PRECISION}.
     * @since 18464 (extracted from {@link LatLon})
     */
    default boolean equalsEpsilon(ILatLon other) {
        return equalsEpsilon(other, MAX_SERVER_PRECISION);
    }

    /**
     * Determines if the other point has almost the same lat/lon values.
     * @param other other lat/lon
     * @param precision The precision to use
     * @return <code>true</code> if the other point has almost the same lat/lon
     * values, only differing by no more than 1 / precision.
     * @since 18464 (extracted from {@link LatLon})
     */
    default boolean equalsEpsilon(ILatLon other, double precision) {
        double p = precision / 2;
        return Math.abs(lat() - other.lat()) <= p && Math.abs(lon() - other.lon()) <= p;
    }
}
