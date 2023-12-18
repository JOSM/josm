// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit test for {@link CycleDetector} validation test.
 */
@BasicPreferences
class CycleDetectorTest {

    @Test
    void testCycleDetection() throws Exception {
        CycleDetector cycleDetector = new CycleDetector();
        DataSet ds = OsmReader.parseDataSet(TestUtils.getRegressionDataStream(21881, "CycleDetector_test_wikipedia.osm"), null);
        cycleDetector.startTest(null);
        cycleDetector.visit(ds.allPrimitives());
        cycleDetector.endTest();

        // we have 4 cycles in the test file
        assertEquals(4, cycleDetector.getErrors().size());
    }
}
