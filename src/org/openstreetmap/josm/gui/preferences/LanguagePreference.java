// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Locale;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.I18n;

public class LanguagePreference implements PreferenceSetting {
	/**
	 * ComboBox with all available Translations
	 */
	private JComboBox langCombo;
	private final Locale AUTO_LANGUAGE = null;

	public void addGui(PreferenceDialog gui) {
		langCombo = new JComboBox(I18n.getAvailableTranslations());
		langCombo.insertItemAt(AUTO_LANGUAGE, 0); // Default
		langCombo.insertItemAt(Locale.ENGLISH, 1); // Built-in language
		String ln = Main.pref.get("language");
		langCombo.setSelectedIndex(0);
		
		if (ln != null) {
			for (int i = 1; i < langCombo.getItemCount(); ++i) {
				if (((Locale) langCombo.getItemAt(i)).toString().equals(ln)) {
					langCombo.setSelectedIndex(i);
					break;
				}
			}
		}

		final ListCellRenderer oldRenderer = langCombo.getRenderer();
		langCombo.setRenderer(new DefaultListCellRenderer() {
			@Override
			public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
					boolean cellHasFocus) {
				Locale l = (Locale) value;
				return oldRenderer.getListCellRendererComponent(list,
						l == AUTO_LANGUAGE ? tr("Default (Auto determined)") : l.getDisplayName(),
						index, isSelected, cellHasFocus);
			}
		});
		langCombo.addActionListener(gui.requireRestartAction);

		gui.display.add(new JLabel(tr("Language")), GBC.std());
		gui.display.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
		gui.display.add(langCombo, GBC.eol().fill(GBC.HORIZONTAL));
	}

	public void ok() {
		if(langCombo.getSelectedItem() == null)
		{
			Main.pref.put("language", null);
		}
		else
		{
			String l = ((Locale)langCombo.getSelectedItem()).toString();
			Main.pref.put("language", l);
		}
	}
}
