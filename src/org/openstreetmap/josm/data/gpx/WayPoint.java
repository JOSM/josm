// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

/**
 * A point in the GPX data
 * @since 12167 implements ILatLon
 */
public class WayPoint extends WithAttributes implements Comparable<WayPoint>, TemplateEngineDataProvider, ILatLon {

    /**
     * The seconds (not milliseconds!) since 1970-01-01 00:00 UTC
     */
    public double time;
    /**
     * The color to draw the segment before this point in
     * @see #drawLine
     */
    public Color customColoring;
    /**
     * <code>true</code> indicates that the line before this point should be drawn
     */
    public boolean drawLine;
    /**
     * The direction of the line before this point. Used as cache to speed up drawing. Should not be relied on.
     */
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
        eastNorthCacheKey = p.eastNorthCacheKey;
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
    private Object eastNorthCacheKey;

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

    @Override
    public double lon() {
        return lon;
    }

    @Override
    public double lat() {
        return lat;
    }

    @Override
    public final EastNorth getEastNorth(Projecting projecting) {
        Object newCacheKey = projecting.getCacheKey();
        if (Double.isNaN(east) || Double.isNaN(north) || !Objects.equals(newCacheKey, this.eastNorthCacheKey)) {
            // projected coordinates haven't been calculated yet,
            // so fill the cache of the projected waypoint coordinates
            EastNorth en = projecting.latlon2eastNorth(this);
            this.east = en.east();
            this.north = en.north();
            this.eastNorthCacheKey = newCacheKey;
        }
        return new EastNorth(east, north);
    }

    @Override
    public String toString() {
        return "WayPoint (" + (attr.containsKey(GPX_NAME) ? get(GPX_NAME) + ", " : "") + getCoor() + ", " + attr + ')';
    }

    /**
     * Sets the {@link #time} field as well as the {@link #PT_TIME} attribute to the specified time.
     *
     * @param time the time to set
     * @since 9383
     */
    public void setTime(Date time) {
        this.time = time.getTime() / 1000.;
        this.attr.put(PT_TIME, time);
    }

    /**
     * Convert the time stamp of the waypoint into seconds from the epoch.
     *
     * @deprecated call {@link #setTimeFromAttribute()} directly if you need this
     */
    @Deprecated
    public void setTime() {
        setTimeFromAttribute();
    }

    /**
     * Sets the {@link #time} field as well as the {@link #PT_TIME} attribute to the specified time.
     *
     * @param ts seconds from the epoch
     * @since 13210
     */
    public void setTime(long ts) {
        setTimeInMillis(ts*1000);
    }

    /**
     * Sets the {@link #time} field as well as the {@link #PT_TIME} attribute to the specified time.
     *
     * @param ts milliseconds from the epoch
     * @since 14434
     */
    public void setTimeInMillis(long ts) {
        this.time = ts / 1000.;
        this.attr.put(PT_TIME, new Date(ts));
    }

    /**
     * Convert the time stamp of the waypoint into seconds from the epoch
     * @return The parsed time if successful, or {@code null}
     * @since 9383
     */
    public Date setTimeFromAttribute() {
        if (attr.containsKey(PT_TIME)) {
            final Object obj = get(PT_TIME);
            if (obj instanceof Date) {
                final Date date = (Date) obj;
                time = date.getTime() / 1000.;
                return date;
            } else if (obj == null) {
                Logging.info("Waypoint {0} value unset", PT_TIME);
            } else {
                Logging.warn("Unsupported waypoint {0} value: {1}", PT_TIME, obj);
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
