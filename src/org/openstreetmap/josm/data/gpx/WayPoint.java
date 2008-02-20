//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class WayPoint extends WithAttributes {
	
	public final LatLon latlon;
	public final EastNorth eastNorth;

	public WayPoint(LatLon ll) {
		latlon = ll; 
		eastNorth = Main.proj.latlon2eastNorth(ll); 
	}

	@Override
	public String toString() {
		return "WayPoint (" + (attr.containsKey("name") ? attr.get("name") + ", " :"") + latlon.toString() + ", " + attr + ")";
	}
	
	/**
	 * convert the time stamp of ther waypoint into seconds from the epoch
	 * @return seconds
	 */
	public double time () {
		if (! attr.containsKey("time"))
			return 0.0;
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // ignore timezone
		Date d = f.parse(attr.get("time").toString(), new ParsePosition(0));
		if (d == null /* failed to parse */)
			return 0.0;
		return d.getTime() / 1000.0; /* ms => seconds */
	}

}
