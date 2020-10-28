// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ShortcutPreference} class.
 */
class ShortcutPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ShortcutPreference#ShortcutPreference}.
     */
    @Test
    void testShortcutPreference() {
        assertNotNull(new ShortcutPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ShortcutPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ShortcutPreference.Factory(), null);
    }
}
