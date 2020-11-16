// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.ExtensibleTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;

/**
 * Display preferences (various settings that influence the visual representation of the whole program).
 * @since 4969
 */
public final class DisplayPreference extends ExtensibleTabPreferenceSetting {

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
        super(/* ICON(preferences/) */ "display", trc("gui", "Display"),
                tr("Various settings that influence the visual representation of the whole program."), false);
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Display");
    }

    @Override
    protected boolean canBeHidden() {
        return true;
    }
}
