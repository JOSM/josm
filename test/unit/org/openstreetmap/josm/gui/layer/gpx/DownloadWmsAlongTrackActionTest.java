// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.awaitility.Awaitility;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction.PrecacheWmsTask;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.TileSourceRule;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadWmsAlongTrackAction} class.
 */
public class DownloadWmsAlongTrackActionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().platform().main().projection().fakeImagery().timeout(20000);

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
    @Ignore("Test fails since r14052 - see #16590")
    public void testTMSLayer() throws Exception {
        final TileSourceRule tileSourceRule = this.test.getTileSourceRule();

        final TMSLayer layer = new TMSLayer(
            tileSourceRule.getSourcesList().get(0).getImageryInfo(tileSourceRule.port())
        );
        try {
            MainApplication.getLayerManager().addLayer(layer);
            TMSLayer.getCache().clear();
            assertTrue(TMSLayer.getCache().getMatching(".*").isEmpty());
            // Perform action
            PrecacheWmsTask task = new DownloadWmsAlongTrackAction(GpxLayerTest.getMinimalGpxData()).createTask();
            assertNotNull(task);
            task.run();
            // Ensure cache is (eventually) not empty
            Awaitility.await().atMost(10000, MILLISECONDS).until(() -> !TMSLayer.getCache().getMatching(".*").isEmpty());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }
}
