// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * "GPS Points" drawing preferences.
 */
public class GPXPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code GPXPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new GPXPreference();
        }
    }

    private GPXSettingsPanel gpxPanel;

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gpxPanel = new GPXSettingsPanel();
        gui.addValidationListener(gpxPanel);
        JPanel panel = gpxPanel;

        JScrollPane scrollpane = new JScrollPane(panel);
        scrollpane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        GuiHelper.setDefaultIncrement(scrollpane);
        gui.getDisplayPreference().addSubTab(this, tr("GPS Points"), scrollpane);
    }

    @Override
    public boolean ok() {
        return gpxPanel.savePreferences();
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getDisplayPreference();
    }
}
