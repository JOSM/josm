// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Mercator}.
 */
class MercatorTest {
    /**
     * Test {@link Mercator#lonIsLinearToEast}
     */
    @Test
    void testLonIsLinearToEast() {
        assertTrue(new Mercator().lonIsLinearToEast());
    }
}
