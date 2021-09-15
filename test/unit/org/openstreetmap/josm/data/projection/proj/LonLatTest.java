// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LonLat}.
 */
class LonLatTest {
    /**
     * Test {@link LonLat#lonIsLinearToEast}
     */
    @Test
    void testLonIsLinearToEast() {
        assertFalse(new LonLat().lonIsLinearToEast());
    }
}
