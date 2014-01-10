// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;

/**
 * Authentication sub-preferences in server preferences.
 * @since 6523
 */
public final class AuthenticationPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code AuthenticationPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new AuthenticationPreference();
        }
    }

    AuthenticationPreferencesPanel pnlAuthPreferences;

    private AuthenticationPreference() {
        super();
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        pnlAuthPreferences = new AuthenticationPreferencesPanel();
        gui.getServerPreference().addApiUrlChangeListener(pnlAuthPreferences);
        gui.getServerPreference().addSubTab(this, tr("Authentication"),
                pnlAuthPreferences.getVerticalScrollPane(),
                tr("Configure your identity and how to authenticate at the OSM server"));
    }

    @Override
    public boolean ok() {
        pnlAuthPreferences.saveToPreferences();
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getServerPreference();
    }
}
