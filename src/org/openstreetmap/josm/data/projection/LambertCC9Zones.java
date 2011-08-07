//License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.proj.LambertConformalConic;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Lambert Conic Conform 9 Zones projection as specified by the IGN
 * in this document http://professionnels.ign.fr/DISPLAY/000/526/700/5267002/transformation.pdf
 * @author Pieren
 *
 */
public class LambertCC9Zones extends AbstractProjection implements ProjectionSubPrefs {

    /**
     * France is divided in 9 Lambert projection zones, CC42 to CC50.
     */
    public static final double cMaxLatZonesRadian = Math.toRadians(51.1);

    public static final double cMinLatZonesDegree = 41.0;

    public static final double cMaxOverlappingZones = 1.5;

    public static final int DEFAULT_ZONE = 0;

    private int layoutZone = DEFAULT_ZONE;

    public LambertCC9Zones() {
        this(DEFAULT_ZONE);
    }

    public LambertCC9Zones(int layoutZone) {
        updateParameters(layoutZone);
    }

    public void updateParameters(int layoutZone) {
        ellps = Ellipsoid.GRS80;
        datum = GRS80Datum.INSTANCE;
        this.layoutZone = layoutZone;
        x_0 = 1700000;
        y_0 = (layoutZone+1) * 1000000 + 200000;
        lon_0 = 3;
        double lat_0 = 42 + layoutZone;
        double lat_1 = 41.25 + layoutZone;
        double lat_2 = 42.75 + layoutZone;
        if (proj == null) {
            proj = new LambertConformalConic();
        }
        ((LambertConformalConic)proj).updateParameters2SP(ellps, lat_0, lat_1, lat_2);
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

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JComboBox prefcb = new JComboBox(lambert9zones);

        prefcb.setSelectedIndex(layoutZone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("Lambert CC Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(new JLabel(ImageProvider.get("data/projection", "LambertCC9Zones.png")), GBC.eol().fill(GBC.HORIZONTAL));
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
        int layoutZone = ((JComboBox)prefcb).getSelectedIndex();
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
                    if(layoutZone < 0 || layoutZone > 8) {
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
        String[] zones = new String[9];
        for (int zone = 0; zone < 9; zone++) {
            zones[zone] = "EPSG:" + (3942 + zone);
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code)
    {
        //zone 1=CC42=EPSG:3942 up to zone 9=CC50=EPSG:3950
        if (code.startsWith("EPSG:39") && code.length() == 9) {
            try {
                String zonestring = code.substring(5,9);
                int zoneval = Integer.parseInt(zonestring)-3942;
                if(zoneval >= 0 && zoneval <= 8)
                    return Collections.singleton(String.valueOf(zoneval+1));
            } catch(NumberFormatException ex) {}
        }
        return null;
    }
}
