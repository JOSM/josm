// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit Test of "Long Segment" validation test.
 */
@BasicPreferences
class LongSegmentTest {
    private static int test(Way w) throws Exception {
        LongSegment test = new LongSegment();
        test.initialize();
        test.startTest(null);
        test.visit(w);
        test.endTest();
        return test.getErrors().size();
    }

    /**
     * Unit test
     * @throws Exception if any error occurs
     */
    @Test
    void testLongSegment() throws Exception {
        // Long way
        Way w = new Way();
        // https://www.openstreetmap.org/node/798475224
        w.addNode(new Node(new LatLon(54.1523672, 12.0979025)));
        // https://www.openstreetmap.org/node/468120683
        w.addNode(new Node(new LatLon(54.5737391, 11.9246324)));
        assertEquals(1, test(w));

        // Ferry route
        w.put("route", "ferry");
        assertEquals(0, test(w));

        // Short way
        w = new Way();
        w.addNode(new Node(new LatLon(54.152, 12.097)));
        w.addNode(new Node(new LatLon(54.153, 12.098)));
        assertEquals(0, test(w));
    }
}
