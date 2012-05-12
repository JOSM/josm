// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.UTM;
import org.openstreetmap.josm.tools.GBC;

public class UTMProjectionChoice extends ListProjectionChoice implements Alias {

    private static final UTM.Hemisphere DEFAULT_HEMISPHERE = UTM.Hemisphere.North;

    private UTM.Hemisphere hemisphere;
    
    /**
     * Applies an additional false easting of 3000000 m if true.
     */
    private boolean offset;

    private final static List<String> cbEntries = new ArrayList<String>();
    static {
        for (int i = 1; i <= 60; i++) {
                cbEntries.add(Integer.toString(i));
        }
    }

    public UTMProjectionChoice() {
        super("core:utm", tr("UTM"), cbEntries.toArray(), tr("UTM Zone"));
    }

    private class UTMPanel extends CBPanel {

        public JRadioButton north, south;
        public JCheckBox offsetBox;

        public UTMPanel(Object[] entries, int initialIndex, String label, ActionListener listener) {
            super(entries, initialIndex, label, listener);

            //Hemisphere
            north = new JRadioButton();
            north.setSelected(hemisphere == UTM.Hemisphere.North);
            south = new JRadioButton();
            south.setSelected(hemisphere == UTM.Hemisphere.South);

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

            this.add(new JLabel(tr("Hemisphere")), GBC.std().insets(5,5,0,5));
            this.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            this.add(bPanel, GBC.eop().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

            //Offset
            offsetBox = new JCheckBox();
            offsetBox.setSelected(offset);

            this.add(new JLabel(tr("Offset 3.000.000m east")), GBC.std().insets(5,5,0,5));
            this.add(GBC.glue(1, 0), GBC.std().fill(GBC.HORIZONTAL));
            this.add(offsetBox, GBC.eop().fill(GBC.HORIZONTAL));
            this.add(GBC.glue(1, 1), GBC.eol().fill(GBC.BOTH));

            if (listener != null) {
                north.addActionListener(listener);
                south.addActionListener(listener);
                offsetBox.addActionListener(listener);
            }
        }
    }

    @Override
    public JPanel getPreferencePanel(ActionListener listener) {
        return new UTMPanel(entries, index, label, listener);
    }

    @Override
    public Projection getProjection() {
        return new UTM(indexToZone(index), hemisphere, offset);
    }

    @Override
    public Collection<String> getPreferences(JPanel panel) {
        UTMPanel p = (UTMPanel) panel;
        int index = p.prefcb.getSelectedIndex();
        UTM.Hemisphere hemisphere = p.south.isSelected()?UTM.Hemisphere.South:UTM.Hemisphere.North;
        boolean offset = p.offsetBox.isSelected();
        return Arrays.asList(Integer.toString(indexToZone(index)), hemisphere.toString(), (offset?"offset":"standard"));
    }

    @Override
    public String[] allCodes() {
        ArrayList<String> projections = new ArrayList<String>(60*4);
        for (int zone = 1;zone <= 60; zone++) {
            for (boolean offset : new boolean[] { false, true }) {
                for (UTM.Hemisphere hemisphere : UTM.Hemisphere.values()) {
                    projections.add("EPSG:" + ((offset?325800:32600) + zone + (hemisphere == UTM.Hemisphere.South?100:0)));
                }
            }
        }
        return projections.toArray(new String[0]);
    }

    @Override
    public Collection<String> getPreferencesFromCode(String code) {
        boolean offset = code.startsWith("EPSG:3258") || code.startsWith("EPSG:3259");

        if (code.startsWith("EPSG:326") || code.startsWith("EPSG:327") || offset) {
            try {
                UTM.Hemisphere hemisphere;
                String zonestring;
                if (offset) {
                    hemisphere = code.charAt(8)=='8'?UTM.Hemisphere.North:UTM.Hemisphere.South;
                    zonestring = code.substring(9);
                } else {
                    hemisphere = code.charAt(7)=='6'?UTM.Hemisphere.North:UTM.Hemisphere.South;
                    zonestring = code.substring(8);
                }

                int zoneval = Integer.parseInt(zonestring);
                if(zoneval > 0 && zoneval <= 60)
                    return Arrays.asList(zonestring, hemisphere.toString(), (offset?"offset":"standard"));
            } catch(NumberFormatException e) {}
        }
        return null;
    }

    @Override
    public void setPreferences(Collection<String> args) {
        super.setPreferences(args);
        UTM.Hemisphere hemisphere = DEFAULT_HEMISPHERE;
        boolean offset = false;

        if(args != null) {
            String[] array = args.toArray(new String[0]);

            if (array.length > 1) {
                hemisphere = UTM.Hemisphere.valueOf(array[1]);
            }

            if (array.length > 2) {
                offset = array[2].equals("offset");
            }
        }
        this.hemisphere = hemisphere;
        this.offset = offset;
    }

    @Override
    protected int indexToZone(int index) {
        return index + 1;
    }

    @Override
    protected int zoneToIndex(int zone) {
        return zone - 1;
    }

    @Override
    public String getAlias() {
        return UTM.class.getName();
    }

}
