// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.tools.GBC;

public class ProjectionPreference implements PreferenceSetting {

	/**
	 * Combobox with all projections available
	 */
	private JComboBox projectionCombo = new JComboBox(Projection.allProjections);

	public void addGui(PreferenceDialog gui) {
		for (int i = 0; i < projectionCombo.getItemCount(); ++i) {
			if (projectionCombo.getItemAt(i).getClass().getName().equals(Main.pref.get("projection"))) {
				projectionCombo.setSelectedIndex(i);
				break;
			}
		}
		projectionCombo.addActionListener(gui.requireRestartAction);
		
		JPanel projPanel = new JPanel();
		projPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.gray), tr("Map Projection")));
		projPanel.setLayout(new GridBagLayout());
		projPanel.add(new JLabel(tr("Projection method")), GBC.std().insets(5,5,0,5));
		projPanel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		projPanel.add(projectionCombo, GBC.eop().fill(GBC.HORIZONTAL).insets(0,5,5,5));
		gui.map.add(projPanel, GBC.eol().insets(0,0,0,10).fill(GBC.HORIZONTAL));
    }

	public void ok() {
		Main.pref.put("projection", projectionCombo.getSelectedItem().getClass().getName());
    }
}
