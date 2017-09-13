// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.Objects;

import org.openstreetmap.josm.data.preferences.IPreferences;

/**
 * Class to hold the global preferences object.
 */
public class Config {

    private static IPreferences preferences;

    /**
     * Get the preferences.
     * @return the preferences
     */
    public static IPreferences getPref() {
        return preferences;
    }

    /**
     * Install the global preference instance.
     * @param preferences the global preference instance to set (must not be null)
     */
    public static void setPreferencesInstance(IPreferences preferences) {
        Config.preferences = Objects.requireNonNull(preferences, "preferences");
    }
}
