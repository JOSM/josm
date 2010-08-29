//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This class implements the Lambert Conic Conform 9 Zones projection as specified by the IGN
 * in this document http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
 * @author Pieren
 *
 */
public class LambertCC9Zones implements Projection, ProjectionSubPrefs {

    /**
     * Lambert 9 zones projection exponents
     */
    public static final double n[] = { 0.6691500006885269, 0.682018118346418, 0.6946784863203991, 0.7071272481559119,
        0.7193606118567315, 0.7313748510399917, 0.7431663060711892, 0.7547313851789208, 0.7660665655489937};

    /**
     * Lambert 9 zones projection constants
     */
    public static final double c[] = { 1.215363305807804E7, 1.2050261119223533E7, 1.195716926884592E7, 1.18737533925172E7,
        1.1799460698022118E7, 1.17337838820243E7, 1.16762559948139E7, 1.1626445901183508E7, 1.1583954251630554E7};

    /**
     * Lambert 9 zones false east
     */
    public static final double Xs = 1700000;

    /**
     * Lambert 9 zones false north
     */
    public static final double Ys[] = { 8293467.503439436, 9049604.665107645, 9814691.693461388, 1.0588107871787189E7,
        1.1369285637569271E7, 1.2157704903382052E7, 1.2952888086405803E7, 1.3754395745267643E7, 1.4561822739114787E7};

    /**
     * Lambert I, II, III, and IV longitudinal offset to Greenwich meridian
     */
    public static final double lg0 = 0.04079234433198; // 2deg20'14.025"

    /**
     * precision in iterative schema
     */

    public static final double epsilon = 1e-12;

    /**
     * France is divided in 9 Lambert projection zones, CC42 to CC50.
     */
    public static final double cMaxLatZonesRadian = Math.toRadians(51.1);

    public static final double cMinLatZonesDegree = 41.0;
    public static final double cMinLatZonesRadian = Math.toRadians(cMinLatZonesDegree);

    public static final double cMinLonZonesRadian = Math.toRadians(-5.0);

    public static final double cMaxLonZonesRadian = Math.toRadians(10.2);

    public static final double lambda0 = Math.toRadians(3);
    public static final double e = Ellipsoid.GRS80.e; // but in doc=0.08181919112
    public static final double e2 =Ellipsoid.GRS80.e2;
    public static final double a = Ellipsoid.GRS80.a;

    public static final double cMaxOverlappingZones = 1.5;

    public static final int DEFAULT_ZONE = 0;

    private static int layoutZone = DEFAULT_ZONE;

    private double L(double phi, double e) {
        double sinphi = Math.sin(phi);
        return (0.5*Math.log((1+sinphi)/(1-sinphi))) - e/2*Math.log((1+e*sinphi)/(1-e*sinphi));
    }

    /**
     * @param p  WGS84 lat/lon (ellipsoid GRS80) (in degree)
     * @return eastnorth projection in Lambert Zone (ellipsoid Clark)
     */
    public EastNorth latlon2eastNorth(LatLon p) {
        double lt = Math.toRadians(p.lat());
        double lg = Math.toRadians(p.lon());
        if (lt >= cMinLatZonesRadian && lt <= cMaxLatZonesRadian && lg >= cMinLonZonesRadian && lg <= cMaxLonZonesRadian)
            return ConicProjection(lt, lg, layoutZone);
        return ConicProjection(lt, lg, 0);
    }

    /**
     *
     * @param lat latitude in grad
     * @param lon longitude in grad
     * @param nz Lambert CC zone number (from 1 to 9) - 1 !
     * @return EastNorth projected coordinates in meter
     */
    private EastNorth ConicProjection(double lat, double lon, int nz) {
        double R = c[nz]*Math.exp(-n[nz]*L(lat,e));
        double gamma = n[nz]*(lon-lambda0);
        double X = Xs + R*Math.sin(gamma);
        double Y = Ys[nz] + -R*Math.cos(gamma);
        return new EastNorth(X, Y);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        return Geographic(p, layoutZone);
    }

    private LatLon Geographic(EastNorth ea, int nz) {
        double R = Math.sqrt(Math.pow(ea.getX()-Xs,2)+Math.pow(ea.getY()-Ys[nz], 2));
        double gamma = Math.atan((ea.getX()-Xs)/(Ys[nz]-ea.getY()));
        double lon = lambda0+gamma/n[nz];
        double latIso = (-1/n[nz])*Math.log(Math.abs(R/c[nz]));
        double lat = Ellipsoid.GRS80.latitude(latIso, e, epsilon);
        return new LatLon(Math.toDegrees(lat), Math.toDegrees(lon));
    }

    @Override public String toString() {
        return tr("Lambert CC9 Zone (France)");
    }

    public static int north2ZoneNumber(double north) {
        int nz = (int)(north /1000000) - 1;
        if (nz < 0) return 0;
        else if (nz > 8) return 8;
        else return nz;
    }

    public String toCode() {
        return "EPSG:"+(3942+layoutZone); //CC42 is EPSG:3942 (up to EPSG:3950 for CC50)
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode()+layoutZone; // our only real variable
    }

    public String getCacheDirectoryName() {
        return "lambert";
    }

    /**
     * Returns the default zoom scale in pixel per degree ({@see #NavigatableComponent#scale}))
     */
    public double getDefaultZoomInPPD() {
        // this will set the map scaler to about 1000 m (in default scale, 1 pixel will be 10 meters)
        return 10.0;
    }

    public Bounds getWorldBoundsLatLon()
    {
        double medLatZone = cMinLatZonesDegree + (layoutZone+1);
        return new Bounds(
                new LatLon(Math.max(medLatZone - 1.0 - cMaxOverlappingZones, cMinLatZonesDegree), -4.9),
                new LatLon(Math.min(medLatZone + 1.0 + cMaxOverlappingZones, Math.toDegrees(cMaxLatZonesRadian)), 10.2));
    }

    public int getLayoutZone() {
        return layoutZone;
    }

    private static String[] lambert9zones = {
        tr("{0} ({1} to {2} degrees)", 1,41,43),
        tr("{0} ({1} to {2} degrees)", 2,42,44),
        tr("{0} ({1} to {2} degrees)", 3,43,45),
        tr("{0} ({1} to {2} degrees)", 4,44,46),
        tr("{0} ({1} to {2} degrees)", 5,45,47),
        tr("{0} ({1} to {2} degrees)", 6,46,48),
        tr("{0} ({1} to {2} degrees)", 7,47,49),
        tr("{0} ({1} to {2} degrees)", 8,48,50),
        tr("{0} ({1} to {2} degrees)", 9,49,51)
    };

    public void setupPreferencePanel(JPanel p) {
        JComboBox prefcb = new JComboBox(lambert9zones);

        prefcb.setSelectedIndex(layoutZone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("Lambert CC Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(new JLabel(ImageProvider.get("data/projection", "LambertCC9Zones.png")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
    }

    public Collection<String> getPreferences(JPanel p) {
        Object prefcb = p.getComponent(2);
        if(!(prefcb instanceof JComboBox))
            return null;
        layoutZone = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton(Integer.toString(layoutZone+1));
    }

    public void setPreferences(Collection<String> args)
    {
        layoutZone = DEFAULT_ZONE;
        if (args != null) {
            try {
                for(String s : args)
                {
                    layoutZone = Integer.parseInt(s)-1;
                    if(layoutZone < 0 || layoutZone > 8) {
                        layoutZone = DEFAULT_ZONE;
                    }
                    break;
                }
            } catch(NumberFormatException e) {}
        }
    }

    public Collection<String> getPreferencesFromCode(String code)
    {
        //zone 1=CC42=EPSG:3942 up to zone 9=CC50=EPSG:3950
        if (code.startsWith("EPSG:39") && code.length() == 9) {
            try {
                String zonestring = code.substring(5,4);
                int zoneval = Integer.parseInt(zonestring)-3942;
                if(zoneval >= 0 && zoneval <= 8)
                    return Collections.singleton(zonestring);
            } catch(NumberFormatException e) {}
        }
        return null;
    }
}
