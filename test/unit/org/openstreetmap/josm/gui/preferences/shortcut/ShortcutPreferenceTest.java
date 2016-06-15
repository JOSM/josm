// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.shortcut;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link ShortcutPreference} class.
 */
public class ShortcutPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ShortcutPreference#ShortcutPreference}.
     */
    @Test
    public void testShortcutPreference() {
        assertNotNull(new ShortcutPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link ShortcutPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new ShortcutPreference.Factory(), null);
    }
}
