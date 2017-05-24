// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DownloadOsmTask}.
 */
public class DownloadOsmTaskTest {

    private static final String REMOTE_FILE = "https://josm.openstreetmap.de/export/head/josm/trunk/data_nodist/direction-arrows.osm";

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().https();

    /**
     * Unit test of {@code DownloadOsmTask#acceptsUrl} method.
     */
    @Test
    public void testAcceptsURL() {
        DownloadOsmTask task = new DownloadOsmTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/node/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/way/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/relation/100"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/nodes?nodes=101,102,103"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/ways?ways=101,102,103"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/relations?relations=101,102,103"));
        assertTrue(task.acceptsUrl(REMOTE_FILE));
    }

    /**
     * Unit test of {@code DownloadOsmTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    public void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        DownloadOsmTask task = new DownloadOsmTask();
        task.loadUrl(false, REMOTE_FILE, null).get();
        DataSet ds = task.getDownloadedData();
        assertNotNull(ds);
        assertFalse(ds.getNodes().isEmpty());
        assertFalse(ds.getWays().isEmpty());
    }
}
