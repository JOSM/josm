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
import org.openstreetmap.josm.tools.UncheckedParseException;
import org.openstreetmap.josm.tools.date.DateUtils;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

public class WayPoint extends WithAttributes implements Comparable<WayPoint>, TemplateEngineDataProvider {

    /**
     * The seconds (not milliseconds!) since 1970-01-01 00:00 UTC
     */
    public double time;
    public Color customColoring;
    public boolean drawLine;
    public int dir;

    /**
     * Constructs a new {@code WayPoint} from an existing one.
     * @param p existing waypoint
     */
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

    /**
     * Constructs a new {@code WayPoint} from lat/lon coordinates.
     * @param ll lat/lon coordinates
     */
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

    /**
     * Returns the waypoint coordinates.
     * @return the waypoint coordinates
     */
    public final LatLon getCoor() {
        return new LatLon(lat, lon);
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
        return "WayPoint (" + (attr.containsKey(GPX_NAME) ? get(GPX_NAME) + ", " : "") + getCoor() + ", " + attr + ')';
    }

    /**
     * Sets the {@link #time} field as well as the {@link #PT_TIME} attribute to the specified time
     *
     * @param time the time to set
     * @since 9383
     */
    public void setTime(Date time) {
        this.time = time.getTime() / 1000.;
        this.attr.put(PT_TIME, DateUtils.fromDate(time));
    }

    /**
     * Convert the time stamp of the waypoint into seconds from the epoch
     */
    public void setTime() {
        setTimeFromAttribute();
    }

    /**
     * Convert the time stamp of the waypoint into seconds from the epoch
     * @return The parsed time if successful, or {@code null}
     * @since 9383
     */
    public Date setTimeFromAttribute() {
        if (attr.containsKey(PT_TIME)) {
            try {
                final Date time = DateUtils.fromString(get(PT_TIME).toString());
                this.time = time.getTime() / 1000.;
                return time;
            } catch (UncheckedParseException e) {
                Main.warn(e);
                time = 0;
            }
        }
        return null;
    }

    @Override
    public int compareTo(WayPoint w) {
        return Double.compare(time, w.time);
    }

    /**
     * Returns the waypoint time.
     * @return the waypoint time
     */
    public Date getTime() {
        return new Date((long) (time * 1000));
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (!special)
            return get(name);
        else
            return null;
    }

    @Override
    public boolean evaluateCondition(Match condition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getTemplateKeys() {
        return new ArrayList<>(attr.keySet());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        long temp = Double.doubleToLongBits(lat);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(lon);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(time);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || !super.equals(obj) || getClass() != obj.getClass())
            return false;
        WayPoint other = (WayPoint) obj;
        return Double.doubleToLongBits(lat) == Double.doubleToLongBits(other.lat)
            && Double.doubleToLongBits(lon) == Double.doubleToLongBits(other.lon)
            && Double.doubleToLongBits(time) == Double.doubleToLongBits(other.time);
    }
}
