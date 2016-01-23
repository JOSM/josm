// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.projection;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ProjectionPreference} class.
 */
public class ProjectionPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ProjectionPreference#ProjectionPreference}.
     */
    @Test
    public void testProjectionPreference()  {
        assertNotNull(new ProjectionPreference.Factory().createPreferenceSetting());
    }
}
