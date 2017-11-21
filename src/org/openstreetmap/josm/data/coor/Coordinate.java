// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import java.io.Serializable;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.BBox;

/**
 * Base class of points of both coordinate systems.
 *
 * The variables are default package protected to allow routines in the
 * data package to access them directly.
 *
 * As the class itself is package protected too, it is not visible
 * outside of the data package. Routines there should only use LatLon or
 * EastNorth.
 *
 * @since 6162
 */
abstract class Coordinate implements Serializable {

    protected final double x;
    protected final double y;

    /**
     * Construct the point with latitude / longitude values.
     *
     * @param x X coordinate of the point.
     * @param y Y coordinate of the point.
     */
    Coordinate(double x, double y) {
        this.x = x; this.y = y;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    /**
     * Returns the euclidean distance from this {@code Coordinate} to a specified {@code Coordinate}.
     *
     * @param coor the specified coordinate to be measured against this {@code Coordinate}
     * @return the euclidean distance from this {@code Coordinate} to a specified {@code Coordinate}
     * @since 6166
     */
    protected final double distance(final Coordinate coor) {
        return distance(coor.x, coor.y);
    }

    /**
     * Returns the euclidean distance from this {@code Coordinate} to a specified coordinate.
     *
     * @param px the X coordinate of the specified point to be measured against this {@code Coordinate}
     * @param py the Y coordinate of the specified point to be measured against this {@code Coordinate}
     * @return the euclidean distance from this {@code Coordinate} to a specified coordinate
     * @since 6166
     */
    public final double distance(final double px, final double py) {
        final double dx = this.x-px;
        final double dy = this.y-py;
        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Returns the square of the euclidean distance from this {@code Coordinate} to a specified {@code Coordinate}.
     *
     * @param coor the specified coordinate to be measured against this {@code Coordinate}
     * @return the square of the euclidean distance from this {@code Coordinate} to a specified {@code Coordinate}
     * @since 6166
     */
    protected final double distanceSq(final Coordinate coor) {
        return distanceSq(coor.x, coor.y);
    }

    /**
     * Returns the square of euclidean distance from this {@code Coordinate} to a specified coordinate.
     *
     * @param px the X coordinate of the specified point to be measured against this {@code Coordinate}
     * @param py the Y coordinate of the specified point to be measured against this {@code Coordinate}
     * @return the square of the euclidean distance from this {@code Coordinate} to a specified coordinate
     * @since 6166
     */
    public final double distanceSq(final double px, final double py) {
        final double dx = this.x-px;
        final double dy = this.y-py;
        return dx*dx + dy*dy;
    }

    /**
     * Creates bbox around this coordinate. Coordinate defines
     * center of bbox, its edge will be 2*r.
     *
     * @param r size
     * @return BBox around this coordinate
     * @since 6203
     * @deprecated use {@link BBox#BBox(double, double, double)} instead
     */
    @Deprecated
    public BBox toBBox(final double r) {
        return new BBox(x, y, r);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Coordinate that = (Coordinate) obj;
        return Double.compare(that.x, x) == 0 &&
               Double.compare(that.y, y) == 0;
    }
}
