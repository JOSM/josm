// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.advanced;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

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
    public void testAdvancedPreference()  {
        assertNotNull(new AdvancedPreference.Factory().createPreferenceSetting());
    }
}
