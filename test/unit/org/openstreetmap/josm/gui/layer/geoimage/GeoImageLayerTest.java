// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.Loader;
import org.openstreetmap.josm.io.GpxReader;

/**
 * Unit tests of {@link GeoImageLayer} class.
 */
public class GeoImageLayerTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Unit test of {@link Loader} class.
     * @throws Exception if any error occurs
     */
    @Test
    public void testLoader() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(12255, "bobrava2.gpx")) {
            GpxReader reader = new GpxReader(in);
            assertTrue(reader.parse(true));
            GpxLayer gpxLayer = new GpxLayer(reader.getGpxData());
            try {
                Main.main.addLayer(gpxLayer);
                assertEquals(1, Main.map.mapView.getNumLayers());
                new Loader(
                        Collections.singleton(new File(TestUtils.getRegressionDataFile(12255, "G0016941.JPG"))),
                        gpxLayer).run();
                assertEquals(2, Main.map.mapView.getNumLayers());
                GeoImageLayer layer = Main.getLayerManager().getLayersOfType(GeoImageLayer.class).iterator().next();
                try {
                    assertEquals(gpxLayer, layer.getGpxLayer());
                    List<ImageEntry> images = layer.getImages();
                    assertEquals(1, images.size());
                    assertEquals("<html>1 image loaded. 0 were found to be GPS tagged.</html>", layer.getInfoComponent());
                    assertEquals("<html>1 image loaded. 0 were found to be GPS tagged.</html>", layer.getToolTipText());
                } finally {
                    // Ensure we clean the place before leaving, even if test fails.
                    Main.map.mapView.removeLayer(layer);
                }
            } finally {
                // Ensure we clean the place before leaving, even if test fails.
                Main.main.removeLayer(gpxLayer);
            }
        }
    }
}
