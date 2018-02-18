// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * To be implemented by modifiable objects to offer a "read-only" mode.
 * @since 13434
 */
public interface ReadOnly {

    /**
     * Enables the read-only mode.
     */
    void setReadOnly();

    /**
     * Disables the read-only mode.
     */
    void unsetReadOnly();

    /**
     * Determines if this is read-only (thus it cannot be modified).
     * @return {@code true} if this is read-only
     */
    boolean isReadOnly();
}
