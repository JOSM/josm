// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

/**
 * Listener called when pressed modifier keys change is detected
 * @since 7217
 * @since 10600 (functional interface)
 */
@FunctionalInterface
public interface ModifierListener {
    void modifiersChanged(int modifiers);
}
