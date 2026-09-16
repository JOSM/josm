// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Unit tests for class {@link ParallelWays}.
 */
@Main
@Projection
class ParallelWaysTest {

    private static Node node(double x, double y) {
        Node n = new Node();
        n.setEastNorth(new EastNorth(x, y));
        return n;
    }

    private static Way way(Node... nodes) {
        Way w = new Way();
        w.setNodes(Arrays.asList(nodes));
        return w;
    }

    private static Way way(double... xy) {
        Node[] nodes = new Node[xy.length / 2];
        for (int i = 0; i < nodes.length; i++) {
            nodes[i] = node(xy[2 * i], xy[2 * i + 1]);
        }
        return way(nodes);
    }

    private static Way closedWay(double... xy) {
        Way w = way(xy);
        w.addNode(w.firstNode());
        return w;
    }

    private static void assertContains(List<EastNorth> pts, double x, double y) {
        EastNorth expected = new EastNorth(x, y);
        assertTrue(pts.stream().anyMatch(p -> p.equalsEpsilon(expected, 1e-6)), pts.toString());
    }

    private static double distanceToWay(Way w, EastNorth p) {
        double best = Double.POSITIVE_INFINITY;
        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            EastNorth c = Geometry.closestPointToSegment(w.getNode(i).getEastNorth(), w.getNode(i + 1).getEastNorth(), p);
            best = Math.min(best, c.distance(p));
        }
        return best;
    }

    private static double maxSegmentLength(Way w) {
        double max = 0;
        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            max = Math.max(max, w.getNode(i).getEastNorth().distance(w.getNode(i + 1).getEastNorth()));
        }
        return max;
    }

    /**
     * Checks the basic invariants of a parallel: every vertex is at least the offset away from the source,
     * and never much further than the offset (the mitre may overshoot by at most half a segment length).
     * @param source the source way
     * @param pts the parallel
     * @param r the (absolute) offset
     */
    private static void assertParallelInvariants(Way source, List<EastNorth> pts, double r) {
        assertVertexDistances(source, pts, r);
        // no self intersections
        for (int i = 0; i < pts.size() - 1; i++) {
            for (int j = i + 2; j < pts.size() - 1; j++) {
                EastNorth x = Geometry.getSegmentSegmentIntersection(pts.get(i), pts.get(i + 1), pts.get(j), pts.get(j + 1));
                assertTrue(x == null, "self intersection between segments " + i + " and " + j + " at " + x);
            }
        }
    }

    /**
     * Checks that every vertex is at least the offset away from the source (a point on an arc chord may be
     * closer by the sagitta of the chord), and never much further than the offset (the mitre may overshoot by at
     * most half a segment length).
     * @param source the source way
     * @param pts the parallel
     * @param r the (absolute) offset
     */
    private static void assertVertexDistances(Way source, List<EastNorth> pts, double r) {
        double maxOvershoot = 0.5 * maxSegmentLength(source);
        // a point where two chords cross may lie inside both circles, i.e. up to two sagittas inside
        double minDist = r * (1 - 2 * (1 - Math.cos(Math.toRadians(ParallelWays.DEFAULT_ARC_STEP_DEGREES / 2)))) * (1 - 1e-6);
        for (EastNorth p : pts) {
            double dist = distanceToWay(source, p);
            assertTrue(dist >= minDist, "vertex " + p + " too close to source: " + dist + " < " + r);
            assertTrue(dist <= r + maxOvershoot + 1e-6, "vertex " + p + " too far from source: " + dist + " > " + r);
        }
    }

    /**
     * A small offset of a simple way must keep the node count
     */
    @Test
    void testSimpleOffset() {
        Way w = way(0, 0, 100, 0, 200, 0);
        ParallelWays pw = new ParallelWays(Arrays.asList(w), false, 0);
        assertFalse(pw.isClosedPath());
        pw.changeOffset(10);
        List<EastNorth> pts = pw.getOffsetPoints();
        assertEquals(3, pts.size());
        assertEquals(new EastNorth(0, 10), pts.get(0));
        assertEquals(new EastNorth(100, 10), pts.get(1));
        assertEquals(new EastNorth(200, 10), pts.get(2));
        pw.changeOffset(-10);
        pts = pw.getOffsetPoints();
        assertEquals(new EastNorth(0, -10), pts.get(0));
        assertEquals(new EastNorth(200, -10), pts.get(2));
    }

    /**
     * Small offsets of a closed way: mitre outside, clipped inside
     */
    @Test
    void testSquare() {
        Way square = closedWay(0, 0, 100, 0, 100, 100, 0, 100); // counter clockwise: left is inside
        ParallelWays pw = new ParallelWays(Arrays.asList(square), false, 0);
        assertTrue(pw.isClosedPath());

        pw.changeOffset(-10); // outside
        List<EastNorth> pts = pw.getOffsetPoints();
        assertTrue(pw.isResultClosed());
        assertEquals(4, pts.size());
        assertParallelInvariants(square, pts, 10);
        assertContains(pts, -10, -10);
        assertContains(pts, 110, 110);

        pw.changeOffset(10); // inside
        pts = pw.getOffsetPoints();
        assertTrue(pw.isResultClosed());
        assertEquals(4, pts.size());
        assertParallelInvariants(square, pts, 10);
        assertContains(pts, 10, 10);
        assertContains(pts, 90, 90);

        pw.changeOffset(60); // inside, larger than the square
        assertTrue(pw.getOffsetPoints().isEmpty());
    }

    /**
     * A large offset of a closed way is a ring with arcs at the corners
     */
    @Test
    void testSquareLargeOffset() {
        Way square = closedWay(0, 0, 100, 0, 100, 100, 0, 100);
        ParallelWays pw = new ParallelWays(Arrays.asList(square), false, 0, 10);
        pw.changeOffset(-1000);
        List<EastNorth> pts = pw.getOffsetPoints();
        assertTrue(pw.isResultClosed());
        // 4 corners, each an arc of 90° in 9 chords -> 10 points per corner
        assertEquals(40, pts.size(), pts.toString());
        assertParallelInvariants(square, pts, 1000);
        for (EastNorth p : pts) {
            assertEquals(1000, distanceToWay(square, p), 1e-6);
        }
    }

    /**
     * Offsets larger than the local radius of curvature must not produce spikes or loops (the use case
     * of a maritime boundary 22 km off a coastline).
     */
    @Test
    void testLargeOffsetSawtooth() {
        double[] xy = new double[2 * 41];
        for (int i = 0; i <= 40; i++) {
            xy[2 * i] = i * 200;
            xy[2 * i + 1] = (i % 2 == 0 ? 0 : 150) + 30 * Math.sin(i);
        }
        Way coast = way(xy);
        ParallelWays pw = new ParallelWays(Arrays.asList(coast), false, 0);
        for (double d : new double[] {10, -10, 300, -300, 5000, -5000, 22000, -22000}) {
            pw.changeOffset(d);
            List<EastNorth> pts = pw.getOffsetPoints();
            assertTrue(pts.size() >= 2, "d=" + d);
            assertFalse(pw.isResultClosed());
            assertParallelInvariants(coast, pts, Math.abs(d));
            // the result must span the whole source
            assertTrue(pts.get(0).getX() < Math.abs(d) + 100, "d=" + d + ": " + pts.get(0));
            assertTrue(pts.get(pts.size() - 1).getX() > 8000 - Math.abs(d) - 100, "d=" + d + ": " + pts.get(pts.size() - 1));
        }
    }

    /**
     * A random "coastline" with many nodes: the offset must be correct and fast enough for interactive use.
     */
    @Test
    void testLargeOffsetRandomCoastline() {
        Random rnd = new Random(42);
        int count = 1600;
        double[] xy = new double[2 * count];
        double x = 0;
        double y = 0;
        double heading = 0;
        for (int i = 0; i < count; i++) {
            xy[2 * i] = x;
            xy[2 * i + 1] = y;
            heading += (rnd.nextDouble() - 0.5) * 2.5;
            double len = 50 + rnd.nextDouble() * 300;
            x += Math.cos(heading) * len;
            y += Math.sin(heading) * len;
        }
        Way coast = way(xy);
        ParallelWays pw = new ParallelWays(Arrays.asList(coast), false, 0);
        for (double d : new double[] {50, -50, 2000, -2000, 22224, -22224}) {
            long start = System.nanoTime();
            pw.changeOffset(d);
            long millis = (System.nanoTime() - start) / 1_000_000;
            List<EastNorth> pts = pw.getOffsetPoints();
            assertTrue(pts.size() >= 2, "d=" + d);
            assertVertexDistances(coast, pts, Math.abs(d));
            assertTrue(millis < 5000, "offset took " + millis + " ms");
            System.out.println("ParallelWays: " + count + " nodes, d=" + d + " -> " + pts.size() + " points in " + millis + " ms");
        }
    }

    /**
     * A path with an excursion which retraces itself exactly (a - b - c - d - c - b - e - f, as produced by
     * converting a routed GPX track): the parallel must cover the whole path, the excursion must not cut it.
     */
    @Test
    void testRetracedExcursion() {
        // main path along y = 0, excursion up along x = 1000 to y = 600 and back on the same nodes' coordinates
        Way w = way(0, 0, 500, 0, 1000, 0, 1000, 300, 1000, 600, 1000, 300, 1000, 0, 1500, 0, 2000, 0);
        ParallelWays pw = new ParallelWays(Arrays.asList(w), false, 0);
        for (double d : new double[] {100, -100, 30, -30}) {
            pw.changeOffset(d);
            List<EastNorth> pts = pw.getOffsetPoints();
            double r = Math.abs(d);
            assertVertexDistances(w, pts, r);
            assertEquals(new EastNorth(0, d), pts.get(0), "d=" + d);
            assertEquals(new EastNorth(2000, d), pts.get(pts.size() - 1), "d=" + d);
            // every vertex of the main path has the parallel within reach
            for (int i : new int[] {0, 1, 2, 6, 7, 8}) {
                EastNorth p = w.getNode(i).getEastNorth();
                double best = Double.POSITIVE_INFINITY;
                for (int k = 0; k < pts.size() - 1; k++) {
                    best = Math.min(best, Geometry.closestPointToSegment(pts.get(k), pts.get(k + 1), p).distance(p));
                }
                // (the junction node sits in a corner of the parallel, r*sqrt(2) away)
                assertTrue(best <= r * 1.5, "d=" + d + ": node " + i + " is " + best + " from the parallel");
            }
            if (d > 0) {
                // the excursion is on the offset side (left, i.e. up): the parallel goes around its tip
                assertTrue(pts.stream().anyMatch(p -> p.getY() > 600 + r * 0.99), "d=" + d + " does not wrap the excursion");
            } else {
                // the excursion is on the far side: the parallel is a straight line
                assertTrue(pts.stream().allMatch(p -> Math.abs(p.getY() - d) < 1e-6), "d=" + d + ": " + pts);
            }
        }
    }

    /**
     * The closest point of the path relates the offset to the segment next to the mouse
     */
    @Test
    void testClosestPoint() {
        Way w = way(0, 0, 100, 0, 100, 100);
        ParallelWays pw = new ParallelWays(Arrays.asList(w), false, 0);
        ParallelWays.ClosestPoint cp = pw.closestPoint(new EastNorth(50, 10));
        assertEquals(new EastNorth(50, 0), cp.point);
        assertEquals(new EastNorth(0, 0), cp.segmentStart);
        assertEquals(new EastNorth(100, 0), cp.segmentEnd);
        assertEquals(10, cp.signedDistance, 1e-9); // left of the path
        cp = pw.closestPoint(new EastNorth(120, 50));
        assertEquals(new EastNorth(100, 50), cp.point);
        assertEquals(new EastNorth(100, 0), cp.segmentStart);
        assertEquals(-20, cp.signedDistance, 1e-9); // right of the path
        cp = pw.closestPoint(new EastNorth(130, -40)); // beyond the corner: closest to the corner node
        assertEquals(new EastNorth(100, 0), cp.point);
        assertEquals(-50, cp.signedDistance, 1e-9);
        // the reference way orientation defines the sign: same path, reversed way
        Way reversed = way(100, 100, 100, 0, 0, 0);
        cp = new ParallelWays(Arrays.asList(reversed), false, 0).closestPoint(new EastNorth(50, 10));
        assertEquals(-10, cp.signedDistance, 1e-9);

        // Sharp (> 90°) left turn: in the wedge beyond the corner the two segments disagree about the side,
        // the point is on the outside of the turn, i.e. on the right
        Way sharp = way(0, 0, 100, 0, 0, 50);
        pw = new ParallelWays(Arrays.asList(sharp), false, 0);
        cp = pw.closestPoint(new EastNorth(150, 10));
        assertEquals(new EastNorth(100, 0), cp.point);
        assertEquals(-Math.hypot(50, 10), cp.signedDistance, 1e-9);
        cp = pw.closestPoint(new EastNorth(150, -10));
        assertEquals(-Math.hypot(50, 10), cp.signedDistance, 1e-9);
        // and on the inside of the turn the foot is on a segment, the side is the left
        cp = pw.closestPoint(new EastNorth(30, 5));
        assertEquals(new EastNorth(30, 0), cp.point);
        assertEquals(5, cp.signedDistance, 1e-9);
    }

    /**
     * Commit of multiple ways: the ways stay connected, keep their direction and their tags.
     */
    @Test
    void testCommitMultipleWays() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "ParallelWaysTest", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            Node shared = node(100, 0);
            Way w1 = way(node(0, 0), shared);
            w1.put("highway", "primary");
            Way w2 = way(node(200, 0), node(150, 0), shared); // reversed direction
            w2.put("highway", "secondary");
            ds.addPrimitive(w1.getNode(0));
            ds.addPrimitive(shared);
            ds.addPrimitive(w2.getNode(0));
            ds.addPrimitive(w2.getNode(1));
            ds.addPrimitive(w1);
            ds.addPrimitive(w2);

            ParallelWays pw = new ParallelWays(Arrays.asList(w1, w2), true, 0);
            pw.changeOffset(10);
            assertTrue(pw.getWays().isEmpty());
            pw.commit();
            List<Way> ways = pw.getWays();
            assertEquals(2, ways.size());
            Way p1 = ways.get(0);
            Way p2 = ways.get(1);
            assertEquals("primary", p1.get("highway"));
            assertEquals("secondary", p2.get("highway"));
            assertEquals(2, p1.getNodesCount());
            assertEquals(3, p2.getNodesCount());
            assertSame(p1.lastNode(), p2.lastNode());
            assertNotSame(shared, p1.lastNode());
            assertEquals(new EastNorth(0, 10), p1.firstNode().getEastNorth());
            assertEquals(new EastNorth(100, 10), p1.lastNode().getEastNorth());
            assertEquals(new EastNorth(200, 10), p2.firstNode().getEastNorth());
            assertEquals(new EastNorth(150, 10), p2.getNode(1).getEastNorth());
            assertEquals(6 + 4 + 2, ds.allPrimitives().size());
            assertSame(ds, p1.getDataSet());
            UndoRedoHandler.getInstance().undo();
            assertEquals(6, ds.allPrimitives().size());
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    private static List<Way> commitWays(DataSet ds, List<Way> source, int refIndex, double d) {
        for (Way w : source) {
            for (Node n : w.getNodes()) {
                if (n.getDataSet() == null) {
                    ds.addPrimitive(n);
                }
            }
            ds.addPrimitive(w);
        }
        ParallelWays pw = new ParallelWays(source, true, refIndex);
        pw.changeOffset(d);
        pw.commit();
        return pw.getWays();
    }

    /**
     * Several connected ways with mixed directions, dragged from any of them: one result way per source way.
     */
    @Test
    void testCommitMixedDirections() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "ParallelWaysTest", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            for (int ref = 0; ref < 3; ref++) {
                for (double d : new double[] {10, -10}) {
                    Node a = node(0, 0);
                    Node b = node(100, 0);
                    Node c = node(200, 50);
                    Node e = node(300, 50);
                    Way w1 = way(a, b);
                    Way w2 = way(c, b); // reversed
                    Way w3 = way(c, e);
                    w1.put("name", "w1");
                    w2.put("name", "w2");
                    w3.put("name", "w3");
                    List<Way> result = commitWays(ds, Arrays.asList(w1, w2, w3), ref, d);
                    assertEquals(3, result.size(), "ref=" + ref + " d=" + d + ": " + result);
                    assertEquals("w1", result.get(0).get("name"));
                    assertEquals("w2", result.get(1).get("name"));
                    assertEquals("w3", result.get(2).get("name"));
                    assertSame(result.get(0).lastNode(), result.get(1).lastNode(), "ref=" + ref + " d=" + d);
                    assertSame(result.get(1).firstNode(), result.get(2).firstNode(), "ref=" + ref + " d=" + d);
                    assertEquals(2, result.get(0).getNodesCount());
                    assertEquals(2, result.get(1).getNodesCount());
                    assertEquals(2, result.get(2).getNodesCount());
                    // the offset side is relative to the reference way
                    double expectedY = ref == 1 ? -d : d;
                    assertEquals(expectedY, result.get(0).firstNode().getEastNorth().getY(), 1e-6, "ref=" + ref + " d=" + d);
                }
            }
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * A closed ring made of several ways with mixed directions: one result way per source way, ring stays closed.
     */
    @Test
    void testCommitRingOfWays() {
        DataSet ds = new DataSet();
        OsmDataLayer layer = new OsmDataLayer(ds, "ParallelWaysTest", null);
        MainApplication.getLayerManager().addLayer(layer);
        try {
            for (int ref = 0; ref < 3; ref++) {
                for (double d : new double[] {10, -10}) {
                    Node a = node(0, 0);
                    Node b = node(100, 0);
                    Node c = node(100, 100);
                    Node e = node(0, 100);
                    Way w1 = way(a, b, c);
                    Way w2 = way(e, c); // reversed
                    Way w3 = way(e, a);
                    List<Way> result = commitWays(ds, Arrays.asList(w1, w2, w3), ref, d);
                    assertEquals(3, result.size(), "ref=" + ref + " d=" + d + ": " + result);
                    assertEquals(3, result.get(0).getNodesCount(), "ref=" + ref + " d=" + d);
                    assertEquals(2, result.get(1).getNodesCount(), "ref=" + ref + " d=" + d);
                    assertEquals(2, result.get(2).getNodesCount(), "ref=" + ref + " d=" + d);
                    assertSame(result.get(0).lastNode(), result.get(1).lastNode(), "ref=" + ref + " d=" + d);
                    assertSame(result.get(1).firstNode(), result.get(2).firstNode(), "ref=" + ref + " d=" + d);
                    assertSame(result.get(2).lastNode(), result.get(0).firstNode(), "ref=" + ref + " d=" + d);
                }
            }
        } finally {
            MainApplication.getLayerManager().removeLayer(layer);
        }
    }

    /**
     * A closed ring made of several ways
     */
    @Test
    void testClosedRingOfWays() {
        Node a = node(0, 0);
        Node b = node(100, 0);
        Node c = node(100, 100);
        Node d = node(0, 100);
        Way w1 = way(a, b, c);
        Way w2 = way(c, d, a);
        ParallelWays pw = new ParallelWays(Arrays.asList(w1, w2), false, 0);
        assertTrue(pw.isClosedPath());
        pw.changeOffset(-10);
        List<EastNorth> pts = pw.getOffsetPoints();
        assertTrue(pw.isResultClosed());
        assertEquals(4, pts.size());
        assertContains(pts, -10, -10);
        assertContains(pts, 110, 110);
    }
}
