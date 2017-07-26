// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

/**
 * Listener called when pressed extended modifier keys change is detected.
 * @since 12516
 */
@FunctionalInterface
public interface ModifierExListener {
    /**
     * Called when the extended modifiers are changed
     * @param modifiers The new extended modifiers
     */
    void modifiersExChanged(int modifiers);
}
