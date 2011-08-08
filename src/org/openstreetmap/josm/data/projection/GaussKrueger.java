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
import org.openstreetmap.josm.data.projection.proj.TransverseMercator;
import org.openstreetmap.josm.tools.GBC;

public class GaussKrueger extends AbstractProjection implements ProjectionSubPrefs {

    public static int DEFAULT_ZONE = 2;
    private int zone;
    
    private static Bounds[] bounds = { 
        new Bounds(new LatLon(-5, 3.5), new LatLon(85, 8.5)),
        new Bounds(new LatLon(-5, 6.5), new LatLon(85, 11.5)),
        new Bounds(new LatLon(-5, 9.5), new LatLon(85, 14.5)),
        new Bounds(new LatLon(-5, 12.5), new LatLon(85, 17.5)),
    };

    private static NTV2GridShiftFile BETA2007 = null;
    
    private static String[] zones = { "2", "3", "4", "5" };

    public GaussKrueger() {
        this(DEFAULT_ZONE);
    }

    public GaussKrueger(int zone) {
        if (BETA2007 == null) {
            try {
                String gridFileName = "BETA2007.gsb";
                InputStream is = Main.class.getResourceAsStream("/data/"+gridFileName);
                if (is == null) {
                    throw new RuntimeException(tr("Error: failed to open input stream for resource ''/data/{0}''.", gridFileName));
                }
                BETA2007 = new NTV2GridShiftFile();
                BETA2007.loadGridShiftFile(is, false);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        updateParameters(zone);
    }

    private void updateParameters(int zone) {
        this.zone = zone;
        ellps = Ellipsoid.Bessel1841;
        nadgrids = BETA2007;
        ////less acurrate datum (errors up to 3m):
        //datum = new SevenParameterDatum(
        //        tr("Deutsches Hauptdreiecksnetz"), null, ellps,
        //        598.1, 73.7, 418.2, 0.202, 0.045, -2.455, 6.70);
        proj = new TransverseMercator(ellps);
        x_0 = 1000000 * zone + 500000;
        lon_0 = 3 * zone;
    }

    @Override 
    public String toString() {
        return tr("Gau\u00DF-Kr\u00FCger");
    }
    
    @Override
    public Integer getEpsgCode() {
        return 31464 + zone;
    }

    @Override
    public String getCacheDirectoryName() {
        return "gausskrueger"+zone;
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return bounds[zone-2];
    }
    
    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JComboBox prefcb = new JComboBox(zones);

        prefcb.setSelectedIndex(zone-2);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("GK Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
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
        int zone = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton(Integer.toString(zone+2));
    }
    
    @Override
    public void setPreferences(Collection<String> args) {
        int zone = DEFAULT_ZONE;
        if (args != null) {
            try {
                for(String s : args)
                {
                    zone = Integer.parseInt(s);
                    if(zone < 2 || zone > 5) {
                        zone = DEFAULT_ZONE;
                    }
                    break;
                }
            } catch(NumberFormatException e) {}
        }
        updateParameters(zone);
    }
    
    @Override
    public String[] allCodes() {
        String[] zones = new String[4];
        for (int zone = 2; zone <= 5; zone++) {
            zones[zone-2] = "EPSG:" + (31464 + zone);
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code)
    {
        //zone 2 = EPSG:31466 up to zone 5 = EPSG:31469
        for (int zone = 2; zone <= 5; zone++) {
            String epsg = "EPSG:" + (31464 + zone);
            if (epsg.equals(code))
                return Collections.singleton(String.valueOf(zone));
        }
        return null;
    }
    
}
