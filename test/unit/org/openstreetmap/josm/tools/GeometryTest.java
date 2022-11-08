// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.io.OsmReader;
import org.openstreetmap.josm.testutils.JOSMTestRules;

/**
 * Unit tests of {@link Geometry} class.
 */
class GeometryTest {
    /**
     * Primitives need preferences and projection.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    static JOSMTestRules test = new JOSMTestRules().preferences().projection();

    /**
     * Test of {@link Geometry#getLineLineIntersection} method.
     */
    @Test
    void testLineLineIntersection() {
        EastNorth p1 = new EastNorth(-9477809.106349014, 1.5392960539974203E7);
        EastNorth p2 = new EastNorth(-9477813.789091509, 1.5392954297092048E7);
        EastNorth p3 = new EastNorth(-9477804.974058038, 1.539295490030348E7);
        EastNorth p4 = new EastNorth(-9477814.628697459, 1.5392962142181376E7);

        EastNorth intersectionPoint = Geometry.getLineLineIntersection(p1, p2, p3, p4);

        assertNotNull(intersectionPoint);
        EastNorth d1 = p3.subtract(intersectionPoint);
        EastNorth d2 = p1.subtract(p2);
        Double crossProduct = d1.east()*d2.north() - d1.north()*d2.east();
        Double scalarProduct = d1.east()*d2.east() + d1.north()*d2.north();
        Double len1 = d1.length();
        Double len2 = d2.length();

        double angle1 = Geometry.getCornerAngle(p1, p2, intersectionPoint);
        double angle2 = Geometry.getCornerAngle(p3, p4, intersectionPoint);
        assertTrue(Math.abs(angle1) < 1e-10, "intersection point not on line, angle: " + angle1);
        assertTrue(Math.abs(angle1) < 1e-10, "intersection point not on line, angle: " + angle2);

        assertTrue(Math.abs(Math.abs(crossProduct/len1/len2) - 1) < 1e-10, "cross product != 1 : " + Math.abs(crossProduct/len1/len2));
        assertTrue(Math.abs(scalarProduct/len1/len2) < 1e-10, "scalar product != 0 : " + scalarProduct/len1/len2);
    }

    /**
     * Test of {@link Geometry#closedWayArea(org.openstreetmap.josm.data.osm.Way)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testClosedWayArea() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm"))) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            assertEquals(5760015.7353515625, Geometry.closedWayArea(closedWay), 1e-3);
            assertEquals(5760015.7353515625, Geometry.computeArea(closedWay), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#multipolygonArea(Relation)}} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testMultipolygonArea() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "multipolygon.osm"))) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            final Relation r = ds.getRelations().iterator().next();
            assertEquals(4401735.20703125, Geometry.multipolygonArea(r), 1e-3);
            assertEquals(4401735.20703125, Geometry.computeArea(r), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getAreaAndPerimeter(List)} method.
     *
     * @throws Exception if an error occurs
     */
    @Test
    void testAreaAndPerimeter() throws Exception {
        try (InputStream in = Files.newInputStream(Paths.get(TestUtils.getTestDataRoot(), "create_multipolygon.osm"))) {
            DataSet ds = OsmReader.parseDataSet(in, null);
            Way closedWay = (Way) SubclassFilteredCollection.filter(ds.allPrimitives(),
                    SearchCompiler.compile("landuse=forest")).iterator().next();
            Geometry.AreaAndPerimeter areaAndPerimeter = Geometry.getAreaAndPerimeter(closedWay.getNodes());
            assertEquals(12495000., areaAndPerimeter.getArea(), 1e-3);
            assertEquals(15093.201209424187, areaAndPerimeter.getPerimeter(), 1e-3);
        }
    }

    /**
     * Test of {@link Geometry#getNormalizedAngleInDegrees(double)} method.
     */
    @Test
    void testRightAngle() {
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

    static Stream<Arguments> testCentroid() {
        // The expected values use the BigDecimal calculations
        return Stream.of(
            Arguments.of(new LatLon(54.10310051693397, 12.094459783282147),
                new LatLon[]{
                    new LatLon(54.1031207, 12.094513),
                    new LatLon(54.1030973, 12.0945423),
                    new LatLon(54.1031188, 12.0944413),
                    new LatLon(54.1030578, 12.0945178),
                    new LatLon(54.1030658, 12.0944275),
                    new LatLon(54.1030826, 12.0945434),
                    new LatLon(54.1031079, 12.0944243),
                    new LatLon(54.1030515, 12.094495),
                    new LatLon(54.103094, 12.0944157),
                    new LatLon(54.1031257, 12.0944893),
                    new LatLon(54.1030687, 12.0945348),
                    new LatLon(54.1031251, 12.0944641),
                    new LatLon(54.1030792, 12.0944168),
                    new LatLon(54.1030508, 12.0944698),
                    new LatLon(54.1030559, 12.0944461),
                    new LatLon(54.1031107, 12.0945316)
                }),
            Arguments.of(new LatLon(54.10309639216633, 12.09463150330365),
                new LatLon[]{new LatLon(54.1031205, 12.094653),
                    new LatLon(54.1030621, 12.0946675),
                    new LatLon(54.1030866, 12.0946874),
                    new LatLon(54.1030732, 12.0946816),
                    new LatLon(54.1030766, 12.0945701),
                    new LatLon(54.1031148, 12.0945865),
                    new LatLon(54.1031122, 12.0946719),
                    new LatLon(54.1030551, 12.0946473),
                    new LatLon(54.1031037, 12.0945724),
                    new LatLon(54.1031003, 12.094684),
                    new LatLon(54.1030647, 12.0945821),
                    new LatLon(54.1031219, 12.0946068),
                    new LatLon(54.1031239, 12.0946301),
                    new LatLon(54.1030903, 12.0945667),
                    new LatLon(54.1030564, 12.0946011),
                    new LatLon(54.1030531, 12.0946239)
                }),
                Arguments.of(new LatLon(54.103185854296896, 12.09457804609505),
                    new LatLon[] {
                        new LatLon(54.1031981, 12.0945501),
                        new LatLon(54.1031782, 12.0945501),
                        new LatLon(54.1031726, 12.0946082),
                        new LatLon(54.1031955, 12.0946015)
                    }),
                Arguments.of(new LatLon(54.103180913681705, 12.094425831813119),
                    new LatLon[] {
                        new LatLon(54.1032057, 12.0943903),
                        new LatLon(54.1031517, 12.0944053),
                        new LatLon(54.1031877, 12.0943743),
                        new LatLon(54.1031697, 12.0943743),
                        new LatLon(54.1031517, 12.0944353),
                        new LatLon(54.1031697, 12.0944663),
                        new LatLon(54.1031877, 12.0944663),
                        new LatLon(54.1032057, 12.0944363)
                    })
        );
    }

    @ParameterizedTest
    @MethodSource
    void testCentroid(LatLon expected, LatLon... coordinates) {
        LatLon actual = ProjectionRegistry.getProjection()
                .eastNorth2latlon(Geometry.getCentroid(Stream.of(coordinates).map(Node::new).collect(Collectors.toList())));
        assertTrue(expected.equalsEpsilon((ILatLon) actual), "Expected " + expected + " but got " + actual);
    }

    /**
     * Test of {@link Geometry#getCentroidEN} method.
     */
    @Test
    void testCentroidEN() {
        EastNorth en1 = new EastNorth(100, 200);
        EastNorth en2 = new EastNorth(150, 400);
        EastNorth en3 = new EastNorth(200, 200);
        assertEquals(en1, Geometry.getCentroidEN(Collections.singletonList(en1)));
        assertEquals(new EastNorth(125, 300), Geometry.getCentroidEN(Arrays.asList(en1, en2)));
        assertEquals(new EastNorth(150, 266d + 2d/3d), Geometry.getCentroidEN(Arrays.asList(en1, en2, en3)));
    }


    /**
     * Test of {@link Geometry#polygonIntersection} method with two triangles.
     */
    @Test
    void testPolygonIntersectionTriangles() {
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
    void testPolygonIntersectionVShapes() {
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
    void testIsPolygonInsideMultiPolygon() {
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

    /**
     * Test of {@link Geometry#filterInsideMultipolygon}
     */
    @Test
    void testFilterInsideMultiPolygon() {
        Node node1 = new Node(new LatLon(1.01, 1.0));
        Node node2 = new Node(new LatLon(1.01, 1.1));
        Node node3 = new Node(new LatLon(1.02, 1.05));
        Way w1 = new Way();
        w1.setNodes(Arrays.asList(node1, node2, node3, node1));
        w1.put("building", "yes");
        Relation mp1 = new Relation();
        mp1.addMember(new RelationMember("outer", w1));
        mp1.put("type", "multipolygon");

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
        Relation mp2 = new Relation();
        mp2.addMember(new RelationMember("outer", outer));
        mp2.addMember(new RelationMember("inner", inner));
        mp2.put("type", "multipolygon");
        assertFalse(Geometry.isPolygonInsideMultiPolygon(w1.getNodes(), mp2, null));
        assertFalse(Geometry.filterInsideMultipolygon(Collections.singletonList(w1), mp2).contains(w1));

        node4.setCoor(new LatLon(1.006, 0.99));
        // now w1 is inside
        assertTrue(Geometry.isPolygonInsideMultiPolygon(w1.getNodes(), mp2, null));
        assertTrue(Geometry.filterInsideMultipolygon(Collections.singletonList(w1), mp2).contains(w1));
        assertTrue(Geometry.filterInsideMultipolygon(Collections.singletonList(mp1), mp2).contains(mp1));
        assertTrue(Geometry.filterInsideMultipolygon(Arrays.asList(w1, mp1), mp2).contains(w1));
        assertTrue(Geometry.filterInsideMultipolygon(Arrays.asList(w1, mp1), mp2).contains(mp1));
    }

    /**
     * Test of {@link Geometry#getDistance} method.
     */
    @Test
    void testGetDistance() {
        Node node1 = new Node(new LatLon(0, 0));
        Node node2 = new Node(new LatLon(0.1, 1));
        Node node3 = new Node(new LatLon(1.1, 0.1));
        Node node4 = new Node(new LatLon(1, 1.1));
        Way way1 = TestUtils.newWay("", node1, node2);
        Way way2 = TestUtils.newWay("", node3, node4);
        Relation testRelation1 = new Relation();
        Relation testRelation2 = new Relation();
        testRelation1.addMember(new RelationMember("", way1));
        testRelation1.addMember(new RelationMember("", way2));
        testRelation2.addMember(new RelationMember("", node1));
        testRelation2.addMember(new RelationMember("", node2));
        testRelation2.addMember(new RelationMember("", node3));
        testRelation2.addMember(new RelationMember("", node4));

        double distance = Geometry.getDistance(null, node3);
        assertEquals(Double.NaN, distance, 0.1);

        distance = Geometry.getDistance(way1, null);
        assertEquals(Double.NaN, distance, 0.1);

        distance = Geometry.getDistance(null, null);
        assertEquals(Double.NaN, distance, 0.1);

        distance = Geometry.getDistance(node1, node2);
        assertEquals(111874.6474307704, distance, 0.1);

        distance = Geometry.getDistance(way1, node3);
        assertEquals(120743.55085962385, distance, 0.1);

        distance = Geometry.getDistance(node3, way1);
        assertEquals(120743.55085962385, distance, 0.1);

        distance = Geometry.getDistance(way1, way2);
        assertEquals(100803.63714283936, distance, 0.1);

        distance = Geometry.getDistance(testRelation1, new Node(new LatLon(0, 0.5)));
        assertEquals(5538.354450686605, distance, 0.1);

        distance = Geometry.getDistance(new Node(new LatLon(0, 0.5)), testRelation1);
        assertEquals(5538.354450686605, distance, 0.1);

        distance = Geometry.getDistance(testRelation1, testRelation2);
        assertEquals(0.0, distance, 0.1);
    }

    /**
     * Test of {@link Geometry#getClosestPrimitive} method
     */
    @Test
    void testGetClosestPrimitive() {
        Node node1 = new Node(new LatLon(0, 0));
        Node node2 = new Node(new LatLon(0.1, 1));
        Node node3 = new Node(new LatLon(1.1, 0.1));
        Node node4 = new Node(new LatLon(1, 1.1));
        Way way1 = TestUtils.newWay("", node1, node2);
        Way way2 = TestUtils.newWay("", node3, node4);

        List<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(way1);
        primitives.add(way2);
        OsmPrimitive closest = Geometry.getClosestPrimitive(node1, primitives);
        assertEquals(way1, closest);
    }

    /**
     * Test of {@link Geometry#getFurthestPrimitive} method
     */
    @Test
    void testGetFurthestPrimitive() {
        Node node1 = new Node(new LatLon(0, 0));
        Node node2 = new Node(new LatLon(0, 1.1));
        Node node3 = new Node(new LatLon(1, 0.1));
        Node node4 = new Node(new LatLon(1.1, 1));
        Way way1 = TestUtils.newWay("", node1, node2);
        Way way2 = TestUtils.newWay("", node3, node4);
        Way way3 = TestUtils.newWay("", node2, node4);
        Way way4 = TestUtils.newWay("", node1, node3);

        List<OsmPrimitive> primitives = new ArrayList<>();
        primitives.add(way1);
        OsmPrimitive furthest = Geometry.getFurthestPrimitive(new Node(new LatLon(0, 0.75)), primitives);
        assertEquals(way1, furthest);
        primitives.add(way2);
        primitives.add(way3);
        primitives.add(way4);
        furthest = Geometry.getFurthestPrimitive(new Node(new LatLon(0, 0.5)), primitives);
        assertEquals(way2, furthest);
        furthest = Geometry.getFurthestPrimitive(new Node(new LatLon(.25, 0.5)), primitives);
        assertEquals(way2, furthest);
    }

    /**
     * Test of {@link Geometry#getClosestWaySegment} method
     */
    @Test
    void testGetClosestWaySegment() {
        Node node1 = new Node(new LatLon(0, 0));
        Node node2 = new Node(new LatLon(0, 1));
        Node node3 = new Node(new LatLon(0.3, 0.5));
        Node node4 = new Node(new LatLon(0.1, 0));
        Way way1 = TestUtils.newWay("", node1, node2, node3, node4);

        Way closestSegment = Geometry.getClosestWaySegment(way1, new Node(new LatLon(0, 0.5))).toWay();
        assertTrue(closestSegment.containsNode(node1));
        assertTrue(closestSegment.containsNode(node2));
    }

    /**
     * Test of {@link Geometry#getDistanceSegmentSegment} method
     */
    @Test
    void testGetDistanceSegmentSegment() {
        Node node1 = new Node(new LatLon(2.0, 2.0));
        Node node2 = new Node(new LatLon(2.0, 3.0));
        Node node3 = new Node(new LatLon(2.3, 2.5));
        Node node4 = new Node(new LatLon(2.1, 2.0));

        // connected segments
        assertEquals(0.0, Geometry.getDistanceSegmentSegment(node1, node2, node3, node1), 0.000001);

        // distance between node 1 and node4 is the shortest
        double expected = node1.getEastNorth().distance(node4.getEastNorth());
        assertEquals(expected, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // crossing segments
        node4.setCoor(new LatLon(1.9998192774806864, 2.0004056993230455));
        assertEquals(0, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // usual case
        node4.setCoor(new LatLon(2.0002098170882276, 2.0000778643530537));
        assertEquals(23.4, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 1.0);

        // similar segments, reversed direction
        node3.setCoor(node2.getCoor());
        node4.setCoor(node1.getCoor());
        assertEquals(0.0, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // overlapping segments
        node3.setCoor(new LatLon(2.0, 2.2));
        node4.setCoor(new LatLon(2.0, 2.3));
        assertEquals(0.0, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // parallel segments, n1 and n3 at same longitude
        node3.setCoor(new LatLon(2.1, 2.0));
        node4.setCoor(new LatLon(2.1, 2.3));
        expected = node1.getEastNorth().distance(node3.getEastNorth());
        assertEquals(expected, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // parallel segments
        node3.setCoor(new LatLon(2.1, 2.1));
        assertEquals(expected, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.000001);

        // almost parallel segments
        node3.setCoor(new LatLon(2.09999999, 2.1));
        assertEquals(expected, Geometry.getDistanceSegmentSegment(node1, node2, node3, node4), 0.01);
        assertTrue(expected > Geometry.getDistanceSegmentSegment(node1, node2, node3, node4));
    }

    static Stream<Arguments> testGetLatLonFrom() {
        // The projection can quickly explode the test matrix, so only test WGS84 (EPSG:3857). If other projections have
        // issues, add them to the first list.
        return TestUtils.createTestMatrix(
                // Check specific projections
                Collections.singletonList(Projections.getProjectionByCode("EPSG:3857")),
                // Check extreme latitudes (degrees)
                Arrays.asList(0, 89, -89),
                // Test extreme longitudes (degrees)
                Arrays.asList(0, -179, 179),
                // Test various angles (degrees)
                // This tests cardinal directions, and then some varying angles.
                // TBH, the cardinal directions should find any issues uncovered by the varying angles,
                // but it may not.
                Arrays.asList(0, 90, 180, 270, 45),
                // Test various distances (meters)
                Arrays.asList(1, 10_000)
                ).map(Arguments::of);
    }

    @ParameterizedTest(name = "[{index}] {3}Â° {4}m @ lat = {1} lon = {2} - {0}")
    @MethodSource
    void testGetLatLonFrom(final Projection projection, final double lat, final double lon, final double angle, final double offsetInMeters) {
        ProjectionRegistry.setProjection(projection);
        final double offset = offsetInMeters / projection.getMetersPerUnit();
        final ILatLon original = new LatLon(lat, lon);

        final ILatLon actual = Geometry.getLatLonFrom(original, Math.toRadians(angle), offset);
        // Due to degree -> radian -> degree conversion, there is a limit to how precise it can be
        assertEquals(offsetInMeters, original.greatCircleDistance(actual), 0.000_000_1);
        // The docs indicate that this should not be highly precise.
        assertEquals(angle, Math.toDegrees(original.bearing(actual)), 0.000_001);
    }
}
