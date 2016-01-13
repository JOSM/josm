// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;

/**
 * Unit tests of {@link TMSLayer} class.
 */
public class TMSLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link TMSLayer#TMSLayer}.
     */
    @Test
    public void testTMSLayer() {
        TMSLayer tms = new TMSLayer(new ImageryInfo("test tms", "http://localhost", "tms", null, null));
        assertEquals(ImageryType.TMS, tms.getInfo().getImageryType());

        TMSLayer bing = new TMSLayer(new ImageryInfo("test bing", "http://localhost", "bing", null, null));
        assertEquals(ImageryType.BING, bing.getInfo().getImageryType());

        TMSLayer scanex = new TMSLayer(new ImageryInfo("test scanex", "http://localhost", "scanex", null, null));
        assertEquals(ImageryType.SCANEX, scanex.getInfo().getImageryType());
    }
}
