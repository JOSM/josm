package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;

/**
 * Belgian Lambert 72 projection as specified by the Belgian IGN
 * in this document: http://www.ngi.be/Common/Lambert2008/Transformation_Geographic_Lambert_FR.pdf
 * @author Don-vip
 *
 */
public class BelgianLambert1972 extends AbstractProjection {

    public BelgianLambert1972() {
        ellps = Ellipsoid.hayford;
        // 7 parameters transformation: http://www.eye4software.com/resources/datum/4313/
        datum = new SevenParameterDatum("Belgium Datum 72", null, ellps, -99.06, 53.32, -112.49, 0.419, -0.830, 1.885, -1);
        x_0 =  150000.013;
        y_0 = 5400088.438;
        lon_0 = convertDegreeMinuteSecond(4, 22, 2.952);
        proj = new LambertConformalConic();
        try {
            proj.initialize(new ProjParameters() {{
                ellps = BelgianLambert1972.this.ellps;
                lat_0 = 90.0;
                lat_1 = 49 + convertMinuteSecond(50, 0.00204);
                lat_2 = 51 + convertMinuteSecond(10, 0.00204);
            }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCacheDirectoryName() {
        return "belgianLambert1972";
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return new Bounds(
                new LatLon(49.51, 2.54),
                new LatLon(51.50, 6.40));
    }

    @Override
    public Integer getEpsgCode() {
        return 31370;
    }

    @Override
    public String toString() {
        return tr("Belgian Lambert 1972");
    }
}
