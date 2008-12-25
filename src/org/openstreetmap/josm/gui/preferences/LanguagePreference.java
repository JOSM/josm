// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
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

        JPanel panel = null;
        for(PreferenceSetting s : gui.settings)
        {
            if(s instanceof LafPreference)
                panel = ((LafPreference)s).panel;
        }
        panel.add(new JLabel(tr("Language")), GBC.std().insets(20, 0, 0, 0));
        panel.add(GBC.glue(5,0), GBC.std().fill(GBC.HORIZONTAL));
        panel.add(langCombo, GBC.eol().fill(GBC.HORIZONTAL));
        panel.add(Box.createVerticalGlue(), GBC.eol().fill(GBC.BOTH));
    }

    public boolean ok() {
        if(langCombo.getSelectedItem() == null)
            return Main.pref.put("language", null);
        else
            return Main.pref.put("language",
            ((Locale)langCombo.getSelectedItem()).toString());
    }
}
