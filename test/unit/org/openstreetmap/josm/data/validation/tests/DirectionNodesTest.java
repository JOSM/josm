// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit Test of Multipolygon validation test.
 */
@BasicPreferences
class DirectionNodesTest {
    /**
     * Test all error cases manually created in direction-nodes.osm.
     * @throws Exception in case of error
     */
    @Test
    void testDirectionsNodesTestFile() throws Exception {
        final DirectionNodes test = new DirectionNodes();
        ValidatorTestUtils.testSampleFile("nodist/data/direction-nodes.osm", ds -> ds.getNodes(), null, test);
    }
}
