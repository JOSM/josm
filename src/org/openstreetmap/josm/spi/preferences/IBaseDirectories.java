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
     * @param createIfMissing if true, automatically creates this directory,
     * in case it is missing
     * @return the preferences directory
     * @since 12856
     */
    File getPreferencesDirectory(boolean createIfMissing);

    /**
     * Get the directory where user-specific data files should be stored.
     * @param createIfMissing if true, automatically creates this directory,
     * in case it is missing
     * @return the user data directory
     * @since 12856
     */
    File getUserDataDirectory(boolean createIfMissing);

    /**
     * Get the directory where user-specific cached content (non-essential data)
     * should be stored.
     * @param createIfMissing if true, automatically creates this directory,
     * in case it is missing
     * @return the cache directory
     * @since 12856
     */
    File getCacheDirectory(boolean createIfMissing);
}
