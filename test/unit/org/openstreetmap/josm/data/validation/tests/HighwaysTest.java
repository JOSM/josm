// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.io.OsmReader;

/**
 * Unit test of {@link HighwaysTest}.
 */
public class HighwaysTest {

    /**
     * Setup test.
     */
    @Before
    public void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static Way createTestSetting(String highway, String highwayLink) {
        DataSet ds = new DataSet();

        Node n00 = new Node(LatLon.ZERO);
        Node n10 = new Node(new LatLon(1, 0));
        Node n20 = new Node(new LatLon(2, 0));
        Node n01 = new Node(new LatLon(0, 1));
        Node n11 = new Node(new LatLon(1, 1));
        Node n21 = new Node(new LatLon(2, 1));

        ds.addPrimitive(n00);
        ds.addPrimitive(n10);
        ds.addPrimitive(n20);
        ds.addPrimitive(n01);
        ds.addPrimitive(n11);
        ds.addPrimitive(n21);

        Way major = new Way();
        major.addNode(n00);
        major.addNode(n10);
        major.addNode(n20);
        major.put("highway", highway);
        Way link = new Way();
        link.addNode(n10);
        link.addNode(n11);
        link.put("highway", highwayLink);
        Way unclassified = new Way();
        unclassified.addNode(n01);
        unclassified.addNode(n11);
        unclassified.addNode(n21);
        unclassified.put("highway", "unclassified");

        ds.addPrimitive(major);
        ds.addPrimitive(link);
        ds.addPrimitive(unclassified);

        return link;
    }

    /**
     * Unit test of {@link Highways#isHighwayLinkOkay}.
     */
    @Test
    public void testCombinations() {
        assertTrue(Highways.isHighwayLinkOkay(createTestSetting("primary", "primary_link")));
        assertTrue(Highways.isHighwayLinkOkay(createTestSetting("primary", "primary")));
        assertFalse(Highways.isHighwayLinkOkay(createTestSetting("primary", "secondary_link")));
        assertFalse(Highways.isHighwayLinkOkay(createTestSetting("secondary", "primary_link")));
        assertFalse(Highways.isHighwayLinkOkay(createTestSetting("secondary", "tertiary_link")));
        assertTrue(Highways.isHighwayLinkOkay(createTestSetting("residential", "residential")));
    }

    /**
     * Test source:maxspeed in United Kingdom.
     */
    @Test
    public void testSourceMaxSpeedUnitedKingdom() {
        Way link = createTestSetting("primary", "primary");
        link.put("maxspeed", "60 mph");
        link.put("source:maxspeed", "UK:nsl_single");
        Highways test = new Highways();
        test.visit(link);
        assertEquals(1, test.getErrors().size());
        TestError error = test.getErrors().get(0);
        assertTrue(error.isFixable());
        assertTrue(error.getFix().executeCommand());
        assertEquals("GB:nsl_single", link.get("source:maxspeed"));
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/14891">Bug #14891</a>.
     * @throws Exception if an error occurs
     */
    @Test
    public void testTicket14891() throws Exception {
        try (InputStream is = TestUtils.getRegressionDataStream(14891, "14891.osm.bz2")) {
            Collection<Way> ways = OsmReader.parseDataSet(is, null).getWays();
            Way roundabout = ways.stream().filter(w -> 10068083 == w.getId()).findFirst().get();
            Highways test = new Highways();
            test.visit(roundabout);
            if (!test.getErrors().isEmpty()) {
                fail(test.getErrors().get(0).getMessage());
            }
        }
    }
}
