// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.openstreetmap.josm.data.projection.Ellipsoid.WGS84;
import static org.openstreetmap.josm.tools.Utils.toRadians;

import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.tools.Logging;

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

    /**
     * Computes the distance between this lat/lon and another point on the earth.
     * Uses <a href="https://en.wikipedia.org/wiki/Haversine_formula">Haversine formula</a>.
     * @param other the other point.
     * @return distance in metres.
     * @since 18494 (extracted from {@link LatLon})
     */
    default double greatCircleDistance(ILatLon other) {
        double sinHalfLat = sin(toRadians(other.lat() - this.lat()) / 2);
        double sinHalfLon = sin(toRadians(other.lon() - this.lon()) / 2);
        double d = 2 * WGS84.a * asin(
                sqrt(sinHalfLat*sinHalfLat +
                        cos(toRadians(this.lat()))*cos(toRadians(other.lat()))*sinHalfLon*sinHalfLon));
        // For points opposite to each other on the sphere,
        // rounding errors could make the argument of asin greater than 1
        // (This should almost never happen.)
        if (Double.isNaN(d)) {
            Logging.error("NaN in greatCircleDistance: {0} {1}", this, other);
            d = PI * WGS84.a;
        }
        return d;
    }

    /**
     * Returns bearing from this point to another.
     *
     * Angle starts from north and increases clockwise, PI/2 means east.
     *
     * Please note that reverse bearing (from other point to this point) should NOT be
     * calculated from return value of this method, because great circle path
     * between the two points have different bearings at each position.
     *
     * To get bearing from another point to this point call other.bearing(this)
     *
     * @param other the "destination" position
     * @return heading in radians in the range 0 &lt;= hd &lt; 2*PI
     * @since 18494 (extracted from {@link LatLon}, added in 9796)
     */
    default double bearing(ILatLon other) {
        double lat1 = toRadians(this.lat());
        double lat2 = toRadians(other.lat());
        double dlon = toRadians(other.lon() - this.lon());
        double bearing = atan2(
                sin(dlon) * cos(lat2),
                cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dlon)
        );
        bearing %= 2 * PI;
        if (bearing < 0) {
            bearing += 2 * PI;
        }
        return bearing;
    }

    /**
     * Does a linear interpolation between two ILatLon instances.
     * @param ll2 The other ILatLon instance.
     * @param proportion The proportion the other instance influences the result.
     * @return The new {@link ILatLon} position.
     * @since 18589
     */
    default ILatLon interpolate(ILatLon ll2, double proportion) {
        // this is an alternate form of this.lat() + proportion * (ll2.lat() - this.lat()) that is slightly faster
        return new LatLon((1 - proportion) * this.lat() + proportion * ll2.lat(),
                (1 - proportion) * this.lon() + proportion * ll2.lon());
    }

    /**
     * Returns the square of euclidean distance from this {@code Coordinate} to a specified coordinate.
     *
     * @param lon the X coordinate of the specified point to be measured against this {@code Coordinate}
     * @param lat the Y coordinate of the specified point to be measured against this {@code Coordinate}
     * @return the square of the euclidean distance from this {@code Coordinate} to a specified coordinate
     * @since 18589
     */
    default double distanceSq(final double lon, final double lat) {
        final double dx = this.lon() - lon;
        final double dy = this.lat() - lat;
        return dx * dx + dy * dy;
    }

    /**
     * Returns the euclidean distance from this {@code ILatLon} to a specified {@code ILatLon}.
     *
     * @param other the specified coordinate to be measured against this {@code ILatLon}
     * @return the euclidean distance from this {@code ILatLon} to a specified {@code ILatLon}
     * @since 18589
     */
    default double distanceSq(final ILatLon other) {
        return this.distanceSq(other.lon(), other.lat());
    }
}
