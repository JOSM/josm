//License: GPLv2 or later
//Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import java.awt.Color;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

public class WayPoint extends WithAttributes implements Comparable<WayPoint>
{
    public final LatLon latlon;
    public final EastNorth eastNorth;
    public double time;
    public Color customColoring;
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
    public final static SimpleDateFormat GPXTIMEFMT =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    public final static SimpleDateFormat GPXTIMEFMT_nofrac =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat GPXTIMEFMT_tz =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    public final static SimpleDateFormat GPXTIMEFMT_tz_nofrac =
        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");

	private final static Pattern colontz = Pattern.compile(".*[+-][0-9][0-9]:[0-9][0-9]\\z"); 
	private final static Pattern colontzreplacement = Pattern.compile("([+-][0-9][0-9]):([0-9][0-9])\\z"); 

	public void setTime() {
        if (! attr.containsKey("time")) {
            return;
        }
        String timestring = attr.get("time").toString();
        
        /* make the string timzeone be conanonical - unfortunately the allowed timezone in a 
         * GPX is Z or +/-hh:mm whereas in simpledateformat it is +/-hhmm only (no colon) 
         * If no timezone is given, the time will be interpreted as local time by parse. */
        if (timestring.substring(timestring.length() - 1).equals("Z")) { 
        	timestring = timestring.substring(0, timestring.length() - 1) + "+0000";
        } else if (colontz.matcher(timestring).matches()) {
        	timestring = colontzreplacement.matcher(timestring).replaceFirst("$1$2");
        }
        Date d = GPXTIMEFMT_tz.parse(timestring, new ParsePosition(0));
        if (d == null) {
        	d = GPXTIMEFMT_tz_nofrac.parse(timestring, new ParsePosition(0));
        	if (d == null) {
        		/* try without a zimezone indication */
        		d = GPXTIMEFMT.parse(timestring, new ParsePosition(0));            	
        		if (d == null) {
        			d = GPXTIMEFMT_nofrac.parse(timestring, new ParsePosition(0));            	
        		}
               	// date has parsed in local time, and been adjusted to UTC by parse
            }
        }
        if (d != null /* parsing ok */) {
            time = d.getTime() / 1000.0; /* ms => seconds */
        }
}
    /**
     * Convert a time stamp of the waypoint from the <cmt> or <desc> field
     * into seconds from the epoch. Handles the date format as it is used by
     * Garmin handhelds. Does not overwrite an existing timestamp (!= 0.0).
     * A value of <time> fields overwrites values set with by method.
     * Does nothing if specified key does not exist or text cannot be parsed.
     *
     * @param key The key that contains the text to convert.
     */
    public void setGarminCommentTime(String key) {
        // do not overwrite time if already set
        if (time != 0.0) {
            return;
        }
        if (! attr.containsKey(key)) {
            return;
        }
        // example date format "18-AUG-08 13:33:03"
        SimpleDateFormat f = new SimpleDateFormat("dd-MMM-yy HH:mm:ss"); // Garmin wpts have no timezone
        Date d = f.parse(attr.get(key).toString(), new ParsePosition(0));
        if (d != null /* parsing OK */) {
            time = d.getTime() / 1000.0; /* ms => seconds */
        }
    }

    public int compareTo(WayPoint w)
    {
        return Double.compare(time, w.time);
    }
}
