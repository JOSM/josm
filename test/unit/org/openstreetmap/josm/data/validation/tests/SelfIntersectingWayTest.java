// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Way;

/**
 * JUnit Test of Multipolygon validation test.
 */
public class SelfIntersectingWayTest {

    /**
     * Setup test.
     * @throws Exception if test cannot be initialized
     */
    @BeforeClass
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
    public void testUnclosedWayNormal() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(1)));
    }

    /**
     * First node is identical to an inner node ("P"-Shape).
     * This is considered okay.
     */
    @Test
    public void testUnclosedWayFirst() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0)); // problem
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        Assert.assertEquals(0, test.getErrors().size());
    }

    /**
     * First node is identical to an inner node ("P"-Shape).
     * This is considered okay.
     */
    @Test
    public void testUnclosedWayFirstRepeated() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1));
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        Assert.assertEquals(0, test.getErrors().size());
    }

    /**
     * Last node is identical to an inner node ("b"-Shape).
     * This is considered okay.
     */
    @Test
    public void testUnclosedWayLast() {
        List<Node> nodes = createNodes();

        Way w = (Way) OsmUtils.createPrimitive("way ");
        List<Node> wayNodes = new ArrayList<>();
        wayNodes.add(nodes.get(0));
        wayNodes.add(nodes.get(1)); // problem node
        wayNodes.add(nodes.get(2));
        wayNodes.add(nodes.get(3));
        wayNodes.add(nodes.get(4));
        wayNodes.add(nodes.get(1));
        w.setNodes(wayNodes);
        SelfIntersectingWay test = new SelfIntersectingWay();
        test.visit(w);
        Assert.assertEquals(0, test.getErrors().size());
    }

    /**
     * Both endpoints join at one inner node ("8"-shape).
     * This is considered to be an error.
     */
    @Test
    public void testClosedWay() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    public void testSpikeWithStartInClosedWay() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    public void testSpikeWithEndInClosedWay() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(0)));
    }

    /**
     * Closed way contains a spike.
     * This is considered to be an error.
     */
    @Test
    public void testSpikeInClosedWay() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(2)));
    }

    /**
     * Closed way with barbell shape (a-b-c-a-d-e-f-d).
     * This is considered to be an error.
     */
    @Test
    public void testClosedWayBarbell() {
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
        Assert.assertEquals(1, test.getErrors().size());
        Assert.assertTrue(test.getErrors().iterator().next().getHighlighted().contains(nodes.get(3)));
    }

}
