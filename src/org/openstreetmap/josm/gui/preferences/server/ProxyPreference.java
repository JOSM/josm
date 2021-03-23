// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import javax.swing.Box;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Proxy sub-preferences in server preferences.
 * @since 6523
 */
public final class ProxyPreference extends DefaultTabPreferenceSetting {

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
        super(/* ICON(preferences/) */ "proxy", tr("Proxy"), tr("Configure whether to use a proxy server"));
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        pnlProxyPreferences = new ProxyPreferencesPanel();
        pnlProxyPreferences.add(Box.createVerticalGlue(), GBC.eol().fill());
        gui.createPreferenceTab(this).add(pnlProxyPreferences, GBC.eol().fill());
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

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/ProxyPreference");
    }
}
