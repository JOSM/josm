// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JUnit Test of Multipolygon validation test.
 */
class DirectionNodesTest {


    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test all error cases manually created in direction-nodes.osm.
     * @throws Exception in case of error
     */
    @Test
    void testMultipolygonFile() throws Exception {
        final DirectionNodes test = new DirectionNodes();
        ValidatorTestUtils.testSampleFile("nodist/data/direction-nodes.osm",
                ds -> ds.getNodes().stream().filter(OsmPrimitive::hasKeys).collect(Collectors.toList()),
        null, test);

    }

}
