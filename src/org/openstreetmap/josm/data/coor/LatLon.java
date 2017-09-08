// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static org.openstreetmap.josm.data.projection.Ellipsoid.WGS84;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.Utils.toRadians;

import java.awt.geom.Area;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.conversion.DMSCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.DecimalDegreesCoordinateFormat;
import org.openstreetmap.josm.data.coor.conversion.LatLonParser;
import org.openstreetmap.josm.data.coor.conversion.NauticalCoordinateFormat;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * LatLon are unprojected latitude / longitude coordinates.
 * <br>
 * <b>Latitude</b> specifies the north-south position in degrees
 * where valid values are in the [-90,90] and positive values specify positions north of the equator.
 * <br>
 * <b>Longitude</b> specifies the east-west position in degrees
 * where valid values are in the [-180,180] and positive values specify positions east of the prime meridian.
 * <br>
 * <img alt="lat/lon" src="https://upload.wikimedia.org/wikipedia/commons/6/62/Latitude_and_Longitude_of_the_Earth.svg">
 * <br>
 * This class is immutable.
 *
 * @author Imi
 */
public class LatLon extends Coordinate implements ILatLon {

    private static final long serialVersionUID = 1L;

    /**
     * Minimum difference in location to not be represented as the same position.
     * The API returns 7 decimals.
     */
    public static final double MAX_SERVER_PRECISION = 1e-7;
    /**
     * The inverse of the server precision
     * @see #MAX_SERVER_PRECISION
     */
    public static final double MAX_SERVER_INV_PRECISION = 1e7;

    /**
     * The (0,0) coordinates.
     * @since 6178
     */
    public static final LatLon ZERO = new LatLon(0, 0);

    /** North pole. */
    public static final LatLon NORTH_POLE = new LatLon(90, 0);
    /** South pole. */
    public static final LatLon SOUTH_POLE = new LatLon(-90, 0);

    /**
     * The normal number format for server precision coordinates
     */
    public static final DecimalFormat cDdFormatter;
    /**
     * The number format used for high precision coordinates
     */
    public static final DecimalFormat cDdHighPecisionFormatter;
    static {
        // Don't use the localized decimal separator. This way we can present
        // a comma separated list of coordinates.
        cDdFormatter = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
        cDdFormatter.applyPattern("###0.0######");
        cDdHighPecisionFormatter = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
        cDdHighPecisionFormatter.applyPattern("###0.0##########");
    }

    /**
     * Character denoting South, as string.
     * @deprecated use {@link LatLonParser#SOUTH}
     */
    @Deprecated
    public static final String SOUTH = trc("compass", "S");
    /**
     * Character denoting North, as string.
     * @deprecated use {@link LatLonParser#NORTH}
     */
    @Deprecated
    public static final String NORTH = trc("compass", "N");
    /**
     * Character denoting West, as string.
     * @deprecated use {@link LatLonParser#WEST}
     */
    @Deprecated
    public static final String WEST = trc("compass", "W");
    /**
     * Character denoting East, as string.
     * @deprecated use {@link LatLonParser#EAST}
     */
    @Deprecated
    public static final String EAST = trc("compass", "E");

    /**
     * Replies true if lat is in the range [-90,90]
     *
     * @param lat the latitude
     * @return true if lat is in the range [-90,90]
     */
    public static boolean isValidLat(double lat) {
        return lat >= -90d && lat <= 90d;
    }

    /**
     * Replies true if lon is in the range [-180,180]
     *
     * @param lon the longitude
     * @return true if lon is in the range [-180,180]
     */
    public static boolean isValidLon(double lon) {
        return lon >= -180d && lon <= 180d;
    }

    /**
     * Make sure longitude value is within <code>[-180, 180]</code> range.
     * @param lon the longitude in degrees
     * @return lon plus/minus multiples of <code>360</code>, as needed to get
     * in <code>[-180, 180]</code> range
     */
    public static double normalizeLon(double lon) {
        if (lon >= -180 && lon <= 180)
            return lon;
        else {
            lon = lon % 360.0;
            if (lon > 180) {
                return lon - 360;
            } else if (lon < -180) {
                return lon + 360;
            }
            return lon;
        }
    }

    /**
     * Replies true if lat is in the range [-90,90] and lon is in the range [-180,180]
     *
     * @return true if lat is in the range [-90,90] and lon is in the range [-180,180]
     */
    public boolean isValid() {
        return isValidLat(lat()) && isValidLon(lon());
    }

    /**
     * Clamp the lat value to be inside the world.
     * @param value The value
     * @return The value clamped to the world.
     */
    public static double toIntervalLat(double value) {
        return Utils.clamp(value, -90, 90);
    }

    /**
     * Returns a valid OSM longitude [-180,+180] for the given extended longitude value.
     * For example, a value of -181 will return +179, a value of +181 will return -179.
     * @param value A longitude value not restricted to the [-180,+180] range.
     * @return a valid OSM longitude [-180,+180]
     */
    public static double toIntervalLon(double value) {
        if (isValidLon(value))
            return value;
        else {
            int n = (int) (value + Math.signum(value)*180.0) / 360;
            return value - n*360.0;
        }
    }

    /**
     * Replies the coordinate in degrees/minutes/seconds format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes/seconds format
     * @deprecated use {@link #degreesMinutesSeconds} instead
     */
    @Deprecated
    public static String dms(double pCoordinate) {
        return degreesMinutesSeconds(pCoordinate);
    }

    /**
     * Replies the coordinate in degrees/minutes/seconds format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes/seconds format
     * @since 12561
     * @deprecated use {@link DMSCoordinateFormat#degreesMinutesSeconds(double)}
     */
    @Deprecated
    public static String degreesMinutesSeconds(double pCoordinate) {
        return DMSCoordinateFormat.degreesMinutesSeconds(pCoordinate);
    }

    /**
     * Replies the coordinate in degrees/minutes format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes format
     * @since 12537
     * @deprecated use {@link NauticalCoordinateFormat#degreesMinutes(double)}
     */
    @Deprecated
    public static String degreesMinutes(double pCoordinate) {
        return NauticalCoordinateFormat.degreesMinutes(pCoordinate);
    }

    /**
     * Replies the coordinate in degrees/minutes format
     * @param pCoordinate The coordinate to convert
     * @return The coordinate in degrees/minutes format
     * @deprecated use {@link #degreesMinutes(double)} instead
     */
    @Deprecated
    public static String dm(double pCoordinate) {
        return degreesMinutes(pCoordinate);
    }

    /**
     * Constructs a new object representing the given latitude/longitude.
     * @param lat the latitude, i.e., the north-south position in degrees
     * @param lon the longitude, i.e., the east-west position in degrees
     */
    public LatLon(double lat, double lon) {
        super(lon, lat);
    }

    /**
     * Creates a new LatLon object for the given coordinate
     * @param coor The coordinates to copy from.
     */
    public LatLon(ILatLon coor) {
        super(coor.lon(), coor.lat());
    }

    @Override
    public double lat() {
        return y;
    }

    /**
     * Formats the latitude part according to the given format
     * @param d the coordinate format to use
     * @return the formatted latitude
     * @deprecated use {@link org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat#latToString(ILatLon)}
     */
    @Deprecated
    public String latToString(CoordinateFormat d) {
        return d.getICoordinateFormat().latToString(this);
    }

    @Override
    public double lon() {
        return x;
    }

    /**
     * Formats the longitude part according to the given format
     * @param d the coordinate format to use
     * @return the formatted longitude
     * @deprecated use {@link org.openstreetmap.josm.data.coor.conversion.ICoordinateFormat#lonToString(ILatLon)}
     */
    @Deprecated
    public String lonToString(CoordinateFormat d) {
        return d.getICoordinateFormat().lonToString(this);
    }

    /**
     * @param other other lat/lon
     * @return <code>true</code> if the other point has almost the same lat/lon
     * values, only differing by no more than 1 / {@link #MAX_SERVER_PRECISION MAX_SERVER_PRECISION}.
     */
    public boolean equalsEpsilon(LatLon other) {
        double p = MAX_SERVER_PRECISION / 2;
        return Math.abs(lat()-other.lat()) <= p && Math.abs(lon()-other.lon()) <= p;
    }

    /**
     * Determines if this lat/lon is outside of the world
     * @return <code>true</code>, if the coordinate is outside the world, compared by using lat/lon.
     */
    public boolean isOutSideWorld() {
        Bounds b = Main.getProjection().getWorldBoundsLatLon();
        return lat() < b.getMinLat() || lat() > b.getMaxLat() ||
                lon() < b.getMinLon() || lon() > b.getMaxLon();
    }

    /**
     * Determines if this lat/lon is within the given bounding box.
     * @param b bounding box
     * @return <code>true</code> if this is within the given bounding box.
     */
    public boolean isWithin(Bounds b) {
        return b.contains(this);
    }

    /**
     * Check if this is contained in given area or area is null.
     *
     * @param a Area
     * @return <code>true</code> if this is contained in given area or area is null.
     */
    public boolean isIn(Area a) {
        return a == null || a.contains(x, y);
    }

    /**
     * Computes the distance between this lat/lon and another point on the earth.
     * Uses Haversine formular.
     * @param other the other point.
     * @return distance in metres.
     */
    public double greatCircleDistance(LatLon other) {
        double sinHalfLat = sin(toRadians(other.lat() - this.lat()) / 2);
        double sinHalfLon = sin(toRadians(other.lon() - this.lon()) / 2);
        double d = 2 * WGS84.a * asin(
                sqrt(sinHalfLat*sinHalfLat +
                        cos(toRadians(this.lat()))*cos(toRadians(other.lat()))*sinHalfLon*sinHalfLon));
        // For points opposite to each other on the sphere,
        // rounding errors could make the argument of asin greater than 1
        // (This should almost never happen.)
        if (java.lang.Double.isNaN(d)) {
            Logging.error("NaN in greatCircleDistance");
            d = PI * WGS84.a;
        }
        return d;
    }

    /**
     * Returns the heading that you have to use to get from this lat/lon to another.
     *
     * Angle starts from north and increases counterclockwise (!), PI/2 means west.
     * You can get usual clockwise angle from {@link #bearing(LatLon)} method.
     * This method is kept as deprecated because it is called from many plugins.
     *
     * (I don't know the original source of this formula, but see
     * <a href="https://math.stackexchange.com/questions/720/how-to-calculate-a-heading-on-the-earths-surface">this question</a>
     * for some hints how it is derived.)
     *
     * @deprecated see bearing method
     * @param other the "destination" position
     * @return heading in radians in the range 0 &lt;= hd &lt; 2*PI
     */
    @Deprecated
    public double heading(LatLon other) {
        double hd = atan2(sin(toRadians(this.lon() - other.lon())) * cos(toRadians(other.lat())),
                cos(toRadians(this.lat())) * sin(toRadians(other.lat())) -
                sin(toRadians(this.lat())) * cos(toRadians(other.lat())) * cos(toRadians(this.lon() - other.lon())));
        hd %= 2 * PI;
        if (hd < 0) {
            hd += 2 * PI;
        }
        return hd;
    }

    /**
     * Returns bearing from this point to another.
     *
     * Angle starts from north and increases clockwise, PI/2 means east.
     * Old deprecated method {@link #heading(LatLon)} used unusual reverse angle.
     *
     * Please note that reverse bearing (from other point to this point) should NOT be
     * calculated from return value of this method, because great circle path
     * between the two points have different bearings at each position.
     *
     * To get bearing from another point to this point call other.bearing(this)
     *
     * @param other the "destination" position
     * @return heading in radians in the range 0 &lt;= hd &lt; 2*PI
     */
    public double bearing(LatLon other) {
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
     * Returns this lat/lon pair in human-readable format.
     *
     * @return String in the format "lat=1.23456 deg, lon=2.34567 deg"
     */
    public String toDisplayString() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(5);
        return "lat=" + nf.format(lat()) + "\u00B0, lon=" + nf.format(lon()) + '\u00B0';
    }

    /**
     * Returns this lat/lon pair in human-readable format separated by {@code separator}.
     * @param separator values separator
     * @return String in the format {@code "1.23456[separator]2.34567"}
     * @deprecated method removed without replacment
     */
    @Deprecated
    public String toStringCSV(String separator) {
        return Utils.join(separator, Arrays.asList(
                DecimalDegreesCoordinateFormat.INSTANCE.latToString(this),
                DecimalDegreesCoordinateFormat.INSTANCE.lonToString(this)
        ));
    }

    /**
     * Interpolate between this and a other latlon
     * @param ll2 The other lat/lon object
     * @param proportion The proportion to interpolate
     * @return a new latlon at this position if proportion is 0, at the other position it proportion is 1 and lineary interpolated otherwise.
     */
    public LatLon interpolate(LatLon ll2, double proportion) {
        // this is an alternate form of this.lat() + proportion * (ll2.lat() - this.lat()) that is slightly faster
        return new LatLon((1 - proportion) * this.lat() + proportion * ll2.lat(),
                (1 - proportion) * this.lon() + proportion * ll2.lon());
    }

    /**
     * Get the center between two lat/lon points
     * @param ll2 The other {@link LatLon}
     * @return The center at the average coordinates of the two points. Does not take the 180° meridian into account.
     */
    public LatLon getCenter(LatLon ll2) {
        // The JIT will inline this for us, it is as fast as the normal /2 approach
        return interpolate(ll2, .5);
    }

    /**
     * Returns the euclidean distance from this {@code LatLon} to a specified {@code LatLon}.
     *
     * @param ll the specified coordinate to be measured against this {@code LatLon}
     * @return the euclidean distance from this {@code LatLon} to a specified {@code LatLon}
     * @since 6166
     */
    public double distance(final LatLon ll) {
        return super.distance(ll);
    }

    /**
     * Returns the square of the euclidean distance from this {@code LatLon} to a specified {@code LatLon}.
     *
     * @param ll the specified coordinate to be measured against this {@code LatLon}
     * @return the square of the euclidean distance from this {@code LatLon} to a specified {@code LatLon}
     * @since 6166
     */
    public double distanceSq(final LatLon ll) {
        return super.distanceSq(ll);
    }

    @Override
    public String toString() {
        return "LatLon[lat="+lat()+",lon="+lon()+']';
    }

    /**
     * Returns the value rounded to OSM precisions, i.e. to {@link #MAX_SERVER_PRECISION}.
     * @param value lat/lon value
     *
     * @return rounded value
     */
    public static double roundToOsmPrecision(double value) {
        return Math.round(value * MAX_SERVER_INV_PRECISION) / MAX_SERVER_INV_PRECISION;
    }

    /**
     * Replies a clone of this lat LatLon, rounded to OSM precisions, i.e. to {@link #MAX_SERVER_PRECISION}
     *
     * @return a clone of this lat LatLon
     */
    public LatLon getRoundedToOsmPrecision() {
        return new LatLon(
                roundToOsmPrecision(lat()),
                roundToOsmPrecision(lon())
                );
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        LatLon that = (LatLon) obj;
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.y, y) == 0;
    }

    /**
     * Parses the given string as lat/lon.
     * @param coord String to parse
     * @return parsed lat/lon
     * @since 11045
     * @deprecated use {@link LatLonParser#parse(java.lang.String)}
     */
    @Deprecated
    public static LatLon parse(String coord) {
        return LatLonParser.parse(coord);
    }
}
