// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * JUnit Test of "Long Segment" validation test.
 */
public class LongSegmentTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static boolean test(int expected, Way w) throws Exception {
        LongSegment test = new LongSegment();
        test.initialize();
        test.startTest(null);
        test.visit(w);
        test.endTest();
        return test.getErrors().size() == expected;
    }

    /**
     * Unit test
     * @throws Exception if any error occurs
     */
    @Test
    public void testLongSegment() throws Exception {
        // Long way
        Way w = new Way();
        // https://www.openstreetmap.org/node/798475224
        w.addNode(new Node(new LatLon(54.1523672, 12.0979025)));
        // https://www.openstreetmap.org/node/468120683
        w.addNode(new Node(new LatLon(54.5737391, 11.9246324)));
        assertTrue(test(1, w));

        // Ferry route
        w.put("route", "ferry");
        assertTrue(test(0, w));

        // Short way
        w = new Way();
        w.addNode(new Node(new LatLon(54.152, 12.097)));
        w.addNode(new Node(new LatLon(54.153, 12.098)));
        assertTrue(test(0, w));
    }
}
