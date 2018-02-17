// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;

import javax.swing.JTabbedPane;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.DefaultTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.tools.GBC;

/**
 * Display preferences (various settings that influence the visual representation of the whole program).
 * @since 4969
 */
public final class DisplayPreference extends DefaultTabPreferenceSetting {

    /**
     * Factory used to create a new {@code DisplayPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new DisplayPreference();
        }
    }

    private DisplayPreference() {
        super(/* ICON(preferences/) */ "display", tr("Display Settings"),
                tr("Various settings that influence the visual representation of the whole program."), false, new JTabbedPane());
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        gui.createPreferenceTab(this).add(getTabPane(), GBC.eol().fill(GBC.BOTH));
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Display");
    }
}
