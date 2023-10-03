// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.downloadtasks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.Component;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.io.ChangesetQuery;

/**
 * Unit tests for class {@link ChangesetQueryTask}.
 */
class ChangesetQueryTaskTest {
    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask}.
     */
    @Test
    void testChangesetQueryTask() {
        Component parent = new Component() {
            // empty component
        };
        assertDoesNotThrow(() -> new ChangesetQueryTask(parent, new ChangesetQuery()));
    }

    /**
     * Unit test of {@code ChangesetQueryTask#ChangesetQueryTask} - null parent.
     */
    @Test
    void testChangesetQueryTaskNullParent() {
        final ChangesetQuery query = new ChangesetQuery();
        assertThrows(IllegalArgumentException.class, () -> new ChangesetQueryTask(query));
    }
}
