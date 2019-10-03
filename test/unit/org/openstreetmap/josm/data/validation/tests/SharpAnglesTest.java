// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * JUnit Test of the Sharp Angles validation test.
 */

public class SharpAnglesTest {
    private SharpAngles angles;

    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @Before
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        angles = new SharpAngles();
        angles.initialize();
    }

    /**
     * Check a closed loop with no sharp angles
     */
    @Test
    public void closedLoopNoSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.1, -0.2)), new Node(new LatLon(-0.1, -0.1)));
        way.addNode(way.firstNode());
        angles.visit(way);
        Assert.assertEquals(0, angles.getErrors().size());
    }

    /**
     * Check a closed loop with a sharp angle
     */
    @Test
    public void closedLoopSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.1, -0.2)));
        way.addNode(way.firstNode());
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        Assert.assertEquals(1, angles.getErrors().size());
    }

    /**
     * Check a way for multiple sharp angles
     */
    @Test
    public void testMultipleSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0.005069377713748322, -0.0014832642674429382)),
                new Node(new LatLon(0.005021097951663415, 0.0008636686205880686)),
                new Node(new LatLon(0.005085470967776624, -0.00013411313295197088)),
                new Node(new LatLon(0.005031826787678042, 0.0020116540789620915)));
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        Assert.assertEquals(2, angles.getErrors().size());
    }

    /**
     * Check for no sharp angles
     */
    @Test
    public void testNoSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.2, 0.3)), new Node(new LatLon(0.3, 0.1)));
        angles.visit(way);
        Assert.assertEquals(0, angles.getErrors().size());
    }

    /**
     * Ensure that we aren't accidentally using the same node twice.
     * This was found during initial testing. See way 10041221 (on 20190914)
     */
    @Test
    public void testCheckBadAnglesFromSameNodeTwice() {
        Way way = TestUtils.newWay("highway=service oneway=yes",
                new Node(new LatLon(52.8903308, 8.4302322)),
                new Node(new LatLon(52.8902468, 8.4302138)),
                new Node(new LatLon(52.8902191, 8.4302282)),
                new Node(new LatLon(52.8901155, 8.4304753)),
                new Node(new LatLon(52.8900669, 8.430763)),
                new Node(new LatLon(52.8901138, 8.4308262)),
                new Node(new LatLon(52.8902482, 8.4307568)));
        way.addNode(way.firstNode());
        angles.visit(way);
        Assert.assertEquals(0, angles.getErrors().size());
    }

    /**
     * Check that special cases are ignored
     */
    @Test
    public void testIgnoredCases() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0, 0.01)));
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        Assert.assertEquals(1, angles.getErrors().size());
        angles.getErrors().clear();

        way.put("highway", "rest_area");
        angles.visit(way);
        Assert.assertEquals(0, angles.getErrors().size());

        way.put("highway", "residential");
        angles.visit(way);
        Assert.assertEquals(1, angles.getErrors().size());
        angles.getErrors().clear();
        way.put("area", "yes");
        angles.visit(way);
        Assert.assertEquals(0, angles.getErrors().size());
        way.put("area", "no");
        angles.visit(way);
        Assert.assertEquals(1, angles.getErrors().size());
    }
}
