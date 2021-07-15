// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection;

import static org.junit.Assert.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests of {@link Projections}.
 */
class ProjectionsTest {

    @Test
    void testGetProjectionByCode_nullSafe() {
        assertNull(Projections.getProjectionByCode(null));
    }
}
