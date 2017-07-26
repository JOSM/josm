// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

/**
 * Listener called when pressed modifier keys change is detected
 * @since 7217
 * @since 10600 (functional interface)
 * @deprecated use ModifierExListener instead
 */
@Deprecated
@FunctionalInterface
public interface ModifierListener {
    /**
     * Called when the modifiers are changed
     * @param modifiers The new modifiers
     */
    void modifiersChanged(int modifiers);
}
