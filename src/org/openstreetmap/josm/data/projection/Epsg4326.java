// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Directly use latitude / longitude values as x/y.
 *
 * @author imi
 */
public class Epsg4326 implements Projection {

    public EastNorth latlon2eastNorth(LatLon p) {
        return new EastNorth(p.lon(), p.lat());
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        return new LatLon(p.north(), p.east());
    }

    @Override public String toString() {
        return tr("WGS84 Geographic");
    }

    public String toCode() {
        return "EPSG:4326";
    }

    public String getCacheDirectoryName() {
        return "epsg4326";
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
        new LatLon(-90.0, -180.0),
        new LatLon(90.0, 180.0));
    }
}
