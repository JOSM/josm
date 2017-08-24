// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.gpx;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.PleaseWaitRunnable;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadAlongTrackAction} class.
 */
public class DownloadAlongTrackActionTest {

    /**
     * The test rules for this test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().platform();

    private static PleaseWaitRunnable createTask(String file) throws Exception {
        final OsmDataLayer layer = new OsmDataLayer(new DataSet(), DownloadAlongTrackActionTest.class.getName(), null);
        try {
            MainApplication.getLayerManager().addLayer(layer);
            // Perform action
            final GpxData gpx = GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + file);
            return new DownloadAlongTrackAction(gpx).createTask();
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(layer);
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
