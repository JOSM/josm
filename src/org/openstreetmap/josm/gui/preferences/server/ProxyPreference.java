// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;

/**
 * Proxy sub-preferences in server preferences.
 * @since 6523
 */
public final class ProxyPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code ProxyPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ProxyPreference();
        }
    }

    private static final Set<ProxyPreferenceListener> listeners = new HashSet<>();

    private ProxyPreferencesPanel pnlProxyPreferences;

    private ProxyPreference() {
        super();
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        pnlProxyPreferences = new ProxyPreferencesPanel();
        gui.getServerPreference().addSubTab(this, tr("Proxy settings"),
                pnlProxyPreferences.getVerticalScrollPane(),
                tr("Configure whether to use a proxy server"));
    }

    @Override
    public boolean ok() {
        pnlProxyPreferences.saveToPreferences();
        for (ProxyPreferenceListener listener : listeners) {
            listener.proxyPreferenceChanged();
        }
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

    /**
     * Adds a new ProxyPreferenceListener.
     * @param listener the listener to add
     * @return {@code true} if the listener has been added, {@code false} otherwise
     * @since 6525
     */
    public static boolean addProxyPreferenceListener(ProxyPreferenceListener listener) {
        if (listener != null) {
            return listeners.add(listener);
        }
        return false;
    }

    /**
     * Removes a ProxyPreferenceListener.
     * @param listener the listener to remove
     * @return {@code true} if the listener has been removed, {@code false} otherwise
     * @since 6525
     */
    public static boolean removeProxyPreferenceListener(ProxyPreferenceListener listener) {
        if (listener != null) {
            return listeners.remove(listener);
        }
        return false;
    }
}
