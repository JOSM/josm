// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.gui.preferences.PreferencesTestUtils;

/**
 * Unit tests of {@link TaggingPresetPreference} class.
 */
class TaggingPresetPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeAll
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link TaggingPresetPreference#TaggingPresetPreference}.
     */
    @Test
    void testTaggingPresetPreference() {
        assertNotNull(new TaggingPresetPreference.Factory().createPreferenceSetting());
    }

    /**
     * Unit test of {@link TaggingPresetPreference#addGui}.
     */
    @Test
    void testAddGui() {
        PreferencesTestUtils.doTestPreferenceSettingAddGui(new TaggingPresetPreference.Factory(), null);
    }
}
