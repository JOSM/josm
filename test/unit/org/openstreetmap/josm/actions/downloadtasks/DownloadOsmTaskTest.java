// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.HTTP;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

/**
 * Unit tests for class {@link DownloadOsmTask}.
 */
@BasicPreferences
@HTTP
@LayerEnvironment
class DownloadOsmTaskTest extends AbstractDownloadTaskTestParent {

    /**
     * Unit test of {@code DownloadOsmTask#acceptsUrl} method.
     */
    @Test
    void testAcceptsURL() {
        DownloadOsmTask task = new DownloadOsmTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/node/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/way/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/relation/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/nodes?nodes=101,102,103"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/ways?ways=101,102,103"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/relations?relations=101,102,103"));
        assertTrue(task.acceptsUrl(getRemoteFileUrl()));
    }

    /**
     * Unit test of {@code DownloadOsmTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        mockHttp();
        DownloadOsmTask task = new DownloadOsmTask();
        task.loadUrl(new DownloadParams(), getRemoteFileUrl(), null).get();
        DataSet ds = task.getDownloadedData();
        assertNotNull(ds);
        assertFalse(ds.getNodes().isEmpty());
        assertFalse(ds.getWays().isEmpty());
    }

    @Override
    protected String getRemoteFile() {
        return "samples/data.osm";
    }
}
