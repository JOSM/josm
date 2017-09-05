// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import javax.swing.JPanel;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.UserIdentityManager;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link DownloadOpenChangesetsTask} class.
 */
public class DownloadOpenChangesetsTaskTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().devAPI();

    /**
     * Test of {@link DownloadOpenChangesetsTask} class.
     */
    @Test
    public void testDownloadOpenChangesetsTask() {
        DownloadOpenChangesetsTask task = new DownloadOpenChangesetsTask(new JPanel());
        assertNull(task.getChangesets());

        assertTrue(UserIdentityManager.getInstance().isAnonymous());
        task.run();
        assertNull(task.getChangesets());

        task = new DownloadOpenChangesetsTask(new JPanel());
        UserIdentityManager.getInstance().setPartiallyIdentified(System.getProperty("osm.username", "josm_test"));
        assertTrue(UserIdentityManager.getInstance().isPartiallyIdentified());
        task.run();
        assertNotNull(task.getChangesets());
    }
}
