// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.io.ChangesetQuery;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetQueryTask}.
 */
class ChangesetQueryTaskTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask}.
     */
    @Test
    void testChangesetQueryTask() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(new ChangesetQueryTask(parent, new ChangesetQuery()));
    }

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask} - null parent.
     */
    @Test
    void testChangesetQueryTaskNullParent() {
        assertThrows(IllegalArgumentException.class, () -> new ChangesetQueryTask(new ChangesetQuery()));
    }
}
