// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link ProjectionBounds}.
 */
class ProjectionBoundsTest {

    /**
     * Unit test of {@link ProjectionBounds#toString}
     */
    @Test
    void testToString() {
        assertEquals("ProjectionBounds[1.0,2.0,3.0,4.0]", new ProjectionBounds(1, 2, 3, 4).toString());
    }
}
