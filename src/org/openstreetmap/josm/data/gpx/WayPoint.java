//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

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
}
