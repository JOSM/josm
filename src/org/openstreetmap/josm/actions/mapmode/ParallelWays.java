// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.NodeGraph;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Helper for {@link ParallelWayAction}.
 * <p>
 * Computes a one-sided offset ("parallel") of a branchless path made of one or more ways.
 * <p>
 * The algorithm is a proper one-sided buffer, which also works when the offset is much larger than
 * the length of the segments of the source path (e.g. a maritime boundary 22 km off a coastline):
 * <ol>
 * <li>Each segment is offset by the requested distance.</li>
 * <li>At every vertex the two neighbouring offset segments are joined: on the inner side of a turn they are
 * clipped at their intersection, on the outer side a mitre is used for gentle turns and a circular arc
 * (approximated by chords) for sharp turns or when the mitre would overshoot too far.</li>
 * <li>The resulting raw polyline is split at its self-intersections. Every piece is kept only if it lies at
 * (at least) the offset distance from the source path. Pieces which are closer belong to inverted loops
 * ("swallowtails") and are dropped. The remaining pieces are chained; the longest chain is the result.</li>
 * </ol>
 * All calculations are done in projected coordinates.
 * <p>
 * Contrary to earlier versions the nodes and ways are only created (and added to the data set) by
 * {@link #commit()}, since the number of nodes of the result depends on the offset. Use
 * {@link #getOffsetPoints()} to draw a preview while the offset is changed.
 */
public class ParallelWays {

    /** Default angular step used to approximate circular arcs, in degrees */
    public static final double DEFAULT_ARC_STEP_DEGREES = 10;

    private static final int NO_NODE = -1;
    /** marker for raw segments of the end caps, which are only used to trim the result and are never part of it */
    private static final int CAP = -2;

    private final List<Way> sourceWays;
    private final boolean copyTags;
    private final double arcStep;

    /** the source nodes, in path order (duplicates by coordinate removed, oriented like the reference way) */
    private final List<Node> sortedNodes;
    private final boolean closed;
    private final int nodeCount;

    private final double[] px;
    private final double[] py;
    /** unit direction of segment i */
    private final double[] dirX;
    private final double[] dirY;
    private final double[] segLen;
    /** bounding boxes of the source segments, used to speed up distance computations */
    private final double[] segMinX;
    private final double[] segMinY;
    private final double[] segMaxX;
    private final double[] segMaxY;

    /** whether the source way runs in the same direction as sortedNodes (aligned with sourceWays) */
    private final boolean[] wayForward;
    /** source way index for each source segment */
    private final int[] segWay;

    // Spatial index of the source segments (rebuilt for every offset, since the cell size depends on it)
    private double gridCell;
    private double gridMinX;
    private double gridMinY;
    private int gridCols;
    private int gridRows;
    private int[][] gridCells;
    private int[] gridStamp;
    private int gridQuery;

    // Result of the last changeOffset call
    private List<EastNorth> resultPts = Collections.emptyList();
    private int[] resultPieceSeg = new int[0];
    private int[] resultPointNode = new int[0];
    private boolean resultClosed;

    private List<Way> ways = Collections.emptyList();

    /**
     * Constructs a new {@code ParallelWays}.
     * @param sourceWays source ways
     * @param copyTags whether tags should be copied
     * @param refWayIndex Need a reference way to determine the direction of the offset when we manage multiple ways
     * @throws IllegalArgumentException if the ways do not form a branchless path
     */
    public ParallelWays(Collection<Way> sourceWays, boolean copyTags, int refWayIndex) {
        this(sourceWays, copyTags, refWayIndex, DEFAULT_ARC_STEP_DEGREES);
    }

    /**
     * Constructs a new {@code ParallelWays}.
     * @param sourceWays source ways
     * @param copyTags whether tags should be copied
     * @param refWayIndex Need a reference way to determine the direction of the offset when we manage multiple ways
     * @param arcStepDegrees angular step (in degrees) of the chords approximating arcs at convex corners
     * @throws IllegalArgumentException if the ways do not form a branchless path
     * @since 19624
     */
    public ParallelWays(Collection<Way> sourceWays, boolean copyTags, int refWayIndex, double arcStepDegrees) {
        this.sourceWays = new ArrayList<>(sourceWays);
        this.copyTags = copyTags;
        this.arcStep = Math.toRadians(Math.max(1, Math.min(90, arcStepDegrees)));

        // Find a linear ordering of the nodes. Fail if there isn't one.
        NodeGraph nodeGraph = NodeGraph.createUndirectedGraphFromNodeWays(this.sourceWays);
        List<Node> sortedNodesPath = nodeGraph.buildSpanningPath();
        if (sortedNodesPath == null || sortedNodesPath.size() < 2)
            throw new IllegalArgumentException("Ways must have spanning path"); // Create a dedicated exception?

        List<Node> nodes = new ArrayList<>(sortedNodesPath.size());
        for (Node n : sortedNodesPath) {
            if (nodes.isEmpty() || !nodes.get(nodes.size() - 1).getEastNorth().equalsEpsilon(n.getEastNorth(), 1e-9)) {
                nodes.add(n);
            }
        }
        closed = nodes.size() > 2 && nodes.get(0) == nodes.get(nodes.size() - 1);
        if (closed) {
            nodes.remove(nodes.size() - 1);
        }
        if (nodes.size() < 2)
            throw new IllegalArgumentException("Ways must have spanning path");

        Way refWay = this.sourceWays.get(refWayIndex);
        if (!isForward(nodes, refWay, closed)) {
            Collections.reverse(nodes); // need to keep the orientation of the reference way.
        }
        if (closed) {
            // rotate so that the path starts at a way boundary: every way is then a contiguous run of nodes
            Node start = this.sourceWays.stream().map(Way::firstNode).filter(nodes::contains).findFirst().orElse(nodes.get(0));
            Collections.rotate(nodes, -nodes.indexOf(start));
            nodes.add(nodes.get(0));
        }
        sortedNodes = nodes;
        nodeCount = nodes.size();

        // Initialize the required parameters. (segment directions, etc.)
        px = new double[nodeCount];
        py = new double[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            EastNorth en = nodes.get(i).getEastNorth();
            px[i] = en.getX();
            py[i] = en.getY();
        }
        int segCount = nodeCount - 1;
        dirX = new double[segCount];
        dirY = new double[segCount];
        segLen = new double[segCount];
        segMinX = new double[segCount];
        segMinY = new double[segCount];
        segMaxX = new double[segCount];
        segMaxY = new double[segCount];
        for (int i = 0; i < segCount; i++) {
            double dx = px[i + 1] - px[i];
            double dy = py[i + 1] - py[i];
            double len = Math.hypot(dx, dy);
            segLen[i] = len;
            dirX[i] = dx / len;
            dirY[i] = dy / len;
            segMinX[i] = Math.min(px[i], px[i + 1]);
            segMaxX[i] = Math.max(px[i], px[i + 1]);
            segMinY[i] = Math.min(py[i], py[i + 1]);
            segMaxY[i] = Math.max(py[i], py[i + 1]);
        }

        // Map the source ways onto the path
        int wayCount = this.sourceWays.size();
        wayForward = new boolean[wayCount];
        segWay = new int[segCount];
        Arrays.fill(segWay, -1);
        for (int w = 0; w < wayCount; w++) {
            Way way = this.sourceWays.get(w);
            Set<Node> wayNodes = new HashSet<>(way.getNodes());
            // indices (without the closing duplicate) of the path nodes belonging to this way
            boolean[] present = new boolean[nodeCount];
            int first = -1;
            int last = -1;
            for (int i = 0; i < nodeCount - (closed ? 1 : 0); i++) {
                if (wayNodes.contains(sortedNodes.get(i))) {
                    present[i] = true;
                    if (first < 0) {
                        first = i;
                    }
                    last = i;
                }
            }
            if (first < 0) {
                first = 0;
                last = 0;
            } else if (closed && present[0] && present[nodeCount - 2]) {
                // the run of this way wraps around the closing node: find where it starts
                int i = nodeCount - 2;
                while (i > 0 && present[i - 1]) {
                    i--;
                }
                first = i;
                last = nodeCount - 1;
            }
            wayForward[w] = isForward(sortedNodes.subList(first, last + 1), way, false);
            for (int i = first; i < last; i++) {
                segWay[i] = w;
            }
        }
    }

    /**
     * Checks whether a way runs in the same direction as a node list.
     * @param path the node list
     * @param way the way
     * @param cyclic whether the node list is a ring (without repeated closing node)
     * @return {@code true} if the first segment of the way, found in the path, has the same orientation
     */
    private static boolean isForward(List<Node> path, Way way, boolean cyclic) {
        int n = path.size();
        for (int k = 0; k < way.getNodesCount() - 1; k++) {
            int a = path.indexOf(way.getNode(k));
            int b = path.indexOf(way.getNode(k + 1));
            if (a >= 0 && b >= 0 && a != b) {
                if (cyclic) {
                    return ((b - a) % n + n) % n < n / 2.0;
                }
                return b > a;
            }
        }
        return true;
    }

    /**
     * Determines if the nodes graph form a closed path
     * @return {@code true} if the nodes graph form a closed path
     */
    public boolean isClosedPath() {
        return closed;
    }

    /**
     * The point of the source path closest to a given point, see {@link #closestPoint(EastNorth)}.
     * @since 19624
     */
    public static final class ClosestPoint {
        /** the closest point on the source path */
        public final EastNorth point;
        /** start of the source segment the closest point lies on */
        public final EastNorth segmentStart;
        /** end of the source segment the closest point lies on */
        public final EastNorth segmentEnd;
        /** distance between the given point and the path; positive if the given point lies to the left of the path
         * (in the direction of the reference way), i.e. the sign matches the offset of {@link #changeOffset(double)} */
        public final double signedDistance;

        ClosestPoint(EastNorth point, EastNorth segmentStart, EastNorth segmentEnd, double signedDistance) {
            this.point = point;
            this.segmentStart = segmentStart;
            this.segmentEnd = segmentEnd;
            this.signedDistance = signedDistance;
        }
    }

    /**
     * Finds the point of the source path closest to the given point. Used to relate the offset to the part of the
     * path the mouse is currently next to (rather than to the segment where the drag started).
     * @param p the point (projected coordinates)
     * @return the closest point, the segment it lies on and the signed distance
     * @since 19624
     */
    public ClosestPoint closestPoint(EastNorth p) {
        double x = p.getX();
        double y = p.getY();
        int bestSeg = 0;
        double bestSq = Double.POSITIVE_INFINITY;
        double bestT = 0;
        for (int i = 0; i < nodeCount - 1; i++) {
            double rx = x - px[i];
            double ry = y - py[i];
            double t = Math.max(0, Math.min(segLen[i], rx * dirX[i] + ry * dirY[i]));
            double ex = rx - t * dirX[i];
            double ey = ry - t * dirY[i];
            double dSq = ex * ex + ey * ey;
            if (dSq < bestSq) {
                bestSq = dSq;
                bestSeg = i;
                bestT = t;
            }
        }
        int i = bestSeg;
        // Side of the path: left of the closest segment is positive. If the closest point is a vertex between two
        // segments, the point lies in the wedge outside the corner, where the two segments may disagree about the
        // side (for turns sharper than 90°): the side is then the outside of the turn.
        double side = dirX[i] * (y - py[i]) - dirY[i] * (x - px[i]);
        int vertex = -1;
        if (bestT <= 0) {
            vertex = i;
        } else if (bestT >= segLen[i]) {
            vertex = i + 1;
        }
        if (vertex >= 0) {
            int segCount = nodeCount - 1;
            int prev = vertex - 1;
            int next = vertex;
            if (closed) {
                prev = (vertex - 1 + segCount) % segCount;
                next = vertex % segCount;
            }
            if (prev >= 0 && next < segCount) {
                double turn = dirX[prev] * dirY[next] - dirY[prev] * dirX[next];
                if (Math.abs(turn) > 1e-12) {
                    side = -turn;
                }
            }
        }
        return new ClosestPoint(new EastNorth(px[i] + bestT * dirX[i], py[i] + bestT * dirY[i]),
                new EastNorth(px[i], py[i]), new EastNorth(px[i + 1], py[i + 1]),
                (side >= 0 ? 1 : -1) * Math.sqrt(bestSq));
    }

    /**
     * Offsets the way(s) d units. Positive d means to the left (relative to the reference way)
     * @param d offset
     */
    public void changeOffset(double d) {
        if (d == 0 || Double.isNaN(d)) {
            // no offset: the result is a copy (for a ring without the repeated closing node)
            int pointCount = closed ? nodeCount - 1 : nodeCount;
            resultPts = new ArrayList<>(pointCount);
            for (int i = 0; i < pointCount; i++) {
                resultPts.add(new EastNorth(px[i], py[i]));
            }
            resultPieceSeg = IntStream.range(0, nodeCount - 1).toArray();
            resultPointNode = IntStream.range(0, pointCount).toArray();
            resultClosed = closed;
            return;
        }
        buildGrid(Math.abs(d));
        RawPolyline raw = buildRawOffset(d);
        trim(raw, Math.abs(d));
    }

    /**
     * Builds a uniform grid over the source segments, with a cell size of (at least) r, so that all segments
     * within distance r of a point are found in the 3x3 cells around it.
     * @param r the (absolute) offset
     */
    private void buildGrid(double r) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < nodeCount; i++) {
            minX = Math.min(minX, px[i]);
            maxX = Math.max(maxX, px[i]);
            minY = Math.min(minY, py[i]);
            maxY = Math.max(maxY, py[i]);
        }
        double extent = Math.max(maxX - minX, maxY - minY);
        gridCell = Math.max(r, extent / 128);
        gridMinX = minX;
        gridMinY = minY;
        gridCols = (int) ((maxX - minX) / gridCell) + 1;
        gridRows = (int) ((maxY - minY) / gridCell) + 1;
        int[] counts = new int[gridCols * gridRows];
        int segCount = nodeCount - 1;
        for (int i = 0; i < segCount; i++) {
            forEachCell(segMinX[i], segMinY[i], segMaxX[i], segMaxY[i], c -> counts[c]++);
        }
        gridCells = new int[counts.length][];
        for (int c = 0; c < counts.length; c++) {
            gridCells[c] = new int[counts[c]];
            counts[c] = 0;
        }
        for (int i = 0; i < segCount; i++) {
            final int seg = i;
            forEachCell(segMinX[i], segMinY[i], segMaxX[i], segMaxY[i], c -> gridCells[c][counts[c]++] = seg);
        }
        gridStamp = new int[segCount];
        gridQuery = 0;
    }

    private void forEachCell(double minX, double minY, double maxX, double maxY, IntConsumer consumer) {
        int c0 = Math.max(0, (int) ((minX - gridMinX) / gridCell));
        int c1 = Math.min(gridCols - 1, (int) ((maxX - gridMinX) / gridCell));
        int r0 = Math.max(0, (int) ((minY - gridMinY) / gridCell));
        int r1 = Math.min(gridRows - 1, (int) ((maxY - gridMinY) / gridCell));
        for (int row = r0; row <= r1; row++) {
            for (int col = c0; col <= c1; col++) {
                consumer.accept(row * gridCols + col);
            }
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Step 1: raw offset polyline

    /**
     * The raw (untrimmed) offset polyline. Segment k runs from point k to point k+1 (or to point 0 for the last
     * segment of a closed polyline). The attributes of a segment are stored at its end point.
     */
    private static final class RawPolyline {
        double[] x = new double[64];
        double[] y = new double[64];
        /** index of the source node a point was derived from, or {@link #NO_NODE} */
        int[] node = new int[64];
        /** source segment index of the segment ending at this point, or {@link #CAP} */
        int[] seg = new int[64];
        /** if the segment ending at this point is an arc chord: the source node the arc is centered on, else NO_NODE */
        int[] arcCenter = new int[64];
        int size;

        /** for each source node with an arc: raw index of the first arc point, else -1 */
        final int[] arcStart;
        /** number of chords of the arc at each source node */
        final int[] arcChords;
        /** whether the arc can be replaced by a mitre when it is not affected by the trimming */
        final boolean[] arcMitre;
        final double[] mitreX;
        final double[] mitreY;

        RawPolyline(int nodeCount) {
            arcStart = new int[nodeCount];
            Arrays.fill(arcStart, -1);
            arcChords = new int[nodeCount];
            arcMitre = new boolean[nodeCount];
            mitreX = new double[nodeCount];
            mitreY = new double[nodeCount];
        }

        void add(double px, double py, int srcNode, int srcSeg, int arc) {
            if (size == x.length) {
                int n = size * 2;
                x = Arrays.copyOf(x, n);
                y = Arrays.copyOf(y, n);
                node = Arrays.copyOf(node, n);
                seg = Arrays.copyOf(seg, n);
                arcCenter = Arrays.copyOf(arcCenter, n);
            }
            x[size] = px;
            y[size] = py;
            node[size] = srcNode;
            seg[size] = srcSeg;
            arcCenter[size] = arc;
            size++;
        }
    }

    private RawPolyline buildRawOffset(double d) {
        RawPolyline raw = new RawPolyline(nodeCount);
        int segCount = nodeCount - 1;
        if (closed) {
            addJoin(raw, 0, segCount - 1, 0, d);
        } else {
            // Start cap: half circle on the back side of the first node. It is never part of the result, but
            // trims pieces of the offset which come closer than d to the first node.
            addCap(raw, 0, Math.atan2(-dirX[0] * d, dirY[0] * d), d > 0 ? -1 : 1, d, true);
            raw.add(px[0] - dirY[0] * d, py[0] + dirX[0] * d, 0, CAP, NO_NODE);
        }
        for (int k = 1; k < segCount; k++) {
            addJoin(raw, k, k - 1, k, d);
        }
        if (!closed) {
            int s = segCount - 1;
            raw.add(px[s + 1] - dirY[s] * d, py[s + 1] + dirX[s] * d, s + 1, s, NO_NODE);
            // End cap: half circle on the front side of the last node
            addCap(raw, s + 1, Math.atan2(dirX[s] * d, -dirY[s] * d), d > 0 ? -1 : 1, d, false);
        } else {
            // the closing segment ends at raw point 0
            raw.seg[0] = segCount - 1;
            raw.arcCenter[0] = NO_NODE;
        }
        return raw;
    }

    /**
     * Adds the chords of a half circle around node {@code k}, starting at the given angle. The chords are marked
     * as {@link #CAP}: they are only used to trim other pieces.
     * @param raw the raw polyline
     * @param k source node index
     * @param startAngle angle of the first point of the half circle
     * @param direction rotation direction (+1 counter clockwise)
     * @param d the offset
     * @param leading {@code true} if the cap precedes the offset path (the end point of the half circle is then
     * added by the caller), {@code false} if it follows it (the start point has been added by the caller)
     */
    private void addCap(RawPolyline raw, int k, double startAngle, int direction, double d, boolean leading) {
        double r = Math.abs(d);
        int steps = (int) Math.ceil(Math.PI / arcStep - 1e-9);
        for (int j = leading ? 0 : 1; j < (leading ? steps : steps + 1); j++) {
            double angle = startAngle + direction * Math.PI * j / steps;
            raw.add(px[k] + r * Math.cos(angle), py[k] + r * Math.sin(angle), NO_NODE, CAP, NO_NODE);
        }
    }

    /**
     * Adds the offset points at vertex {@code k}, where segment {@code prev} ends and segment {@code next} starts.
     * @param raw the raw polyline
     * @param k source node index
     * @param prev index of the segment ending at k
     * @param next index of the segment starting at k
     * @param d the offset
     */
    private void addJoin(RawPolyline raw, int k, int prev, int next, double d) {
        double r = Math.abs(d);
        // offset end point of prev, offset start point of next
        double bx = px[k] - dirY[prev] * d;
        double by = py[k] + dirX[prev] * d;
        double ax = px[k] - dirY[next] * d;
        double ay = py[k] + dirX[next] * d;

        double cross = dirX[prev] * dirY[next] - dirY[prev] * dirX[next];
        double dot = dirX[prev] * dirX[next] + dirY[prev] * dirY[next];
        double theta = Math.atan2(Math.abs(cross), dot); // turn angle in [0, pi]

        if (theta < 1e-9) {
            // collinear: the offset points coincide
            raw.add(bx, by, k, prev, NO_NODE);
            return;
        }
        boolean concave = cross * d > 0; // the turn goes towards the offset side
        if (concave && theta < Math.PI - 1e-9) {
            // Clip: intersect the two offset segments.
            // X = b + t*dir[prev] with -len[prev] <= t <= 0 and X = a + s*dir[next] with 0 <= s <= len[next]
            double ex = ax - bx;
            double ey = ay - by;
            double t = (ex * dirY[next] - ey * dirX[next]) / cross;
            double s = (ex * dirY[prev] - ey * dirX[prev]) / cross;
            if (-t <= segLen[prev] && s <= segLen[next] && t <= 1e-9 && s >= -1e-9) {
                raw.add(bx + t * dirX[prev], by + t * dirY[prev], k, prev, NO_NODE);
            } else {
                // The offset is larger than the neighbouring segments allow: local inversion.
                // Emit both points; the inverted part is removed when trimming.
                raw.add(bx, by, k, prev, NO_NODE);
                raw.add(ax, ay, NO_NODE, next, k);
            }
            return;
        }
        // Convex corner (or a hairpin): arc from b to a around the vertex. If the arc survives the trimming
        // untouched, it may later be replaced by a mitre (for gentle corners, see arcMitre).
        raw.add(bx, by, k, prev, NO_NODE);
        int steps = (int) Math.ceil(theta / arcStep - 1e-9);
        double angle0 = Math.atan2(by - py[k], bx - px[k]);
        double delta;
        if (Math.abs(cross) < 1e-12) {
            // hairpin: rotate away from the segments, i.e. towards dir[prev]
            delta = theta * (d > 0 ? -1 : 1);
        } else {
            delta = theta * (cross >= 0 ? 1 : -1);
        }
        raw.arcStart[k] = raw.size - 1;
        raw.arcChords[k] = steps;
        for (int j = 1; j < steps; j++) {
            double angle = angle0 + delta * j / steps;
            raw.add(px[k] + r * Math.cos(angle), py[k] + r * Math.sin(angle), NO_NODE, next, k);
        }
        raw.add(ax, ay, NO_NODE, next, k);

        if (theta < Math.PI - 1e-6) {
            double mitreDist = r / Math.cos(theta / 2);
            double overshoot = mitreDist - r;
            raw.arcMitre[k] = theta <= arcStep || overshoot <= 0.5 * Math.min(segLen[prev], segLen[next]);
            // the mitre point lies on the bisector of the two normals
            double mx = (bx - px[k]) + (ax - px[k]);
            double my = (by - py[k]) + (ay - py[k]);
            double ml = Math.hypot(mx, my);
            raw.mitreX[k] = px[k] + mx / ml * mitreDist;
            raw.mitreY[k] = py[k] + my / ml * mitreDist;
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // Step 2: trimming

    /** Growable list of pieces (sub segments of the raw polyline) */
    private static final class Pieces {
        int[] start = new int[256];
        int[] end = new int[256];
        int[] rawSeg = new int[256];
        boolean[] valid = new boolean[256];
        int size;

        void add(int s, int e, int seg, boolean v) {
            if (size == start.length) {
                int n = size * 2;
                start = Arrays.copyOf(start, n);
                end = Arrays.copyOf(end, n);
                rawSeg = Arrays.copyOf(rawSeg, n);
                valid = Arrays.copyOf(valid, n);
            }
            start[size] = s;
            end[size] = e;
            rawSeg[size] = seg;
            valid[size] = v;
            size++;
        }
    }

    /** Point table used while trimming: raw points first, then intersection and transition points */
    private static final class Points {
        final List<double[]> xy = new ArrayList<>();
        final List<Integer> node = new ArrayList<>();

        int add(double x, double y, int srcNode) {
            xy.add(new double[] {x, y});
            node.add(srcNode);
            return xy.size() - 1;
        }

        double[] get(int id) {
            return xy.get(id);
        }

        double distance(int a, int b) {
            double[] p = xy.get(a);
            double[] q = xy.get(b);
            return Math.hypot(q[0] - p[0], q[1] - p[1]);
        }
    }

    private void trim(RawPolyline raw, double r) {
        int m = raw.size;
        int rawSegCount = closed ? m : m - 1;
        if (rawSegCount < 1) {
            setEmptyResult();
            return;
        }

        Points points = new Points();
        for (int i = 0; i < m; i++) {
            points.add(raw.x[i], raw.y[i], raw.node[i]);
        }
        // Raw segments which lie completely inside the offset distance ("deep") can neither be part of the result
        // nor trim it: skip them when looking for intersections. This is a huge saving for large offsets.
        boolean[] deep = new boolean[rawSegCount];
        double tolerance = r * 1e-7 + 1e-7;
        double sagitta = r * (1 - Math.cos(arcStep / 2));
        for (int i = 0; i < rawSegCount; i++) {
            int endPoint = (i + 1) % m;
            double mx = (raw.x[i] + raw.x[endPoint]) / 2;
            double my = (raw.y[i] + raw.y[endPoint]) / 2;
            double halfLen = Math.hypot(raw.x[endPoint] - raw.x[i], raw.y[endPoint] - raw.y[i]) / 2;
            // every point of the segment is within halfLen (+ sagitta for a chord) of the middle
            double cutoff = r - halfLen - tolerance - (raw.arcCenter[endPoint] != NO_NODE ? sagitta : 0);
            deep[i] = cutoff > 0 && distanceToSource(mx, my, cutoff) < cutoff;
        }

        // breakpoints per raw segment: parallel lists of (param, pointId)
        List<List<double[]>> breaks = new ArrayList<>(Collections.nCopies(rawSegCount, null));
        findSelfIntersections(raw, rawSegCount, deep, points, breaks);

        // Build the pieces and determine their validity: a piece must lie at (at least) distance r from the source
        Pieces pieces = new Pieces();
        boolean anyInvalid = false;
        for (int i = 0; i < rawSegCount; i++) {
            int endPoint = (i + 1) % m;
            if (deep[i]) {
                pieces.add(i, endPoint, i, false);
                continue;
            }
            List<double[]> b = breaks.get(i);
            int prevId = i;
            if (b != null) {
                b.sort((o1, o2) -> Double.compare(o1[0], o2[0]));
                for (double[] bp : b) {
                    classify(raw, r, points, pieces, i, prevId, (int) bp[1]);
                    prevId = (int) bp[1];
                }
            }
            classify(raw, r, points, pieces, i, prevId, endPoint);
        }
        for (int p = 0; p < pieces.size; p++) {
            anyInvalid |= !pieces.valid[p];
        }

        // Chain the valid pieces. Consecutive valid pieces of the same component meet at a common point
        // (self-intersection or transition), but pieces of other components (e.g. an excursion of the path, or an
        // inner loop) may lie between them in raw order: a piece is therefore attached to whichever open chain ends
        // where it starts. Chord approximations of arcs can leave small gaps - such gaps are bridged; larger gaps
        // separate different components.
        double bridgeTolerance = 2 * r * Math.sin(arcStep / 2);
        double spikeTolerance = 2 * r * (1 - Math.cos(arcStep / 2)) + tolerance;
        List<Chain> chains = new ArrayList<>();
        int pieceCount = pieces.size;
        int startPiece = 0;
        if (closed && anyInvalid) {
            while (pieces.valid[startPiece]) {
                startPiece++;
            }
            startPiece = (startPiece + 1) % pieceCount;
        }
        for (int c = 0; c < pieceCount; c++) {
            int p = (startPiece + c) % pieceCount;
            if (!pieces.valid[p]) {
                continue;
            }
            int srcSeg = raw.seg[(pieces.rawSeg[p] + 1) % m];
            Chain chain = findChainEndingAt(chains, pieces.start[p], points, bridgeTolerance);
            if (chain == null) {
                chain = new Chain(pieces.start[p], spikeTolerance);
                chains.add(chain);
            } else if (chain.lastId != pieces.start[p]) {
                chain.append(pieces.start[p], srcSeg, points);
            }
            chain.append(pieces.end[p], srcSeg, points);
        }
        // Join chains whose end meets the start of another chain (a ring may also close onto itself)
        boolean merged = true;
        while (merged && chains.size() > 1) {
            merged = false;
            for (int i = 0; i < chains.size() && !merged; i++) {
                Chain a = chains.get(i);
                for (int j = 0; j < chains.size(); j++) {
                    Chain b = chains.get(j);
                    int bFirst = b.ids.get(0);
                    if (a != b && (a.lastId == bFirst || points.distance(a.lastId, bFirst) <= bridgeTolerance)) {
                        if (a.lastId != bFirst) {
                            a.append(bFirst, b.segs.get(0), points);
                        }
                        a.appendChain(b);
                        chains.remove(j);
                        merged = true;
                        break;
                    }
                }
            }
        }
        if (chains.isEmpty()) {
            setEmptyResult();
            return;
        }
        Chain best = chains.get(0);
        for (Chain ch : chains) {
            if (ch.length > best.length) {
                best = ch;
            }
        }
        if (closed && best.lastId != best.ids.get(0) && points.distance(best.lastId, best.ids.get(0)) <= bridgeTolerance) {
            // close a ring which is open by a small gap only
            best.append(best.ids.get(0), best.segs.get(best.segs.size() - 1), points);
        }

        // Convert to the result
        boolean isRing = closed && best.ids.size() > 2 && best.ids.get(0) == best.lastId;
        List<Integer> ids = new ArrayList<>(best.ids);
        List<Integer> segs = new ArrayList<>(best.segs);
        replaceIntactArcsByMitres(raw, points, ids, segs);
        if (isRing) {
            ids.remove(ids.size() - 1);
            // rotate so that the ring starts at a way boundary
            int n = segs.size();
            int rot = 0;
            for (int i = 0; i < n; i++) {
                if (segWay[segs.get((i + n - 1) % n)] != segWay[segs.get(i)]) {
                    rot = i;
                    break;
                }
            }
            Collections.rotate(ids, -rot);
            Collections.rotate(segs, -rot);
        }
        resultPts = new ArrayList<>(ids.size());
        resultPointNode = new int[ids.size()];
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            double[] pt = points.get(id);
            resultPts.add(new EastNorth(pt[0], pt[1]));
            resultPointNode[i] = points.node.get(id);
        }
        resultPieceSeg = segs.stream().mapToInt(Integer::intValue).toArray();
        resultClosed = isRing;
    }

    private void setEmptyResult() {
        resultPts = Collections.emptyList();
        resultPieceSeg = new int[0];
        resultPointNode = new int[0];
        resultClosed = false;
    }

    /**
     * Determines the validity of the piece of raw segment {@code i} between two points, and adds the resulting
     * piece(s). The validity is sampled at both ends and in the middle; if it changes, the transition point is
     * located and the piece is split there.
     */
    private void classify(RawPolyline raw, double r, Points points, Pieces pieces, int i, int sId, int eId) {
        int endPoint = (i + 1) % raw.size;
        if (raw.seg[endPoint] == CAP) {
            pieces.add(sId, eId, i, false); // end caps are never part of the result
            return;
        }
        int arc = raw.arcCenter[endPoint];
        double[] s = points.get(sId);
        double[] e = points.get(eId);
        double tolerance = r * 1e-7 + 1e-7;
        boolean vs = isValid(s[0], s[1], r, tolerance, arc);
        boolean ve = isValid(e[0], e[1], r, tolerance, arc);
        boolean vm = isValid(s[0] + (e[0] - s[0]) / 2, s[1] + (e[1] - s[1]) / 2, r, tolerance, arc);
        if (vs == vm && vm == ve) {
            pieces.add(sId, eId, i, vm);
            return;
        }
        // Transitions very close to an end point are artefacts of the chord approximation (a point where two
        // chords cross lies inside both circles): the validity of the middle is then used for the whole piece.
        double snapTolerance = 2 * r * (1 - Math.cos(arcStep / 2)) + tolerance;
        double len = Math.hypot(e[0] - s[0], e[1] - s[1]);
        int prev = sId;
        boolean state = vs;
        if (vs != vm) {
            double t1 = locateTransition(s, e, 0, 0.5, r, tolerance, arc);
            if (t1 * len <= snapTolerance) {
                state = vm;
            } else {
                int id = points.add(s[0] + (e[0] - s[0]) * t1, s[1] + (e[1] - s[1]) * t1, NO_NODE);
                pieces.add(prev, id, i, state);
                prev = id;
                state = vm;
            }
        }
        if (vm != ve) {
            double t2 = locateTransition(s, e, 0.5, 1, r, tolerance, arc);
            if ((1 - t2) * len > snapTolerance) {
                int id = points.add(s[0] + (e[0] - s[0]) * t2, s[1] + (e[1] - s[1]) * t2, NO_NODE);
                pieces.add(prev, id, i, state);
                prev = id;
                state = ve;
            }
        }
        pieces.add(prev, eId, i, state);
    }

    /**
     * Locates (by bisection) the parameter between {@code a} and {@code b} where the validity changes.
     * @return the parameter of the transition
     */
    private double locateTransition(double[] s, double[] e, double a, double b, double r, double tolerance, int arc) {
        boolean va = isValid(s[0] + (e[0] - s[0]) * a, s[1] + (e[1] - s[1]) * a, r, tolerance, arc);
        for (int it = 0; it < 40 && b - a > 1e-12; it++) {
            double mid = (a + b) / 2;
            boolean vmid = isValid(s[0] + (e[0] - s[0]) * mid, s[1] + (e[1] - s[1]) * mid, r, tolerance, arc);
            if (vmid == va) {
                a = mid;
            } else {
                b = mid;
            }
        }
        return (a + b) / 2;
    }

    /**
     * Checks whether a point of the raw polyline lies at (at least) distance r from the source path.
     * @param x point
     * @param y point
     * @param r offset
     * @param tolerance allowed deficit
     * @param arc if the point lies on an arc chord: the center node of the arc; the point is then projected onto
     * the arc before testing, else {@link #NO_NODE}
     * @return {@code true} if the point is valid
     */
    private boolean isValid(double x, double y, double r, double tolerance, int arc) {
        if (arc != NO_NODE) {
            double vx = x - px[arc];
            double vy = y - py[arc];
            double vl = Math.hypot(vx, vy);
            if (vl > 0) {
                x = px[arc] + vx / vl * r;
                y = py[arc] + vy / vl * r;
            }
        }
        return distanceToSource(x, y, r - tolerance) >= r - tolerance;
    }

    /**
     * Replaces arcs which are completely part of the result by a mitre (where the corner is gentle enough).
     */
    private static void replaceIntactArcsByMitres(RawPolyline raw, Points points, List<Integer> ids, List<Integer> segs) {
        int[] arcOfRawPoint = new int[raw.size];
        Arrays.fill(arcOfRawPoint, NO_NODE);
        for (int k = 0; k < raw.arcStart.length; k++) {
            if (raw.arcStart[k] >= 0 && raw.arcMitre[k]) {
                arcOfRawPoint[raw.arcStart[k]] = k;
            }
        }
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.get(i);
            if (id >= raw.size || arcOfRawPoint[id] == NO_NODE) {
                continue;
            }
            int k = arcOfRawPoint[id];
            int chords = raw.arcChords[k];
            if (i + chords >= ids.size()) {
                continue;
            }
            boolean intact = true;
            for (int j = 1; j <= chords && intact; j++) {
                intact = ids.get(i + j) == id + j;
            }
            if (!intact) {
                continue;
            }
            int mitreId = points.add(raw.mitreX[k], raw.mitreY[k], k);
            // the chord pieces i..i+chords-1 collapse into two pieces: (i-1 -> mitre), (mitre -> i+chords)
            ids.set(i, mitreId);
            ids.subList(i + 1, i + chords + 1).clear();
            // segs.get(j) belongs to the piece ending at ids.get(j+1); keep the segment of the last chord for the
            // piece leaving the mitre, and drop the others
            segs.subList(i, i + chords).clear();
        }
    }

    /**
     * Finds the chain ending at (or within the tolerance of) the given point, preferring an exact match.
     * @return the chain, or {@code null}
     */
    private static Chain findChainEndingAt(List<Chain> chains, int id, Points points, double tolerance) {
        Chain near = null;
        double nearDist = tolerance;
        for (Chain ch : chains) {
            if (ch.lastId == id) {
                return ch;
            }
            double dist = points.distance(ch.lastId, id);
            if (dist <= nearDist) {
                near = ch;
                nearDist = dist;
            }
        }
        return near;
    }

    /** A chain of connected valid pieces */
    private static final class Chain {
        final List<Integer> ids = new ArrayList<>();
        final List<Integer> segs = new ArrayList<>();
        final double spikeTolerance;
        int lastId;
        double length;

        Chain(int startId, double spikeTolerance) {
            ids.add(startId);
            lastId = startId;
            this.spikeTolerance = spikeTolerance;
        }

        void append(int endId, int srcSeg, Points points) {
            lastId = endId;
            if (ids.size() >= 2 && points.distance(ids.get(ids.size() - 2), endId) <= spikeTolerance) {
                // tiny out-and-back spike (an artefact of the chord approximation at a crossing): drop its tip
                int tip = ids.remove(ids.size() - 1);
                segs.remove(segs.size() - 1);
                length -= points.distance(ids.get(ids.size() - 1), tip);
            }
            double len = points.distance(ids.get(ids.size() - 1), endId);
            if (len < 1e-9) {
                // zero length piece: keep the connectivity, but don't add a point
                return;
            }
            length += len;
            ids.add(endId);
            segs.add(srcSeg);
        }

        void appendChain(Chain other) {
            ids.addAll(other.ids.subList(1, other.ids.size()));
            segs.addAll(other.segs);
            lastId = other.lastId;
            length += other.length;
        }
    }

    /**
     * Finds all intersections between non adjacent segments of the raw polyline (sweep on x).
     * @param raw the raw polyline
     * @param rawSegCount number of raw segments
     * @param skip raw segments to ignore
     * @param points point table, intersection points are appended
     * @param breaks per raw segment list of (param, pointId), filled
     */
    private void findSelfIntersections(RawPolyline raw, int rawSegCount, boolean[] skip, Points points,
            List<List<double[]>> breaks) {
        int m = raw.size;
        double[] minX = new double[rawSegCount];
        double[] maxX = new double[rawSegCount];
        double[] minY = new double[rawSegCount];
        double[] maxY = new double[rawSegCount];
        int count = 0;
        Integer[] order = new Integer[rawSegCount];
        for (int i = 0; i < rawSegCount; i++) {
            if (skip[i]) {
                continue;
            }
            int j = (i + 1) % m;
            minX[i] = Math.min(raw.x[i], raw.x[j]);
            maxX[i] = Math.max(raw.x[i], raw.x[j]);
            minY[i] = Math.min(raw.y[i], raw.y[j]);
            maxY[i] = Math.max(raw.y[i], raw.y[j]);
            order[count++] = i;
        }
        Arrays.sort(order, 0, count, (a, b) -> Double.compare(minX[a], minX[b]));
        double[] uv = new double[2];
        for (int oi = 0; oi < count; oi++) {
            int i = order[oi];
            for (int oj = oi + 1; oj < count; oj++) {
                int j = order[oj];
                if (minX[j] > maxX[i]) {
                    break;
                }
                if (minY[j] > maxY[i] || maxY[j] < minY[i]) {
                    continue;
                }
                int lo = Math.min(i, j);
                int hi = Math.max(i, j);
                if (hi - lo == 1 || (closed && lo == 0 && hi == rawSegCount - 1)) {
                    continue; // adjacent
                }
                int i2 = (i + 1) % m;
                int j2 = (j + 1) % m;
                if (segmentIntersection(raw.x[i], raw.y[i], raw.x[i2], raw.y[i2], raw.x[j], raw.y[j], raw.x[j2], raw.y[j2], uv)) {
                    int id = points.add(raw.x[i] + (raw.x[i2] - raw.x[i]) * uv[0], raw.y[i] + (raw.y[i2] - raw.y[i]) * uv[0], NO_NODE);
                    if (breaks.get(i) == null) {
                        breaks.set(i, new ArrayList<>(2));
                    }
                    if (breaks.get(j) == null) {
                        breaks.set(j, new ArrayList<>(2));
                    }
                    breaks.get(i).add(new double[] {uv[0], id});
                    breaks.get(j).add(new double[] {uv[1], id});
                }
            }
        }
    }

    /**
     * Segment/segment intersection.
     * @param uv output: parameters along the first and the second segment
     * @return true if the segments intersect (touching end points count as intersection)
     */
    private static boolean segmentIntersection(double x1, double y1, double x2, double y2,
            double x3, double y3, double x4, double y4, double[] uv) {
        double a1 = x2 - x1;
        double a2 = y2 - y1;
        double b1 = x3 - x4;
        double b2 = y3 - y4;
        double c1 = x3 - x1;
        double c2 = y3 - y1;
        double det = a1 * b2 - a2 * b1;
        double uu = b2 * c1 - b1 * c2;
        double vv = a1 * c2 - a2 * c1;
        double mag = Math.abs(uu) + Math.abs(vv);
        if (det == 0 || Math.abs(det) <= 1e-12 * mag) {
            return false; // parallel or collinear
        }
        double u = uu / det;
        double v = vv / det;
        if (u < -1e-9 || u > 1 + 1e-9 || v < -1e-9 || v > 1 + 1e-9) {
            return false;
        }
        uv[0] = Math.max(0, Math.min(1, u));
        uv[1] = Math.max(0, Math.min(1, v));
        return true;
    }

    /**
     * Distance from a point to the source path.
     * @param x point
     * @param y point
     * @param cutoff the search can stop as soon as a distance below this value is found
     * @return the distance (or any value below cutoff if such a distance exists)
     */
    private double distanceToSource(double x, double y, double cutoff) {
        // Only segments within the cutoff matter (the result is only compared against it); with a cell size of
        // at least the offset they all lie in the 3x3 cells around the point.
        int col = (int) Math.floor((x - gridMinX) / gridCell);
        int row = (int) Math.floor((y - gridMinY) / gridCell);
        if (col < -1 || col > gridCols || row < -1 || row > gridRows) {
            return Double.POSITIVE_INFINITY;
        }
        gridQuery++;
        double best = Double.POSITIVE_INFINITY;
        double bestSq = Double.POSITIVE_INFINITY;
        double cutoffSq = cutoff * cutoff;
        for (int rr = Math.max(0, row - 1); rr <= Math.min(gridRows - 1, row + 1); rr++) {
            for (int cc = Math.max(0, col - 1); cc <= Math.min(gridCols - 1, col + 1); cc++) {
                for (int i : gridCells[rr * gridCols + cc]) {
                    if (gridStamp[i] == gridQuery) {
                        continue;
                    }
                    gridStamp[i] = gridQuery;
                    if (x < segMinX[i] - best || x > segMaxX[i] + best || y < segMinY[i] - best || y > segMaxY[i] + best) {
                        continue;
                    }
                    double rx = x - px[i];
                    double ry = y - py[i];
                    double t = rx * dirX[i] + ry * dirY[i];
                    double dSq;
                    if (t <= 0) {
                        dSq = rx * rx + ry * ry;
                    } else if (t >= segLen[i]) {
                        double ex = x - px[i + 1];
                        double ey = y - py[i + 1];
                        dSq = ex * ex + ey * ey;
                    } else {
                        double c = rx * dirY[i] - ry * dirX[i];
                        dSq = c * c;
                    }
                    if (dSq < bestSq) {
                        bestSq = dSq;
                        best = Math.sqrt(dSq);
                        if (bestSq < cutoffSq) {
                            return best;
                        }
                    }
                }
            }
        }
        return best;
    }

    // ---------------------------------------------------------------------------------------------------------
    // Result access and commit

    /**
     * Returns the points of the offset path computed by the last call to {@link #changeOffset(double)}.
     * For a closed result the first point is not repeated at the end, see {@link #isResultClosed()}.
     * @return the offset points (projected coordinates), empty if nothing has been computed or nothing remains
     * @since 19624
     */
    public List<EastNorth> getOffsetPoints() {
        return Collections.unmodifiableList(resultPts);
    }

    /**
     * Determines if the result of the last {@link #changeOffset(double)} call is a closed ring.
     * @return {@code true} if the offset path is a closed ring
     * @since 19624
     */
    public boolean isResultClosed() {
        return resultClosed;
    }

    /**
     * Creates the nodes and ways of the offset path (as computed by the last call to {@link #changeOffset(double)}),
     * and adds them to the edit data set by adding a new sequence command to the undo/redo queue.
     * <p>
     * Does nothing if there is no offset path.
     */
    public void commit() {
        List<Command> commands = makeAddWayAndNodesCommandList();
        if (!commands.isEmpty()) {
            UndoRedoHandler.getInstance().add(new SequenceCommand("Make parallel way(s)", commands));
        }
    }

    private List<Command> makeAddWayAndNodesCommandList() {
        DataSet ds = OsmDataManager.getInstance().getEditDataSet();
        List<Way> newWays = buildWays();
        ways = newWays;
        List<Command> commands = new ArrayList<>();
        if (newWays.isEmpty()) {
            return commands;
        }
        List<Node> added = new ArrayList<>();
        for (Way w : newWays) {
            for (Node n : w.getNodes()) {
                // don't add the same node twice, see #18386
                if (!added.contains(n)) {
                    added.add(n);
                    commands.add(new AddCommand(ds, n));
                }
            }
        }
        for (Way w : newWays) {
            commands.add(new AddCommand(ds, w));
        }
        return commands;
    }

    /**
     * Builds the (not yet added) ways from the last computed offset path.
     * @return the ways, in the order of the source ways; ways swallowed by the offset are omitted
     */
    private List<Way> buildWays() {
        int pointCount = resultPts.size();
        List<Way> result = new ArrayList<>(sourceWays.size());
        if (pointCount < 2) {
            return result;
        }
        int pieceCount = resultPieceSeg.length;
        Node[] nodes = new Node[pointCount];
        for (int w = 0; w < sourceWays.size(); w++) {
            int first = -1;
            int last = -1;
            for (int p = 0; p < pieceCount; p++) {
                if (segWay[resultPieceSeg[p]] == w) {
                    if (first < 0) {
                        first = p;
                    }
                    last = p;
                }
            }
            if (first < 0) {
                continue; // way is swallowed by the offset
            }
            List<Node> wayNodes = new ArrayList<>(last - first + 2);
            for (int p = first; p <= last + 1; p++) {
                int idx = p % pointCount;
                if (nodes[idx] == null) {
                    nodes[idx] = makeNode(idx);
                }
                wayNodes.add(nodes[idx]);
            }
            if (!wayForward[w]) {
                Collections.reverse(wayNodes);
            }
            Way source = sourceWays.get(w);
            Way copy = new Way();
            copy.setNodes(wayNodes);
            if (copyTags) {
                copy.setKeys(source.getKeys());
            }
            result.add(copy);
        }
        return result;
    }

    private Node makeNode(int idx) {
        Node n;
        int src = resultPointNode[idx];
        if (copyTags && src != NO_NODE) {
            n = new Node(sortedNodes.get(src), true);
        } else {
            n = new Node();
        }
        n.setEastNorth(resultPts.get(idx));
        return n;
    }

    /**
     * Returns the resulting parallel ways, available after {@link #commit()}.
     * @return the resulting parallel ways (empty before commit)
     */
    public final List<Way> getWays() {
        return ways;
    }
}
