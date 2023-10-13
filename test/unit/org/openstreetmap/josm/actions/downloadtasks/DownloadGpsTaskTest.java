// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;

/**
 * Unit tests for class {@link DownloadGpsTask}.
 */
@BasicWiremock
class DownloadGpsTaskTest extends AbstractDownloadTaskTestParent {

    /**
     * Unit test of {@code DownloadGpsTask#acceptsUrl} method.
     */
    @Test
    void testAcceptsURL() {
        DownloadGpsTask task = new DownloadGpsTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("https://api.openstreetmap.org/api/0.6/trackpoints?bbox=0,51.5,0.25,51.75"));
        assertTrue(task.acceptsUrl("https://api.openstreetmap.org/api/0.6/trackpoints?bbox=0,51.5,0.25,51.75&page=0"));
        assertTrue(task.acceptsUrl("https://api.openstreetmap.org/trace/5000/data"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/trace/5000/data"));
        assertTrue(task.acceptsUrl("http://www.trackmyjourney.co.uk/exportgpx.php?session=S6rZR2Bh6GwX1wpB0C&trk=79292"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/user/simon04/traces/750057"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/edit?gpx=750057"));
        assertTrue(task.acceptsUrl("https://www.openstreetmap.org/edit?gpx=2277313#map=14/-20.7321/-40.5328"));
        assertTrue(task.acceptsUrl("https://tasks.hotosm.org/api/v1/project/4019/tasks_as_gpx?tasks=125&as_file=true"));
        assertTrue(task.acceptsUrl(getRemoteFileUrl()));
    }

    /**
     * Unit test of {@code DownloadGpsTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        mockHttp();
        DownloadGpsTask task = new DownloadGpsTask();
        task.loadUrl(new DownloadParams(), getRemoteFileUrl(), null).get();
        GpxData data = task.getDownloadedData();
        assertNotNull(data);
        assertFalse(data.waypoints.isEmpty());
        assertFalse(data.tracks.isEmpty());
    }

    @Override
    protected String getRemoteFile() {
        return "samples/data.gpx";
    }
}
