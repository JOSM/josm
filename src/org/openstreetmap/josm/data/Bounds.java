// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.MessageFormat;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * This is a simple data class for "rectangular" areas of the world, given in
 * lat/lon min/max values.  The values are rounded to LatLon.OSM_SERVER_PRECISION
 *
 * @author imi
 */
public class Bounds {
    /**
     * The minimum and maximum coordinates.
     */
    private double minLat, minLon, maxLat, maxLon;

    public LatLon getMin() {
        return new LatLon(minLat, minLon);
    }

    /**
     * Returns min latitude of bounds. Efficient shortcut for {@code getMin().lat()}.
     *
     * @return min latitude of bounds.
     * @since 6203
     */
    public double getMinLat() {
        return minLat;
    }

    /**
     * Returns min longitude of bounds. Efficient shortcut for {@code getMin().lon()}.
     *
     * @return min longitude of bounds.
     * @since 6203
     */
    public double getMinLon() {
        return minLon;
    }

    public LatLon getMax() {
        return new LatLon(maxLat, maxLon);
    }

    /**
     * Returns max latitude of bounds. Efficient shortcut for {@code getMax().lat()}.
     *
     * @return max latitude of bounds.
     * @since 6203
     */
    public double getMaxLat() {
        return maxLat;
    }

    /**
     * Returns max longitude of bounds. Efficient shortcut for {@code getMax().lon()}.
     *
     * @return max longitude of bounds.
     * @since 6203
     */
    public double getMaxLon() {
        return maxLon;
    }

    public enum ParseMethod {
        MINLAT_MINLON_MAXLAT_MAXLON,
        LEFT_BOTTOM_RIGHT_TOP
    }

    /**
     * Construct bounds out of two points. Coords will be rounded.
     */
    public Bounds(LatLon min, LatLon max) {
        this(min.lat(), min.lon(), max.lat(), max.lon());
    }

    public Bounds(LatLon min, LatLon max, boolean roundToOsmPrecision) {
        this(min.lat(), min.lon(), max.lat(), max.lon(), roundToOsmPrecision);
    }

    public Bounds(LatLon b) {
        this(b, true);
    }

    /**
     * Single point Bounds defined by lat/lon {@code b}.
     * Coordinates will be rounded to osm precision if {@code roundToOsmPrecision} is true.
     *
     * @param b lat/lon of given point.
     * @param roundToOsmPrecision defines if lat/lon will be rounded.
     */
    public Bounds(LatLon b, boolean roundToOsmPrecision) {
        this(b.lat(), b.lon(), roundToOsmPrecision);
    }

    /**
     * Single point Bounds defined by point [lat,lon].
     * Coordinates will be rounded to osm precision if {@code roundToOsmPrecision} is true.
     *
     * @param lat latitude of given point.
     * @param lon longitude of given point.
     * @param roundToOsmPrecision defines if lat/lon will be rounded.
     * @since 6203
     */
    public Bounds(double lat, double lon, boolean roundToOsmPrecision) {
        // Do not call this(b, b) to avoid GPX performance issue (see #7028) until roundToOsmPrecision() is improved
        if (roundToOsmPrecision) {
            this.minLat = LatLon.roundToOsmPrecision(lat);
            this.minLon = LatLon.roundToOsmPrecision(lon);
        } else {
            this.minLat = lat;
            this.minLon = lon;
        }
        this.maxLat = this.minLat;
        this.maxLon = this.minLon;
    }

    public Bounds(double minlat, double minlon, double maxlat, double maxlon) {
        this(minlat, minlon, maxlat, maxlon, true);
    }

    public Bounds(double minlat, double minlon, double maxlat, double maxlon, boolean roundToOsmPrecision) {
        if (roundToOsmPrecision) {
            this.minLat = LatLon.roundToOsmPrecision(minlat);
            this.minLon = LatLon.roundToOsmPrecision(minlon);
            this.maxLat = LatLon.roundToOsmPrecision(maxlat);
            this.maxLon = LatLon.roundToOsmPrecision(maxlon);
        } else {
            this.minLat = minlat;
            this.minLon = minlon;
            this.maxLat = maxlat;
            this.maxLon = maxlon;
        }
    }

    public Bounds(double [] coords) {
        this(coords, true);
    }

    public Bounds(double [] coords, boolean roundToOsmPrecision) {
        CheckParameterUtil.ensureParameterNotNull(coords, "coords");
        if (coords.length != 4)
            throw new IllegalArgumentException(MessageFormat.format("Expected array of length 4, got {0}", coords.length));
        if (roundToOsmPrecision) {
            this.minLat = LatLon.roundToOsmPrecision(coords[0]);
            this.minLon = LatLon.roundToOsmPrecision(coords[1]);
            this.maxLat = LatLon.roundToOsmPrecision(coords[2]);
            this.maxLon = LatLon.roundToOsmPrecision(coords[3]);
        } else {
            this.minLat = coords[0];
            this.minLon = coords[1];
            this.maxLat = coords[2];
            this.maxLon = coords[3];
        }
    }

    public Bounds(String asString, String separator) throws IllegalArgumentException {
        this(asString, separator, ParseMethod.MINLAT_MINLON_MAXLAT_MAXLON);
    }

    public Bounds(String asString, String separator, ParseMethod parseMethod) throws IllegalArgumentException {
        this(asString, separator, parseMethod, true);
    }

    public Bounds(String asString, String separator, ParseMethod parseMethod, boolean roundToOsmPrecision) throws IllegalArgumentException {
        CheckParameterUtil.ensureParameterNotNull(asString, "asString");
        String[] components = asString.split(separator);
        if (components.length != 4)
            throw new IllegalArgumentException(MessageFormat.format("Exactly four doubles expected in string, got {0}: {1}", components.length, asString));
        double[] values = new double[4];
        for (int i=0; i<4; i++) {
            try {
                values[i] = Double.parseDouble(components[i]);
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException(MessageFormat.format("Illegal double value ''{0}''", components[i]), e);
            }
        }

        switch (parseMethod) {
            case LEFT_BOTTOM_RIGHT_TOP:
                this.minLat = initLat(values[1], roundToOsmPrecision);
                this.minLon = initLon(values[0], roundToOsmPrecision);
                this.maxLat = initLat(values[3], roundToOsmPrecision);
                this.maxLon = initLon(values[2], roundToOsmPrecision);
                break;
            case MINLAT_MINLON_MAXLAT_MAXLON:
            default:
                this.minLat = initLat(values[0], roundToOsmPrecision);
                this.minLon = initLon(values[1], roundToOsmPrecision);
                this.maxLat = initLat(values[2], roundToOsmPrecision);
                this.maxLon = initLon(values[3], roundToOsmPrecision);
        }
    }

    protected static double initLat(double value, boolean roundToOsmPrecision) {
        if (!LatLon.isValidLat(value))
            throw new IllegalArgumentException(tr("Illegal latitude value ''{0}''", value));
        return roundToOsmPrecision ? LatLon.roundToOsmPrecision(value) : value;
    }

    protected static double initLon(double value, boolean roundToOsmPrecision) {
        if (!LatLon.isValidLon(value))
            throw new IllegalArgumentException(tr("Illegal longitude value ''{0}''", value));
        return roundToOsmPrecision ? LatLon.roundToOsmPrecision(value) : value;
    }

    /**
     * Creates new {@code Bounds} from an existing one.
     * @param other The bounds to copy
     */
    public Bounds(final Bounds other) {
        this(other.minLat, other.minLon, other.maxLat, other.maxLon);
    }

    public Bounds(Rectangle2D rect) {
        this(rect.getMinY(), rect.getMinX(), rect.getMaxY(), rect.getMaxX());
    }

    /**
     * Creates new bounds around a coordinate pair <code>center</code>. The
     * new bounds shall have an extension in latitude direction of <code>latExtent</code>,
     * and in longitude direction of <code>lonExtent</code>.
     *
     * @param center  the center coordinate pair. Must not be null.
     * @param latExtent the latitude extent. &gt; 0 required.
     * @param lonExtent the longitude extent. &gt; 0 required.
     * @throws IllegalArgumentException thrown if center is null
     * @throws IllegalArgumentException thrown if latExtent &lt;= 0
     * @throws IllegalArgumentException thrown if lonExtent &lt;= 0
     */
    public Bounds(LatLon center, double latExtent, double lonExtent) {
        CheckParameterUtil.ensureParameterNotNull(center, "center");
        if (latExtent <= 0.0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0.0 expected, got {1}", "latExtent", latExtent));
        if (lonExtent <= 0.0)
            throw new IllegalArgumentException(MessageFormat.format("Parameter ''{0}'' > 0.0 expected, got {1}", "lonExtent", lonExtent));

        this.minLat = LatLon.roundToOsmPrecision(LatLon.toIntervalLat(center.lat() - latExtent / 2));
        this.minLon = LatLon.roundToOsmPrecision(LatLon.toIntervalLon(center.lon() - lonExtent / 2));
        this.maxLat = LatLon.roundToOsmPrecision(LatLon.toIntervalLat(center.lat() + latExtent / 2));
        this.maxLon = LatLon.roundToOsmPrecision(LatLon.toIntervalLon(center.lon() + lonExtent / 2));
    }

    /**
     * Creates BBox with same coordinates.
     *
     * @return BBox with same coordinates.
     * @since 6203
     */
    public BBox toBBox() {
        return new BBox(minLon, minLat, maxLon, maxLat);
    }

    @Override public String toString() {
        return "Bounds["+minLat+","+minLon+","+maxLat+","+maxLon+"]";
    }

    public String toShortString(DecimalFormat format) {
        return
        format.format(minLat) + " "
        + format.format(minLon) + " / "
        + format.format(maxLat) + " "
        + format.format(maxLon);
    }

    /**
     * @return Center of the bounding box.
     */
    public LatLon getCenter() {
        if (crosses180thMeridian()) {
            double lat = (minLat + maxLat) / 2;
            double lon = (minLon + maxLon - 360.0) / 2;
            if (lon < -180.0){
                lon += 360.0;
            }
            return new LatLon(lat, lon);
        } else {
            return new LatLon((minLat + maxLat) / 2, (minLon + maxLon) / 2);
        }
    }

    /**
     * Extend the bounds if necessary to include the given point.
     * @param ll The point to include into these bounds
     */
    public void extend(LatLon ll) {
        extend(ll.lat(), ll.lon());
    }

    /**
     * Extend the bounds if necessary to include the given point [lat,lon].
     * Good to use if you know coordinates to avoid creation of LatLon object.
     * @param lat Latitude of point to include into these bounds
     * @param lon Longitude of point to include into these bounds
     * @since 6203
     */
    public void extend(final double lat, final double lon) {
        if (lat < minLat) {
            minLat = LatLon.roundToOsmPrecision(lat);
        }
        if (lat > maxLat) {
            maxLat = LatLon.roundToOsmPrecision(lat);
        }
        if (crosses180thMeridian()) {
            if (lon > maxLon && lon < minLon) {
                if (Math.abs(lon - minLon) <= Math.abs(lon - maxLon)) {
                    minLon = LatLon.roundToOsmPrecision(lon);
                } else {
                    maxLon = LatLon.roundToOsmPrecision(lon);
                }
            }
        } else {
            if (lon < minLon) {
                minLon = LatLon.roundToOsmPrecision(lon);
            }
            if (lon > maxLon) {
                maxLon = LatLon.roundToOsmPrecision(lon);
            }
        }
    }

    public void extend(Bounds b) {
        extend(b.minLat, b.minLon);
        extend(b.maxLat, b.maxLon);
    }

    /**
     * Determines if the given point {@code ll} is within these bounds.
     * @param ll The lat/lon to check
     * @return {@code true} if {@code ll} is within these bounds, {@code false} otherwise
     */
    public boolean contains(LatLon ll) {
        if (ll.lat() < minLat || ll.lat() > maxLat)
            return false;
        if (crosses180thMeridian()) {
            if (ll.lon() > maxLon && ll.lon() < minLon)
                return false;
        } else {
            if (ll.lon() < minLon || ll.lon() > maxLon)
                return false;
        }
        return true;
    }

    private static boolean intersectsLonCrossing(Bounds crossing, Bounds notCrossing) {
        return notCrossing.minLon <= crossing.maxLon || notCrossing.maxLon >= crossing.minLon;
    }

    /**
     * The two bounds intersect? Compared to java Shape.intersects, if does not use
     * the interior but the closure. ("&gt;=" instead of "&gt;")
     */
    public boolean intersects(Bounds b) {
        if (b.maxLat < minLat || b.minLat > maxLat)
            return false;

        if (crosses180thMeridian() && !b.crosses180thMeridian()) {
            return intersectsLonCrossing(this, b);
        } else if (!crosses180thMeridian() && b.crosses180thMeridian()) {
            return intersectsLonCrossing(b, this);
        } else if (crosses180thMeridian() && b.crosses180thMeridian()) {
            return true;
        } else {
            return b.maxLon >= minLon && b.minLon <= maxLon;
        }
    }

    /**
     * Determines if this Bounds object crosses the 180th Meridian.
     * See http://wiki.openstreetmap.org/wiki/180th_meridian
     * @return true if this Bounds object crosses the 180th Meridian.
     */
    public boolean crosses180thMeridian() {
        return this.minLon > this.maxLon;
    }

    /**
     * Converts the lat/lon bounding box to an object of type Rectangle2D.Double
     * @return the bounding box to Rectangle2D.Double
     */
    public Rectangle2D.Double asRect() {
        double w = maxLon-minLon + (crosses180thMeridian() ? 360.0 : 0.0);
        return new Rectangle2D.Double(minLon, minLat, w, maxLat-minLat);
    }

    public double getArea() {
        double w = maxLon-minLon + (crosses180thMeridian() ? 360.0 : 0.0);
        return w * (maxLat - minLat);
    }

    public String encodeAsString(String separator) {
        StringBuilder sb = new StringBuilder();
        sb.append(minLat).append(separator).append(minLon)
        .append(separator).append(maxLat).append(separator)
        .append(maxLon);
        return sb.toString();
    }

    /**
     * <p>Replies true, if this bounds are <em>collapsed</em>, i.e. if the min
     * and the max corner are equal.</p>
     *
     * @return true, if this bounds are <em>collapsed</em>
     */
    public boolean isCollapsed() {
        return Double.doubleToLongBits(minLat) == Double.doubleToLongBits(maxLat)
            && Double.doubleToLongBits(minLon) == Double.doubleToLongBits(maxLon);
    }

    public boolean isOutOfTheWorld() {
        return
        minLat < -90 || minLat > 90 ||
        maxLat < -90 || maxLat > 90 ||
        minLon < -180 || minLon > 180 ||
        maxLon < -180 || maxLon > 180;
    }

    public void normalize() {
        minLat = LatLon.toIntervalLat(minLat);
        maxLat = LatLon.toIntervalLat(maxLat);
        minLon = LatLon.toIntervalLon(minLon);
        maxLon = LatLon.toIntervalLon(maxLon);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(maxLat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(maxLon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minLat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(minLon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Bounds other = (Bounds) obj;
        if (Double.doubleToLongBits(maxLat) != Double.doubleToLongBits(other.maxLat))
            return false;
        if (Double.doubleToLongBits(maxLon) != Double.doubleToLongBits(other.maxLon))
            return false;
        if (Double.doubleToLongBits(minLat) != Double.doubleToLongBits(other.minLat))
            return false;
        if (Double.doubleToLongBits(minLon) != Double.doubleToLongBits(other.minLon))
            return false;
        return true;
    }
}
