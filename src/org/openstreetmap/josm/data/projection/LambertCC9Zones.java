//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;

/**
 * Lambert Conic Conform 9 Zones projection as specified by the IGN
 * in this document http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
 * @author Pieren
 *
 */
public class LambertCC9Zones extends AbstractProjection {

    /**
     * France is divided in 9 Lambert projection zones, CC42 to CC50.
     */
    public static final double cMaxLatZonesRadian = Math.toRadians(51.1);

    public static final double cMinLatZonesDegree = 41.0;

    public static final double cMaxOverlappingZones = 1.5;

    public static final int DEFAULT_ZONE = 0;

    private final int layoutZone;

    public LambertCC9Zones() {
        this(DEFAULT_ZONE);
    }

    public LambertCC9Zones(final int layoutZone) {
        ellps = Ellipsoid.GRS80;
        datum = GRS80Datum.INSTANCE;
        this.layoutZone = layoutZone;
        x_0 = 1700000;
        y_0 = (layoutZone+1) * 1000000 + 200000;
        lon_0 = 3;
        if (proj == null) {
            proj = new LambertConformalConic();
        }
        try {
            proj.initialize(new ProjParameters() {{
                ellps = LambertCC9Zones.this.ellps;
                lat_0 = 42.0 + layoutZone;
                lat_1 = 41.25 + layoutZone;
                lat_2 = 42.75 + layoutZone;
            }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return tr("Lambert CC9 Zone (France)");
    }

    public static int north2ZoneNumber(double north) {
        int nz = (int)(north /1000000) - 1;
        if (nz < 0) return 0;
        else if (nz > 8) return 8;
        else return nz;
    }

    @Override
    public Integer getEpsgCode() {
        return 3942+layoutZone; //CC42 is EPSG:3942 (up to EPSG:3950 for CC50)
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode()+layoutZone; // our only real variable
    }

    @Override
    public String getCacheDirectoryName() {
        return "lambert";
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        double medLatZone = cMinLatZonesDegree + (layoutZone+1);
        return new Bounds(
                new LatLon(Math.max(medLatZone - 1.0 - cMaxOverlappingZones, cMinLatZonesDegree), -5.5),
                new LatLon(Math.min(medLatZone + 1.0 + cMaxOverlappingZones, Math.toDegrees(cMaxLatZonesRadian)), 10.2));
    }

    public int getLayoutZone() {
        return layoutZone;
    }

}
