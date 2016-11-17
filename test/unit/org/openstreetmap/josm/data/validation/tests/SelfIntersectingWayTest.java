// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import java.util.ArrayList;
import java.util.List;

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
        JOSMFixture.createUnitTestFixture().init(true);
    }

    private static List<Node> createNodes() {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            nodes.add(new Node(i+1));
        }
        nodes.get(0).setCoor(new LatLon(34.2680878298, 133.56336369008));
        nodes.get(1).setCoor(new LatLon(34.25096598132, 133.54891792012));
        nodes.get(2).setCoor(new LatLon(34.24466741332, 133.56693544639));
        nodes.get(3).setCoor(new LatLon(34.26815342405, 133.56066502976));
        nodes.get(4).setCoor(new LatLon(34.26567411471, 133.56132705125));
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
     * This is considered okay.
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
        Assert.assertEquals(0, test.getErrors().size());
    }

}
