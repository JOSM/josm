// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.awt.geom.Area;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;

import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests for class {@link DownloadTaskList}.
 */
@BasicPreferences
class DownloadTaskListTest {
    /**
     * Unit test of {@code DownloadTaskList#DownloadTaskList}.
     */
    @Test
    void testDownloadTaskList() {
        assertTrue(new DownloadTaskList().getDownloadedPrimitives().isEmpty());
    }

    /**
     * Unit test of {@code DownloadTaskList#download} - empty cases.
     * @throws Exception in case of error
     */
    @Test
    void testDownloadAreaEmpty() throws Exception {
        DownloadTaskList list = new DownloadTaskList();
        assertNull(list.download(false,
                Collections.<Area>emptyList(), true, true, NullProgressMonitor.INSTANCE).get());
        assertTrue(list.getDownloadedPrimitives().isEmpty());
        assertNull(list.download(false,
                Collections.<Area>emptyList(), false, false, NullProgressMonitor.INSTANCE).get());
        assertTrue(list.getDownloadedPrimitives().isEmpty());
        assertNull(list.download(false,
                Collections.<Area>singletonList(new Area(new Bounds(0, 0, true).asRect())), false, false, NullProgressMonitor.INSTANCE).get());
        assertTrue(list.getDownloadedPrimitives().isEmpty());
    }
}

