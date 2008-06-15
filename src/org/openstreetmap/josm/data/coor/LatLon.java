// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.projection.Projection;
import java.text.NumberFormat;

/**
 * LatLon are unprojected latitude / longitude coordinates.
 * 
 * This class is immutable.
 * 
 * @author Imi
 */
public class LatLon extends Coordinate {

	public LatLon(double lat, double lon) {
		super(lon, lat);
	}

	public double lat() {
		return y;
	}

	public double lon() {
		return x;
	}

	/**
	 * @return <code>true</code> if the other point has almost the same lat/lon
	 * values, only differing by no more than
	 * 1 / {@link org.openstreetmap.josm.data.projection.Projection#MAX_SERVER_PRECISION MAX_SERVER_PRECISION}.
	 */
	public boolean equalsEpsilon(LatLon other) {
		final double p = 1/Projection.MAX_SERVER_PRECISION;
		return Math.abs(lat()-other.lat()) <= p && Math.abs(lon()-other.lon()) <= p;
	}

	/**
	 * @return <code>true</code>, if the coordinate is outside the world, compared
	 * by using lat/lon.
	 */
	public boolean isOutSideWorld() {
		return lat() < -Projection.MAX_LAT || lat() > Projection.MAX_LAT || 
			lon() < -Projection.MAX_LON || lon() > Projection.MAX_LON;
	}

	/**
	 * @return <code>true</code> if this is within the given bounding box.
	 */
	public boolean isWithin(Bounds b) {
		return lat() >= b.min.lat() && lat() <= b.max.lat() && lon() > b.min.lon() && lon() < b.max.lon();
	}
	
	/**
	 * Computes the distance between this lat/lon and another point on the earth.
	 * Uses spherical law of cosines formula, not Haversine.
	 * @param other the other point.
	 * @return distance in metres.
	 */
	public double greatCircleDistance(LatLon other) {
		return (Math.acos(
			Math.sin(Math.toRadians(lat())) * Math.sin(Math.toRadians(other.lat())) + 
		    Math.cos(Math.toRadians(lat()))*Math.cos(Math.toRadians(other.lat())) *
		                  Math.cos(Math.toRadians(other.lon()-lon()))) * 6378135);
	}
	
	/**
	 * Returns the heading, in radians, that you have to use to get from 
	 * this lat/lon to another.
	 * 
	 * @param other the "destination" position
	 * @return heading 
	 */
	public double heading(LatLon other) {
		double rv;
		if (other.lat() == lat()) {
			rv = (other.lon()>lon() ? Math.PI / 2 : Math.PI * 3 / 2);
		} else {
			rv = Math.atan((other.lon()-lon())/(other.lat()-lat()));
			if (rv < 0) rv += Math.PI;
			if (other.lon() < lon()) rv += Math.PI;
		}
		return rv;
	}

	/**
	 * Returns this lat/lon pair in human-readable format.
	 * 
	 * @return String in the format "lat=1.23456°, lon=2.34567°"
	 */
	public String toDisplayString() {
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(5);
		return "lat=" + nf.format(lat()) + "°, lon=" + nf.format(lon()) + "°";
	}
	
	@Override public String toString() {
		return "LatLon[lat="+lat()+",lon="+lon()+"]";
    }
}
