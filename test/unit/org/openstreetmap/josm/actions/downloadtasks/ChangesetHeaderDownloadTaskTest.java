// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link ChangesetHeaderDownloadTask}.
 */
class ChangesetHeaderDownloadTaskTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets}.
     */
    @Test
    void testBuildTaskForChangesets() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.singleton(new Changeset(1))));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.<Changeset>singleton(null)));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, null));
    }

    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets} - null parent.
     */
    @Test
    void testBuildTaskForChangesetsNullParent() {
        assertThrows(NullPointerException.class,
                () -> ChangesetHeaderDownloadTask.buildTaskForChangesets(Collections.singleton(new Changeset(1))));
    }
}
