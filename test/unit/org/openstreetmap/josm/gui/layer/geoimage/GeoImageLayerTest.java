// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.geoimage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.layer.geoimage.GeoImageLayer.Loader;
import org.openstreetmap.josm.io.GpxReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link GeoImageLayer} class.
 */
public class GeoImageLayerTest {
    /**
     * We need prefs for this.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();


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
            MainApplication.getLayerManager().addLayer(gpxLayer);
            assertEquals(1, MainApplication.getLayerManager().getLayers().size());
            new Loader(
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

    /**
     * Test that {@link GeoImageLayer#mergeFrom} throws IAE for invalid arguments
     */
    @Test(expected = IllegalArgumentException.class)
    public void testMergeFromIAE() {
        new GeoImageLayer(null, null).mergeFrom(new OsmDataLayer(new DataSet(), "", null));
    }
}
