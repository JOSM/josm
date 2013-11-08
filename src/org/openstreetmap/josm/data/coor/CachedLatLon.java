// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;

/**
 * LatLon class that maintains a cache of projected EastNorth coordinates.
 *
 * This class is convenient to use, but has relatively high memory costs.
 * It keeps a pointer to the last known projection in order to detect projection
 * changes.
 *
 * Node and WayPoint have another, optimized, cache for projected coordinates.
 */
public class CachedLatLon extends LatLon {
    private EastNorth eastNorth;
    private Projection proj;

    public CachedLatLon(double lat, double lon) {
        super(lat, lon);
    }

    public CachedLatLon(LatLon coor) {
        super(coor.lat(), coor.lon());
        proj = null;
    }

    public CachedLatLon(EastNorth eastNorth) {
        super(Main.getProjection().eastNorth2latlon(eastNorth));
        proj = Main.getProjection();
        this.eastNorth = eastNorth;
    }

    /**
     * Replies the projected east/north coordinates.
     *
     * @return the internally cached east/north coordinates. null, if the globally defined projection is null
     */
    public final EastNorth getEastNorth() {
        if(proj != Main.getProjection())
        {
            proj = Main.getProjection();
            eastNorth = proj.latlon2eastNorth(this);
        }
        return eastNorth;
    }
    @Override public String toString() {
        return "CachedLatLon[lat="+lat()+",lon="+lon()+"]";
    }

    // Only for Node.get3892DebugInfo()
    public Projection getProjection() {
        return proj;
    }
}
