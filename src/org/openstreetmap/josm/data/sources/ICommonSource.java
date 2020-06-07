// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

/**
 * This interface is used to ensure that a class can get a enum from a string.
 * For various reasons, the fromString method cannot be implemented statically.
 *
 * @author Taylor Smock
 *
 * @param <T> The enum type
 * @since 16545
 */
public interface ICommonSource<T extends Enum<T>> {
    /**
     * Get the default value for the Enum
     * @return The default value
     */
    T getDefault();

    /**
     * Returns the source category from the given category string.
     * @param s The category string
     * @return the source category matching the given category string
     */
    T getFromString(String s);
}
