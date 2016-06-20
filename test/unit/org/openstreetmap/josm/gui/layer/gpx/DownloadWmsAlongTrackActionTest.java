// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction.PrecacheWmsTask;

/**
 * Unit tests of {@link DownloadWmsAlongTrackAction} class.
 */
public class DownloadWmsAlongTrackActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test action without layer.
     */
    @Test
    public void testNoLayer() {
        assertNull(new DownloadWmsAlongTrackAction(new GpxData()).createTask());
    }

    /**
     * Test action with a TMS layer.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTMSLayer() throws Exception {
        // Create new TMS layer and clear cache
        TMSLayer layer = new TMSLayer(new ImageryInfo("OSM TMS", "https://a.tile.openstreetmap.org/{zoom}/{x}/{y}.png", "tms", null, null));
        try {
            Main.getLayerManager().addLayer(layer);
            TMSLayer.getCache().clear();
            assertTrue(TMSLayer.getCache().getMatching(".*").isEmpty());
            // Perform action
            PrecacheWmsTask task = new DownloadWmsAlongTrackAction(GpxLayerTest.getMinimalGpxData()).createTask();
            assertNotNull(task);
            task.run();
            // Ensure cache is not empty
            assertFalse(TMSLayer.getCache().getMatching(".*").isEmpty());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.getLayerManager().removeLayer(layer);
        }
    }
}
