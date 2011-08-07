// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Lambert conic conform 4 zones using the French geodetic system NTF.
 * This newer version uses the grid translation NTF<->RGF93 provided by IGN for a submillimetric accuracy.
 * (RGF93 is the French geodetic system similar to WGS84 but not mathematically equal)
 * @author Pieren
 */
public class Lambert extends AbstractProjection implements ProjectionSubPrefs {
    /**
     * Lambert I, II, III, and IV projection exponents
     */
    private static final double n[] = { 0.7604059656, 0.7289686274, 0.6959127966, 0.6712679322 };

    /**
     * Lambert I, II, III, and IV projection constants
     */
    private static final double c[] = { 11603796.98, 11745793.39, 11947992.52, 12136281.99 };

    /**
     * Lambert I, II, III, and IV false east
     */
    private static final double x_0s[] = { 600000.0, 600000.0, 600000.0, 234.358 };

    /**
     * Lambert I, II, III, and IV false north
     */
    private static final double y_fs[] = { 5657616.674, 6199695.768, 6791905.085, 7239161.542 };

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

    private static NTV2GridShiftFile ntf_rgf93Grid = null;

    public static NTV2GridShiftFile getNtf_rgf93Grid() {
        return ntf_rgf93Grid;
    }

    public Lambert() {
        if (ntf_rgf93Grid == null) {
            try {
                String gridFileName = "ntf_r93_b.gsb";
                InputStream is = Main.class.getResourceAsStream("/data/"+gridFileName);
                if (is == null) {
                    throw new RuntimeException(tr("Error: failed to open input stream for resource ''/data/{0}''. Cannot load NTF<->RGF93 grid", gridFileName));
                }
                ntf_rgf93Grid = new NTV2GridShiftFile();
                ntf_rgf93Grid.loadGridShiftFile(is, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        updateParameters(DEFAULT_ZONE);
    }
    
    private void updateParameters(int layoutZone) {
        this.layoutZone = layoutZone;
        ellps = Ellipsoid.clarke;
        datum = null; // no datum needed, we have a shift file
        nadgrids = ntf_rgf93Grid;
        x_0 = x_0s[layoutZone];
        lon_0 = 2.0 + 20.0 / 60 + 14.025 / 3600; // 0 grade Paris
        if (proj == null) {
            proj = new LambertConformalConic();
        }
        ((LambertConformalConic)proj).updateParametersDirect(
                Ellipsoid.clarke, n[layoutZone], c[layoutZone] / ellps.a, y_fs[layoutZone] / ellps.a);
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
                new LatLon(Math.min(zoneLimitsDegree[layoutZone][0] + cMaxOverlappingZonesDegree, Math.toDegrees(cMaxLatZone1Radian)), Math.toDegrees(cMaxLonZonesRadian)));
        return b;
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

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JComboBox prefcb = new JComboBox(lambert4zones);

        prefcb.setSelectedIndex(layoutZone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("Lambert CC Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(new JLabel(ImageProvider.get("data/projection", "Departements_Lambert4Zones.png")), GBC.eol().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

        if (listener != null) {
            prefcb.addActionListener(listener);
        }
    }

    @Override
    public Collection<String> getPreferences(JPanel p) {
        Object prefcb = p.getComponent(2);
        if(!(prefcb instanceof JComboBox))
            return null;
        layoutZone = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton(Integer.toString(layoutZone+1));
    }

    @Override
    public void setPreferences(Collection<String> args) {
        int layoutZone = DEFAULT_ZONE;
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
        updateParameters(layoutZone);
    }

    @Override
    public String[] allCodes() {
        String[] zones = new String[4];
        for (int zone = 0; zone < 4; zone++) {
            zones[zone] = "EPSG:"+(27561+zone);
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        if (code.startsWith("EPSG:2756") && code.length() == 10) {
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
