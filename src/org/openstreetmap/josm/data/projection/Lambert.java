// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;

/**
 * This class provides the two methods <code>latlon2eastNorth</code> and <code>eastNorth2latlon</code>
 * converting the JOSM LatLon coordinates in WGS84 system (GPS) to and from East North values in
 * the projection Lambert conic conform 4 zones using the French geodetic system NTF.
 * This newer version uses the grid translation NTF<->RGF93 provided by IGN for a submillimetric accuracy.
 * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
 * @author Pieren
 */
public class Lambert implements Projection, ProjectionSubPrefs {
    /**
     * Lambert I, II, III, and IV projection exponents
     */
    public static final double n[] = { 0.7604059656, 0.7289686274, 0.6959127966, 0.6712679322 };

    /**
     * Lambert I, II, III, and IV projection constants
     */
    public static final double c[] = { 11603796.98, 11745793.39, 11947992.52, 12136281.99 };

    /**
     * Lambert I, II, III, and IV false east
     */
    public static final double Xs[] = { 600000.0, 600000.0, 600000.0, 234.358 };

    /**
     * Lambert I, II, III, and IV false north
     */
    public static final double Ys[] = { 5657616.674, 6199695.768, 6791905.085, 7239161.542 };

    /**
     * Lambert I, II, III, and IV longitudinal offset to Greenwich meridian
     */
    public static final double lg0 = 0.04079234433198; // 2deg20'14.025"

    /**
     * precision in iterative schema
     */

    public static final double epsilon = 1e-11;

    /**
     * France is divided in 4 Lambert projection zones (1,2,3 + 4th for Corsica)
     */
    public static final double cMaxLatZone1Radian = Math.toRadians(57 * 0.9);
    public static final double cMinLatZone1Radian = Math.toRadians(46.1 * 0.9);// lowest latitude of Zone 4 (South Corsica)

    public static final double zoneLimitsDegree[][] = {
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

    private static int layoutZone = DEFAULT_ZONE;

    private static NTV2GridShiftFile ntf_rgf93Grid = null;

    public static NTV2GridShiftFile getNtf_rgf93Grid() {
        return ntf_rgf93Grid;
    }

    public Lambert() {
        if (ntf_rgf93Grid == null) {
            try {
                String gridFileName = "ntf_r93_b.gsb";
                InputStream is = Main.class.getResourceAsStream("/data/"+gridFileName);
                ntf_rgf93Grid = new NTV2GridShiftFile();
                ntf_rgf93Grid.loadGridShiftFile(is, false);
                //System.out.println("NTF<->RGF93 grid loaded.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param p  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Lambert Zone (ellipsoid Clark)
     * @throws IOException
     */
    public EastNorth latlon2eastNorth(LatLon p) {
        // translate ellipsoid GRS80 (WGS83) => Clark
        LatLon geo = WGS84_to_NTF(p);
        double lt = Math.toRadians(geo.lat()); // in radian
        double lg = Math.toRadians(geo.lon());

        // check if longitude and latitude are inside the French Lambert zones
        if (lt >= cMinLatZone1Radian && lt <= cMaxLatZone1Radian && lg >= cMinLonZonesRadian && lg <= cMaxLonZonesRadian)
            return ConicProjection(lt, lg, Xs[layoutZone], Ys[layoutZone], c[layoutZone], n[layoutZone]);
        return ConicProjection(lt, lg, Xs[0], Ys[0], c[0], n[0]);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        LatLon geo;
        geo = Geographic(p, Xs[layoutZone], Ys[layoutZone], c[layoutZone], n[layoutZone]);
        // translate geodetic system from NTF (ellipsoid Clark) to RGF93/WGS84 (ellipsoid GRS80)
        return NTF_to_WGS84(geo);
    }

    @Override public String toString() {
        return tr("Lambert 4 Zones (France)");
    }

    public String toCode() {
        return "EPSG:"+(27561+layoutZone);
    }

    public String getCacheDirectoryName() {
        return "lambert";
    }

    /**
     * Initializes from geographic coordinates. Note that reference ellipsoid
     * used by Lambert is the Clark ellipsoid.
     *
     * @param lat latitude in grad
     * @param lon longitude in grad
     * @param Xs  false east (coordinate system origin) in meters
     * @param Ys  false north (coordinate system origin) in meters
     * @param c   projection constant
     * @param n   projection exponent
     * @return EastNorth projected coordinates in meter
     */
    private EastNorth ConicProjection(double lat, double lon, double Xs, double Ys, double c, double n) {
        double eslt = Ellipsoid.clarke.e * Math.sin(lat);
        double l = Math.log(Math.tan(Math.PI / 4.0 + (lat / 2.0))
                * Math.pow((1.0 - eslt) / (1.0 + eslt), Ellipsoid.clarke.e / 2.0));
        double east = Xs + c * Math.exp(-n * l) * Math.sin(n * (lon - lg0));
        double north = Ys - c * Math.exp(-n * l) * Math.cos(n * (lon - lg0));
        return new EastNorth(east, north);
    }

    /**
     * Initializes from projected coordinates (conic projection). Note that
     * reference ellipsoid used by Lambert is Clark
     *
     * @param eastNorth projected coordinates pair in meters
     * @param Xs        false east (coordinate system origin) in meters
     * @param Ys        false north (coordinate system origin) in meters
     * @param c         projection constant
     * @param n         projection exponent
     * @return LatLon in degree
     */
    private LatLon Geographic(EastNorth eastNorth, double Xs, double Ys, double c, double n) {
        double dx = eastNorth.east() - Xs;
        double dy = Ys - eastNorth.north();
        double R = Math.sqrt(dx * dx + dy * dy);
        double gamma = Math.atan(dx / dy);
        double l = -1.0 / n * Math.log(Math.abs(R / c));
        l = Math.exp(l);
        double lon = lg0 + gamma / n;
        double lat = 2.0 * Math.atan(l) - Math.PI / 2.0;
        double delta = 1.0;
        while (delta > epsilon) {
            double eslt = Ellipsoid.clarke.e * Math.sin(lat);
            double nlt = 2.0 * Math.atan(Math.pow((1.0 + eslt) / (1.0 - eslt), Ellipsoid.clarke.e / 2.0) * l) - Math.PI
            / 2.0;
            delta = Math.abs(nlt - lat);
            lat = nlt;
        }
        return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon)); // in radian
    }

    /**
     * Translate latitude/longitude in WGS84, (ellipsoid GRS80) to Lambert
     * geographic, (ellipsoid Clark)
     * @throws IOException
     */
    private LatLon WGS84_to_NTF(LatLon wgs) {
        NTV2GridShift gs = new NTV2GridShift(wgs);
        if (ntf_rgf93Grid != null) {
            ntf_rgf93Grid.gridShiftReverse(gs);
            return new LatLon(wgs.lat()+gs.getLatShiftDegrees(), wgs.lon()+gs.getLonShiftPositiveEastDegrees());
        }
        return new LatLon(0,0);
    }

    private LatLon NTF_to_WGS84(LatLon ntf) {
        NTV2GridShift gs = new NTV2GridShift(ntf);
        if (ntf_rgf93Grid != null) {
            ntf_rgf93Grid.gridShiftForward(gs);
            return new LatLon(ntf.lat()+gs.getLatShiftDegrees(), ntf.lon()+gs.getLonShiftPositiveEastDegrees());
        }
        return new LatLon(0,0);
    }

    public Bounds getWorldBoundsLatLon()
    {
        Bounds b= new Bounds(
                new LatLon(zoneLimitsDegree[layoutZone][1] - cMaxOverlappingZonesDegree, -4.9074074074074059),
                new LatLon(zoneLimitsDegree[layoutZone][0] + cMaxOverlappingZonesDegree, 10.2));
        return b;
    }

    /**
     * Returns the default zoom scale in pixel per degree ({@see #NavigatableComponent#scale}))
     */
    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m (in default scale, 1 pixel will be 10 meters)
        return 10.0;
    }

    public int getLayoutZone() {
        return layoutZone;
    }

    public static String[] lambert4zones = {
        tr("{0} ({1} to {2} degrees)", 1,"51.30","48.15"),
        tr("{0} ({1} to {2} degrees)", 2,"48.15","45.45"),
        tr("{0} ({1} to {2} degrees)", 3,"45.45","42.76"),
        tr("{0} (Corsica)", 4)
    };

    public void setupPreferencePanel(JPanel p) {
        JComboBox prefcb = new JComboBox(lambert4zones);

        prefcb.setSelectedIndex(layoutZone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("Lambert CC Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
    }

    public Collection<String> getPreferences(JPanel p) {
        Object prefcb = p.getComponent(2);
        if(!(prefcb instanceof JComboBox))
            return null;
        layoutZone = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton(Integer.toString(layoutZone+1));
    }

    public void setPreferences(Collection<String> args) {
        layoutZone = DEFAULT_ZONE;
        if (args != null) {
            try {
                for(String s : args)
                {
                    layoutZone = Integer.parseInt(s)-1;
                    if(layoutZone < 0 || layoutZone > 3) {
                        layoutZone = DEFAULT_ZONE;
                    }
                    break;
                }
            } catch(NumberFormatException e) {}
        }
    }

    public Collection<String> getPreferencesFromCode(String code) {
        if (code.startsWith("EPSG:2756") && code.length() == 9) {
            try {
                String zonestring = code.substring(9);
                int zoneval = Integer.parseInt(zonestring);
                if(zoneval >= 1 && zoneval <= 4)
                    return Collections.singleton(zonestring);
            } catch(NumberFormatException e) {}
        }
        return null;
    }

}
