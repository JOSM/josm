// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link DrawingPreference} class.
 */
public class DrawingPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link DrawingPreference#DrawingPreference}.
     */
    @Test
    public void testDrawingPreference() {
        assertNotNull(new DrawingPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link DrawingPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new DrawingPreference.Factory(), null);
    }
}
