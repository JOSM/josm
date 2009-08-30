// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * This is a simple data class for "rectangular" areas of the world, given in
 * lat/lon min/max values.
 *
 * @author imi
 */
public class ProjectionBounds {
    /**
     * The minimum and maximum coordinates.
     */
    public EastNorth min, max;

    /**
     * Construct bounds out of two points
     */
    public ProjectionBounds(EastNorth min, EastNorth max) {
        this.min = min;
        this.max = max;
    }
    public ProjectionBounds(EastNorth p) {
        this.min = p;
        this.max = p;
    }
    public ProjectionBounds(EastNorth center, double east, double north) {
        this.min = new EastNorth(center.east()-east/2.0, center.north()-north/2.0);
        this.max = new EastNorth(center.east()+east/2.0, center.north()+north/2.0);
    }
    public void extend(EastNorth e)
    {
        if (e.east() < min.east() || e.north() < min.north())
            min = new EastNorth(Math.min(e.east(), min.east()), Math.min(e.north(), min.north()));
        if (e.east() > max.east() || e.north() > max.north())
            max = new EastNorth(Math.max(e.east(), max.east()), Math.max(e.north(), max.north()));
    }
    public EastNorth getCenter()
    {
        return min.getCenter(max);
    }

    @Override public String toString() {
        return "ProjectionBounds["+min.east()+","+min.north()+","+max.east()+","+max.north()+"]";
    }
}
