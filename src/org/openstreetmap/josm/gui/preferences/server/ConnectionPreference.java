// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Connection preferences.
 * @since 17160
 */
public final class ConnectionPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code AuthenticationPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ConnectionPreference();
        }
    }

    private ConnectionPreference() {
        super(/* ICON(preferences/) */ "connection", tr("Connection Settings"),
                tr("Connection Settings for the OSM server."), false, new JTabbedPane());
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

}
