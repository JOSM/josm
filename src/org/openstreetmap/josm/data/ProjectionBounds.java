// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import java.awt.geom.Rectangle2D;

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
    public ProjectionBounds(EastNorth center, double east, double north) {
        this.min = new EastNorth(center.east()-east/2.0, center.north()-north/2.0);
        this.max = new EastNorth(center.east()+east/2.0, center.north()+north/2.0);
    }
    public void extend(EastNorth e)
    {
        if (e.east() < min.east() || e.north() < min.north())
            min = e;
        else if (e.east() > max.east() || e.north() > max.north())
            max = e;
    }
    public EastNorth getCenter()
    {
        return  new EastNorth(min.east()/2+max.east()/2, min.north()/2+max.north()/2);
    }
}
