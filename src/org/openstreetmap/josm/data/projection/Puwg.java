// License: GPL. Copyright 2007 by Immanuel Scholz and others
//                         2009 by Łukasz Stelmach
package org.openstreetmap.josm.data.projection;

import java.text.DecimalFormat;

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
import org.openstreetmap.josm.tools.GBC;

/**
 * PUWG 1992 and 2000 are the official cordinate systems in Poland.
 * They use the same math as UTM only with different constants.
 *
 * @author steelman
 */
public class Puwg extends UTM implements Projection,ProjectionSubPrefs {
    public static final int DEFAULT_ZONE = 0;
    private int zone = DEFAULT_ZONE;

    private static PuwgData[] Zones = new PuwgData[]{
        new Epsg2180(),
        new Epsg2176(),
        new Epsg2177(),
        new Epsg2178(),
        new Epsg2179()
    };

    private static DecimalFormat decFormatter = new DecimalFormat("###0");

    public EastNorth latlon2eastNorth(LatLon p) {
        PuwgData z = Zones[zone];
        double easting = z.getPuwgFalseEasting();
        double northing = z.getPuwgFalseNorthing();
        double scale = z.getPuwgScaleFactor();
        double center = z.getPuwgCentralMeridian(); /* in radians */
        EastNorth a = MapLatLonToXY(Math.toRadians(p.lat()), Math.toRadians(p.lon()), center);
        return new EastNorth(a.east() * scale + easting, a.north() * scale + northing);
    }

    public LatLon eastNorth2latlon(EastNorth p) {
        PuwgData z = Zones[zone];
        double easting = z.getPuwgFalseEasting();
        double northing = z.getPuwgFalseNorthing();
        double scale = z.getPuwgScaleFactor();
        double center = z.getPuwgCentralMeridian(); /* in radians */
        return MapXYToLatLon((p.east() - easting)/scale, (p.north() - northing)/scale, center);
    }

    @Override public String toString() {
        return tr("PUWG (Poland)");
    }

    public String toCode() {
        return Zones[zone].toCode();
    }

    public String getCacheDirectoryName() {
        return Zones[zone].getCacheDirectoryName();
    }

    public Bounds getWorldBoundsLatLon() {
        return Zones[zone].getWorldBoundsLatLon();
    }

    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 km
        return 0.009;
    }

    public String eastToString(EastNorth p) {
        return decFormatter.format(p.east());
    }

    public String northToString(EastNorth p) {
        return decFormatter.format(p.north());
    }

    public void setupPreferencePanel(JPanel p) {
        JComboBox prefcb = new JComboBox(Puwg.Zones);

        prefcb.setSelectedIndex(zone);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("PUWG Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(prefcb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
    }

    public Collection<String> getPreferences(JPanel p) {
    Object prefcb = p.getComponent(2);
        if(!(prefcb instanceof JComboBox))
            return null;
        int zone = ((JComboBox)prefcb).getSelectedIndex();
        return Collections.singleton((Puwg.Zones[zone]).toCode());
    }

    public Collection<String> getPreferencesFromCode(String code)
    {
        for (Projection p : Puwg.Zones)
        {
            if(code.equals(p.toCode()))
            return Collections.singleton(code);
        }
        return null;
    }

    public void setPreferences(Collection<String> args)
    {
        zone = DEFAULT_ZONE;
        if(!args != null)
        {
            try {
                for(String s : args)
                {
                for (int i=0; i < Puwg.Zones.length; ++i)
                    if(s.equals(Zones[i].toCode()))
                    zone = i;
                break;
                }
            } catch (NullPointerException e) {};
        }
    }
}

interface PuwgData extends Projection {
    public double getPuwgCentralMeridianDeg();
    public double getPuwgCentralMeridian();
    public double getPuwgFalseEasting();
    public double getPuwgFalseNorthing();
    public double getPuwgScaleFactor();
}

class Epsg2180 implements PuwgData {

    final private double Epsg2180FalseEasting = 500000.0; /* y */
    final private double Epsg2180FalseNorthing = -5300000.0; /* x */
    final private double Epsg2180ScaleFactor = 0.9993;
    final private double Epsg2180CentralMeridian = 19.0;
    private static DecimalFormat decFormatter = new DecimalFormat("###0");

    @Override public String toString() {
        return tr("PUWG 1992 (Poland)");
    }

    public String toCode() {
        return "EPSG:2180";
    }

    public String getCacheDirectoryName() {
        return "epsg2180";
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(49.00, 14.12),
                new LatLon(54.84, 24.15));
    }

    /* These two MUST NOT be used. Use Puwg implementation instead. */
    public EastNorth latlon2eastNorth(LatLon p) { return null; }
    public LatLon eastNorth2latlon(EastNorth p) { return null; }

    public double getPuwgCentralMeridianDeg() { return Epsg2180CentralMeridian; }
    public double getPuwgCentralMeridian() { return Math.toRadians(Epsg2180CentralMeridian); }
    public double getPuwgFalseEasting() { return Epsg2180FalseEasting; }
    public double getPuwgFalseNorthing() { return Epsg2180FalseNorthing; }
    public double getPuwgScaleFactor() { return Epsg2180ScaleFactor; }

    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 km
        return 0.009;
    }

    public String eastToString(EastNorth p) {
        return decFormatter.format(p.east());
    }

    public String northToString(EastNorth p) {
        return decFormatter.format(p.north());
    }
}

abstract class Puwg2000 implements PuwgData {

    final private double PuwgFalseEasting = 500000.0;
    final private double PuwgFalseNorthing = 0;
    final private double PuwgScaleFactor = 0.999923;
    final private double[] Puwg2000CentralMeridian = {15.0, 18.0, 21.0, 24.0};
    final private String[] Puwg2000Code = { "EPSG:2176",  "EPSG:2177", "EPSG:2178", "EPSG:2179"};
    final private String[] Puwg2000CDName = { "epsg2176",  "epsg2177", "epsg2178", "epsg2179"};
    private static DecimalFormat decFormatter = new DecimalFormat("###0.00");

    @Override public String toString() {
        return tr("PUWG 2000 Zone {0} (Poland)", Integer.toString(getZone()));
    }

    public String toCode() {
        return Puwg2000Code[getZoneIndex()];
    }

    public String getCacheDirectoryName() {
        return Puwg2000CDName[getZoneIndex()];
    }

    public Bounds getWorldBoundsLatLon()
    {
        return new Bounds(
                new LatLon(49.00, (3 * getZone()) - 1.5),
                new LatLon(54.84, (3 * getZone()) + 1.5));
    }

    /* These two MUST NOT be used. Use Puwg implementation instead. */
    public EastNorth latlon2eastNorth(LatLon p) { return null; }
    public LatLon eastNorth2latlon(EastNorth p) { return null; }

    public double getPuwgCentralMeridianDeg() { return getZone() * 3.0; }
    public double getPuwgCentralMeridian() { return Math.toRadians(getZone() * 3.0); }
    public double getPuwgFalseNorthing() { return PuwgFalseNorthing;}
    public double getPuwgFalseEasting() { return 1e6 * getZone() + PuwgFalseEasting; }
    public double getPuwgScaleFactor() { return PuwgScaleFactor; }
    public abstract int getZone();

    public int getZoneIndex() { return getZone() - 5; }

    public double getDefaultZoomInPPD() {
        // This will set the scale bar to about 100 km
        return 0.009;
    }

    public String eastToString(EastNorth p) {
        return Integer.toString(getZone()) + decFormatter.format(p.east());
    }

    public String northToString(EastNorth p) {
        return decFormatter.format(p.north());
    }

}

class Epsg2176 extends Puwg2000 implements Projection {
    final private int PuwgZone = 5;

    public int getZone() { return PuwgZone; }
}

class Epsg2177 extends Puwg2000 implements Projection {
    final private int PuwgZone = 6;

    public int getZone() { return PuwgZone; }
}

class Epsg2178 extends Puwg2000 implements Projection {
    final private int PuwgZone = 7;

    public int getZone() { return PuwgZone; }
}

class Epsg2179 extends Puwg2000 implements Projection {
    final private int PuwgZone = 8;

    public int getZone() { return PuwgZone; }
}
