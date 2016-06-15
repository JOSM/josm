// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link AdvancedPreference} class.
 */
public class AdvancedPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AdvancedPreference#AdvancedPreference}.
     */
    @Test
    public void testAdvancedPreference() {
        assertNotNull(new AdvancedPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link AdvancedPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new AdvancedPreference.Factory(), null);
    }
}
