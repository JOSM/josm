//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.Date;

import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.PrimaryDateParser;

public class WayPoint extends WithAttributes implements Comparable<WayPoint>
{
    public double time;
    public Color customColoring;
    public boolean drawLine;
    public int dir;

    private static ThreadLocal<PrimaryDateParser> dateParser = new ThreadLocal<PrimaryDateParser>() {
        @Override protected PrimaryDateParser initialValue() {
            return new PrimaryDateParser();
        }
    };

    private final CachedLatLon coor;

    public final LatLon getCoor() {
        return coor;
    }

    public final EastNorth getEastNorth() {
        return coor.getEastNorth();
    }

    public WayPoint(LatLon ll) {
        coor = new CachedLatLon(ll);
    }

    @Override
    public String toString() {
        return "WayPoint (" + (attr.containsKey("name") ? attr.get("name") + ", " :"") + coor.toString() + ", " + attr + ")";
    }

    /**
     * Convert the time stamp of the waypoint into seconds from the epoch
     */
    public void setTime() {
        if(attr.containsKey("time")) {
            try {
                time = dateParser.get().parse(attr.get("time").toString()).getTime() / 1000.; /* ms => seconds */
            } catch(Exception e) {
                time = 0;
            }
        }
    }

    public int compareTo(WayPoint w)
    {
        return Double.compare(time, w.time);
    }

    public Date getTime() {
        return new Date((long) (time * 1000));
    }
}
