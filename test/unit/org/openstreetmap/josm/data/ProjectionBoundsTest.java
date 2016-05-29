// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Unit tests for class {@link ProjectionBounds}.
 */
public class ProjectionBoundsTest {

    /**
     * Unit test of {@link ProjectionBounds#toString}
     */
    @Test
    public void testToString() {
        assertEquals("ProjectionBounds[1.0,2.0,3.0,4.0]", new ProjectionBounds(1, 2, 3, 4).toString());
    }
}
