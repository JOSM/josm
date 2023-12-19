// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.actions.MergeLayerActionTest.MergeLayerExtendedDialogMocker;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction.PrecacheWmsTask;
import org.openstreetmap.josm.testutils.annotations.FakeImagery;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

/**
 * Unit tests of {@link DownloadWmsAlongTrackAction} class.
 */
@FakeImagery
@Main
@Projection
@Timeout(20)
class DownloadWmsAlongTrackActionTest {

    /**
     * Test action without layer.
     */
    @Test
    void testNoLayer() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            Collections.singletonMap("There are no imagery layers.", 0)
        );

        assertNull(new DownloadWmsAlongTrackAction(new GpxData()).createTask());

        assertEquals(1, jopsMocker.getInvocationLog().size());
        Object[] invocationLogEntry = jopsMocker.getInvocationLog().get(0);
        assertEquals(0, (int) invocationLogEntry[0]);
        assertEquals("No imagery layers", invocationLogEntry[2]);
    }

    /**
     * Test action with a TMS layer.
     * @throws Exception if an error occurs
     */
    @Test
    void testTMSLayer(FakeImagery.FakeImageryWireMockExtension fakeImageryWireMockExtension) throws Exception {
        TestUtils.assumeWorkingJMockit();
        final MergeLayerExtendedDialogMocker edMocker = new MergeLayerExtendedDialogMocker();
        edMocker.getMockResultMap().put("Please select the imagery layer.", "Download");

        final TMSLayer layer = new TMSLayer(
                fakeImageryWireMockExtension.getSourcesList().get(0).getImageryInfo(fakeImageryWireMockExtension.getRuntimeInfo().getHttpPort())
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

        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Select imagery layer", invocationLogEntry[2]);
    }
}
