// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Represents a "rectangular" area of the world, given in lat/lon min/max values.
 *
 * @since 17703
 */
public interface IBounds {

    /**
     * Gets the point that has both the minimal lat and lon coordinate
     *
     * @return The point
     */
    default ILatLon getMin() {
        return new LatLon(getMinLat(), getMinLon());
    }

    /**
     * Returns min latitude of bounds. Efficient shortcut for {@code getMin().lat()}.
     *
     * @return min latitude of bounds.
     */
    double getMinLat();

    /**
     * Returns min longitude of bounds. Efficient shortcut for {@code getMin().lon()}.
     *
     * @return min longitude of bounds.
     */
    double getMinLon();

    /**
     * Gets the point that has both the maximum lat and lon coordinate
     *
     * @return The point
     */
    default ILatLon getMax() {
        return new LatLon(getMaxLat(), getMaxLon());
    }

    /**
     * Returns max latitude of bounds. Efficient shortcut for {@code getMax().lat()}.
     *
     * @return max latitude of bounds.
     */
    double getMaxLat();

    /**
     * Returns max longitude of bounds. Efficient shortcut for {@code getMax().lon()}.
     *
     * @return max longitude of bounds.
     */
    double getMaxLon();

    /**
     * Returns center of the bounding box.
     *
     * @return Center of the bounding box.
     */
    ILatLon getCenter();

    /**
     * Determines if the given point {@code ll} is within these bounds.
     * <p>
     * Points with unknown coordinates are always outside the coordinates.
     *
     * @param ll The lat/lon to check
     * @return {@code true} if {@code ll} is within these bounds, {@code false} otherwise
     */
    default boolean contains(ILatLon ll) {
        return getMinLon() <= ll.lon() && ll.lon() <= getMaxLon()
                && getMinLat() <= ll.lat() && ll.lat() <= getMaxLat();
    }

    /**
     * Tests, whether the bbox {@code b} lies completely inside this bbox.
     *
     * @param b bounding box
     * @return {@code true} if {@code b} lies completely inside this bbox
     */
    default boolean contains(IBounds b) {
        return getMinLon() <= b.getMinLon() && getMaxLon() >= b.getMaxLon()
                && getMinLat() <= b.getMinLat() && getMaxLat() >= b.getMaxLat();
    }

    /**
     * The two bounds intersect? Compared to java Shape.intersects, if does not use
     * the interior but the closure. ("&gt;=" instead of "&gt;")
     *
     * @param b other bounds
     * @return {@code true} if the two bounds intersect
     */
    default boolean intersects(IBounds b) {
        return getMinLon() <= b.getMaxLon() && getMaxLon() >= b.getMinLon()
                && getMinLat() <= b.getMaxLat() && getMaxLat() >= b.getMinLat();
    }

    /**
     * Returns the bounds width.
     *
     * @return the bounds width
     */
    double getHeight();

    /**
     * Returns the bounds width.
     *
     * @return the bounds width
     */
    double getWidth();

    /**
     * Gets the area of this bounds (in lat/lon space)
     *
     * @return The area
     */
    default double getArea() {
        return getWidth() * getHeight();
    }

    /**
     * Determines if the bbox covers a part of the planet surface.
     *
     * @return true if the bbox covers a part of the planet surface.
     * Height and width must be non-negative, but may (both) be 0.
     */
    default boolean isValid() {
        return true;
    }

    /**
     * Determines if this Bounds object crosses the 180th Meridian.
     * See http://wiki.openstreetmap.org/wiki/180th_meridian
     *
     * @return true if this Bounds object crosses the 180th Meridian.
     */
    default boolean crosses180thMeridian() {
        return false;
    }
}
