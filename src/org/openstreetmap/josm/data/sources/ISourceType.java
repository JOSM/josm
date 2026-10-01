// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.sources;

/**
 * This interface should only be used for Enums
 * @author Taylor Smock
 *
 * @param <T> The source type (e.g., Imagery or otherwise -- should be the name of the class)
 * @since 16545
 */
public interface ISourceType<T extends Enum<T>> extends ICommonSource<T> {
    /**
     * Returns the unique string identifying this type.
     * @return the unique string identifying this type
     */
    String getTypeString();
}
