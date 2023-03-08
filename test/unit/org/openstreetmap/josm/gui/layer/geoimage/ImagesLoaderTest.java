// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImagesLoader} class.
 */
@BasicPreferences
class ImagesLoaderTest {

    @AfterEach
    void tearDown() {
        if (ImageViewerDialog.hasInstance()) {
            ImageViewerDialog.getInstance().destroy();
        }
    }

    /**
     * Unit test of {@link ImagesLoader} class.
     * @throws Exception if any error occurs
     */
    @Test
    void testLoader() throws Exception {
        try (InputStream in = TestUtils.getRegressionDataStream(12255, "bobrava2.gpx")) {
            GpxReader reader = new GpxReader(in);
            assertTrue(reader.parse(true));
            GpxLayer gpxLayer = new GpxLayer(reader.getGpxData());
            MainApplication.getLayerManager().addLayer(gpxLayer);
            assertEquals(1, MainApplication.getLayerManager().getLayers().size());
            new ImagesLoader(
                    Collections.singleton(new File(TestUtils.getRegressionDataFile(12255, "G0016941.JPG"))),
                    gpxLayer).run();
            assertEquals(2, MainApplication.getLayerManager().getLayers().size());
            GeoImageLayer layer = MainApplication.getLayerManager().getLayersOfType(GeoImageLayer.class).iterator().next();
            assertEquals(gpxLayer, layer.getGpxLayer());
            List<ImageEntry> images = layer.getImages();
            assertEquals(1, images.size());
            assertEquals("<html>1 image loaded. 0 were found to be GPS tagged.</html>", layer.getInfoComponent());
            assertEquals("<html>1 image loaded. 0 were found to be GPS tagged.</html>", layer.getToolTipText());
        }
    }
}
