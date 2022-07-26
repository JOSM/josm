// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link DataChangedEvent} class.
 */
class DataChangedEventTest {
    /**
     * Unit test of {@link DataChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("DATA_CHANGED", new DataChangedEvent(null).toString());
    }
}
