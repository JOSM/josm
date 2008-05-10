// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;

public class LafPreference implements PreferenceSetting {

	/**
	 * ComboBox with all look and feels.
	 */
	private JComboBox lafCombo;

	public void addGui(PreferenceDialog gui) {
		lafCombo = new JComboBox(UIManager.getInstalledLookAndFeels());
		
		String laf = Main.pref.get("laf");
		for (int i = 0; i < lafCombo.getItemCount(); ++i) {
			if (((LookAndFeelInfo)lafCombo.getItemAt(i)).getClassName().equals(laf)) {
				lafCombo.setSelectedIndex(i);
				break;
			}
		}

		final ListCellRenderer oldRenderer = lafCombo.getRenderer();
		lafCombo.setRenderer(new DefaultListCellRenderer(){
			@Override public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
				return oldRenderer.getListCellRendererComponent(list, ((LookAndFeelInfo)value).getName(), index, isSelected, cellHasFocus);
			}
		});
		lafCombo.addActionListener(gui.requireRestartAction);

		gui.display.add(new JLabel(tr("Look and Feel")), GBC.std());
		gui.display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		gui.display.add(lafCombo, GBC.eol().fill(GBC.HORIZONTAL));
	}

	public void ok() {
		Main.pref.put("laf", ((LookAndFeelInfo)lafCombo.getSelectedItem()).getClassName());
    }

}
