// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.display;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

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
    public void testDisplayPreference()  {
        assertNotNull(new DisplayPreference.Factory().createPreferenceSetting());
    }
}
