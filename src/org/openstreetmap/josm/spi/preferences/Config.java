// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.Objects;

/**
 * Class to hold the global preferences object.
 * @since 12847
 */
public final class Config {

    private static IPreferences preferences;

    private Config() {
        // hide constructor
    }

    /**
     * Get the preferences.
     * @return the preferences
     * @since 12847
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
