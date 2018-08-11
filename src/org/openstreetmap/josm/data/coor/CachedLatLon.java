// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import java.util.Objects;

import org.openstreetmap.josm.data.projection.Projecting;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;

/**
 * LatLon class that maintains a cache of projected EastNorth coordinates.
 *
 * This class is convenient to use, but has relatively high memory costs.
 * It keeps a pointer to the last known projection in order to detect projection changes.
 *
 * Node and WayPoint have another, optimized, cache for projected coordinates.
 */
public class CachedLatLon extends LatLon {

    private static final long serialVersionUID = 1L;

    private EastNorth eastNorth;
    private transient Object cacheKey;

    /**
     * Constructs a new {@code CachedLatLon}.
     * @param lat latitude
     * @param lon longitude
     */
    public CachedLatLon(double lat, double lon) {
        super(lat, lon);
    }

    /**
     * Constructs a new {@code CachedLatLon}.
     * @param coor lat/lon
     */
    public CachedLatLon(LatLon coor) {
        super(coor.lat(), coor.lon());
        cacheKey = null;
    }

    /**
     * Constructs a new {@code CachedLatLon}.
     * @param eastNorth easting/northing
     */
    public CachedLatLon(EastNorth eastNorth) {
        this(eastNorth, ProjectionRegistry.getProjection());
    }

    private CachedLatLon(EastNorth eastNorth, Projection projection) {
        super(projection.eastNorth2latlon(eastNorth));
        cacheKey = projection.getCacheKey();
        this.eastNorth = eastNorth;
    }

    /**
     * Replies the projected east/north coordinates.
     *
     * @return the internally cached east/north coordinates. null, if the globally defined projection is null
     */
    @Override
    public final EastNorth getEastNorth(Projecting projecting) {
        if (!Objects.equals(cacheKey, projecting.getCacheKey())) {
            cacheKey = projecting.getCacheKey();
            eastNorth = projecting.latlon2eastNorth(this);
        }
        return eastNorth;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, eastNorth);
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj))
            return false;
        CachedLatLon other = (CachedLatLon) obj;
        return Objects.equals(eastNorth, other.eastNorth);
    }

    @Override
    public String toString() {
        return "CachedLatLon[lat="+lat()+",lon="+lon()+']';
    }
}
