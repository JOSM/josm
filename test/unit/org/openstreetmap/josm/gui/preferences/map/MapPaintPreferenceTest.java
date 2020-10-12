// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link MapPaintPreference} class.
 */
public class MapPaintPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link MapPaintPreference#MapPaintPreference}.
     */
    @Test
    public void testMapPaintPreference() {
        assertNotNull(new MapPaintPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link MapPaintPreference#addGui}.
     */
    @Test
    public void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new MapPaintPreference.Factory(), null);
    }
}
