// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.osm.DataSet;

/**
 * JUnit Test of {@link DirectionNodes} validation test.
 */
class DirectionNodesTest {
    /**
     * Test all error cases manually created in direction-nodes.osm.
     * @throws Exception in case of error
     */
    @Test
    void testDirectionNodesTestFile() throws Exception {
        final DirectionNodes test = new DirectionNodes();
        ValidatorTestUtils.testSampleFile("nodist/data/direction-nodes.osm", DataSet::getNodes, null, test);
    }
}
