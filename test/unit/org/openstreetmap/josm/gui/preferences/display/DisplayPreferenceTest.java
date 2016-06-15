// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link DisplayPreference} class.
 */
public class DisplayPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link DisplayPreference#DisplayPreference}.
     */
    @Test
    public void testDisplayPreference() {
        assertNotNull(new DisplayPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link DisplayPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new DisplayPreference.Factory(), null);
    }
}
