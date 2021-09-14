// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openstreetmap.josm.data.osm.NoteData;
import org.openstreetmap.josm.testutils.annotations.BasicWiremock;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

/**
 * Unit tests for class {@link DownloadNotesTask}.
 */
@ExtendWith(BasicWiremock.OsmApiExtension.class)
@LayerEnvironment
class DownloadNotesTaskTest extends AbstractDownloadTaskTestParent {

    /**
     * Unit test of {@code DownloadNotesTask#acceptsUrl} method.
     */
    @Test
    void testAcceptsURL() {
        DownloadNotesTask task = new DownloadNotesTask();
        assertFalse(task.acceptsUrl(null));
        assertFalse(task.acceptsUrl(""));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.json?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.xml?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl("http://api.openstreetmap.org/api/0.6/notes.gpx?bbox=-0.65094,51.312159,0.374908,51.669148"));
        assertTrue(task.acceptsUrl(getRemoteFileUrl()));
    }

    /**
     * Unit test of {@code DownloadNotesTask#loadUrl} method with an external file.
     * @throws ExecutionException if the computation threw an exception
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    @Test
    void testDownloadExternalFile() throws InterruptedException, ExecutionException {
        mockHttp();
        DownloadNotesTask task = new DownloadNotesTask();
        task.loadUrl(new DownloadParams(), getRemoteFileUrl(), null).get();
        NoteData data = task.getDownloadedData();
        assertNotNull(data);
        assertFalse(data.getNotes().isEmpty());
    }

    @Override
    protected String getRemoteFile() {
        return "samples/data.osn";
    }
}
