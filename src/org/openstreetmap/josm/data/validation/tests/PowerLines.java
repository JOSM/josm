// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.JoinedWay;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks for
 * <ul>
 * <li>nodes in power lines/minor_lines that do not have a power=tower/pole/portal tag
 * <li>nodes where the reference numbering not consistent
 * <li>ways where are unusually long segments without line support feature
 * <li>ways where the line type is possibly misused
 * </ul>
 * See #7812 and #20716 for discussions about this test.
 */
public class PowerLines extends Test {
    // Common strings
    private static final String MINOR_LINE = "minor_line";
    private static final String BUILDING = "building";
    private static final String POWER = "power";

    // Test identifiers
    protected static final int POWER_SUPPORT = 2501;
    protected static final int POWER_CONNECTION = 2502;
    protected static final int POWER_SEGMENT_LENGTH = 2503;
    protected static final int POWER_LOCAL_REF_CONTINUITY = 2504;
    protected static final int POWER_WAY_REF_CONTINUITY = 2505;
    protected static final int POWER_LINE_TYPE = 2506;

    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + PowerLines.class.getSimpleName();

    /** Values for {@code power} key interpreted as power lines */
    static final Collection<String> POWER_LINE_TAGS = Arrays.asList("line", MINOR_LINE);
    /** Values for {@code power} key interpreted as power towers */
    static final Collection<String> POWER_TOWER_TAGS = Arrays.asList("catenary_mast", "pole", "portal", "tower");
    /** Values for {@code power} key interpreted as power stations */
    static final Collection<String> POWER_STATION_TAGS = Arrays.asList("generator", "plant", "substation");
    /** Values for {@code building} key interpreted as power stations */
    static final Collection<String> BUILDING_STATION_TAGS = Collections.singletonList("transformer_tower");
    /** Values for {@code power} key interpreted as allowed power items */
    static final Collection<String> POWER_INFRASTRUCTURE_TAGS = Arrays.asList("compensator", "connection", "converter",
            "generator", "insulator", "switch", "switchgear", "terminal", "transformer");

    private double hillyCompensation;
    private double hillyThreshold;
    private final Set<Node> badConnections = new HashSet<>();
    private final Set<Node> missingTags = new HashSet<>();
    private final Set<Way> wrongLineType = new HashSet<>();
    private final Set<WaySegment> missingNodes = new HashSet<>();
    private final Set<OsmPrimitive> refDiscontinuities = new HashSet<>();

    private final List<Set<Node>> segmentRefDiscontinuities = new ArrayList<>();
    private final List<OsmPrimitive> powerStations = new ArrayList<>();

    private final Collection<Way> foundPowerLines = new HashSet<>();
    /** All waterway segments, grouped by cells */
    private final Map<Point2D, List<WaySegment>> cellSegmentsWater = new HashMap<>(32);

    /**
     * Constructs a new {@code PowerLines} test.
     */
    public PowerLines() {
        super(tr("Power lines"), tr("Checks if power line missing a support node and " +
                "for nodes in power lines that do not have a power=tower/pole tag"));
    }

    @Override
    public void visit(Node n) {
        boolean nodeInLineOrCable = false;
        boolean connectedToUnrelated = false;
        for (Way parent : n.getParentWays()) {
            if (parent.hasTag(POWER, "line", MINOR_LINE, "cable"))
                nodeInLineOrCable = true;
            else if (!isRelatedToPower(parent))
                connectedToUnrelated = true;
        }
        if (nodeInLineOrCable && connectedToUnrelated)
            badConnections.add(n);
    }

    @Override
    public void visit(Way w) {
        if (!isPrimitiveUsable(w)) return;

        if (isPowerLine(w) && !w.hasKey("line") && !w.hasTag("location", "underground") && w.isUsable()) {
            foundPowerLines.add(w);
        } else if (w.isClosed() && isPowerStation(w)) {
            powerStations.add(w);
        } else if (concernsWaterArea(w)) {
            this.addWaterWaySegments(w);
        }
    }

    /**
     * Add segments to the appropriate cells
     * @param w The way to add segments from
     */
    private void addWaterWaySegments(Way w) {
        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            final WaySegment es1 = new WaySegment(w, i);
            final Node first = es1.getFirstNode();
            final Node second = es1.getSecondNode();

            if (first.isLatLonKnown() && second.isLatLonKnown()) {
                CrossingWays.getSegments(this.cellSegmentsWater, first, second).forEach(list -> list.add(es1));
            }
        }
    }

    @Override
    public void visit(Relation r) {
        if (r.isMultipolygon() && isPowerStation(r)) {
            powerStations.add(r);
        } else if (concernsWaterArea(r)) {
            r.getMemberPrimitives(Way.class).forEach(this::addWaterWaySegments);
        }
    }

    @Override
    public void startTest(ProgressMonitor progressMonitor) {
        super.startTest(progressMonitor);
        hillyCompensation = Config.getPref().getDouble(PREFIX + ".hilly_compensation", 0.2);
        hillyThreshold = Config.getPref().getDouble(PREFIX + ".hilly_threshold", 4.0);
    }

    @Override
    public void endTest() {
        // Do the actual checks
        for (Way w : this.foundPowerLines) {
            powerlineChecks(w);
        }
        // Then return the errors
        for (Node n : missingTags) {
            if (!isInPowerStation(n)) {
                errors.add(TestError.builder(this, Severity.WARNING, POWER_SUPPORT)
                        // the "missing tag" grouping can become broken if the MapCSS message get reworded
                        .message(tr("missing tag"), tr("node without power=*"))
                        .primitives(n)
                        .build());
            }
        }

        for (Node n : badConnections) {
            errors.add(TestError.builder(this, Severity.WARNING, POWER_CONNECTION)
                    .message(tr("Node connects a power line or cable with an object "
                            + "which is not related to the power infrastructure"))
                    .primitives(n)
                    .build());
        }

        for (WaySegment s : missingNodes) {
            errors.add(TestError.builder(this, Severity.WARNING, POWER_SEGMENT_LENGTH)
                    .message(tr("Possibly missing line support node within power line"))
                    .primitives(s.getFirstNode(), s.getSecondNode())
                    .highlightWaySegments(new HashSet<>(Collections.singleton(s)))
                    .build());
        }

        for (OsmPrimitive p : refDiscontinuities) {
            if (p instanceof Way)
                errors.add(TestError.builder(this, Severity.WARNING, POWER_WAY_REF_CONTINUITY)
                        .message(tr("Mixed reference numbering"))
                        .primitives(p)
                        .build());
        }

        final String discontinuityMsg = tr("Reference numbering don''t match majority of way''s nodes");

        for (OsmPrimitive p : refDiscontinuities) {
            if (p instanceof Node)
                errors.add(TestError.builder(this, Severity.WARNING, POWER_LOCAL_REF_CONTINUITY)
                        .message(discontinuityMsg)
                        .primitives(p)
                        .build());
        }

        for (Set<Node> nodes : segmentRefDiscontinuities) {
            errors.add(TestError.builder(this, Severity.WARNING, POWER_LOCAL_REF_CONTINUITY)
                    .message(discontinuityMsg)
                    .primitives(nodes)
                    .build());
        }

        for (Way w : wrongLineType) {
            errors.add(TestError.builder(this, Severity.WARNING, POWER_LINE_TYPE)
                    .message(tr("Possibly wrong power line type used"))
                    .primitives(w)
                    .build());
        }

        super.endTest();
    }

    /**
     * The base powerline checks
     * @param w The powerline to check
     */
    private void powerlineChecks(Way w) {
        final int segmentCount = w.getNodesCount() - 1;
        final double mean = w.getLength() / segmentCount;
        final double stdDev = Utils.getStandardDeviation(w.getSegmentLengths(), mean);
        final boolean isContinuesAsMinorLine = isContinuesAsMinorLine(w);
        boolean isCrossingWater = false;
        int poleCount = 0;
        int towerCount = 0;
        Node prevNode = w.firstNode();

        double baseThreshold = w.hasTag(POWER, "line") ? 1.6 : 1.8;
        if (mean / stdDev < hillyThreshold) {
            //compensate for possibly hilly areas where towers can't be put anywhere
            baseThreshold += hillyCompensation;
        }

        for (Node n : w.getNodes()) {

            /// handle power station line connections (e.g. power=line + line=*)
            if (isConnectedToStationLine(n, w) || n.hasTag(POWER, "connection")) {
                prevNode = n;
                continue;   // skip, it would be false positive
            }

            /// handle missing power line support tags (e.g. tower)
            if (!isPowerTower(n) && !isPowerInfrastructure(n) && IN_DOWNLOADED_AREA.test(n)
                    && (!w.isFirstLastNode(n) || !isPowerStation(n)))
                missingTags.add(n);

            /// handle missing nodes
            double segmentLen = n.greatCircleDistance(prevNode);
            final Set<Way> crossingWaterWays = new HashSet<>(8);
            final Set<ILatLon> crossingPositions = new HashSet<>(8);
            findCrossings(this.cellSegmentsWater, w, crossingWaterWays, crossingPositions);

            if (!crossingWaterWays.isEmpty()) {
                double compensation = calculateIntersectingLen(prevNode, crossingPositions);
                segmentLen -= compensation;
            }

            if (segmentCount > 4
                    && segmentLen > mean * baseThreshold
                    && !isPowerInfrastructure(n)
                    && IN_DOWNLOADED_AREA.test(n))
                missingNodes.add(WaySegment.forNodePair(w, prevNode, n));

            /// handle wrong line types
            if (!crossingWaterWays.isEmpty())
                isCrossingWater = true;

            if (n.hasTag(POWER, "pole"))
                poleCount++;
            else if (n.hasTag(POWER, "tower", "portal"))
                towerCount++;

            prevNode = n;
        }

        /// handle ref=* numbering discontinuities
        if (detectDiscontinuity(w, refDiscontinuities, segmentRefDiscontinuities))
            refDiscontinuities.add(w);

        /// handle wrong line types
        if (((poleCount > towerCount && w.hasTag(POWER, "line"))
                || (poleCount < towerCount && w.hasTag(POWER, MINOR_LINE)
                && !isCrossingWater
                && !isContinuesAsMinorLine))
                && IN_DOWNLOADED_AREA.test(w))
            wrongLineType.add(w);

    }

    /**
     * The summarized length (in metres) of a way where a power line hangs over a water area.
     * @param ref Reference point
     * @param crossingNodes Crossing nodes, unordered
     * @return The summarized length (in metres) of a way where a power line hangs over a water area
     */
    private static double calculateIntersectingLen(Node ref, Set<ILatLon> crossingNodes) {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;

        for (ILatLon coor : crossingNodes) {

            if (ref != null && coor != null) {
                double dist = ref.greatCircleDistance(coor);

                if (dist < min)
                    min = dist;
                if (dist > max)
                    max = dist;
            }
        }
        return max - min;
    }

    /**
     * Searches for way intersections, which intersect the {@code pair} attribute.
     * @param ways collection of ways to search for crossings
     * @param parent parent powerline way to find crossings for
     * @param crossingWays found crossing ways
     * @param crossingPositions collection of the crossing positions
     * @implNote Inspired by {@code utilsplugin2/selection/NodeWayUtils.java#addWaysIntersectingWay()}
     */
    private static void findCrossings(Map<Point2D, List<WaySegment>> ways, Way parent, Set<Way> crossingWays,
                                      Set<ILatLon> crossingPositions) {
        int nodesSize = parent.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            final WaySegment es1 = new WaySegment(parent, i);
            if (!es1.getFirstNode().isLatLonKnown() || !es1.getSecondNode().isLatLonKnown()) {
                Logging.warn("PowerLines crossing ways test section skipped " + es1);
                continue;
            }
            for (List<WaySegment> segments : CrossingWays.getSegments(ways, es1.getFirstNode(), es1.getSecondNode())) {
                for (WaySegment segment : segments) {
                    if (es1.intersects(segment)) {
                        final ILatLon ll = Geometry.getSegmentSegmentIntersection(es1.getFirstNode(), es1.getSecondNode(),
                                segment.getFirstNode(), segment.getSecondNode());
                        if (ll != null) {
                            crossingWays.add(es1.getWay());
                            crossingPositions.add(ll);
                        }
                    }
                }
            }
        }
    }

    /** Power line support features ref=* numbering direction. */
    private enum NumberingDirection {
        /** No direction */
        NONE,
        /** Numbering follows way direction */
        SAME,
        /** Numbering goes opposite way direction */
        OPPOSITE
    }

    /** Helper class for reference numbering test. Used for storing continuous reference segment info. */
    private static class SegmentInfo {
        /** Node index, follows way direction */
        private final int startIndex;
        /** ref=* value at {@link SegmentInfo#startIndex} */
        private final int startRef;
        /** Segment length */
        private final int length;
        /** Segment direction */
        private final NumberingDirection direction;

        SegmentInfo(int startIndex, int length, int ref, NumberingDirection direction) {
            this.startIndex = startIndex;
            this.length = length;
            this.direction = direction;

            if (direction == NumberingDirection.SAME)
                this.startRef = ref - length;
            else
                this.startRef = ref + length;

            if (length == 0 && direction != NumberingDirection.NONE) {
                throw new IllegalArgumentException("When the segment length is zero, the direction should be NONE");
            }
        }

        @Override
        public String toString() {
            return String.format("SegmentInfo{startIndex=%d, startRef=%d, length=%d, direction=%s}",
                    startIndex, startRef, length, direction);
        }
    }

    /**
     * Detects ref=* numbering discontinuities in the given way.
     * @param way checked way
     * @param nRefDiscontinuities single node ref=* discontinuities
     * @param sRefDiscontinuities continuous node ref=* discontinuities
     * @return {@code true} if warning needs to be issued for the whole way
     */
    static boolean detectDiscontinuity(Way way, Set<OsmPrimitive> nRefDiscontinuities, List<Set<Node>> sRefDiscontinuities) {
        final RefChecker checker = new RefChecker(way);
        final List<SegmentInfo> segments = checker.getSegments();
        final SegmentInfo referenceSegment = checker.getLongestSegment();

        if (referenceSegment == null)
            return !segments.isEmpty();

        // collect disconnected ref segments which are not align up to the reference
        for (SegmentInfo segment : segments) {
            if (!isSegmentAlign(referenceSegment, segment)) {
                if (referenceSegment.length == 0)
                    return true;

                if (segment.length == 0) {
                    nRefDiscontinuities.add(way.getNode(segment.startIndex));
                } else {
                    Set<Node> nodeGroup = new HashSet<>();

                    for (int i = segment.startIndex; i <= segment.startIndex + segment.length; i++) {
                        nodeGroup.add(way.getNode(i));
                    }
                    sRefDiscontinuities.add(nodeGroup);
                }
            }
        }

        return false;
    }

    /**
     * Checks if parameter segments align. The {@code reference} is expected to be at least as long as the {@code candidate}.
     * @param reference Reference segment to check against
     * @param candidate Candidate segment
     * @return {@code true} if the two segments ref=* numbering align
     */
    private static boolean isSegmentAlign(SegmentInfo reference, SegmentInfo candidate) {
        if (reference.direction == NumberingDirection.NONE
                || reference.direction == candidate.direction
                || candidate.direction == NumberingDirection.NONE)
            return Math.abs(candidate.startIndex - reference.startIndex) == Math.abs(candidate.startRef - reference.startRef);
        return false;
    }

    /**
     * Detects continuous reference numbering sequences. Ignores the first and last node because
     * ways can be connected, and the connection nodes can have different numbering.
     * <p>
     * If the numbering switches in the middle of the way, this can also be seen as error,
     * because line relations would require split ways.
     */
    static class RefChecker {
        private final List<SegmentInfo> segments = new ArrayList<>();
        private NumberingDirection direction = NumberingDirection.NONE;
        private Integer startIndex;
        private Integer previousRef;

        RefChecker(final Way way) {
            run(way);
        }

        private void run(Way way) {
            final int wayLength = way.getNodesCount();

            // first and last node skipped
            for (int i = 1; i < wayLength - 1; i++) {
                Node n = way.getNode(i);
                if (!isPowerTower(n)) {
                    continue;
                }
                maintain(parseRef(n.get("ref")), i);
            }

            // needed for creation of the last segment
            maintain(null, wayLength - 1);
        }

        /**
         * Maintains class variables and constructs a new segment when necessary.
         * @param ref   recognised ref=* number
         * @param index node index in a {@link Way}
         */
        private void maintain(Integer ref, int index) {
            if (previousRef == null && ref != null) {
                // ref change: null -> number
                startIndex = index;
            } else if (previousRef != null && ref == null) {
                // ref change: number -> null
                segments.add(new SegmentInfo(startIndex, index - 1 - startIndex, previousRef, direction));
                direction = NumberingDirection.NONE;    // to fix directionality
            } else if (previousRef != null) {
                // ref change: number -> number
                if (Math.abs(ref - previousRef) != 1) {
                    segments.add(new SegmentInfo(startIndex, index - 1 - startIndex, previousRef, direction));
                    startIndex = index;
                    previousRef = ref;                  // to fix directionality
                }
                direction = detectDirection(ref, previousRef);
            }
            previousRef = ref;
        }

        /**
         * Parses integer tag values. Later can be relatively easily extended or rewritten to handle
         * complex references like 25/A, 25/B etc.
         * @param value the value to be parsed
         * @return parsed int or {@code null} in case of {@link NumberFormatException}
         */
        private static Integer parseRef(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignore) {
                Logging.trace("The " + RefChecker.class + " couldn't parse ref=" + value + ", consider rewriting the parser");
                return null;
            }
        }

        /**
         * Detects numbering direction. The parameters should follow way direction.
         * @param ref         last known reference value
         * @param previousRef reference value before {@code ref}
         * @return recognised direction
         */
        private static NumberingDirection detectDirection(int ref, int previousRef) {
            if (ref > previousRef)
                return NumberingDirection.SAME;
            else if (ref < previousRef)
                return NumberingDirection.OPPOSITE;
            return NumberingDirection.NONE;
        }

        /**
         * Calculates the longest segment.
         * @return the longest segment, or the lowest index if there are more than one with same length and direction,
         * or {@code null} if there are more than one with same length and different direction
         */
        SegmentInfo getLongestSegment() {
            final Set<NumberingDirection> directions = EnumSet.noneOf(NumberingDirection.class);
            int longestLength = -1;
            int counter = 0;
            SegmentInfo longest = null;

            for (SegmentInfo segment : segments) {
                if (segment.length > longestLength) {
                    longestLength = segment.length;
                    longest = segment;
                    counter = 0;
                    directions.clear();
                    directions.add(segment.direction);
                } else if (segment.length == longestLength) {
                    counter++;
                    directions.add(segment.direction);
                }
            }

            // there are multiple segments with the same longest length and their directions don't match
            if (counter > 0 && directions.size() > 1)
                return null;

            return longest;
        }

        /**
         * @return the detected segments
         */
        List<SegmentInfo> getSegments() {
            return segments;
        }
    }

    private static boolean isRelatedToPower(Way way) {
        if (way.hasTag(POWER) || way.hasTag(BUILDING))
            return true;
        for (OsmPrimitive ref : way.getReferrers()) {
            if (ref instanceof Relation && ref.isMultipolygon() && (ref.hasTag(POWER) || ref.hasTag(BUILDING))) {
                for (RelationMember rm : ((Relation) ref).getMembers()) {
                    if (way == rm.getMember())
                        return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the current node connected to a line which usually used inside power stations.
     * @param n node to check
     * @param w parent way of {@code n}
     * @return {@code true} if {@code n} connected to power=line + line=*
     */
    private static boolean isConnectedToStationLine(Node n, Way w) {
        for (OsmPrimitive p : n.getReferrers()) {
            if (p instanceof Way && !p.equals(w) && isPowerLine((Way) p) && p.hasKey("line"))
                return true;
        }
        return false;
    }

    /**
     * Checks if the way continues as a power=minor_line.
     * @param way Way to be checked
     * @return {@code true} if the way continues as a power=minor_line
     */
    private static boolean isContinuesAsMinorLine(Way way) {
        return way.firstNode().referrers(Way.class).filter(referrer -> !way.equals(referrer)).anyMatch(PowerLines::isMinorLine) ||
                way.lastNode().referrers(Way.class).filter(referrer -> !way.equals(referrer)).anyMatch(PowerLines::isMinorLine);
    }

    /**
     * Checks if the given primitive denotes a power=minor_line.
     * @param p primitive to be checked
     * @return {@code true} if the given primitive denotes a power=minor_line
     */
    private static boolean isMinorLine(OsmPrimitive p) {
        return p.hasTag(POWER, MINOR_LINE);
    }

    /**
     * Check if primitive has a tag that marks it as a water area or boundary of a water area.
     * @param p the primitive
     * @return {@code true} if primitive has a tag that marks it as a water area or boundary of a water area
     */
    private static boolean concernsWaterArea(OsmPrimitive p) {
        return p.hasTag("water", "river", "lake") || p.hasKey("waterway") || p.hasTag("natural", "coastline");
    }

    /**
     * Checks if the given node is inside a power station.
     * @param n Node to be checked
     * @return true if the given node is inside a power station
     */
    protected final boolean isInPowerStation(Node n) {
        for (OsmPrimitive station : powerStations) {
            List<List<Node>> nodesLists = new ArrayList<>();
            if (station instanceof Way) {
                nodesLists.add(((Way) station).getNodes());
            } else if (station instanceof Relation) {
                Multipolygon polygon = MultipolygonCache.getInstance().get((Relation) station);
                if (polygon != null) {
                    for (JoinedWay outer : Multipolygon.joinWays(polygon.getOuterWays())) {
                        nodesLists.add(outer.getNodes());
                    }
                }
            }
            for (List<Node> nodes : nodesLists) {
                if (Geometry.nodeInsidePolygon(n, nodes)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determines if the specified way denotes a power line.
     * @param w The way to be tested
     * @return {@code true} if power key is set and equal to line/minor_line
     */
    protected static boolean isPowerLine(Way w) {
        return isPowerIn(w, POWER_LINE_TAGS);
    }

    /**
     * Determines if the specified primitive denotes a power station.
     * @param p The primitive to be tested
     * @return {@code true} if power key is set and equal to generator/substation/plant
     */
    protected static boolean isPowerStation(OsmPrimitive p) {
        return isPowerIn(p, POWER_STATION_TAGS) || isBuildingIn(p, BUILDING_STATION_TAGS);
    }

    /**
     * Determines if the specified node denotes a power support feature.
     * @param n The node to be tested
     * @return {@code true} if power key is set and equal to pole/tower/portal/catenary_mast
     */
    protected static boolean isPowerTower(Node n) {
        return isPowerIn(n, POWER_TOWER_TAGS);
    }

    /**
     * Determines if the specified node denotes a power infrastructure allowed on a power line.
     * @param n The node to be tested
     * @return {@code true} if power key is set and equal to compensator/converter/generator/insulator
     * /switch/switchgear/terminal/transformer
     */
    protected static boolean isPowerInfrastructure(Node n) {
        return isPowerIn(n, POWER_INFRASTRUCTURE_TAGS);
    }

    /**
     * Helper function to check if power tag is a certain value.
     * @param p The primitive to be tested
     * @param values List of possible values
     * @return {@code true} if power key is set and equal to possible values
     */
    private static boolean isPowerIn(OsmPrimitive p, Collection<String> values) {
        return p.hasTag(POWER, values);
    }

    /**
     * Helper function to check if building tag is a certain value.
     * @param p The primitive to be tested
     * @param values List of possible values
     * @return {@code true} if power key is set and equal to possible values
     */
    private static boolean isBuildingIn(OsmPrimitive p, Collection<String> values) {
        return p.hasTag(BUILDING, values);
    }

    @Override
    public void clear() {
        super.clear();
        badConnections.clear();
        cellSegmentsWater.clear();
        foundPowerLines.clear();
        missingNodes.clear();
        missingTags.clear();
        powerStations.clear();
        refDiscontinuities.clear();
        segmentRefDiscontinuities.clear();
        wrongLineType.clear();
    }
}
