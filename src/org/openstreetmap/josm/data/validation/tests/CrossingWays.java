// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.ValUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests if there are segments that crosses in the same layer/level
 *
 * @author frsantos
 */
public abstract class CrossingWays extends Test {

    static final String BARRIER = "barrier";
    static final String HIGHWAY = "highway";
    static final String RAILWAY = "railway";
    static final String WATERWAY = "waterway";
    static final String LANDUSE = "landuse";

    static final class MessageHelper {
        final String message;
        final int code;

        MessageHelper(String message, int code) {
            this.message = message;
            this.code = code;
        }
    }

    /**
     * Type of way. Entries have to be declared in alphabetical order, see sort below.
     */
    private enum WayType {
        BARRIER, BUILDING, HIGHWAY, RAILWAY, RESIDENTIAL_AREA, WATERWAY, WAY;

        static WayType of(Way w) {
            if (w.hasKey(CrossingWays.BARRIER))
                return BARRIER;
            if (isBuilding(w))
                return BUILDING;
            else if (w.hasKey(CrossingWays.HIGHWAY))
                return HIGHWAY;
            else if (isRailway(w))
                return RAILWAY;
            else if (isResidentialArea(w))
                return RESIDENTIAL_AREA;
            else if (w.hasKey(CrossingWays.WATERWAY))
                return WATERWAY;
            else
                return WAY;
        }
    }

    /** All way segments, grouped by cells */
    private final Map<Point2D, List<WaySegment>> cellSegments = new HashMap<>(1000);
    /** The already detected ways in error */
    private final Map<List<Way>, List<WaySegment>> seenWays = new HashMap<>(50);
    private final Set<Way> waysToTest = new HashSet<>();

    protected final int code;

    /**
     * General crossing ways test.
     */
    public static class Ways extends CrossingWays {

        protected static final int CROSSING_WAYS = 601;

        /**
         * Constructs a new crossing {@code Ways} test.
         */
        public Ways() {
            super(tr("Crossing ways"), CROSSING_WAYS);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive w) {
            return super.isPrimitiveUsable(w)
                    && !isProposedOrAbandoned(w)
                    && (isHighway(w)
                    || w.hasKey(WATERWAY)
                    || isRailway(w)
                    || isCoastline(w)
                    || isBuilding(w)
                    || w.hasKey(BARRIER)
                    || isResidentialArea(w));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            if (w1 == w2)
                return true;
            if (areLayerOrLevelDifferent(w1, w2))
                return true;
            if (isBuilding(w1) && isBuilding(w2))
                return true; // handled by mapcss tests
            if (((isResidentialArea(w1) || w1.hasKey(BARRIER, HIGHWAY, RAILWAY, WATERWAY) || isWaterArea(w1))
                    && isResidentialArea(w2))
                    || ((isResidentialArea(w2) || w2.hasKey(BARRIER, HIGHWAY, RAILWAY, WATERWAY) || isWaterArea(w2))
                            && isResidentialArea(w1)))
                return true;
            if (isWaterArea(w1) && isWaterArea(w2))
                return true; // handled by mapcss tests
            if (w1.hasKey(RAILWAY) && w2.hasKey(RAILWAY) && (w1.hasTag(RAILWAY, "yard") != w2.hasTag(RAILWAY, "yard")
                    || w1.hasTag(RAILWAY, "halt") != w2.hasTag(RAILWAY, "halt")))
                return true;  // see #20089, #21541
            return (w1.hasTag(WATERWAY, "river", "stream", "canal", "drain", "ditch") && isWaterArea(w2))
                    || (w2.hasTag(WATERWAY, "river", "stream", "canal", "drain", "ditch") && isWaterArea(w1));
        }

        @Override
        MessageHelper createMessage(Way w1, Way w2) {
            WayType[] types = {WayType.of(w1), WayType.of(w2)};
            Arrays.sort(types);

            if (types[0] == types[1]) {
                switch (types[0]) {
                // 610 and 640 where removed for #16707
                case BARRIER:
                    return new MessageHelper(tr("Crossing barriers"), 603);
                case HIGHWAY:
                    return new MessageHelper(tr("Crossing highways"), 620);
                case RAILWAY:
                    return new MessageHelper(tr("Crossing railways"), 630);
                case WATERWAY:
                    return new MessageHelper(tr("Crossing waterways"), 650);
                case WAY:
                default:
                    return new MessageHelper(tr("Crossing ways"), CROSSING_WAYS);
                }
            } else {
                switch (types[0]) {
                case BARRIER:
                    switch (types[1]) {
                    case BUILDING:
                        return new MessageHelper(tr("Crossing barrier/building"), 661);
                    case HIGHWAY:
                        return new MessageHelper(tr("Crossing barrier/highway"), 662);
                    case RAILWAY:
                        return new MessageHelper(tr("Crossing barrier/railway"), 663);
                    case WATERWAY:
                        return new MessageHelper(tr("Crossing barrier/waterway"), 664);
                    case WAY:
                    default:
                        return new MessageHelper(tr("Crossing barrier/way"), 665);
                    }
                case BUILDING:
                    switch (types[1]) {
                    case HIGHWAY:
                        return new MessageHelper(tr("Crossing building/highway"), 612);
                    case RAILWAY:
                        return new MessageHelper(tr("Crossing building/railway"), 613);
                    case RESIDENTIAL_AREA:
                        return new MessageHelper(tr("Crossing building/residential area"), 614);
                    case WATERWAY:
                        return new MessageHelper(tr("Crossing building/waterway"), 615);
                    case WAY:
                    default:
                        return new MessageHelper(tr("Crossing building/way"), 611);
                    }
                case HIGHWAY:
                    switch (types[1]) {
                    case RAILWAY:
                        return new MessageHelper(tr("Crossing highway/railway"), 622);
                    case WATERWAY:
                        return new MessageHelper(tr("Crossing highway/waterway"), 623);
                    case WAY:
                    default:
                        return new MessageHelper(tr("Crossing highway/way"), 621);
                    }
                case RAILWAY:
                    switch (types[1]) {
                    case WATERWAY:
                        return new MessageHelper(tr("Crossing railway/waterway"), 632);
                    case WAY:
                    default:
                        return new MessageHelper(tr("Crossing railway/way"), 631);
                    }
                case RESIDENTIAL_AREA:
                    return new MessageHelper(tr("Crossing residential area/way"), 641);
                case WATERWAY:
                default:
                    return new MessageHelper(tr("Crossing waterway/way"), 651);
                }
            }
        }
    }

    /**
     * Crossing boundaries ways test.
     */
    public static class Boundaries extends CrossingWays {

        protected static final int CROSSING_BOUNDARIES = 602;

        /**
         * Constructs a new crossing {@code Boundaries} test.
         */
        public Boundaries() {
            super(tr("Crossing boundaries"), CROSSING_BOUNDARIES);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("boundary") && !p.hasTag("boundary", "protected_area")
                    && (!(p instanceof Relation) || p.isMultipolygon());
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            // ignore ways which have no common boundary tag value
            Set<String> s1 = getBoundaryTags(w1);
            Set<String> s2 = getBoundaryTags(w2);
            return s1.stream().noneMatch(s2::contains);
        }

        /**
         * Collect all boundary tag values of the way and its parent relations
         * @param w the way to check
         * @return set with the found boundary tag values
         */
        private static Set<String> getBoundaryTags(Way w) {
            final Set<String> types = new HashSet<>();
            String type = w.get("boundary");
            if (type != null)
                types.add(type);
            w.referrers(Relation.class).filter(Relation::isMultipolygon).map(r -> r.get("boundary"))
                    .filter(Objects::nonNull).forEach(types::add);
            types.remove("protected_area");
            return types;
        }

        @Override
        public void visit(Relation r) {
            for (Way w : r.getMemberPrimitives(Way.class)) {
                if (!w.isIncomplete())
                    visit(w);
            }
        }
    }

    /**
     * Self crossing ways test (for all ways)
     */
    public static class SelfCrossing extends CrossingWays {

        protected static final int CROSSING_SELF = 604;

        /**
         * Constructs a new SelfIntersection test.
         */
        public SelfCrossing() {
            super(tr("Self crossing ways"), CROSSING_SELF);
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return false; // we should not get here
        }
    }

    /**
     * Constructs a new {@code CrossingWays} test.
     * @param title The test title
     * @param code The test code
     * @since 12958
     */
    protected CrossingWays(String title, int code) {
        super(title, tr("This test checks if two roads, railways, waterways or buildings crosses in the same layer, " +
                "but are not connected by a node."));
        this.code = code;
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        cellSegments.clear();
        seenWays.clear();
    }

    @Override
    public void endTest() {
        runTest();
        // free storage
        cellSegments.clear();
        seenWays.clear();
        if (partialSelection)
            removeIrrelevantErrors(waysToTest);
        waysToTest.clear();
        super.endTest();
    }

    protected void runTest() {
        final Collection<Way> selection;
        if (this instanceof SelfCrossing || !partialSelection) {
            selection = waysToTest;
        } else {
            selection = addNearbyObjects();
        }
        for (Way w : selection) {
            testWay(w);
        }

    }

    private Collection<Way> addNearbyObjects() {
        final Collection<Way> selection = new HashSet<>();
        DataSet ds = OsmDataManager.getInstance().getActiveDataSet();
        if (ds != null) {
            for (Way wt : waysToTest) {
                selection.addAll(ds.searchWays(wt.getBBox()).stream()
                        .filter(w -> !w.isDeleted() && isPrimitiveUsable(w)).collect(Collectors.toList()));
                if (this instanceof CrossingWays.Boundaries) {
                    List<Relation> relations = ds.searchRelations(wt.getBBox()).stream()
                            .filter(p -> isPrimitiveUsable(p)).collect(Collectors.toList());
                    for (Relation r: relations) {
                        for (Way w : r.getMemberPrimitives(Way.class)) {
                            if (!w.isIncomplete())
                                selection.add(w);
                        }
                    }
                }
            }
        }
        return selection;
    }
    static boolean isCoastline(OsmPrimitive w) {
        return w.hasTag("natural", "water", "coastline") || w.hasTag(LANDUSE, "reservoir");
    }

    static boolean isWaterArea(OsmPrimitive w) {
        return w.hasTag("natural", "water") || w.hasTag("waterway", "riverbank") || w.hasTag(LANDUSE, "reservoir");
    }

    static boolean isHighway(OsmPrimitive w) {
        return w.hasTagDifferent(HIGHWAY, "rest_area", "services", "bus_stop", "platform");
    }

    static boolean isRailway(OsmPrimitive w) {
        return w.hasKey(RAILWAY) && !isSubwayOrTramOrRazed(w);
    }

    static boolean isSubwayOrTramOrRazed(OsmPrimitive w) {
        return w.hasTag(RAILWAY, "subway", "tram", "razed") ||
              (w.hasTag(RAILWAY, "construction") && w.hasTag("construction", "tram")) ||
              (w.hasTag(RAILWAY, "disused") && w.hasTag("disused", "tram"));
    }

    static boolean isProposedOrAbandoned(OsmPrimitive w) {
        return w.hasTag(HIGHWAY, "proposed") || w.hasTag(RAILWAY, "proposed", "abandoned");
    }

    abstract boolean ignoreWaySegmentCombination(Way w1, Way w2);

    MessageHelper createMessage(Way w1, Way w2) {
        return new MessageHelper(this.name, this.code);
    }

    @Override
    public void visit(Way w) {
        waysToTest.add(w);
    }

    private void testWay(Way w) {
        boolean findSelfCrossingOnly = this instanceof SelfCrossing;
        if (findSelfCrossingOnly) {
            // free memory, we are not interested in previous ways
            cellSegments.clear();
            seenWays.clear();
        }

        int nodesSize = w.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            final WaySegment es1 = new WaySegment(w, i);
            if (!es1.getFirstNode().isLatLonKnown() || !es1.getSecondNode().isLatLonKnown()) {
                Logging.warn("Crossing ways test skipped " + es1);
                continue;
            }
            for (List<WaySegment> segments : getSegments(cellSegments, es1.getFirstNode(), es1.getSecondNode())) {
                for (WaySegment es2 : segments) {
                    List<Way> prims;
                    List<WaySegment> highlight;

                    if (!es1.intersects(es2)
                            || (!findSelfCrossingOnly && ignoreWaySegmentCombination(es1.getWay(), es2.getWay()))) {
                        continue;
                    }

                    prims = new ArrayList<>();
                    prims.add(es1.getWay());
                    if (es1.getWay() != es2.getWay())
                        prims.add(es2.getWay());
                    if ((highlight = seenWays.get(prims)) == null) {
                        highlight = new ArrayList<>();
                        highlight.add(es1);
                        highlight.add(es2);

                        final MessageHelper message = createMessage(es1.getWay(), es2.getWay());
                        errors.add(TestError.builder(this, Severity.WARNING, message.code)
                                .message(message.message)
                                .primitives(prims)
                                .highlightWaySegments(highlight)
                                .build());
                        seenWays.put(prims, highlight);
                    } else {
                        highlight.add(es1);
                        highlight.add(es2);
                    }
                }
                segments.add(es1);
            }
        }
    }

    private static boolean areLayerOrLevelDifferent(Way w1, Way w2) {
        return !Objects.equals(OsmUtils.getLayer(w1), OsmUtils.getLayer(w2))
            || !Objects.equals(w1.get("level"), w2.get("level"));
    }

    /**
     * Returns all the cells this segment crosses.  Each cell contains the list
     * of segments already processed
     * @param cellSegments map with already collected way segments
     * @param n1 The first EastNorth
     * @param n2 The second EastNorth
     * @return A list with all the cells the segment crosses
     */
    public static List<List<WaySegment>> getSegments(Map<Point2D, List<WaySegment>> cellSegments, EastNorth n1, EastNorth n2) {
        return ValUtil.getSegmentCells(n1, n2, OsmValidator.getGridDetail()).stream()
                .map(cell -> cellSegments.computeIfAbsent(cell, k -> new ArrayList<>()))
                .collect(Collectors.toList());
    }

    /**
     * Returns all the cells this segment crosses.  Each cell contains the list
     * of segments already processed
     * @param cellSegments map with already collected way segments
     * @param n1 The first EastNorth
     * @param n2 The second EastNorth
     * @return A list with all the cells the segment crosses
     * @since 18553
     */
    public static List<List<WaySegment>> getSegments(Map<Point2D, List<WaySegment>> cellSegments, ILatLon n1, ILatLon n2) {
        return ValUtil.getSegmentCells(n1, n2, OsmValidator.getGridDetail()).stream()
                .map(cell -> cellSegments.computeIfAbsent(cell, k -> new ArrayList<>()))
                .collect(Collectors.toList());
    }

    /**
     * Find ways which are crossing without sharing a node.
     * @param w way that is to be checked
     * @param cellSegments map with already collected way segments
     * @param crossingWays map to collect crossing ways and related segments
     * @param findSharedWaySegments true: find shared way segments instead of crossings
     */
    public static void findIntersectingWay(Way w, Map<Point2D, List<WaySegment>> cellSegments,
            Map<List<Way>, List<WaySegment>> crossingWays, boolean findSharedWaySegments) {
        int nodesSize = w.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            final WaySegment es1 = new WaySegment(w, i);
            final EastNorth en1 = es1.getFirstNode().getEastNorth();
            final EastNorth en2 = es1.getSecondNode().getEastNorth();
            if (en1 == null || en2 == null) {
                Logging.warn("Crossing ways test skipped " + es1);
                continue;
            }
            for (List<WaySegment> segments : CrossingWays.getSegments(cellSegments, en1, en2)) {
                for (WaySegment es2 : segments) {

                    List<WaySegment> highlight;
                    if (es2.getWay() == w // reported by CrossingWays.SelfIntersection
                            || (findSharedWaySegments && !es1.isSimilar(es2))
                            || (!findSharedWaySegments && !es1.intersects(es2)))
                        continue;

                    List<Way> prims = Arrays.asList(es1.getWay(), es2.getWay());
                    if ((highlight = crossingWays.get(prims)) == null) {
                        highlight = new ArrayList<>(2);
                        highlight.add(es1);
                        highlight.add(es2);
                        crossingWays.put(prims, highlight);
                    } else {
                        highlight.add(es1);
                        highlight.add(es2);
                    }
                }
                segments.add(es1);
            }
        }
    }

    /**
     * Check if the given way is self crossing
     * @param way the way to check
     * @return {@code true} if one or more segments of the way are crossing
     * @see SelfIntersectingWay
     * @since 17393
     */
    public static boolean isSelfCrossing(Way way) {
        CheckParameterUtil.ensureParameterNotNull(way, "way");
        SelfCrossing test = new SelfCrossing();
        test.visit(way);
        test.runTest();
        return !test.getErrors().isEmpty();
    }
}
