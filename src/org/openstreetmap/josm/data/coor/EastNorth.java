// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

/**
 * Northern, Easting of the projected coordinates.
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
		return new EastNorth(this.x + proportion * (en2.x - this.x),this.y + proportion * (en2.y - this.y));
	}
	
	@Override public String toString() {
		return "EastNorth[e="+x+", n="+y+"]";
	}
}
