// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link ChangesetIdChangedEvent} class.
 */
class ChangesetIdChangedEventTest {
    /**
     * Unit test of {@link ChangesetIdChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("CHANGESET_ID_CHANGED", new ChangesetIdChangedEvent(null, null, 0, 0).toString());
    }
}
