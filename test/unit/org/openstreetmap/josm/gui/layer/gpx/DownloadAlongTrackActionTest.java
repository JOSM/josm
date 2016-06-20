// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

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

    private static PleaseWaitRunnable createTask(String file) throws Exception {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), DownloadAlongTrackActionTest.class.getName(), null);
        try {
            Main.getLayerManager().addLayer(layer);
            // Perform action
            final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + file);
            return new DownloadAlongTrackAction(gpx).createTask();
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            Main.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * Test action with nominal data set.
     * @throws Exception if an error occurs
     */
    @Test
    public void testDownload() throws Exception {
        assertNotNull(createTask("minimal.gpx"));
    }

    /**
     * Test action with empty data set.
     * @throws Exception if an error occurs
     */
    @Test
    public void testDownloadEmpty() throws Exception {
        assertNull(createTask("empty.gpx"));
    }
}
