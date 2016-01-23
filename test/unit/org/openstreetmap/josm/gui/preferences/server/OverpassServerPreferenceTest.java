// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.server;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link OverpassServerPreference} class.
 */
public class OverpassServerPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link OverpassServerPreference#OverpassServerPreference}.
     */
    @Test
    public void testOverpassServerPreference()  {
        assertNotNull(new OverpassServerPreference.Factory().createPreferenceSetting());
    }
}
