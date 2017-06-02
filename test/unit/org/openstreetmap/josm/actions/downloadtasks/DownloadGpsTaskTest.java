// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DownloadGpsTask}.
 */
public class DownloadGpsTaskTest {

    private static final String REMOTE_FILE = "https://josm.openstreetmap.de/export/head/josm/trunk/data_nodist/munich.gpx";

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https();

    /**
     * Unit test of {@code DownloadGpsTask#acceptsUrl} method.
     */
    @Test
    public void testAcceptsURL() {
        DownloadGpsTask task = new DownloadGpsTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/trackpoints?bbox=0,51.5,0.25,51.75"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/trackpoints?bbox=0,51.5,0.25,51.75&page=0"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/trace/5000/data"));
        assertTrue(task.acceptsUrl("http://www.openstreetmap.org/trace/5000/data"));
        assertTrue(task.acceptsUrl("http://www.trackmyjourney.co.uk/exportgpx.php?session=S6rZR2Bh6GwX1wpB0C&trk=79292"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/user/simon04/traces/750057"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/edit?gpx=750057"));
        assertTrue(task.acceptsUrl("http://www.openstreetmap.org/edit?gpx=2277313#map=14/-20.7321/-40.5328"));
        assertTrue(task.acceptsUrl(REMOTE_FILE));
    }

    /**
     * Unit test of {@code DownloadGpsTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    public void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        DownloadGpsTask task = new DownloadGpsTask();
        task.loadUrl(false, REMOTE_FILE, null).get();
        GpxData data = task.getDownloadedData();
        assertNotNull(data);
        assertFalse(data.waypoints.isEmpty());
        assertFalse(data.tracks.isEmpty());
    }
}
