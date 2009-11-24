// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * LatLon are unprojected latitude / longitude coordinates.
 *
 * This class is immutable.
 *
 * @author Imi
 */
public class LatLon extends Coordinate {

    private static DecimalFormat cDmsMinuteFormatter = new DecimalFormat("00");
    private static DecimalFormat cDmsSecondFormatter = new DecimalFormat("00.0");
    private static DecimalFormat cDdFormatter = new DecimalFormat("###0.0000");

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
                /* short symbol for South */ tr("S") :
                    /* short symbol for North */ tr("N"));
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
                /* short symbol for West */ tr("W") :
                    /* short symbol for East */ tr("E"));
        default: return "ERR";
        }
    }

    /**
     * @return <code>true</code> if the other point has almost the same lat/lon
     * values, only differing by no more than
     * 1 / {@link org.openstreetmap.josm.data.projection.Projection#MAX_SERVER_PRECISION MAX_SERVER_PRECISION}.
     */
    public boolean equalsEpsilon(LatLon other) {
        final double p = 1/Projection.MAX_SERVER_PRECISION;
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
     * Uses spherical law of cosines formula, not Haversine.
     * @param other the other point.
     * @return distance in metres.
     */
    public double greatCircleDistance(LatLon other) {
        return (Math.acos(
                Math.sin(Math.toRadians(lat())) * Math.sin(Math.toRadians(other.lat())) +
                Math.cos(Math.toRadians(lat()))*Math.cos(Math.toRadians(other.lat())) *
                Math.cos(Math.toRadians(other.lon()-lon()))) * 6378135);
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
