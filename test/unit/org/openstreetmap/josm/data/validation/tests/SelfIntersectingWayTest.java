// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

/**
 * JUnit Test of {@link SelfIntersectingWay} validation test.
 */
class SelfIntersectingWayTest {

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @BeforeAll
    public static void setUp() throws Exception {
        JOSMFixture.createUnitTestFixture().init();
    }

    private static List<Node> createNodes() {
        List<Node> nodes = IntStream.range(0, 6).mapToObj(i -> new Node(i + 1)).collect(Collectors.toList());
        nodes.get(0).setCoor(new LatLon(34.2680, 133.563));
        nodes.get(1).setCoor(new LatLon(34.2509, 133.548));
        nodes.get(2).setCoor(new LatLon(34.2446, 133.566));
        nodes.get(3).setCoor(new LatLon(34.2681, 133.560));
        nodes.get(4).setCoor(new LatLon(34.2656, 133.561));
        nodes.get(5).setCoor(new LatLon(34.2655, 133.562));
        return nodes;
    }

    /**
     * Self-Intersection at inner node (not first / last).
     */
    @Test
    void testUnclosedWayNormal() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(1)); // problem
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(1)));
    }

    static List<Arguments> testOkInnerNode() {
        // The first two are duplicates
        return Arrays.asList(
                Arguments.of("testUnclosedWayFirst - First node is identical to an inner node (\"P\"-Shape)",
                        new int[] {0, 1, 2, 0 /* problem */, 3, 4}),
                Arguments.of("testUnclosedWayFirstRepeated - First node is identical to an inner node (\"P\"-Shape)",
                        new int[] {0, 1, 2, 0 /* problem */, 3, 4}),
                Arguments.of("testUnclosedWayLast - Last node is identical to an inner node (\"b\"-Shape)",
                        new int[] {0, 1 /* problem */, 2, 3, 4, 1})
        );
    }

    /**
     * The starting or ending nodes are also an inner node.
     * This is considered okay.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("testOkInnerNode")
    void testUnclosedWayFirst(String description, int[] nodeIndex) {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>(nodeIndex.length);
        for (int i : nodeIndex) {
            wayNodes.add(nodes.get(i));
        }
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(0, test.getErrors().size());
    }

    /**
     * Both endpoints join at one inner node ("8"-shape).
     * This is considered to be an error.
     */
    @Test
    void testClosedWay() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0)); // problem
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        wayNodes.add(nodes.get(0));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    void testSpikeWithStartInClosedWay() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(0)); // problem
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        wayNodes.add(nodes.get(0));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    void testSpikeWithEndInClosedWay() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0)); // problem
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(0));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    void testSpikeInClosedWay() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(2)));
    }

    /**
     * Closed way with barbell shape (a-b-c-a-d-e-f-d).
     * This is considered to be an error.
     */
    @Test
    void testClosedWayBarbell() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        wayNodes.add(nodes.get(5));
        wayNodes.add(nodes.get(3));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        assertEquals(1, test.getErrors().size());
        assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(3)));
    }

}
