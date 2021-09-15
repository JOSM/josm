// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.event;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link PrimitivesRemovedEvent} class.
 */
class PrimitivesRemovedEventTest {
    /**
     * Unit test of {@link PrimitivesRemovedEvent#toString}.
     */
    @Test
    void testToString() {
        assertEquals("PRIMITIVES_REMOVED", new PrimitivesRemovedEvent(null, Collections.emptyList(), false).toString());
    }
}
