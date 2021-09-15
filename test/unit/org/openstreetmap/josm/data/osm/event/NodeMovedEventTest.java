// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link NodeMovedEvent} class.
 */
class NodeMovedEventTest {
    /**
     * Unit test of {@link NodeMovedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("NODE_MOVED", new NodeMovedEvent(null, null).toString());
    }
}
