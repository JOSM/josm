// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.imagery;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ImageryPreference} class.
 */
public class ImageryPreferenceTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link ImageryPreference#ImageryPreference}.
     */
    @Test
    public void testImageryPreference()  {
        assertNotNull(new ImageryPreference.Factory().createPreferenceSetting());
    }
}
