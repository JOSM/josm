// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * JUnit Test of the Sharp Angles validation test.
 */
@BasicPreferences
class SharpAnglesTest {
    private SharpAngles angles;

    /**
     * Setup test.
     * @throws Exception if an error occurs
     */
    @BeforeEach
    public void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
        angles = new SharpAngles();
        angles.initialize();
    }

    /**
     * Check a closed loop with no sharp angles
     */
    @Test
    void testClosedLoopNoSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.1, -0.2)), new Node(new LatLon(-0.1, -0.1)));
        way.addNode(way.firstNode());
        angles.visit(way);
        assertEquals(0, angles.getErrors().size());
    }

    /**
     * Check a closed loop with a sharp angle
     */
    @Test
    void testClosedLoopSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.1, -0.2)));
        way.addNode(way.firstNode());
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        assertEquals(1, angles.getErrors().size());
    }

    /**
     * Check a way for multiple sharp angles
     */
    @Test
    void testMultipleSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0.005069377713748322, -0.0014832642674429382)),
                new Node(new LatLon(0.005021097951663415, 0.0008636686205880686)),
                new Node(new LatLon(0.005085470967776624, -0.00013411313295197088)),
                new Node(new LatLon(0.005031826787678042, 0.0020116540789620915)));
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        assertEquals(2, angles.getErrors().size());
    }

    /**
     * Check for no sharp angles
     */
    @Test
    void testNoSharpAngles() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0.2, 0.3)), new Node(new LatLon(0.3, 0.1)));
        angles.visit(way);
        assertEquals(0, angles.getErrors().size());
    }

    /**
     * Ensure that we aren't accidentally using the same node twice.
     * This was found during initial testing. See way 10041221 (on 20190914)
     */
    @Test
    void testCheckBadAnglesFromSameNodeTwice() {
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
        assertEquals(0, angles.getErrors().size());
    }

    /**
     * Check that special cases are ignored
     */
    @Test
    void testIgnoredCases() {
        Way way = TestUtils.newWay("highway=residential",
                new Node(new LatLon(0, 0)), new Node(new LatLon(0.1, 0.1)),
                new Node(new LatLon(0, 0.01)));
        angles.setMaxLength(Double.MAX_VALUE);
        angles.visit(way);
        assertEquals(1, angles.getErrors().size());
        angles.getErrors().clear();

        way.put("highway", "rest_area");
        angles.visit(way);
        assertEquals(0, angles.getErrors().size());

        way.put("highway", "residential");
        angles.visit(way);
        assertEquals(1, angles.getErrors().size());
        angles.getErrors().clear();
        way.put("area", "yes");
        angles.visit(way);
        assertEquals(0, angles.getErrors().size());
        way.put("area", "no");
        angles.visit(way);
        assertEquals(1, angles.getErrors().size());
    }

    static Stream<Arguments> testNonRegression22600() {
        return Stream.of(
                // All invalid nodes
                Arguments.of(new Node(), new Node(), new Node()),
                // Single valid nodes
                Arguments.of(new Node(LatLon.SOUTH_POLE), new Node(), new Node()),
                Arguments.of(new Node(), new Node(LatLon.ZERO), new Node()),
                Arguments.of(new Node(), new Node(), new Node(LatLon.NORTH_POLE)),
                // Two valid nodes
                Arguments.of(new Node(), new Node(LatLon.ZERO), new Node(LatLon.NORTH_POLE)),
                Arguments.of(new Node(LatLon.SOUTH_POLE), new Node(LatLon.ZERO), new Node()),
                Arguments.of(new Node(LatLon.SOUTH_POLE), new Node(), new Node(LatLon.NORTH_POLE)),
                // All valid nodes
                Arguments.of(new Node(LatLon.SOUTH_POLE), new Node(LatLon.ZERO), new Node(LatLon.NORTH_POLE))
        );
    }

    /**
     * Non-regression test for #22600: NPE: Cannot invoke {@link EastNorth#isValid()} because "common" is null
     */
    @ParameterizedTest
    @MethodSource
    void testNonRegression22600(Node node1, Node node2, Node node3) {
        Way invalidWay = new Way();
        invalidWay.put("highway", "trunk");
        invalidWay.setNodes(Arrays.asList(node1, node2, node3));
        assertDoesNotThrow(() -> this.angles.visit(invalidWay));
        for (Node node : invalidWay.getNodes()) {
            node.setCoor(LatLon.ZERO);
            assertDoesNotThrow(() -> this.angles.visit(invalidWay));
        }
    }
}
