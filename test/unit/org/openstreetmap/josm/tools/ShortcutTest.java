// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link Shortcut} class.
 */
class ShortcutTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test method for {@code Shortcut#makeTooltip}
     */
    @Test
    void testMakeTooltip() {
        final String tooltip = Shortcut.makeTooltip("Foo Bar", KeyStroke.getKeyStroke(KeyEvent.VK_J, InputEvent.SHIFT_DOWN_MASK));
        if (Platform.determinePlatform() == Platform.OSX) {
            assertEquals("Foo Bar (⇧+J)", tooltip);
        } else {
            assertEquals("<html>Foo Bar <font size='-2'>(Shift+J)</font>&nbsp;</html>", tooltip);
        }
    }

}
