// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import java.awt.geom.Rectangle2D;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;

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
    public LatLon min, max;

    /**
     * Construct bounds out of two points
     */
    public Bounds(LatLon min, LatLon max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Construct bounds that span the whole world.
     */
    public Bounds() {
        min = new LatLon(-Projection.MAX_LAT, -Projection.MAX_LON);
        max = new LatLon(Projection.MAX_LAT, Projection.MAX_LON);
    }

    @Override public String toString() {
        return "Bounds["+min.lat()+","+min.lon()+","+max.lat()+","+max.lon()+"]";
    }

    /**
     * @return Center of the bounding box.
     */
    public LatLon center() {
        // FIXME: not sure whether this calculation is right; maybe there is some
        // more complex calculation needed to get a center of a spherical
        // dimension?
        return new LatLon((min.lat()+max.lat())/2, (min.lon()+max.lon())/2);
    }

    /**
     * Extend the bounds if necessary to include the given point.
     */
    public void extend(LatLon ll) {
        if (ll.lat() < min.lat() || ll.lon() < min.lon())
            min = new LatLon(Math.min(ll.lat(), min.lat()), Math.min(ll.lon(), min.lon()));
        if (ll.lat() > max.lat() || ll.lon() > max.lon())
            max = new LatLon(Math.max(ll.lat(), max.lat()), Math.max(ll.lon(), max.lon()));
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

}
