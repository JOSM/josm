// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.Assert.assertNotNull;

import java.awt.Component;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetQueryTask}.
 */
public class ChangesetQueryTaskTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask}.
     */
    @Test
    public void testChangesetQueryTask() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(new ChangesetQueryTask(parent, new ChangesetQuery()));
    }

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask} - null parent.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testChangesetQueryTaskNullParent() {
        new ChangesetQueryTask(new ChangesetQuery());
    }
}
