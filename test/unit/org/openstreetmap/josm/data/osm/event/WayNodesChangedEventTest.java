// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link WayNodesChangedEvent} class.
 */
class WayNodesChangedEventTest {
    /**
     * Unit test of {@link WayNodesChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("WAY_NODES_CHANGED", new WayNodesChangedEvent(null, null).toString());
    }
}
