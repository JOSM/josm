// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import static org.openstreetmap.josm.tools.I18n.trc;

import static java.lang.Math.PI;
import static java.lang.Math.asin;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;

/**
 * LatLon are unprojected latitude / longitude coordinates.
 *
 * This class is immutable.
 *
 * @author Imi
 */
public class LatLon extends Coordinate {

    /**
     * Minimum difference in location to not be represented as the same position.
     * The API returns 7 decimals.
     */
    public static final double MAX_SERVER_PRECISION = 1e-7;

    private static DecimalFormat cDmsMinuteFormatter = new DecimalFormat("00");
    private static DecimalFormat cDmsSecondFormatter = new DecimalFormat("00.0");
    private static DecimalFormat cDdFormatter;
    static {
        // Don't use the localized decimal separator. This way we can present
        // a comma separated list of coordinates.
        cDdFormatter = (DecimalFormat) NumberFormat.getInstance(Locale.UK);
        cDdFormatter.applyPattern("###0.00000");
    }

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
     * Replies the coordinate in degrees/minutes/seconds format
     */
    public static String dms(double pCoordinate) {

        double tAbsCoord = Math.abs(pCoordinate);
        int tDegree = (int) tAbsCoord;
        double tTmpMinutes = (tAbsCoord - tDegree) * 60;
        int tMinutes = (int) tTmpMinutes;
        double tSeconds = (tTmpMinutes - tMinutes) * 60;

        return tDegree + "\u00B0" + cDmsMinuteFormatter.format(tMinutes) + "\'"
        + cDmsSecondFormatter.format(tSeconds) + "\"";
    }

    public LatLon(double lat, double lon) {
        super(lon, lat);
    }

    public LatLon(LatLon coor) {
        super(coor.lon(), coor.lat());
    }

    public double lat() {
        return y;
    }

    public String latToString(CoordinateFormat d) {
        switch(d) {
        case DECIMAL_DEGREES: return cDdFormatter.format(y);
        case DEGREES_MINUTES_SECONDS: return dms(y) + ((y < 0) ?
                /* short symbol for South */ trc("compass", "S") :
                    /* short symbol for North */ trc("compass", "N"));
        case EAST_NORTH: return cDdFormatter.format(Main.proj.latlon2eastNorth(this).north());
        default: return "ERR";
        }
    }

    public double lon() {
        return x;
    }

    public String lonToString(CoordinateFormat d) {
        switch(d) {
        case DECIMAL_DEGREES: return cDdFormatter.format(x);
        case DEGREES_MINUTES_SECONDS: return dms(x) + ((x < 0) ?
                /* short symbol for West */ trc("compass", "W") :
                    /* short symbol for East */ trc("compass", "E"));
        case EAST_NORTH: return cDdFormatter.format(Main.proj.latlon2eastNorth(this).east());
        default: return "ERR";
        }
    }

    /**
     * @return <code>true</code> if the other point has almost the same lat/lon
     * values, only differing by no more than
     * 1 / {@link #MAX_SERVER_PRECISION MAX_SERVER_PRECISION}.
     */
    public boolean equalsEpsilon(LatLon other) {
        double p = MAX_SERVER_PRECISION / 2;
        return Math.abs(lat()-other.lat()) <= p && Math.abs(lon()-other.lon()) <= p;
    }

    /**
     * @return <code>true</code>, if the coordinate is outside the world, compared
     * by using lat/lon.
     */
    public boolean isOutSideWorld() {
        Bounds b = Main.proj.getWorldBoundsLatLon();
        return lat() < b.getMin().lat() || lat() > b.getMax().lat() ||
        lon() < b.getMin().lon() || lon() > b.getMax().lon();
    }

    /**
     * @return <code>true</code> if this is within the given bounding box.
     */
    public boolean isWithin(Bounds b) {
        return lat() >= b.getMin().lat() && lat() <= b.getMax().lat() && lon() > b.getMin().lon() && lon() < b.getMax().lon();
    }

    /**
     * Computes the distance between this lat/lon and another point on the earth.
     * Uses Haversine formular.
     * @param other the other point.
     * @return distance in metres.
     */
    public double greatCircleDistance(LatLon other) {
        double R = 6378135;
        double sinHalfLat = sin(toRadians(other.lat() - this.lat()) / 2);
        double sinHalfLon = sin(toRadians(other.lon() - this.lon()) / 2);
        double d = 2 * R * asin(
                sqrt(sinHalfLat*sinHalfLat +
                        cos(toRadians(this.lat()))*cos(toRadians(other.lat()))*sinHalfLon*sinHalfLon));
        // For points opposite to each other on the sphere,
        // rounding errors could make the argument of asin greater than 1
        // (This should almost never happen.)
        if (java.lang.Double.isNaN(d)) {
            System.err.println("Error: NaN in greatCircleDistance");
            d = PI * R;
        }
        return d;
    }

    /**
     * Returns the heading, in radians, that you have to use to get from
     * this lat/lon to another.
     *
     * @param other the "destination" position
     * @return heading
     */
    public double heading(LatLon other) {
        double rv;
        if (other.lat() == lat()) {
            rv = (other.lon()>lon() ? Math.PI / 2 : Math.PI * 3 / 2);
        } else {
            rv = Math.atan((other.lon()-lon())/(other.lat()-lat()));
            if (rv < 0) {
                rv += Math.PI;
            }
            if (other.lon() < lon()) {
                rv += Math.PI;
            }
        }
        return rv;
    }

    /**
     * Returns this lat/lon pair in human-readable format.
     *
     * @return String in the format "lat=1.23456°, lon=2.34567°"
     */
    public String toDisplayString() {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(5);
        return "lat=" + nf.format(lat()) + "\u00B0, lon=" + nf.format(lon()) + "\u00B0";
    }

    public LatLon interpolate(LatLon ll2, double proportion) {
        return new LatLon(this.lat() + proportion * (ll2.lat() - this.lat()),
                this.lon() + proportion * (ll2.lon() - this.lon()));
    }

    public LatLon getCenter(LatLon ll2) {
        return new LatLon((this.lat() + ll2.lat())/2.0, (this.lon() + ll2.lon())/2.0);
    }

    @Override public String toString() {
        return "LatLon[lat="+lat()+",lon="+lon()+"]";
    }

    /**
     * Replies a clone of this lat LatLon, rounded to OSM precisions, i.e. to
     * MAX_SERVER_PRECISION
     *
     * @return a clone of this lat LatLon
     */
    public LatLon getRoundedToOsmPrecision() {
        return new LatLon(
                Math.round(lat() / MAX_SERVER_PRECISION) * MAX_SERVER_PRECISION,
                Math.round(lon() / MAX_SERVER_PRECISION) * MAX_SERVER_PRECISION
        );
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = java.lang.Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = java.lang.Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Coordinate other = (Coordinate) obj;
        if (java.lang.Double.doubleToLongBits(x) != java.lang.Double.doubleToLongBits(other.x))
            return false;
        if (java.lang.Double.doubleToLongBits(y) != java.lang.Double.doubleToLongBits(other.y))
            return false;
        return true;
    }
}
