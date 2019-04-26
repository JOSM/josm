// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link Geometry} class.
 */
public class GeometryTest {
    /**
     * Primitives need preferences and projection.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Test of {@link Geometry#getLineLineIntersection} method.
     */
    @Test
    public void testLineLineIntersection() {
        EastNorth p1 = new EastNorth(-9477809.106349014, 1.5392960539974203E7);
        EastNorth p2 = new EastNorth(-9477813.789091509, 1.5392954297092048E7);
        EastNorth p3 = new EastNorth(-9477804.974058038, 1.539295490030348E7);
        EastNorth p4 = new EastNorth(-9477814.628697459, 1.5392962142181376E7);

        EastNorth intersectionPoint = Geometry.getLineLineIntersection(p1, p2, p3, p4);

        EastNorth d1 = p3.subtract(intersectionPoint);
        EastNorth d2 = p1.subtract(p2);
        Double crossProduct = d1.east()*d2.north() - d1.north()*d2.east();
        Double scalarProduct = d1.east()*d2.east() + d1.north()*d2.north();
        Double len1 = d1.length();
        Double len2 = d2.length();

        Double angle1 = Geometry.getCornerAngle(p1, p2, intersectionPoint);
        Double angle2 = Geometry.getCornerAngle(p3, p4, intersectionPoint);
        Assert.assertTrue("intersection point not on line, angle: " + angle1,
                Math.abs(angle1) < 1e-10);
        Assert.assertTrue("intersection point not on line, angle: " + angle2,
                Math.abs(angle1) < 1e-10);

        Assert.assertTrue("cross product != 1 : " + Math.abs(crossProduct/len1/len2),
                Math.abs(Math.abs(crossProduct/len1/len2) - 1) < 1e-10);
        Assert.assertTrue("scalar product != 0 : " + scalarProduct/len1/len2,
                Math.abs(scalarProduct/len1/len2) < 1e-10);
    }

    /**
     * Test of {@link Geometry#closedWayArea(org.openstreetmap.josm.data.osm.Way)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testClosedWayArea() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            Assert.assertEquals(5760015.7353515625, Geometry.closedWayArea(closedWay), 1e-3);
            Assert.assertEquals(5760015.7353515625, Geometry.computeArea(closedWay), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#multipolygonArea(Relation)}} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testMultipolygonArea() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            final Relation r = ds.getRelations().iterator().next();
            Assert.assertEquals(4401735.20703125, Geometry.multipolygonArea(r), 1e-3);
            Assert.assertEquals(4401735.20703125, Geometry.computeArea(r), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getAreaAndPerimeter(List)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    public void testAreaAndPerimeter() throws Exception {
        try (FileInputStream in = new FileInputStream(TestUtils.getTestDataRoot() + "create_multipolygon.osm")) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            Geometry.AreaAndPerimeter areaAndPerimeter = Geometry.getAreaAndPerimeter(closedWay.getNodes());
            Assert.assertEquals(12495000., areaAndPerimeter.getArea(), 1e-3);
            Assert.assertEquals(15093.201209424187, areaAndPerimeter.getPerimeter(), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getNormalizedAngleInDegrees(double)} method.
     */
    @Test
    public void testRightAngle() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        n1.setCoor(new LatLon(10.22873540462851, 6.169719398316592));
        n2.setCoor(new LatLon(10.229332494162811, 6.16978130985785));
        n3.setCoor(new LatLon(10.22924937004949, 6.17060908367496));

        double angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n3.getEastNorth()));
        assertEquals(90, angle, 1e-8);
        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n1.getEastNorth()));
        assertEquals(0, angle, 1e-8);

        n1.setCoor(new LatLon(10.2295011, 6.1693106));
        n2.setCoor(new LatLon(10.2294958, 6.16930635));
        n3.setCoor(new LatLon(10.2294895, 6.1693039));

        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n1.getEastNorth(),
                n2.getEastNorth(), n3.getEastNorth()));
        assertEquals(162.66381817961337, angle, 1e-5);

        angle = Geometry.getNormalizedAngleInDegrees(Geometry.getCornerAngle(n3.getEastNorth(),
                n2.getEastNorth(), n1.getEastNorth()));
        assertEquals(162.66381817961337, angle, 1e-5);
    }

    /**
     * Test of {@link Geometry#getCentroidEN} method.
     */
    @Test
    public void testCentroidEN() {
        EastNorth en1 = new EastNorth(100, 200);
        EastNorth en2 = new EastNorth(150, 400);
        EastNorth en3 = new EastNorth(200, 200);
        assertEquals(en1, Geometry.getCentroidEN(Arrays.asList(en1)));
        assertEquals(new EastNorth(125, 300), Geometry.getCentroidEN(Arrays.asList(en1, en2)));
        assertEquals(new EastNorth(150, 266d + 2d/3d), Geometry.getCentroidEN(Arrays.asList(en1, en2, en3)));
    }


    /**
     * Test of {@link Geometry#polygonIntersection} method with two triangles.
     */
    @Test
    public void testPolygonIntersectionTriangles() {
        Node node1 = new Node(new LatLon(0.0, 1.0));
        Node node2 = new Node(new LatLon(0.0, 2.0));
        Node node3 = new Node(new LatLon(5.0, 1.5));
        List<Node> poly1 = Arrays.asList(node1, node2, node3, node1);
        Node node4 = new Node(new LatLon(10.0, 1.0));
        Node node5 = new Node(new LatLon(10.0, 2.0));
        Node node6 = new Node(new LatLon(5.000001, 1.5));
        List<Node> poly2 = Arrays.asList(node4, node5, node6, node4);
        // no intersection, not even touching
        assertEquals(Geometry.PolygonIntersection.OUTSIDE, Geometry.polygonIntersection(poly1, poly2));

        node5.setCoor(new LatLon(5.0, 1.5));
        // touching in a single point with two different nodes
        assertEquals(Geometry.PolygonIntersection.OUTSIDE, Geometry.polygonIntersection(poly1, poly2));

        node5.setCoor(new LatLon(4.99999999, 1.5));
        // now node5 lies inside way1, intersection is a very small area, in OSM precision nodes are equal
        assertEquals(node5.getCoor().getRoundedToOsmPrecision(), node3.getCoor().getRoundedToOsmPrecision());
        assertEquals(Geometry.PolygonIntersection.CROSSING, Geometry.polygonIntersection(poly1, poly2));

        node5.setCoor(new LatLon(4.9999999, 1.5));
        // intersection area is too big to ignore
        assertNotEquals(node5.getCoor().getRoundedToOsmPrecision(), node3.getCoor().getRoundedToOsmPrecision());
        assertEquals(Geometry.PolygonIntersection.CROSSING, Geometry.polygonIntersection(poly1, poly2));
    }

    /**
     * Test of {@link Geometry#polygonIntersection} method with two V-shapes
     */
    @Test
    public void testPolygonIntersectionVShapes() {
        Node node1 = new Node(new LatLon(1.0, 1.0));
        Node node2 = new Node(new LatLon(2.0, 2.0));
        Node node3 = new Node(new LatLon(0.9, 1.0));
        Node node4 = new Node(new LatLon(2.0, 0.0));
        List<Node> poly1 = Arrays.asList(node1, node2, node3, node4, node1);
        Node node5 = new Node(new LatLon(3.0, 1.0));
        Node node6 = new Node(new LatLon(2.0, 2.0)); // like node2
        Node node7 = new Node(new LatLon(3.1, 1.0));
        Node node8 = new Node(new LatLon(2.0, 0.0)); // like node4
        List<Node> poly2 = Arrays.asList(node5, node6, node7, node8, node5);

        // touching in two points but not overlapping
        assertEquals(Geometry.PolygonIntersection.OUTSIDE, Geometry.polygonIntersection(poly1, poly2));

        // touching in one point, small overlap at the other
        node6.setCoor(new LatLon(1.9999999, 2.0));
        assertEquals(Geometry.PolygonIntersection.CROSSING, Geometry.polygonIntersection(poly1, poly2));

        // two small overlaps, but clearly visible because lines are crossing
        node6.setCoor(new LatLon(1.99999999, 2.0));
        node8.setCoor(new LatLon(1.99999999, 0.0));
        assertEquals(Geometry.PolygonIntersection.OUTSIDE, Geometry.polygonIntersection(poly1, poly2));
    }

    /**
     * Test of {@link Geometry#isPolygonInsideMultiPolygon}
     * See #17652. Triangle crosses outer way of multipolygon.
     */
    @Test
    public void testIsPolygonInsideMultiPolygon() {
        Node node1 = new Node(new LatLon(1.01, 1.0));
        Node node2 = new Node(new LatLon(1.01, 1.1));
        Node node3 = new Node(new LatLon(1.02, 1.05));
        Way w1 = new Way();
        w1.setNodes(Arrays.asList(node1, node2, node3, node1));
        w1.put("building", "yes");

        Node node4 = new Node(new LatLon(1.0, 1.09));
        Node node5 = new Node(new LatLon(1.0, 1.12));
        Node node6 = new Node(new LatLon(1.1, 1.12));
        Node node7 = new Node(new LatLon(1.1, 1.09));
        Way outer = new Way();
        outer.setNodes(Arrays.asList(node4, node5, node6, node7, node4));
        Node node8 = new Node(new LatLon(1.04, 1.1));
        Node node9 = new Node(new LatLon(1.04, 1.11));
        Node node10 = new Node(new LatLon(1.06, 1.105));
        Way inner = new Way();
        inner.setNodes(Arrays.asList(node8, node9, node10, node8));
        Relation mp = new Relation();
        mp.addMember(new RelationMember("outer", outer));
        mp.addMember(new RelationMember("inner", inner));
        mp.put("type", "multipolygon");
        assertFalse(Geometry.isPolygonInsideMultiPolygon(w1.getNodes(), mp, null));

        node4.setCoor(new LatLon(1.006, 0.99));
        // now w1 is inside
        assertTrue(Geometry.isPolygonInsideMultiPolygon(w1.getNodes(), mp, null));
    }
}
