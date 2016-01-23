// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link TaggingPresetPreference} class.
 */
public class TaggingPresetPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link TaggingPresetPreference#TaggingPresetPreference}.
     */
    @Test
    public void testTaggingPresetPreference()  {
        assertNotNull(new TaggingPresetPreference.Factory().createPreferenceSetting());
    }
}
