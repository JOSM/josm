// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.gui.help.HelpUtil;
import org.openstreetmap.josm.gui.preferences.ExtensibleTabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;

/**
 * Map preferences, including map paint styles, tagging presets and autosave sub-preferences.
 */
public final class MapPreference extends ExtensibleTabPreferenceSetting {

    /**
     * Factory used to create a new {@code MapPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new MapPreference();
        }
    }

    private MapPreference() {
        super(/* ICON(preferences/) */ "map", tr("Map"),
                tr("Settings for the map projection and data interpretation."), false);
    }

    @Override
    public boolean ok() {
        return false;
    }

    @Override
    public String getHelpContext() {
        return HelpUtil.ht("/Preferences/Map");
    }

    @Override
    protected boolean canBeHidden() {
        return true;
    }
}
