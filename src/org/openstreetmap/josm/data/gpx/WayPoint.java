//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class WayPoint extends WithAttributes implements Comparable{
	
	public final LatLon latlon;
	public final EastNorth eastNorth;
	public double time;
	public Color speedLineColor;
	public boolean drawLine;
	public int dir;

	public WayPoint(LatLon ll) {
		latlon = ll; 
		eastNorth = Main.proj.latlon2eastNorth(ll);
	}

	@Override
	public String toString() {
		return "WayPoint (" + (attr.containsKey("name") ? attr.get("name") + ", " :"") + latlon.toString() + ", " + attr + ")";
	}
	
	/**
	 * Convert the time stamp of the waypoint into seconds from the epoch
	 */
	public void setTime () {
		if (! attr.containsKey("time")) {
			time = 0.0;
			return;
		}
		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"); // ignore timezone
		Date d = f.parse(attr.get("time").toString(), new ParsePosition(0));
		if (d == null /* failed to parse */) {
			time = 0.0;
		} else {
			time = d.getTime() / 1000.0; /* ms => seconds */
		}
	}

    public int compareTo(Object other){
        if(other instanceof WayPoint){
            WayPoint w = (WayPoint)other;
            return (int)time - (int)w.time;
        }
        return 0;
    }
}
