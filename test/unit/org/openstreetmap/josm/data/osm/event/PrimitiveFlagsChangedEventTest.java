// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PrimitiveFlagsChangedEvent} class.
 */
class PrimitiveFlagsChangedEventTest {
    /**
     * Unit test of {@link PrimitiveFlagsChangedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("PRIMITIVE_FLAGS_CHANGED", new PrimitiveFlagsChangedEvent(null, null).toString());
    }
}
