// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.Changeset;

/**
 * Unit tests for class {@link ChangesetHeaderDownloadTask}.
 */
class ChangesetHeaderDownloadTaskTest {
    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets}.
     */
    @Test
    void testBuildTaskForChangesets() {
        Component parent = new Component() {
            // empty component
        };
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.singleton(new Changeset(1))));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, Collections.singleton(null)));
        assertNotNull(ChangesetHeaderDownloadTask.buildTaskForChangesets(parent, null));
    }

    /**
     * Unit test of {@code ChangesetHeaderDownloadTask#buildTaskForChangesets} - null parent.
     */
    @Test
    void testBuildTaskForChangesetsNullParent() {
        final Collection<Changeset> changesets = Collections.singleton(new Changeset(1));
        assertThrows(NullPointerException.class,
                () -> ChangesetHeaderDownloadTask.buildTaskForChangesets(changesets));
    }
}
