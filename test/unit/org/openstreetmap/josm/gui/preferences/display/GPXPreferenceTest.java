// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link GPXPreference} class.
 */
public class GPXPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link GPXPreference.Factory}.
     */
    @Test
    public void testGPXPreference() {
        assertNotNull(new GPXPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link GPXPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new GPXPreference.Factory(), DisplayPreference.class);
    }
}
