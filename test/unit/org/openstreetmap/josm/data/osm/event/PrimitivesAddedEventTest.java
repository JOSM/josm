// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PrimitivesAddedEvent} class.
 */
class PrimitivesAddedEventTest {
    /**
     * Unit test of {@link PrimitivesAddedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("PRIMITIVES_ADDED", new PrimitivesAddedEvent(null, Collections.emptyList(), false).toString());
    }
}
