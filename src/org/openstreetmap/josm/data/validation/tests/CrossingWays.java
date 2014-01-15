// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.ValUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

/**
 * Tests if there are segments that crosses in the same layer
 *
 * @author frsantos
 */
public abstract class CrossingWays extends Test {
    protected static final int CROSSING_WAYS = 601;
    
    private static final String HIGHWAY = "highway";
    private static final String RAILWAY = "railway";
    private static final String WATERWAY = "waterway";

    /** All way segments, grouped by cells */
    private Map<Point2D,List<WaySegment>> cellSegments;
    /** The already detected errors */
    private Set<WaySegment> errorSegments;
    /** The already detected ways in error */
    private Map<List<Way>, List<WaySegment>> seenWays;

    /**
     * General crossing ways test.
     */
    public static class Ways extends CrossingWays {

        /**
         * Constructs a new crossing {@code Ways} test.
         */
        public Ways() {
            super(tr("Crossing ways"));
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive w) {
            return super.isPrimitiveUsable(w)
                    && !isProposedOrAbandoned(w)
                    && (w.hasKey(HIGHWAY)
                    || w.hasKey(WATERWAY)
                    || (w.hasKey(RAILWAY) && !isSubwayOrTram(w))
                    || isCoastline(w)
                    || isBuilding(w));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            if (!Utils.equal(getLayer(w1), getLayer(w2))) {
                return true;
            }
            if (w1.hasKey(HIGHWAY) && w2.hasKey(HIGHWAY) && !Utils.equal(w1.get("level"), w2.get("level"))) {
                return true;
            }
            if (isSubwayOrTram(w2)) {
                return true;
            }
            if (isCoastline(w1) != isCoastline(w2)) {
                return true;
            }
            if ((w1.hasTag(WATERWAY, "river") && w2.hasTag(WATERWAY, "riverbank"))
                    || (w2.hasTag(WATERWAY, "river") && w1.hasTag(WATERWAY, "riverbank"))) {
                return true;
            }
            if (isProposedOrAbandoned(w2)) {
                return true;
            }
            return false;
        }

        @Override
        String createMessage(Way w1, Way w2) {
            if (isBuilding(w1)) {
                return ("Crossing buildings");
            } else if (w1.hasKey(WATERWAY) && w2.hasKey(WATERWAY)) {
                return tr("Crossing waterways");
            } else if ((w1.hasKey(HIGHWAY) && w2.hasKey(WATERWAY))
                    || (w2.hasKey(HIGHWAY) && w1.hasKey(WATERWAY))) {
                return tr("Crossing waterway/highway");
            } else {
                return tr("Crossing ways");
            }
        }

    }

    /**
     * Crossing boundaries ways test.
     */
    public static class Boundaries extends CrossingWays {

        /**
         * Constructs a new crossing {@code Boundaries} test.
         */
        public Boundaries() {
            super(tr("Crossing boundaries"));
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("boundary")
                    && (!(p instanceof Relation) || (((Relation) p).isMultipolygon() && !((Relation) p).hasIncompleteMembers()));
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return !Utils.equal(w1.get("boundary"), w2.get("boundary"));
        }

        @Override
        String createMessage(Way w1, Way w2) {
            return tr("Crossing boundaries");
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

        /**
         * Constructs a new crossing {@code Barrier} test.
         */
        public Barrier() {
            super(tr("Crossing barriers"));
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("barrier");
        }

        @Override
        boolean ignoreWaySegmentCombination(Way w1, Way w2) {
            return false;
        }

        @Override
        String createMessage(Way w1, Way w2) {
            return tr("Crossing barriers");
        }
    }

    /**
     * Constructs a new {@code CrossingWays} test.
     * @param title The test title
     * @since 6691
     */
    public CrossingWays(String title) {
        super(title, tr("This test checks if two roads, railways, waterways or buildings crosses in the same layer, but are not connected by a node."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        cellSegments = new HashMap<Point2D,List<WaySegment>>(1000);
        errorSegments = new HashSet<WaySegment>();
        seenWays = new HashMap<List<Way>, List<WaySegment>>(50);
    }

    @Override
    public void endTest() {
        super.endTest();
        cellSegments = null;
        errorSegments = null;
        seenWays = null;
    }

    static String getLayer(OsmPrimitive w) {
        String layer1 = w.get("layer");
        if ("0".equals(layer1)) {
            layer1 = null; // 0 is default value for layer.
        }
        return layer1;
    }

    static boolean isCoastline(OsmPrimitive w) {
        return w.hasTag("natural", "water", "coastline") || w.hasTag("landuse", "reservoir");
    }

    static boolean isSubwayOrTram(OsmPrimitive w) {
        return w.hasTag(RAILWAY, "subway", "tram");
    }

    static boolean isProposedOrAbandoned(OsmPrimitive w) {
        return w.hasTag(HIGHWAY, "proposed") || w.hasTag(RAILWAY, "proposed", "abandoned");
    }

    abstract boolean ignoreWaySegmentCombination(Way w1, Way w2);

    abstract String createMessage(Way w1, Way w2);

    @Override
    public void visit(Way w) {

        int nodesSize = w.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            final WaySegment es1 = new WaySegment(w, i);
            for (List<WaySegment> segments : getSegments(es1.getFirstNode(), es1.getSecondNode())) {
                for (WaySegment es2 : segments) {
                    List<Way> prims;
                    List<WaySegment> highlight;

                    if (errorSegments.contains(es1) && errorSegments.contains(es2)
                            || !es1.intersects(es2)
                            || ignoreWaySegmentCombination(es1.way, es2.way)) {
                        continue;
                    }

                    prims = Arrays.asList(es1.way, es2.way);
                    if ((highlight = seenWays.get(prims)) == null) {
                        highlight = new ArrayList<WaySegment>();
                        highlight.add(es1);
                        highlight.add(es2);

                        final String message = createMessage(es1.way, es2.way);
                        errors.add(new TestError(this, Severity.WARNING,
                                message,
                                CROSSING_WAYS,
                                prims,
                                highlight));
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
     *
     * @param n1 The first node
     * @param n2 The second node
     * @return A list with all the cells the segment crosses
     */
    public List<List<WaySegment>> getSegments(Node n1, Node n2) {

        List<List<WaySegment>> cells = new ArrayList<List<WaySegment>>();
        for (Point2D cell : ValUtil.getSegmentCells(n1, n2, OsmValidator.griddetail)) {
            List<WaySegment> segments = cellSegments.get(cell);
            if (segments == null) {
                segments = new ArrayList<WaySegment>();
                cellSegments.put(cell, segments);
            }
            cells.add(segments);
        }
        return cells;
    }

}
