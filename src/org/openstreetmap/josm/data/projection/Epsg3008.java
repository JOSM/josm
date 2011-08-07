// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;

/**
 * SWEREF99 13 30 projection. Based on data from spatialreference.org.
 * http://spatialreference.org/ref/epsg/3008/
 *
 * @author Hanno Hecker
 */
public class Epsg3008 extends AbstractProjection {

    public Epsg3008() {
        ellps = Ellipsoid.GRS80;
        proj = new TransverseMercator(ellps);
        datum = GRS80Datum.INSTANCE;
        lon_0 = 13.5;
        x_0 = 150000;
    }
    
    @Override
    public String toString() {
        return tr("SWEREF99 13 30 / EPSG:3008 (Sweden)");
    }

    @Override
    public Integer getEpsgCode() {
        return 3008;
    }

    @Override
    public int hashCode() {
        return toCode().hashCode();
    }

    @Override
    public String getCacheDirectoryName() {
        return "epsg"+ getEpsgCode();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(55.2, 12.1),     // new LatLon(-90.0, -180.0),
                new LatLon(62.26, 14.65));  // new LatLon(90.0, 180.0));
    }

}
