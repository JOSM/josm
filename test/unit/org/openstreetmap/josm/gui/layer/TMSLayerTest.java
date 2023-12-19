// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link TMSLayer} class.
 */
@Main
@Projection
public class TMSLayerTest {
    /**
     * Creates a new TMS layer.
     * @return a new TMS layer
     */
    public static TMSLayer createTmsLayer() {
        return new TMSLayer(new ImageryInfo("test tms", "http://localhost", "tms", null, null));
    }

    /**
     * Creates a new Bing layer.
     * @return a new Bing layer
     */
    public static TMSLayer createBingLayer() {
        return new TMSLayer(new ImageryInfo("test bing", "http://localhost", "bing", null, null));
    }

    /**
     * Creates a new Scanex layer.
     * @return a new Scanex layer
     */
    public static TMSLayer createScanexLayer() {
        return new TMSLayer(new ImageryInfo("test scanex", "http://localhost", "scanex", null, null));
    }

    private static void test(ImageryType expected, TMSLayer layer) {
        try {
            MainApplication.getLayerManager().addLayer(layer);
            assertEquals(expected, layer.getInfo().getImageryType());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Unit test of {@link TMSLayer#TMSLayer}.
     */
    @Test
    void testTMSLayer() {
        test(ImageryType.TMS, createTmsLayer());
        test(ImageryType.BING, createBingLayer());
        test(ImageryType.SCANEX, createScanexLayer());
    }
}
