// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link RelationMembersChangedEvent} class.
 */
class RelationMembersChangedEventTest {
    /**
     * Unit test of {@link RelationMembersChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("RELATION_MEMBERS_CHANGED", new RelationMembersChangedEvent(null, null).toString());
    }
}
