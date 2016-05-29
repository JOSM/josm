// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link ImageryLayer} class.
 */
public class ImageryLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link ImageryLayer.ColorfulImageProcessor#toString}
     *          and {@link ImageryLayer.GammaImageProcessor#toString()}.
     *          and {@link ImageryLayer.SharpenImageProcessor#toString()}.
     */
    @Test
    public void testToString() {
        ImageryLayer layer = TMSLayerTest.createTmsLayer();
        assertEquals("ColorfulImageProcessor [colorfulness=1.0]", layer.collorfulnessImageProcessor.toString());
        assertEquals("GammaImageProcessor [gamma=1.0]", layer.gammaImageProcessor.toString());
        assertEquals("SharpenImageProcessor [sharpenLevel=1.0]", layer.sharpenImageProcessor.toString());
    }
}
