// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.Objects;

/**
 * Class to hold the global preferences object and the provider of base directories.
 * @since 12847
 */
public final class Config {

    private static IPreferences preferences;
    private static IBaseDirectories baseDirectories;
    private static IUrls urls;

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
     * Get class that provides the location of certain base directories
     * @return the global {@link IBaseDirectories} instance
     * @since 12855
     */
    public static IBaseDirectories getDirs() {
        return baseDirectories;
    }

    /**
     * Get class that provides the value of certain URLs
     * @return the global {@link IUrls} instance
     * @since 14119
     */
    public static IUrls getUrls() {
        return urls;
    }

    /**
     * Install the global preference instance.
     * @param preferences the global preference instance to set (must not be null)
     * @since 12847
     */
    public static void setPreferencesInstance(IPreferences preferences) {
        Config.preferences = Objects.requireNonNull(preferences, "preferences");
    }

    /**
     * Install the global base directories provider.
     * @param baseDirectories the global base directories provider instance to set
     * (must not be null)
     * @since 12855
     */
    public static void setBaseDirectoriesProvider(IBaseDirectories baseDirectories) {
        Config.baseDirectories = Objects.requireNonNull(baseDirectories, "baseDirectories");
    }

    /**
     * Install the global URLs provider.
     * @param urls the global URLs provider instance to set (must not be null)
     * @since xx
     */
    public static void setUrlsProvider(IUrls urls) {
        Config.urls = Objects.requireNonNull(urls, "urls");
    }
}
