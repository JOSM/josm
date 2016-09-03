// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertNotNull;

import java.awt.Component;
import java.util.Arrays;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetContentDownloadTask}.
 */
public class ChangesetContentDownloadTaskTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code ChangesetContentDownloadTask#ChangesetContentDownloadTask}.
     */
    @Test
    public void testChangesetContentDownloadTask() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(new ChangesetContentDownloadTask(parent, 1));
        assertNotNull(new ChangesetContentDownloadTask(parent, Arrays.asList(1, 2)));
        assertNotNull(new ChangesetContentDownloadTask(parent, null));
    }

    /**
     * Unit test of {@code ChangesetContentDownloadTask#ChangesetContentDownloadTask} - invalid changeset id.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChangesetContentDownloadTaskInvalidId() {
        new ChangesetContentDownloadTask(0);
    }

    /**
     * Unit test of {@code ChangesetContentDownloadTask#ChangesetContentDownloadTask} - null parent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChangesetContentDownloadTaskNullParent1() {
        new ChangesetContentDownloadTask(1);
    }

    /**
     * Unit test of {@code ChangesetContentDownloadTask#ChangesetContentDownloadTask} - null parent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChangesetContentDownloadTaskNullParent2() {
        new ChangesetContentDownloadTask(Arrays.asList(1, 2));
    }
}
