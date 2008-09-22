// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import java.awt.geom.Point2D;

/**
 * Base class of points of both coordinate systems.
 * 
 * The variables are default package protected to allow routines in the data package
 * to access them directly.
 * 
 * As the class itself is package protected too, it is not visible outside of the data
 * package. Routines there should only use LatLon or EastNorth
 *
 * @author imi
 */ 
abstract class Coordinate extends Point2D {

    protected double x;
    protected double y;
    
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
    
    public void setLocation (double x, double y) {
        this.x = x; 
        this.y = y;
    }

}
