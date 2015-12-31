// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.event.KeyEvent;

/**
 * Interface that is used to detect key pressing and releasing.
 * @since 7219
 */
public interface KeyPressReleaseListener {
    /**
     * This is called when key press event is actually pressed
     * (no fake events while holding key)
     * @param e key event
     */
    void doKeyPressed(KeyEvent e);

    /**
     * This is called when key press event is actually released
     * (no fake events while holding key)
     * @param e key event
     */
    void doKeyReleased(KeyEvent e);
}
