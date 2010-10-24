// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.GBC;

/**
 *
 * @author Dirk Stöcker
 * code based on JavaScript from Chuck Taylor
 */
public class UTM extends TransverseMercator implements ProjectionSubPrefs {

    private static final int DEFAULT_ZONE = 30;
    private int zone = DEFAULT_ZONE;

    public enum Hemisphere {North, South}
    private static final Hemisphere DEFAULT_HEMISPHERE = Hemisphere.North;
    private Hemisphere hemisphere = DEFAULT_HEMISPHERE;

    private boolean offset = false;

    public UTM()
    {
        updateParameters();
    }

    /*
     * UTMCentralMeridian
     *
     * Determines the central meridian for the given UTM zone.
     *
     * Inputs:
     *     zone - An integer value designating the UTM zone, range [1,60].
     *
     * Returns:
     *   The central meridian for the given UTM zone, in radians, or zero
     *   if the UTM zone parameter is outside the range [1,60].
     *   Range of the central meridian is the radian equivalent of [-177,+177].
     *
     */
    private double UTMCentralMeridianDeg(int zone)
    {
        return -183.0 + (zone * 6.0);
    }

    @Override public String toString() {
        return tr("UTM");
    }

    private void updateParameters() {
        setProjectionParameters(this.UTMCentralMeridianDeg(getzone()), getEastOffset(), getNorthOffset());
    }

    public int getzone()
    {
        return zone;
    }

    private double getEastOffset() {
        return 500000 + (offset?3000000:0);
    }

    private double getNorthOffset() {
        if (hemisphere == Hemisphere.North)
            return 0;
        else
            return 10000000;
    }

    private int epsgCode() {
        return ((offset?325800:32600) + getzone() + (hemisphere == Hemisphere.South?100:0));
    }

    public String toCode() {
        return "EPSG:"+ epsgCode();
    }

    @Override
    public int hashCode() {
        return toCode().hashCode();
    }

    public String getCacheDirectoryName() {
        return "epsg"+ epsgCode();
    }

    public Bounds getWorldBoundsLatLon()
    {
        if (hemisphere == Hemisphere.North)
            return new Bounds(
                    new LatLon(-5.0, UTMCentralMeridianDeg(getzone())-5.0),
                    new LatLon(85.0, UTMCentralMeridianDeg(getzone())+5.0));
        else
            return new Bounds(
                    new LatLon(-85.0, UTMCentralMeridianDeg(getzone())-5.0),
                    new LatLon(5.0, UTMCentralMeridianDeg(getzone())+5.0));
    }

    public void setupPreferencePanel(JPanel p) {
        //Zone
        JComboBox zonecb = new JComboBox();
        for(int i = 1; i <= 60; i++) {
            zonecb.addItem(i);
        }

        zonecb.setSelectedIndex(zone - 1);
        p.setLayout(new GridBagLayout());
        p.add(new JLabel(tr("UTM Zone")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(zonecb, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

        //Hemisphere
        JRadioButton north = new JRadioButton();
        north.setSelected(hemisphere == Hemisphere.North);
        JRadioButton south = new JRadioButton();
        south.setSelected(hemisphere == Hemisphere.South);

        ButtonGroup group = new ButtonGroup();
        group.add(north);
        group.add(south);

        JPanel bPanel = new JPanel();
        bPanel.setLayout(new GridBagLayout());

        bPanel.add(new JLabel(tr("North")), GBC.std().insets(5, 5, 0, 5));
        bPanel.add(north, GBC.std().fill(GBC.HORIZONTAL));
        bPanel.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        bPanel.add(new JLabel(tr("South")), GBC.std().insets(5, 5, 0, 5));
        bPanel.add(south, GBC.std().fill(GBC.HORIZONTAL));
        bPanel.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

        p.add(new JLabel(tr("Hemisphere")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        p.add(bPanel, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

        //Offset
        JCheckBox offsetBox = new JCheckBox();
        offsetBox.setSelected(offset);

        p.add(new JLabel(tr("Offset 3.000.000m east")), GBC.std().insets(5,5,0,5));
        p.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
        /* Note: we use component position 2 below to find this again */
        p.add(offsetBox, GBC.eop().fill(GBC.HORIZONTAL));
        p.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));
    }

    public Collection<String> getPreferences(JPanel p) {
        int zone = DEFAULT_ZONE;
        Hemisphere hemisphere = DEFAULT_HEMISPHERE;
        boolean offset = false;

        Object zonecb = p.getComponent(2);
        if (zonecb instanceof JComboBox) {
            zone = ((JComboBox)zonecb).getSelectedIndex() + 1;
        }

        Object bPanel = p.getComponent(6);
        if (bPanel instanceof JPanel) {
            Object south = ((JPanel)bPanel).getComponent(4);
            if (south instanceof JRadioButton) {
                hemisphere = ((JRadioButton)south).isSelected()?Hemisphere.South:Hemisphere.North;
            }
        }

        Object offsetBox = p.getComponent(10);
        if (offsetBox instanceof JCheckBox) {
            offset = ((JCheckBox) offsetBox).isSelected();
        }

        return Arrays.asList(Integer.toString(zone), hemisphere.toString(), (offset?"offset":"standard"));
    }

    public void setPreferences(Collection<String> args)
    {
        zone = DEFAULT_ZONE;
        hemisphere = DEFAULT_HEMISPHERE;
        offset = true; //Default in previous versions

        if(args != null)
        {
            String[] array = args.toArray(new String[0]);
            try {
                zone = Integer.parseInt(array[0]);
                if(zone <= 0 || zone > 60) {
                    zone = DEFAULT_ZONE;
                }
            } catch(NumberFormatException e) {}

            if (array.length > 1) {
                hemisphere = Hemisphere.valueOf(array[1]);
            }

            if (array.length > 2) {
                offset = array[2].equals("offset");
            }
        }
        updateParameters();
    }

    public Collection<String> getPreferencesFromCode(String code)
    {
        boolean offset = code.startsWith("EPSG:3258") || code.startsWith("EPSG:3259");

        if(code.startsWith("EPSG:326") || code.startsWith("EPSG:327") || offset)
        {
            try {
                Hemisphere hemisphere;
                String zonestring;
                if (offset) {
                    hemisphere = code.charAt(8)=='8'?Hemisphere.North:Hemisphere.South;
                    zonestring = code.substring(9);
                } else {
                    hemisphere = code.charAt(7)=='6'?Hemisphere.North:Hemisphere.South;
                    zonestring = code.substring(8);
                }

                int zoneval = Integer.parseInt(zonestring);
                if(zoneval > 0 && zoneval <= 60)
                    return Arrays.asList(zonestring, hemisphere.toString(), (offset?"offset":"standard"));
            } catch(NumberFormatException e) {}
        }
        return null;
    }
}
