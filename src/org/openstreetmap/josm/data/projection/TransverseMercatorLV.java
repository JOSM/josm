// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;

/**
 * LKS-92/ Latvia TM projection. Based on data from spatialreference.org.
 * http://spatialreference.org/ref/epsg/3059/
 *
 * @author Viesturs Zarins
 */
public class TransverseMercatorLV extends AbstractProjection {

    public TransverseMercatorLV() {
        ellps = Ellipsoid.GRS80;
        proj = new org.openstreetmap.josm.data.projection.proj.TransverseMercator(ellps);
        datum = GRS80Datum.INSTANCE;
        lon_0 = 24;
        x_0 = 500000;
        y_0 = -6000000;
        k_0 = 0.9996;
    }
    
    @Override 
    public String toString() {
        return tr("LKS-92 (Latvia TM)");
    }

    @Override
    public Integer getEpsgCode() {
        return 3059;
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
                new LatLon(-90.0, -180.0),
                new LatLon(90.0, 180.0));
    }
}
