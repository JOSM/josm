// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.awt.geom.Area;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DownloadTaskList}.
 */
public class DownloadTaskListTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code DownloadTaskList#DownloadTaskList}.
     */
    @Test
    public void testDownloadTaskList() {
        assertTrue(new DownloadTaskList().getDownloadedPrimitives().isEmpty());
    }

    /**
     * Unit test of {@code DownloadTaskList#download} - empty cases.
     * @throws Exception in case of error
     */
    @Test
    public void testDownloadAreaEmpty() throws Exception {
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
