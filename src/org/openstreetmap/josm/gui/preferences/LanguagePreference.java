// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
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
    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new LanguagePreference();
        }
    }

    /** the combo box with the available locales */
    private JComboBox langCombo;
    /** the model for the combo box */
    private LanguageComboBoxModel model;

    public void addGui(final PreferenceTabbedPane gui) {
        model = new LanguageComboBoxModel();
        langCombo = new JComboBox(model);
        langCombo.setRenderer(new LanguageCellRenderer(langCombo.getRenderer()));
        model.selectLanguage(Main.pref.get("language"));

        LafPreference lafPreference = gui.getSetting(LafPreference.class);
        final JPanel panel = lafPreference.panel;
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

    private static class LanguageComboBoxModel extends DefaultComboBoxModel {
        private final List<Locale> data = new ArrayList<Locale>();

        public LanguageComboBoxModel(){
            data.add(0,null);
            data.addAll(Arrays.asList(I18n.getAvailableTranslations()));
        }

        public void selectLanguage(String language) {
            setSelectedItem(null);
            if (language != null) {
                for (Locale locale: data) {
                    if (locale == null) {
                        continue;
                    }
                    if (locale.toString().equals(language)) {
                        setSelectedItem(locale);
                        return;
                    }
                }
            }
        }

        @Override
        public Object getElementAt(int index) {
            return data.get(index);
        }

        @Override
        public int getSize() {
            return data.size();
        }
    }

    static private class LanguageCellRenderer extends DefaultListCellRenderer {
        private ListCellRenderer dispatch;
        public LanguageCellRenderer(ListCellRenderer dispatch) {
            this.dispatch = dispatch;
        }
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                boolean cellHasFocus) {
            Locale l = (Locale) value;
            return dispatch.getListCellRendererComponent(list,
                    l == null ? tr("Default (Auto determined)") : l.getDisplayName(),
                            index, isSelected, cellHasFocus);
        }
    }
}
