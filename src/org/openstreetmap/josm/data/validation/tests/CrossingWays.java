// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
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
import org.openstreetmap.josm.tools.Logging;

/**
 * Tests if there are segments that crosses in the same layer
 *
 * @author frsantos
 */
public abstract class CrossingWays extends Test {

    static final String HIGHWAY = "highway";
    static final String RAILWAY = "railway";
    static final String WATERWAY = "waterway";
    static final String LANDUSE = "landuse";

    private static final class MessageHelper {
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
        BUILDING, HIGHWAY, RAILWAY, RESIDENTIAL_AREA, WATERWAY, WAY;

        static WayType of(Way w) {
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
                    || isResidentialArea(w));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            if (w1 == w2)
                return false;
            if (!Objects.equals(OsmUtils.getLayer(w1), OsmUtils.getLayer(w2))) {
                return true;
            }
            if (w1.hasKey(HIGHWAY) && w2.hasKey(HIGHWAY) && !Objects.equals(w1.get("level"), w2.get("level"))) {
                return true;
            }
            if ((w1.hasKey(HIGHWAY, RAILWAY, WATERWAY) && isResidentialArea(w2))
             || (w2.hasKey(HIGHWAY, RAILWAY, WATERWAY) && isResidentialArea(w1)))
                return true;
            if (isSubwayOrTramOrRazed(w2)) {
                return true;
            }
            if (isCoastline(w1) != isCoastline(w2)) {
                return true;
            }
            if ((w1.hasTag(WATERWAY, "river", "stream", "canal", "drain", "ditch") && w2.hasTag(WATERWAY, "riverbank"))
             || (w2.hasTag(WATERWAY, "river", "stream", "canal", "drain", "ditch") && w1.hasTag(WATERWAY, "riverbank"))) {
                return true;
            }
            return isProposedOrAbandoned(w2);
        }

        @Override
        MessageHelper createMessage(Way w1, Way w2) {
            WayType[] types = {WayType.of(w1), WayType.of(w2)};
            Arrays.sort(types);

            if (types[0] == types[1]) {
                switch(types[0]) {
                    case BUILDING:
                        return new MessageHelper(tr("Crossing buildings"), 610);
                    case HIGHWAY:
                        return new MessageHelper(tr("Crossing highways"), 620);
                    case RAILWAY:
                        return new MessageHelper(tr("Crossing railways"), 630);
                    case RESIDENTIAL_AREA:
                        return new MessageHelper(tr("Crossing residential areas"), 640);
                    case WATERWAY:
                        return new MessageHelper(tr("Crossing waterways"), 650);
                    case WAY:
                    default:
                        return new MessageHelper(tr("Crossing ways"), CROSSING_WAYS);
                }
            } else {
                switch (types[0]) {
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
                        switch (types[1]) {
                            case WAY:
                            default:
                                return new MessageHelper(tr("Crossing residential area/way"), 641);
                        }
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
            return super.isPrimitiveUsable(p) && p.hasKey("boundary")
                    && (!(p instanceof Relation) || (((Relation) p).isMultipolygon() && !((Relation) p).hasIncompleteMembers()));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return !Objects.equals(w1.get("boundary"), w2.get("boundary"));
        }

        @Override
        public void visit(Relation r) {
            for (Way w : r.getMemberPrimitives(Way.class)) {
                visit(w);
            }
        }
    }

    /**
     * Crossing barriers ways test.
     */
    public static class Barrier extends CrossingWays {

        protected static final int CROSSING_BARRIERS = 603;

        /**
         * Constructs a new crossing {@code Barrier} test.
         */
        public Barrier() {
            super(tr("Crossing barriers"), CROSSING_BARRIERS);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("barrier");
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return !Objects.equals(OsmUtils.getLayer(w1), OsmUtils.getLayer(w2));
        }

    }

    /**
     * Self crossing ways test (for all the rest)
     */
    public static class SelfCrossing extends CrossingWays {

        protected static final int CROSSING_SELF = 604;

        CrossingWays.Ways normalTest = new Ways();
        CrossingWays.Barrier barrierTest = new Barrier();
        CrossingWays.Boundaries boundariesTest = new Boundaries();

        /**
         * Constructs a new SelfIntersection test.
         */
        public SelfCrossing() {
            super(tr("Self crossing ways"), CROSSING_SELF);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && !(normalTest.isPrimitiveUsable(p) || barrierTest.isPrimitiveUsable(p)
                    || boundariesTest.isPrimitiveUsable(p));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return w1 != w2; // should not happen
        }
    }

    /**
     * Constructs a new {@code CrossingWays} test.
     * @param title The test title
     * @param code The test code
     * @since 12958
     */
    public CrossingWays(String title, int code) {
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
        super.endTest();
        cellSegments.clear();
        seenWays.clear();
    }

    static boolean isCoastline(OsmPrimitive w) {
        return w.hasTag("natural", "water", "coastline") || w.hasTag(LANDUSE, "reservoir");
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
        if (this instanceof SelfCrossing) {
            // free memory, we are not interested in previous ways
            cellSegments.clear();
            seenWays.clear();
        }

        int nodesSize = w.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            final WaySegment es1 = new WaySegment(w, i);
            final EastNorth en1 = es1.getFirstNode().getEastNorth();
            final EastNorth en2 = es1.getSecondNode().getEastNorth();
            if (en1 == null || en2 == null) {
                Logging.warn("Crossing ways test skipped " + es1);
                continue;
            }
            for (List<WaySegment> segments : getSegments(cellSegments, en1, en2)) {
                for (WaySegment es2 : segments) {
                    List<Way> prims;
                    List<WaySegment> highlight;

                    if (!es1.intersects(es2) || ignoreWaySegmentCombination(es1.way, es2.way)) {
                        continue;
                    }

                    prims = new ArrayList<>();
                    prims.add(es1.way);
                    if (es1.way != es2.way)
                        prims.add(es2.way);
                    if ((highlight = seenWays.get(prims)) == null) {
                        highlight = new ArrayList<>();
                        highlight.add(es1);
                        highlight.add(es2);

                        final MessageHelper message = createMessage(es1.way, es2.way);
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

    /**
     * Returns all the cells this segment crosses.  Each cell contains the list
     * of segments already processed
     * @param cellSegments map with already collected way segments
     * @param n1 The first EastNorth
     * @param n2 The second EastNorth
     * @return A list with all the cells the segment crosses
     */
    public static List<List<WaySegment>> getSegments(Map<Point2D, List<WaySegment>> cellSegments, EastNorth n1, EastNorth n2) {
        List<List<WaySegment>> cells = new ArrayList<>();
        for (Point2D cell : ValUtil.getSegmentCells(n1, n2, OsmValidator.getGridDetail())) {
            cells.add(cellSegments.computeIfAbsent(cell, k -> new ArrayList<>()));
        }
        return cells;
    }
}
