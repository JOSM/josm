// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JPanel;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Keyboard shortcut preferences.
 */
public final class ShortcutPreference extends DefaultTabPreferenceSetting {

    private String defaultFilter;

    /**
     * Factory used to create a new {@code ShortcutPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ShortcutPreference();
        }
    }

    private ShortcutPreference() {
        super(/* ICON(preferences/) */ "shortcuts", tr("Keyboard Shortcuts"), tr("Changing keyboard shortcuts manually."));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        JPanel p = gui.createPreferenceTab(this);

        PrefJPanel prefpanel = new PrefJPanel();
        p.add(prefpanel, GBC.eol().fill(GBC.BOTH));
        if (defaultFilter != null) {
            prefpanel.filter(defaultFilter);
        }
    }

    @Override
    public boolean ok() {
        return Shortcut.savePrefs();
    }

    /**
     * Sets the default filter used to show only shortcuts with descriptions containing given substring.
     * @param substring The substring used to filter
     * @see PrefJPanel#filter(String)
     */
    public void setDefaultFilter(String substring) {
        defaultFilter = substring;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Shortcuts");
    }
}
