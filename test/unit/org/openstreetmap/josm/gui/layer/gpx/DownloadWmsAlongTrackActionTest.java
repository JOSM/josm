// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.awaitility.Awaitility;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.actions.MergeLayerActionTest.MergeLayerExtendedDialogMocker;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.GpxLayerTest;
import org.openstreetmap.josm.gui.layer.TMSLayer;
import org.openstreetmap.josm.gui.layer.gpx.DownloadWmsAlongTrackAction.PrecacheWmsTask;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.TileSourceRule;
import org.openstreetmap.josm.testutils.mockers.JOptionPaneSimpleMocker;

import com.google.common.collect.ImmutableMap;

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
    public JOSMTestRules test = new JOSMTestRules().main().projection().fakeImagery().timeout(20000);

    /**
     * Test action without layer.
     */
    @Test
    public void testNoLayer() {
        TestUtils.assumeWorkingJMockit();
        final JOptionPaneSimpleMocker jopsMocker = new JOptionPaneSimpleMocker(
            ImmutableMap.<String, Object>of("There are no imagery layers.", 0)
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
    public void testTMSLayer() throws Exception {
        TestUtils.assumeWorkingJMockit();
        final MergeLayerExtendedDialogMocker edMocker = new MergeLayerExtendedDialogMocker();
        edMocker.getMockResultMap().put("Please select the imagery layer.", "Download");

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

        assertEquals(1, edMocker.getInvocationLog().size());
        Object[] invocationLogEntry = edMocker.getInvocationLog().get(0);
        assertEquals(1, (int) invocationLogEntry[0]);
        assertEquals("Select imagery layer", invocationLogEntry[2]);
    }
}
