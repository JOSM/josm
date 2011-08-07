// License: GPL. Copyright 2007 by Immanuel Scholz and others
//                         2009 by Łukasz Stelmach
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
import org.openstreetmap.josm.tools.GBC;

/**
 * PUWG 1992 and 2000 are the official cordinate systems in Poland.
 * They use the same math as UTM only with different constants.
 *
 * @author steelman
 */
public class Puwg extends AbstractProjection implements ProjectionSubPrefs {
    
    public static final int DEFAULT_ZONE = 0;
    
    private int zone;

    static PuwgData[] Zones = new PuwgData[] {
        new Epsg2180(),
        new Epsg2176(),
        new Epsg2177(),
        new Epsg2178(),
        new Epsg2179()
    };

    public Puwg() {
        this(DEFAULT_ZONE);
    }

    public Puwg(int zone) {
        ellps = Ellipsoid.GRS80;
        proj = new org.openstreetmap.josm.data.projection.proj.TransverseMercator(ellps);
        datum = GRS80Datum.INSTANCE;
        updateParameters(zone);
    }
    
    public void updateParameters(int zone) {
        this.zone = zone;
        PuwgData z = Zones[zone];
        x_0 = z.getPuwgFalseEasting();
        y_0 = z.getPuwgFalseNorthing();
        lon_0 = z.getPuwgCentralMeridianDeg();
        k_0 = z.getPuwgScaleFactor();
    }

    @Override 
    public String toString() {
        return tr("PUWG (Poland)");
    }

    @Override
    public Integer getEpsgCode() {
        return Zones[zone].getEpsgCode();
    }

    @Override
    public int hashCode() {
        return getClass().getName().hashCode()+zone; // our only real variable
    }

    @Override
    public String getCacheDirectoryName() {
        return Zones[zone].getCacheDirectoryName();
    }

    @Override
    public Bounds getWorldBoundsLatLon() {
        return Zones[zone].getWorldBoundsLatLon();
    }

    @Override
    public void setupPreferencePanel(JPanel p, ActionListener listener) {
        JComboBox prefcb = new JComboBox(Puwg.Zones);

        prefcb.setSelectedIndex(zone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("PUWG Zone")), GBC.std().insets(5,5,0,5));
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
        return Collections.singleton((Puwg.Zones[zone]).toCode());
    }

    @Override
    public String[] allCodes() {
        String[] zones = new String[Zones.length];
        for (int zone = 0; zone < Zones.length; zone++) {
            zones[zone] = Zones[zone].toCode();
        }
        return zones;
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        for (PuwgData p : Puwg.Zones) {
            if (code.equals(p.toCode()))
                return Collections.singleton(code);
        }
        return null;
    }

    @Override
    public void setPreferences(Collection<String> args) {
        int z = DEFAULT_ZONE;
        if (args != null) {
            try {
                for (String s : args) {
                    for (int i=0; i < Zones.length; ++i)
                        if (s.equals(Zones[i].toCode())) {
                            z = i;
                            break;
                        }
                    break;
                }
            } catch (NullPointerException e) {}
        }
        updateParameters(z);
    }
}

interface PuwgData {
    double getPuwgCentralMeridianDeg();
    double getPuwgCentralMeridian();
    double getPuwgFalseEasting();
    double getPuwgFalseNorthing();
    double getPuwgScaleFactor();

    // Projection methods
    Integer getEpsgCode();
    String toCode();
    String getCacheDirectoryName();
    Bounds getWorldBoundsLatLon();
}

class Epsg2180 implements PuwgData {

    private static final double Epsg2180FalseEasting = 500000.0; /* y */
    private static final double Epsg2180FalseNorthing = -5300000.0; /* x */
    private static final double Epsg2180ScaleFactor = 0.9993;
    private static final double Epsg2180CentralMeridian = 19.0;

    @Override public String toString() {
        return tr("PUWG 1992 (Poland)");
    }

    @Override
    public Integer getEpsgCode() {
        return 2180;
    }
    
    @Override
    public String toCode() {
        return "EPSG:" + getEpsgCode();
    }

    @Override
    public String getCacheDirectoryName() {
        return "epsg2180";
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(49.00, 14.12),
                new LatLon(54.84, 24.15));
    }

    @Override public double getPuwgCentralMeridianDeg() { return Epsg2180CentralMeridian; }
    @Override public double getPuwgCentralMeridian() { return Math.toRadians(Epsg2180CentralMeridian); }
    @Override public double getPuwgFalseEasting() { return Epsg2180FalseEasting; }
    @Override public double getPuwgFalseNorthing() { return Epsg2180FalseNorthing; }
    @Override public double getPuwgScaleFactor() { return Epsg2180ScaleFactor; }
}

abstract class Puwg2000 implements PuwgData {

    private static final double PuwgFalseEasting = 500000.0;
    private static final double PuwgFalseNorthing = 0;
    private static final double PuwgScaleFactor = 0.999923;
    //final private double[] Puwg2000CentralMeridian = {15.0, 18.0, 21.0, 24.0};
    final private Integer[] Puwg2000Code = { 2176,  2177, 2178, 2179 };
    final private String[] Puwg2000CDName = { "epsg2176",  "epsg2177", "epsg2178", "epsg2179" };

    @Override public String toString() {
        return tr("PUWG 2000 Zone {0} (Poland)", Integer.toString(getZone()));
    }

    @Override
    public Integer getEpsgCode() {
        return Puwg2000Code[getZoneIndex()];
    }

    @Override
    public String toCode() {
        return "EPSG:" + getEpsgCode();
    }

    @Override
    public String getCacheDirectoryName() {
        return Puwg2000CDName[getZoneIndex()];
    }

    @Override
    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(49.00, (3 * getZone()) - 1.5),
                new LatLon(54.84, (3 * getZone()) + 1.5));
    }

    @Override public double getPuwgCentralMeridianDeg() { return getZone() * 3.0; }
    @Override public double getPuwgCentralMeridian() { return Math.toRadians(getZone() * 3.0); }
    @Override public double getPuwgFalseNorthing() { return PuwgFalseNorthing;}
    @Override public double getPuwgFalseEasting() { return 1e6 * getZone() + PuwgFalseEasting; }
    @Override public double getPuwgScaleFactor() { return PuwgScaleFactor; }
    public abstract int getZone();

    public int getZoneIndex() { return getZone() - 5; }

}

class Epsg2176 extends Puwg2000 {
    private static final int PuwgZone = 5;

    @Override
    public int getZone() { return PuwgZone; }
}

class Epsg2177 extends Puwg2000 {
    private static final int PuwgZone = 6;

    @Override
    public int getZone() { return PuwgZone; }
}

class Epsg2178 extends Puwg2000 {
    private static final int PuwgZone = 7;

    @Override
    public int getZone() { return PuwgZone; }
}

class Epsg2179 extends Puwg2000 {
    private static final int PuwgZone = 8;

    @Override
    public int getZone() { return PuwgZone; }
}
