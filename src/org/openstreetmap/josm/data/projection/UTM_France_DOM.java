// License: GPL. For details, see LICENSE file.
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
import org.openstreetmap.josm.data.projection.datum.Datum;
import org.openstreetmap.josm.data.projection.datum.GRS80Datum;
import org.openstreetmap.josm.data.projection.datum.SevenParameterDatum;
import org.openstreetmap.josm.data.projection.datum.ThreeParameterDatum;
import org.openstreetmap.josm.tools.GBC;

/**
 * This class implements all projections for French departements in the Caribbean Sea and
 * Indian Ocean using the UTM transvers Mercator projection and specific geodesic settings (7 parameters transformation algorithm).
 * 
 */
public class UTM_France_DOM extends AbstractProjection implements ProjectionSubPrefs {

    private final static String FortMarigotName = tr("Guadeloupe Fort-Marigot 1949");
    private final static String SainteAnneName = tr("Guadeloupe Ste-Anne 1948");
    private final static String MartiniqueName = tr("Martinique Fort Desaix 1952");
    private final static String Reunion92Name = tr("Reunion RGR92");
    private final static String Guyane92Name = tr("Guyane RGFG95");
    private final static String[] utmGeodesicsNames = { FortMarigotName, SainteAnneName, MartiniqueName, Reunion92Name, Guyane92Name};

    private final static Bounds FortMarigotBounds = new Bounds( new LatLon(17.6,-63.25), new LatLon(18.5,-62.5));
    private final static Bounds SainteAnneBounds = new Bounds( new LatLon(15.8,-61.9), new LatLon(16.6,-60.9));
    private final static Bounds MartiniqueBounds = new Bounds( new LatLon(14.25,-61.25), new LatLon(15.025,-60.725));
    private final static Bounds ReunionBounds = new Bounds( new LatLon(-25.92,37.58), new LatLon(-10.6, 58.27));
    private final static Bounds GuyaneBounds = new Bounds( new LatLon(2.16 , -54.0), new LatLon(9.06 , -49.62));
    private final static Bounds[] utmBounds = { FortMarigotBounds, SainteAnneBounds, MartiniqueBounds, ReunionBounds, GuyaneBounds };

    private final static Integer FortMarigotEPSG = 2969;
    private final static Integer SainteAnneEPSG = 2970;
    private final static Integer MartiniqueEPSG = 2973;
    private final static Integer ReunionEPSG = 2975;
    private final static Integer GuyaneEPSG = 2972;
    private final static Integer[] utmEPSGs = { FortMarigotEPSG, SainteAnneEPSG, MartiniqueEPSG, ReunionEPSG, GuyaneEPSG };

    private final static Datum FortMarigotDatum = new ThreeParameterDatum("FortMarigot Datum", null, Ellipsoid.hayford, 136.596, 248.148, -429.789);
    private final static Datum SainteAnneDatum = new SevenParameterDatum("SainteAnne Datum", null, Ellipsoid.hayford, -472.29, -5.63, -304.12, 0.4362, -0.8374, 0.2563, 1.8984);
    private final static Datum MartiniqueDatum = new SevenParameterDatum("Martinique Datum", null, Ellipsoid.hayford, 126.926, 547.939, 130.409, -2.78670, 5.16124, -0.85844, 13.82265);
    private final static Datum ReunionDatum = GRS80Datum.INSTANCE;
    private final static Datum GuyaneDatum = GRS80Datum.INSTANCE;
    private final static Datum[] utmDatums = { FortMarigotDatum, SainteAnneDatum, MartiniqueDatum, ReunionDatum, GuyaneDatum };

    private final static int[] utmZones = { 20, 20, 20, 40, 22 };

    /**
     * UTM zone (from 1 to 60)
     */
    private static int zone;
    /**
     * whether north or south hemisphere
     */
    private boolean isNorth;

    public static final int DEFAULT_GEODESIC = 0;

    public int currentGeodesic;


    public UTM_France_DOM() {
        updateParameters(DEFAULT_GEODESIC);
    }
    
    public void updateParameters(int currentGeodesic) {
        this.currentGeodesic = currentGeodesic;
        datum = utmDatums[currentGeodesic];
        ellps = datum.getEllipsoid();
        proj = new org.openstreetmap.josm.data.projection.proj.TransverseMercator(ellps);
        isNorth = currentGeodesic != 3;
        zone = utmZones[currentGeodesic];
        x_0 = 500000;
        y_0 = isNorth ? 0.0 : 10000000.0;
        lon_0 = 6 * zone - 183;
        k_0 = 0.9996;
    }
    
    public int getCurrentGeodesic() {
        return currentGeodesic;
    }

    @Override 
    public String toString() {
        return tr("UTM France (DOM)");
    }

    @Override
    public String getCacheDirectoryName() {
        return this.toString();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return utmBounds[currentGeodesic];
    }

    @Override
    public Integer getEpsgCode() {
        return utmEPSGs[currentGeodesic];
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode()+currentGeodesic; // our only real variable
    }

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JComboBox prefcb = new JComboBox(utmGeodesicsNames);

        prefcb.setSelectedIndex(currentGeodesic);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("UTM Geodesic system")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
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
        currentGeodesic = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton(Integer.toString(currentGeodesic+1));
    }

    @Override
    public String[] allCodes() {
        String[] res = new String[utmEPSGs.length];
        for (int i=0; i<utmEPSGs.length; ++i) {
            res[i] = "EPSG:"+utmEPSGs[i];
        }
        return res;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (int i=0; i < utmEPSGs.length; i++ )
            if (("EPSG:"+utmEPSGs[i]).equals(code))
                return Collections.singleton(Integer.toString(i+1));
        return null;
    }

    @Override
    public void setPreferences(Collection<String> args) {
        int currentGeodesic = DEFAULT_GEODESIC;
        if (args != null) {
            try {
                for(String s : args)
                {
                    currentGeodesic = Integer.parseInt(s)-1;
                    if(currentGeodesic < 0 || currentGeodesic >= utmEPSGs.length) {
                        currentGeodesic = DEFAULT_GEODESIC;
                    }
                    break;
                }
            } catch(NumberFormatException e) {}
        }
        updateParameters(currentGeodesic);
    }
}
