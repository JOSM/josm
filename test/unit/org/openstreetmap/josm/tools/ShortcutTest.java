// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link Shortcut} class.
 */
class ShortcutTest {
    /**
     * Test method for {@code Shortcut#makeTooltip}
     */
    @Test
    void testMakeTooltip() {
        final String tooltip = Shortcut.makeTooltip("Foo Bar", KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.SHIFT_DOWN_MASK));
        final String shift = Platform.determinePlatform() == Platform.OSX ? "⇧" : "Shift";
        if (PlatformManager.getPlatform().isHtmlSupportedInMenuTooltips()) {
            assertEquals("<html>Foo Bar <font size='-2'>(" + shift + "+J)</font>&nbsp;</html>", tooltip);
        } else {
            assertEquals("Foo Bar (" + shift + "+J)", tooltip);
        }
    }

}
