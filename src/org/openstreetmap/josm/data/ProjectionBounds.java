// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * This is a simple data class for "rectangular" areas of the world, given in
 * east/north min/max values.
 *
 * @author imi
 */
public class ProjectionBounds {
    /**
     * The minimum and maximum coordinates.
     */
    public double minEast, minNorth, maxEast, maxNorth;

    /**
     * Construct bounds out of two points.
     */
    public ProjectionBounds(EastNorth min, EastNorth max) {
        this.minEast = min.east();
        this.minNorth = min.north();
        this.maxEast = max.east();
        this.maxNorth = max.north();
    }
    public ProjectionBounds(EastNorth p) {
        this.minEast = this.maxEast = p.east();
        this.minNorth = this.maxNorth = p.north();
    }
    public ProjectionBounds(EastNorth center, double east, double north) {
        this.minEast = center.east()-east/2.0;
        this.minNorth = center.north()-north/2.0;
        this.maxEast = center.east()+east/2.0;
        this.maxNorth = center.north()+north/2.0;
    }
    public ProjectionBounds(double minEast, double minNorth, double maxEast, double maxNorth) {
        this.minEast = minEast;
        this.minNorth = minNorth;
        this.maxEast = maxEast;
        this.maxNorth = maxNorth;
    }
    public void extend(EastNorth e)
    {
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
    public EastNorth getCenter()
    {
        return new EastNorth((minEast + maxEast) / 2.0, (minNorth + maxNorth) / 2.0);
    }

    @Override public String toString() {
        return "ProjectionBounds["+minEast+","+minNorth+","+maxEast+","+maxNorth+"]";
    }

    /**
     * The two bounds intersect? Compared to java Shape.intersects, if does not use
     * the interior but the closure. ("&gt;=" instead of "&gt;")
     */
    public boolean intersects(ProjectionBounds b) {
        return b.maxEast >= minEast &&
        b.maxNorth >= minNorth &&
        b.minEast <= maxEast &&
        b.minNorth <= maxNorth;
    }

    public EastNorth getMin() {
        return new EastNorth(minEast, minNorth);
    }

    public EastNorth getMax() {
        return new EastNorth(maxEast, maxNorth);
    }

}
