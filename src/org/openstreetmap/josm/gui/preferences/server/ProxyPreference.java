// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;

/**
 * Proxy sub-preferences in server preferences.
 * @since 6523
 */
public class ProxyPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code ProxyPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ProxyPreference();
        }
    }

    ProxyPreferencesPanel pnlProxyPreferences;

    private ProxyPreference() {
        super();
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        pnlProxyPreferences = new ProxyPreferencesPanel();
        gui.getServerPreference().addSubTab(this, tr("Proxy settings"), pnlProxyPreferences,
                tr("Configure whether to use a proxy server"));
    }

    @Override
    public boolean ok() {
        pnlProxyPreferences.saveToPreferences();
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
