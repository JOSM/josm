// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.io.File;

/**
 * Interface for a provider of certain base directory locations.
 * <p>
 * Depending on the OS and preferred layout, some directories may coincide.
 * @since 12855
 */
public interface IBaseDirectories {

    /**
     * Get the directory where user-specific configuration and preferences
     * should be stored.
     * @return the preferences directory
     */
    File getPreferencesDirectory();

    /**
     * Get the directory where user-specific data files should be stored.
     * @return the user data directory
     */
    File getUserDataDirectory();

    /**
     * Get the directory where user-specific cached content (non-essential data)
     * should be stored.
     * @return the cache directory
     */
    File getCacheDirectory();
}
