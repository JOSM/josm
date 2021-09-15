// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Toolkit;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import javax.swing.KeyStroke;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.gui.preferences.display.LafPreference;
import org.openstreetmap.josm.testutils.annotations.JavaProperty;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link Shortcut} class.
 */
@JavaProperty
@Main
class ShortcutTest {
    private static LookAndFeel originalLook;
    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUp() throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException,
            IllegalAccessException {
        // Force the toolkit to load (Shift -> ⇧ when run on OSX)
        Toolkit.getDefaultToolkit();
        // Set the platform specific UI (no html tags on Mac with default ui)
        originalLook = UIManager.getLookAndFeel();
        LafPreference.LAF.put(PlatformManager.getPlatform().getDefaultStyle());
        UIManager.setLookAndFeel(PlatformManager.getPlatform().getDefaultStyle());
        System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    @AfterAll
    public static void tearDown() throws UnsupportedLookAndFeelException {
        // Unfortunately, there is no good way (without reflection) to reset the toolkit.
        // We can reset the LAF though.
        UIManager.setLookAndFeel(originalLook);
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
