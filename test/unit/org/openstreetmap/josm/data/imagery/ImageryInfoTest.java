// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.imagery;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 *
 * Unit tests for class {@link ImageryInfo}.
 *
 */
public class ImageryInfoTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Test if extended URL is returned properly
     */
    @Test
    public void testGetExtendedUrl() {
        ImageryInfo testImageryTMS =  new ImageryInfo("test imagery", "http://localhost", "tms", null, null);
        testImageryTMS.setDefaultMinZoom(16);
        testImageryTMS.setDefaultMaxZoom(23);
        assertEquals("tms[16,23]:http://localhost", testImageryTMS.getExtendedUrl());
    }
}
