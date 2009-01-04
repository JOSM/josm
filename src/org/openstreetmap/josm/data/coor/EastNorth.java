// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

/**
 * Northing, Easting of the projected coordinates.
 *
 * This class is immutable.
 *
 * @author Imi
 */
public class EastNorth extends Coordinate {

    public EastNorth(double east, double north) {
        super(east,north);
    }

    public double east() {
        return x;
    }

    public double north() {
        return y;
    }

    public EastNorth add(double dx, double dy) {
        return new EastNorth(x+dx, y+dy);
    }

    public EastNorth interpolate(EastNorth en2, double proportion) {
        return new EastNorth(this.x + proportion * (en2.x - this.x),
            this.y + proportion * (en2.y - this.y));
    }

    /**
     * Returns the heading, in radians, that you have to use to get from
     * this EastNorth to another. Heading is mapped into [0, 2pi)
     *
     * @param other the "destination" position
     * @return heading
     */
    public double heading(EastNorth other) {
        double hd = Math.atan2(other.east() - east(), other.north() - north());
        if(hd < 0) hd = 2 * Math.PI + hd;
        return hd;
    }

    public EastNorth sub(EastNorth en) {
        return new EastNorth(en.east() - east(), en.north() - north());
    }

    /**
     * Returns an EastNorth representing the this EastNorth rotated around
     * a given EastNorth by a given angle
     * @param pivot the center of the rotation
     * @param angle the angle of the rotation
     * @return EastNorth rotated object
     */
    public EastNorth rotate(EastNorth pivot, double angle) {
        double cosPhi = Math.cos(angle);
        double sinPhi = Math.sin(angle);
        double x = east() - pivot.east();
        double y = north() - pivot.north();
        double nx =  cosPhi * x + sinPhi * y + pivot.east();
        double ny = -sinPhi * x + cosPhi * y + pivot.north();
        return new EastNorth(nx, ny);
    }

    @Override public String toString() {
        return "EastNorth[e="+x+", n="+y+"]";
    }

    /**
     * Compares two EastNorth values
     *
     * @return true if "x" and "y" values are within 1E-6 of each other
     */
    public boolean equalsEpsilon(EastNorth other, double e) {
        return (Math.abs(x - other.x) < e && Math.abs(y - other.y) < e);
    }
}
