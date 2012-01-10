package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;

/**
 * Lambert 93 projection as specified by the IGN
 * in this document http://professionnels.ign.fr/DISPLAY/000/526/702/5267026/NTG_87.pdf
 * @author Don-vip 
 *
 */
public class Lambert93 extends AbstractProjection {

    public Lambert93() {
        ellps = Ellipsoid.GRS80;
        datum = GRS80Datum.INSTANCE;
        x_0 =  700000;
        y_0 = 6600000;
        lon_0 = 3;
        double lat_0 = 46.50;
        double lat_1 = 44.00;
        double lat_2 = 49.00;
        proj = new LambertConformalConic();
        ((LambertConformalConic)proj).updateParameters2SP(ellps, lat_0, lat_1, lat_2);
    }
    
    @Override
    public String getCacheDirectoryName() {
        return "lambert93";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(41.0, -5.5),
                new LatLon(51.0, 10.2));
    }

    @Override
    public Integer getEpsgCode() {
        return 2154;
    }
    
    @Override
    public String toString() {
        return tr("Lambert 93 (France)");
    }
}
