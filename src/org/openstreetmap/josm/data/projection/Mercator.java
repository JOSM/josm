// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Implement Mercator Projection code, coded after documentation
 * from wikipedia.
 *
 * The center of the mercator projection is always the 0 grad
 * coordinate.
 *
 * See also USGS Bulletin 1532
 * (http://egsc.usgs.gov/isb/pubs/factsheets/fs08799.html)
 *
 * @author imi
 */
public class Mercator implements Projection {

    public EastNorth latlon2eastNorth(LatLon p) {
        return new EastNorth(
                p.lon()*Math.PI/180,
                Math.log(Math.tan(Math.PI/4+p.lat()*Math.PI/360)));
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        return new LatLon(
                Math.atan(Math.sinh(p.north()))*180/Math.PI,
                p.east()*180/Math.PI);
    }

    @Override public String toString() {
        return tr("Mercator");
    }

    public String toCode() {
        return "EPSG:3857"; /* TODO: Check if that is correct */
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    public String getCacheDirectoryName() {
        return "mercator";
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(-85.05112877980659, -180.0),
                new LatLon(85.05112877980659, 180.0));
    }

    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 km
        return 0.000158;
    }
}
