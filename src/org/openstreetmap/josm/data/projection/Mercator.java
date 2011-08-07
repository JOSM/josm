// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.WGS84Datum;

/**
 * Mercator Projection
 *
 * The center of the mercator projection is always the 0 grad
 * coordinate.
 *
 * See also USGS Bulletin 1532
 * (http://egsc.usgs.gov/isb/pubs/factsheets/fs08799.html)
 *
 * @author imi
 */
public class Mercator extends AbstractProjection {

    public Mercator() {
        ellps = Ellipsoid.WGS84;
        datum = WGS84Datum.INSTANCE;
        proj = new org.openstreetmap.josm.data.projection.proj.Mercator();
    }

    @Override 
    public String toString() {
        return tr("Mercator");
    }

    @Override
    public Integer getEpsgCode() {
        /* initially they used 3785 but that has been superseded, 
         * see http://www.epsg-registry.org/ */
        return 3857;
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode(); // we have no variables
    }

    @Override
    public String getCacheDirectoryName() {
        return "mercator";
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(-85.05112877980659, -180.0),
                new LatLon(85.05112877980659, 180.0));
    }

}
