// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.data.coor.LatLon;

/**
 * This is a simple data class for "rectangular" areas of the world, given in
 * lat/lon min/max values.
 *
 * @author imi
 */
public class Bounds {
    /**
     * The minimum and maximum coordinates.
     */
    private LatLon min, max;

    public LatLon getMin() {
        return min;
    }

    public LatLon getMax() {
        return max;
    }

    /**
     * Construct bounds out of two points
     */
    public Bounds(LatLon min, LatLon max) {
        this.min = min;
        this.max = max;
    }

    public Bounds(LatLon b) {
        this.min = b;
        this.max = b;
    }

    public Bounds(double minlat, double minlon, double maxlat, double maxlon) {
        this.min = new LatLon(minlat, minlon);
        this.max = new LatLon(maxlat, maxlon);
    }

    public Bounds(double [] coords) {
        if (coords == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "coords"));
        if (coords.length != 4)
            throw new IllegalArgumentException(tr("Expected array of length 4, got {0}", coords.length));
        this.min = new LatLon(coords[0], coords[1]);
        this.max = new LatLon(coords[2], coords[3]);
    }

    public Bounds(String asString, String separator) throws IllegalArgumentException {
        if (asString == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "asString"));
        String[] components = asString.split(separator);
        if (components.length != 4)
            throw new IllegalArgumentException(tr("Exactly four doubles excpected in string, got {0}", components.length));
        double[] values = new double[4];
        for (int i=0; i<4; i++) {
            try {
                values[i] = Double.parseDouble(components[i]);
            } catch(NumberFormatException e) {
                throw new IllegalArgumentException(tr("Illegal double value ''{0}''", components[i]));
            }
        }
        this.min = new LatLon(values[0], values[1]);
        this.max = new LatLon(values[2], values[3]);
    }

    public Bounds(Bounds other) {
        this.min = new LatLon(other.min);
        this.max = new LatLon(other.max);
    }

    public Bounds(Rectangle2D rect) {
        this.min = new LatLon(rect.getMinY(), rect.getMinX());
        this.max = new LatLon(rect.getMaxY(), rect.getMaxX());
    }

    /**
     * Creates new bounds around a coordinate pair <code>center</code>. The
     * new bounds shall have an extension in latitude direction of <code>latExtent</code>,
     * and in longitude direction of <code>lonExtent</code>.
     * 
     * @param center  the center coordinate pair. Must not be null.
     * @param latExtent the latitude extent. > 0 required.
     * @param lonExtent the longitude extent. > 0 required.
     * @throws IllegalArgumentException thrown if center is null
     * @throws IllegalArgumentException thrown if latExtent <= 0
     * @throws IllegalArgumentException thrown if lonExtent <= 0
     */
    public Bounds(LatLon center, double latExtent, double lonExtent) {
        if (center == null)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' must not be null", "center"));
        if (latExtent <= 0.0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0.0 exptected, got {1}", "latExtent", latExtent));
        if (lonExtent <= 0.0)
            throw new IllegalArgumentException(tr("Parameter ''{0}'' > 0.0 exptected, got {1}", "lonExtent", lonExtent));

        this.min = new LatLon(
                center.lat() - latExtent / 2,
                center.lon() - lonExtent / 2
        );
        this.max = new LatLon(
                center.lat() + latExtent / 2,
                center.lon() + lonExtent / 2
        );
    }

    @Override public String toString() {
        return "Bounds["+min.lat()+","+min.lon()+","+max.lat()+","+max.lon()+"]";
    }

    /**
     * @return Center of the bounding box.
     */
    public LatLon getCenter()
    {
        return min.getCenter(max);
    }

    /**
     * Extend the bounds if necessary to include the given point.
     */
    public void extend(LatLon ll) {
        if (ll.lat() < min.lat() || ll.lon() < min.lon()) {
            min = new LatLon(Math.min(ll.lat(), min.lat()), Math.min(ll.lon(), min.lon()));
        }
        if (ll.lat() > max.lat() || ll.lon() > max.lon()) {
            max = new LatLon(Math.max(ll.lat(), max.lat()), Math.max(ll.lon(), max.lon()));
        }
    }
    /**
     * Is the given point within this bounds?
     */
    public boolean contains(LatLon ll) {
        if (ll.lat() < min.lat() || ll.lon() < min.lon())
            return false;
        if (ll.lat() > max.lat() || ll.lon() > max.lon())
            return false;
        return true;
    }

    /**
     * Converts the lat/lon bounding box to an object of type Rectangle2D.Double
     * @return the bounding box to Rectangle2D.Double
     */
    public Rectangle2D.Double asRect() {
        return new Rectangle2D.Double(min.lon(), min.lat(), max.lon()-min.lon(), max.lat()-min.lat());
    }

    public double getArea() {
        return (max.lon() - min.lon()) * (max.lat() - min.lat());
    }

    public String encodeAsString(String separator) {
        StringBuffer sb = new StringBuffer();
        sb.append(min.lat()).append(separator).append(min.lon())
        .append(separator).append(max.lat()).append(separator)
        .append(max.lon());
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((max == null) ? 0 : max.hashCode());
        result = prime * result + ((min == null) ? 0 : min.hashCode());
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
        if (max == null) {
            if (other.max != null)
                return false;
        } else if (!max.equals(other.max))
            return false;
        if (min == null) {
            if (other.min != null)
                return false;
        } else if (!min.equals(other.min))
            return false;
        return true;
    }

}
