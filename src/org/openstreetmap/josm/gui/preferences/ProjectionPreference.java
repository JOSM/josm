// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.CoordinateFormat;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.projection.Mercator;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new ProjectionPreference();
        }
    }

    /**
     * Combobox with all projections available
     */
    private JComboBox projectionCombo = new JComboBox(Projection.allProjections);
    private JComboBox coordinatesCombo = new JComboBox(CoordinateFormat.values());

    public void addGui(PreferenceDialog gui) {

        for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
            if (projectionCombo.getItemAt(i).getClass().getName().equals(Main.pref.get("projection", Mercator.class.getName()))) {
                projectionCombo.setSelectedIndex(i);
                break;
            }
        }

        for (int i = 0; i < coordinatesCombo.getItemCount(); ++i) {
            if (((CoordinateFormat)coordinatesCombo.getItemAt(i)).name().equals(Main.pref.get("coordinates"))) {
                coordinatesCombo.setSelectedIndex(i);
                break;
            }
        }

        JPanel projPanel = new JPanel();
        projPanel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        projPanel.setLayout(new GridBagLayout());
        projPanel.add(new JLabel(tr("Display coordinates as")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(coordinatesCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5,5,0,5));
        projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
        projPanel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
        JScrollPane scrollpane = new JScrollPane(projPanel);
        gui.mapcontent.addTab(tr("Map Projection"), scrollpane);
    }

    public boolean ok() {
        String projname = projectionCombo.getSelectedItem().getClass().getName();
        if(Main.pref.put("projection", projname)) {
            Main.setProjection(projname);
        }
        if(Main.pref.put("coordinates",
                ((CoordinateFormat)coordinatesCombo.getSelectedItem()).name())) {
            CoordinateFormat.setCoordinateFormat((CoordinateFormat)coordinatesCombo.getSelectedItem());
        }
        return false;
    }
}
