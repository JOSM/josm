// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;

public class MapPaintPreference implements PreferenceSetting {

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new MapPaintPreference();
        }
    }

    public void addGui(final PreferenceDialog gui) {
        // this is intended for a future configuration panel for mappaint!
    }

    public boolean ok() {
        return false; // dummy
    }

    /**
     * Initialize the styles
     */
    public static void initialize() {
        MapPaintStyles.readFromPreferences();
    }
}
