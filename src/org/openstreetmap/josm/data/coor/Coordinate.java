// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import java.io.Serializable;

/**
 * Base class of points of both coordinate system.
 * 
 * The variables are default package protected to allow routines in the data package
 * to access them directly.
 * 
 * As the class itself is package protected too, it is not visible outside of the data
 * package. Routines there should only use LatLon or EastNorth
 *
 * @author imi
 */ 
abstract class Coordinate implements Serializable {

	/**
	 * Either easting or latitude
	 */
	final double x;
	/**
	 * Either northing or longitude
	 */
	final double y;

	/**
	 * Construct the point with latitude / longitude values.
	 * The x/y values are left uninitialized.
	 * 
	 * @param lat Latitude of the point.
	 * @param lon Longitude of the point.
	 */
	Coordinate(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * Return the squared distance of the northing/easting values between 
	 * this and the argument.
	 *
	 * @param other The other point to calculate the distance to.
	 * @return The square of the distance between this and the other point,
	 * 		regarding to the x/y values.
	 */
	public double distance(Coordinate other) {
		return (x-other.x)*(x-other.x)+(y-other.y)*(y-other.y);
	}

	@Override public boolean equals(Object obj) {
		return obj instanceof Coordinate ? x == ((Coordinate)obj).x && ((Coordinate)obj).y == y : false;
	}

	@Override public int hashCode() {
		return (int)(x*65536+y*4096);
	}
}
