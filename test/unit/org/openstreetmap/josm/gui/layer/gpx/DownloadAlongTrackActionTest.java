// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertNotNull;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.GpxReaderTest;

/**
 * Unit tests of {@link DownloadAlongTrackAction} class.
 */
public class DownloadAlongTrackActionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
    }

    /**
     * Test action.
     * @throws Exception if an error occurs
     */
    @Test
    public void testDownload() throws Exception {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), getClass().getName(), null);
        try {
            Main.main.addLayer(layer);
            // Perform action
            final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "minimal.gpx");
            PleaseWaitRunnable task = new DownloadAlongTrackAction(gpx).createTask();
            assertNotNull(task);
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.main.removeLayer(layer);
        }
    }
}
