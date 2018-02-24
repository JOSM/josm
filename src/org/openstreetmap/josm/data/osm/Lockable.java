// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * To be implemented by modifiable objects to offer a "read-only/locked" mode.
 * @since 13453
 */
public interface Lockable {

    /**
     * Enables the read-only/locked mode.
     */
    void lock();

    /**
     * Disables the read-only/locked mode.
     */
    void unlock();

    /**
     * Determines if this is read-only/locked (thus it cannot be modified).
     * @return {@code true} if this is read-only/locked
     */
    boolean isLocked();
}
