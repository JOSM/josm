// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.coor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;

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
        super(Main.proj.eastNorth2latlon(eastNorth));
        proj = Main.proj;
        this.eastNorth = eastNorth;
    }

    public final void setCoor(LatLon coor) {
        setLocation(coor.lon(), coor.lat());
        proj = null;
    }

    public final void setEastNorth(EastNorth eastNorth) {
        proj = Main.proj;
        this.eastNorth = eastNorth;
        LatLon l = proj.eastNorth2latlon(eastNorth);
        setLocation(l.lon(), l.lat());
    }

    public final EastNorth getEastNorth() {
        if(proj != Main.proj)
        {
            proj = Main.proj;
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
