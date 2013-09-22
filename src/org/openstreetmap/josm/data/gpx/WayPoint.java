// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.PrimaryDateParser;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

public class WayPoint extends WithAttributes implements Comparable<WayPoint>, TemplateEngineDataProvider {

    private static ThreadLocal<PrimaryDateParser> dateParser = new ThreadLocal<PrimaryDateParser>() {
        @Override protected PrimaryDateParser initialValue() {
            return new PrimaryDateParser();
        }
    };

    public double time;
    public Color customColoring;
    public boolean drawLine;
    public int dir;

    public WayPoint(WayPoint p) {
        attr.putAll(p.attr);
        lat = p.lat;
        lon = p.lon;
        east = p.east;
        north = p.north;
        time = p.time;
        customColoring = p.customColoring;
        drawLine = p.drawLine;
        dir = p.dir;
    }

    public WayPoint(LatLon ll) {
        lat = ll.lat();
        lon = ll.lon();
    }

    /*
     * We "inline" lat/lon, rather than usinga LatLon internally => reduces memory overhead. Relevant
     * because a lot of GPX waypoints are created when GPS tracks are downloaded from the OSM server.
     */
    private final double lat;
    private final double lon;

    /*
     * internal cache of projected coordinates
     */
    private double east = Double.NaN;
    private double north = Double.NaN;

    /**
     * Invalidate the internal cache of east/north coordinates.
     */
    public void invalidateEastNorthCache() {
        this.east = Double.NaN;
        this.north = Double.NaN;
    }

    public final LatLon getCoor() {
        return new LatLon(lat,lon);
    }

    /**
     * <p>Replies the projected east/north coordinates.</p>
     *
     * <p>Uses the {@link Main#getProjection() global projection} to project the lan/lon-coordinates.
     * Internally caches the projected coordinates.</p>
     *
     * <p><strong>Caveat:</strong> doesn't listen to projection changes. Clients must
     * {@link #invalidateEastNorthCache() invalidate the internal cache}.</p>
     *
     * @return the east north coordinates or {@code null}
     * @see #invalidateEastNorthCache()
     */
    public final EastNorth getEastNorth() {
        if (Double.isNaN(east) || Double.isNaN(north)) {
            // projected coordinates haven't been calculated yet,
            // so fill the cache of the projected waypoint coordinates
            EastNorth en = Projections.project(new LatLon(lat, lon));
            this.east = en.east();
            this.north = en.north();
        }
        return new EastNorth(east, north);
    }

    @Override
    public String toString() {
        return "WayPoint (" + (attr.containsKey("name") ? attr.get("name") + ", " :"") + getCoor().toString() + ", " + attr + ")";
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

    @Override
    public int compareTo(WayPoint w) {
        return Double.compare(time, w.time);
    }

    public Date getTime() {
        return new Date((long) (time * 1000));
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (!special)
            return attr.get(name);
        else
            return null;
    }

    @Override
    public boolean evaluateCondition(Match condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTemplateKeys() {
        return new ArrayList<String>(attr.keySet());
    }
}
