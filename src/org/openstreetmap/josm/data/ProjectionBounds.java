// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is a simple data class for "rectangular" areas of the world, given in
 * east/north min/max values.
 *
 * @author imi
 */
public class ProjectionBounds {
    /**
     * The minimum east coordinate.
     */
    public double minEast;
    /**
     * The minimum north coordinate.
     */
    public double minNorth;
    /**
     * The maximum east coordinate.
     */
    public double maxEast;
    /**
     * The minimum north coordinate.
     */
    public double maxNorth;

    /**
     * Construct bounds out of two points.
     * @param min min east/north
     * @param max max east/north
     */
    public ProjectionBounds(EastNorth min, EastNorth max) {
        this.minEast = min.east();
        this.minNorth = min.north();
        this.maxEast = max.east();
        this.maxNorth = max.north();
    }

    /**
     * Construct bounds out of a single point.
     * @param p east/north
     */
    public ProjectionBounds(EastNorth p) {
        this.minEast = this.maxEast = p.east();
        this.minNorth = this.maxNorth = p.north();
    }

    /**
     * Construct bounds out of a center point and east/north dimensions.
     * @param center center east/north
     * @param east east dimension
     * @param north north dimension
     */
    public ProjectionBounds(EastNorth center, double east, double north) {
        this.minEast = center.east()-east/2.0;
        this.minNorth = center.north()-north/2.0;
        this.maxEast = center.east()+east/2.0;
        this.maxNorth = center.north()+north/2.0;
    }

    /**
     * Construct bounds out of two points.
     * @param minEast min east
     * @param minNorth min north
     * @param maxEast max east
     * @param maxNorth max north
     */
    public ProjectionBounds(double minEast, double minNorth, double maxEast, double maxNorth) {
        this.minEast = minEast;
        this.minNorth = minNorth;
        this.maxEast = maxEast;
        this.maxNorth = maxNorth;
    }

    /**
     * Construct uninitialized bounds.
     * <p>
     * At least one call to {@link #extend(EastNorth)} or {@link #extend(ProjectionBounds)}
     * is required immediately after construction to initialize the {@code ProjectionBounds}
     * instance and make it valid.
     * <p>
     * Uninitialized {@code ProjectionBounds} must not be passed to other methods
     * or used in any way other than initializing it.
     */
    public ProjectionBounds() {
        this.minEast = Double.POSITIVE_INFINITY;
        this.minNorth = Double.POSITIVE_INFINITY;
        this.maxEast = Double.NEGATIVE_INFINITY;
        this.maxNorth = Double.NEGATIVE_INFINITY;
    }

    /**
     * Extends bounds to include point {@code e}.
     * @param e east/north to include
     */
    public void extend(EastNorth e) {
        if (e.east() < minEast) {
            minEast = e.east();
        }
        if (e.east() > maxEast) {
            maxEast = e.east();
        }
        if (e.north() < minNorth) {
            minNorth = e.north();
        }
        if (e.north() > maxNorth) {
            maxNorth = e.north();
        }
    }

    /**
     * Extends bounds to include bounds {@code b}.
     * @param b bounds to include
     * @since 11774
     */
    public void extend(ProjectionBounds b) {
        if (b.minEast < minEast) {
            minEast = b.minEast;
        }
        if (b.maxEast > maxEast) {
            maxEast = b.maxEast;
        }
        if (b.minNorth < minNorth) {
            minNorth = b.minNorth;
        }
        if (b.maxNorth > maxNorth) {
            maxNorth = b.maxNorth;
        }
    }

    /**
     * Returns the center east/north.
     * @return the center east/north
     */
    public EastNorth getCenter() {
        return new EastNorth((minEast + maxEast) / 2.0, (minNorth + maxNorth) / 2.0);
    }

    @Override
    public String toString() {
        return "ProjectionBounds["+minEast+','+minNorth+','+maxEast+','+maxNorth+']';
    }

    /**
     * The two bounds intersect? Compared to java Shape.intersects, if does not use
     * the interior but the closure. ("&gt;=" instead of "&gt;")
     * @param b projection bounds
     * @return {@code true} if the two bounds intersect
     */
    public boolean intersects(ProjectionBounds b) {
        return b.maxEast >= minEast &&
        b.maxNorth >= minNorth &&
        b.minEast <= maxEast &&
        b.minNorth <= maxNorth;
    }

    /**
     * Check, if a point is within the bounds.
     * @param en the point
     * @return true, if <code>en</code> is within the bounds
     */
    public boolean contains(EastNorth en) {
        return minEast <= en.east() && en.east() <= maxEast &&
                minNorth <= en.north() && en.north() <= maxNorth;
    }

    /**
     * Returns the min east/north.
     * @return the min east/north
     */
    public EastNorth getMin() {
        return new EastNorth(minEast, minNorth);
    }

    /**
     * Returns the max east/north.
     * @return the max east/north
     */
    public EastNorth getMax() {
        return new EastNorth(maxEast, maxNorth);
    }

    /**
     * Determines if the bounds area is not null
     * @return {@code true} if the area is not null
     */
    public boolean hasExtend() {
        return !Utils.equalsEpsilon(minEast, maxEast) || !Utils.equalsEpsilon(minNorth, maxNorth);
    }

    /**
     * Computes the scale of this bounds with respect to the given width/height.
     * @param width the width
     * @param height the height
     * @return the computed scale
     */
    public double getScale(final int width, final int height) {
        // -20 to leave some border
        int w = width - 20;
        if (w < 20) {
            w = 20;
        }
        int h = height - 20;
        if (h < 20) {
            h = 20;
        }

        double scaleX = getDeltaEast() / w;
        double scaleY = getDeltaNorth() / h;
        return Math.max(scaleX, scaleY);
    }

    private double getDeltaNorth() {
        return maxNorth - minNorth;
    }

    private double getDeltaEast() {
        return maxEast - minEast;
    }
}
