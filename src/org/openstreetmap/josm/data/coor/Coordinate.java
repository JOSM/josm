// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import java.awt.geom.Point2D;
import java.io.Serializable;

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
 * @author imi
 */
abstract class Coordinate extends Point2D implements Serializable {

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp;
        temp = java.lang.Double.doubleToLongBits(x);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = java.lang.Double.doubleToLongBits(y);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        Coordinate other = (Coordinate) obj;
        if (java.lang.Double.doubleToLongBits(x) != java.lang.Double.doubleToLongBits(other.x))
            return false;
        if (java.lang.Double.doubleToLongBits(y) != java.lang.Double.doubleToLongBits(other.y))
            return false;
        return true;
    }
}
