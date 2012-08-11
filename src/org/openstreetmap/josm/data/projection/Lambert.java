// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.NTV2Datum;
import org.openstreetmap.josm.data.projection.datum.NTV2GridShiftFileWrapper;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.data.projection.proj.ProjParameters;

/**
 * Lambert conic conform 4 zones using the French geodetic system NTF.
 * This newer version uses the grid translation NTF<->RGF93 provided by IGN for a submillimetric accuracy.
 * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
 *
 * Source: http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
 * @author Pieren
 */
public class Lambert extends AbstractProjection {

    /**
     * Lambert I, II, III, and IV latitude origin
     */
    private static final double lat_0s[] = { 49.5, 46.8, 44.1, 42.165 };
    
    /**
     * Lambert I, II, III, and IV latitude of first standard parallel
     */
    private static final double lat_1s[] = { 
            convertDegreeMinuteSecond(48, 35, 54.682), 
            convertDegreeMinuteSecond(45, 53, 56.108),
            convertDegreeMinuteSecond(43, 11, 57.449),
            convertDegreeMinuteSecond(41, 33, 37.396)};
    
    /**
     * Lambert I, II, III, and IV latitude of second standard parallel
     */
    private static final double lat_2s[] = {
            convertDegreeMinuteSecond(50, 23, 45.282),
            convertDegreeMinuteSecond(47, 41, 45.652),
            convertDegreeMinuteSecond(44, 59, 45.938),
            convertDegreeMinuteSecond(42, 46, 3.588)};

    /**
     * Lambert I, II, III, and IV false east
     */
    private static final double x_0s[] = { 600000.0, 600000.0, 600000.0, 234.358 };
    
    /**
     * Lambert I, II, III, and IV false north
     */
    private static final double y_0s[] = { 200000.0, 200000.0, 200000.0, 185861.369 };
    
    /**
     * France is divided in 4 Lambert projection zones (1,2,3 + 4th for Corsica)
     */
    public static final double cMaxLatZone1Radian = Math.toRadians(57 * 0.9);
    public static final double cMinLatZone1Radian = Math.toRadians(46.1 * 0.9);// lowest latitude of Zone 4 (South Corsica)

    public static final double[][] zoneLimitsDegree = {
        {Math.toDegrees(cMaxLatZone1Radian), (53.5 * 0.9)}, // Zone 1  (reference values in grad *0.9)
        {(53.5 * 0.9), (50.5 * 0.9)}, // Zone 2
        {(50.5 * 0.9), (47.0 * 0.9)}, // Zone 3
        {(47.51963 * 0.9), Math.toDegrees(cMinLatZone1Radian)} // Zone 4
    };

    public static final double cMinLonZonesRadian = Math.toRadians(-4.9074074074074059 * 0.9);

    public static final double cMaxLonZonesRadian = Math.toRadians(10.2 * 0.9);

    /**
     *  Allow some extension beyond the theoretical limits
     */
    public static final double cMaxOverlappingZonesDegree = 1.5;

    public static final int DEFAULT_ZONE = 0;

    private int layoutZone;

    public Lambert() {
        this(DEFAULT_ZONE);
    }

    public Lambert(final int layoutZone) {
        if (layoutZone < 0 || layoutZone >= 4)
            throw new IllegalArgumentException();
        this.layoutZone = layoutZone;
        ellps = Ellipsoid.clarkeIGN;
        datum = new NTV2Datum("ntf_rgf93Grid", null, ellps, NTV2GridShiftFileWrapper.ntf_rgf93);
        x_0 = x_0s[layoutZone];
        y_0 = y_0s[layoutZone];
        lon_0 = 2.0 + 20.0 / 60 + 14.025 / 3600; // 0 grade Paris
        if (proj == null) {
            proj = new LambertConformalConic();
        }
        proj = new LambertConformalConic();
        try {
            proj.initialize(new ProjParameters() {{
                ellps = Lambert.this.ellps;
                lat_0 = lat_0s[layoutZone];
                lat_1 = lat_1s[layoutZone];
                lat_2 = lat_2s[layoutZone];
            }});
        } catch (ProjectionConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return tr("Lambert 4 Zones (France)");
    }

    @Override
    public Integer getEpsgCode() {
        return 27561+layoutZone;
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
        Bounds b= new Bounds(
                new LatLon(Math.max(zoneLimitsDegree[layoutZone][1] - cMaxOverlappingZonesDegree, Math.toDegrees(cMinLatZone1Radian)), Math.toDegrees(cMinLonZonesRadian)),
                new LatLon(Math.min(zoneLimitsDegree[layoutZone][0] + cMaxOverlappingZonesDegree, Math.toDegrees(cMaxLatZone1Radian)), Math.toDegrees(cMaxLonZonesRadian)),
                false);
        return b;
    }

    public int getLayoutZone() {
        return layoutZone;
    }

}
