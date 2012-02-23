package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;

/**
 * Belgian Lambert 2008 projection as specified by the Belgian IGN
 * in this document: http://www.ngi.be/Common/Lambert2008/Transformation_Geographic_Lambert_FR.pdf
 * @author Don-vip 
 *
 */
public class BelgianLambert2008 extends AbstractProjection {

    public BelgianLambert2008() {
        ellps = Ellipsoid.GRS80;
        datum = GRS80Datum.INSTANCE;
        x_0 =  649328.0;
        y_0 = 665262.0;
        lon_0 = convertDegreeMinuteSecond(4, 21, 33.177);
        double lat_0 = convertDegreeMinuteSecond(50, 47, 52.134);
        double lat_1 = convertDegreeMinuteSecond(49, 50,  0);
        double lat_2 = convertDegreeMinuteSecond(51, 10,  0);
        proj = new LambertConformalConic();
        ((LambertConformalConic)proj).updateParameters2SP(ellps, lat_0, lat_1, lat_2);
    }
    
    @Override
    public String getCacheDirectoryName() {
        return "belgianLambert2008";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(49.51, 2.54),
                new LatLon(51.50, 6.40));
    }

    @Override
    public Integer getEpsgCode() {
        return 3812;
    }
    
    @Override
    public String toString() {
        return tr("Belgian Lambert 2008");
    }
}
