// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.awt.Color;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.template_engine.TemplateEngineDataProvider;

/**
 * A point in the GPX data
 * @since 12167 implements ILatLon
 */
public class WayPoint extends WithAttributes implements Comparable<WayPoint>, TemplateEngineDataProvider, ILatLon {

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

    /*
     * We "inline" lat/lon, rather than using a LatLon internally => reduces memory overhead. Relevant
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
     * Constructs a new {@code WayPoint} from an existing one.
     *
     * Except for PT_TIME attribute, all attribute objects are shallow copied.
     * This means modification of attr objects will affect original and new {@code WayPoint}.
     *
     * @param p existing waypoint
     */
    public WayPoint(WayPoint p) {
        attr = new HashMap<>(0);
        attr.putAll(p.attr);
        lat = p.lat;
        lon = p.lon;
        east = p.east;
        north = p.north;
        eastNorthCacheKey = p.eastNorthCacheKey;
        customColoring = p.customColoring;
        drawLine = p.drawLine;
        dir = p.dir;
    }

    /**
     * Constructs a new {@code WayPoint} from lat/lon coordinates.
     * @param ll lat/lon coordinates
     */
    public WayPoint(LatLon ll) {
        attr = new HashMap<>(0);
        lat = ll.lat();
        lon = ll.lon();
    }

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
     * Sets the {@link #PT_TIME} attribute to the specified time.
     *
     * @param ts milliseconds from the epoch
     * @since 14434
     */
    public void setTimeInMillis(long ts) {
        setInstant(Instant.ofEpochMilli(ts));
    }

    /**
     * Sets the {@link #PT_TIME} attribute to the specified time.
     *
     * @param instant the time to set
     */
    public void setInstant(Instant instant) {
        attr.put(PT_TIME, instant);
    }

    @Override
    public int compareTo(WayPoint w) {
        return Long.compare(getTimeInMillis(), w.getTimeInMillis());
    }

    /**
     * Returns the waypoint time in seconds since the epoch.
     *
     * @return the waypoint time
     */
    public double getTime() {
        return getTimeInMillis() / 1000.;
    }

    /**
     * Returns the waypoint time in milliseconds since the epoch.
     *
     * @return the waypoint time
     * @since 14456
     */
    public long getTimeInMillis() {
        Instant d = getInstant();
        return d == null ? 0 : d.toEpochMilli();
    }

    /**
     * Returns true if this waypoint has a time.
     *
     * @return true if a time is set, false otherwise
     * @since 14456
     */
    public boolean hasDate() {
        return attr.get(PT_TIME) instanceof Instant;
    }

    /**
     * Returns the waypoint instant.
     *
     * @return the instant associated with this waypoint
     * @since 14456
     */
    public Instant getInstant() {
        if (attr != null) {
            final Object obj = attr.get(PT_TIME);

            if (obj instanceof Instant) {
                return (Instant) obj;
            } else if (obj == null) {
                Logging.trace("Waypoint {0} value unset", PT_TIME);
            } else {
                Logging.warn("Unsupported waypoint {0} value: {1}", PT_TIME, obj);
            }
        }

        return null;
    }

    @Override
    public Object getTemplateValue(String name, boolean special) {
        if (special) {
            return null;
        } else if ("desc".equals(name)) {
            final Object value = get(name);
            return value instanceof String ? Utils.stripHtml(((String) value)) : value;
        } else {
            return get(name);
        }
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
        return Objects.hash(super.hashCode(), lat, lon, getTimeInMillis());
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
            && getTimeInMillis() == other.getTimeInMillis();
    }
}
