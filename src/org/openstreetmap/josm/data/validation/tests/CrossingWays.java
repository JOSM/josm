// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.data.validation.util.ValUtil;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Tests if there are segments that crosses in the same layer
 *
 * @author frsantos
 */
public class CrossingWays extends Test {
    protected static int CROSSING_WAYS = 601;

    /** All way segments, grouped by cells */
    Map<Point2D,List<ExtendedSegment>> cellSegments;
    /** The already detected errors */
    HashSet<WaySegment> errorSegments;
    /** The already detected ways in error */
    Map<List<Way>, List<WaySegment>> ways_seen;

    /**
     * Constructor
     */
    public CrossingWays() {
        super(tr("Crossing ways."),
              tr("This test checks if two roads, railways, waterways or buildings crosses in the same layer, but are not connected by a node."));
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        cellSegments = new HashMap<Point2D,List<ExtendedSegment>>(1000);
        errorSegments = new HashSet<WaySegment>();
        ways_seen = new HashMap<List<Way>, List<WaySegment>>(50);
    }

    @Override
    public void endTest() {
        super.endTest();
        cellSegments = null;
        errorSegments = null;
        ways_seen = null;
    }

    @Override
    public void visit(Way w) {
        if(!w.isUsable())
            return;

        String coastline1 = w.get("natural");
        boolean isCoastline1 = "water".equals(coastline1) || "coastline".equals(coastline1);
        String railway1 = w.get("railway");
        boolean isSubway1 = "subway".equals(railway1);
        boolean isTram1 = "tram".equals(railway1);
        boolean isBuilding = (w.get("building") != null);

        if (w.get("highway") == null && w.get("waterway") == null
                && (railway1 == null || isSubway1 || isTram1)
                && !isCoastline1 && !isBuilding)
            return;

        String layer1 = w.get("layer");

        int nodesSize = w.getNodesCount();
        for (int i = 0; i < nodesSize - 1; i++) {
            WaySegment ws = new WaySegment(w, i);
            ExtendedSegment es1 = new ExtendedSegment(ws, layer1, railway1, coastline1);
            List<List<ExtendedSegment>> cellSegments = getSegments(es1.n1, es1.n2);
            for (List<ExtendedSegment> segments : cellSegments) {
                for (ExtendedSegment es2 : segments) {
                    List<Way> prims;
                    List<WaySegment> highlight;

                    if (errorSegments.contains(ws) && errorSegments.contains(es2.ws))
                        continue;

                    String layer2 = es2.layer;
                    String railway2 = es2.railway;
                    String coastline2 = es2.coastline;
                    if (layer1 == null ? layer2 != null : !layer1.equals(layer2))
                        continue;

                    if (!es1.intersects(es2) ) continue;
                    if (isSubway1 && "subway".equals(railway2)) continue;
                    if (isTram1 && "tram".equals(railway2)) continue;

                    boolean isCoastline2 = coastline2 != null && (coastline2.equals("water") || coastline2.equals("coastline"));
                    if (isCoastline1 != isCoastline2) continue;

                    if ((es1.railway != null && es1.railway.equals("abandoned"))
                            || (railway2 != null && railway2.equals("abandoned"))) continue;

                    prims = Arrays.asList(es1.ws.way, es2.ws.way);
                    if ((highlight = ways_seen.get(prims)) == null) {
                        highlight = new ArrayList<WaySegment>();
                        highlight.add(es1.ws);
                        highlight.add(es2.ws);

                        errors.add(new TestError(this, Severity.WARNING,
                            isBuilding ? tr("Crossing buildings") : tr("Crossing ways"),
                            CROSSING_WAYS,
                            prims,
                            highlight));
                        ways_seen.put(prims, highlight);
                    } else {
                        highlight.add(es1.ws);
                        highlight.add(es2.ws);
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
    public List<List<ExtendedSegment>> getSegments(Node n1, Node n2) {

        List<List<ExtendedSegment>> cells = new ArrayList<List<ExtendedSegment>>();
        for(Point2D cell : ValUtil.getSegmentCells(n1, n2, OsmValidator.griddetail)) {
            List<ExtendedSegment> segments = cellSegments.get(cell);
            if (segments == null) {
                segments = new ArrayList<ExtendedSegment>();
                cellSegments.put(cell, segments);
            }
            cells.add(segments);
        }
        return cells;
    }

    /**
     * A way segment with some additional information
     * @author frsantos
     */
    public static class ExtendedSegment {
        public Node n1, n2;

        public WaySegment ws;

        /** The layer */
        public String layer;

        /** The railway type */
        public String railway;

        /** The coastline type */
        public String coastline;

        /**
         * Constructor
         * @param ws The way segment
         * @param layer The layer of the way this segment is in
         * @param railway The railway type of the way this segment is in
         * @param coastline The coastline typo of the way the segment is in
         */
        public ExtendedSegment(WaySegment ws, String layer, String railway, String coastline) {
            this.ws = ws;
            this.n1 = ws.way.getNodes().get(ws.lowerIndex);
            this.n2 = ws.way.getNodes().get(ws.lowerIndex + 1);
            this.layer = layer;
            this.railway = railway;
            this.coastline = coastline;
        }

        /**
         * Checks whether this segment crosses other segment
         * @param s2 The other segment
         * @return true if both segments crosses
         */
        public boolean intersects(ExtendedSegment s2) {
            if (n1.equals(s2.n1) || n2.equals(s2.n2) ||
                    n1.equals(s2.n2) || n2.equals(s2.n1))
                return false;

            return Line2D.linesIntersect(
                n1.getEastNorth().east(), n1.getEastNorth().north(),
                n2.getEastNorth().east(), n2.getEastNorth().north(),
                s2.n1.getEastNorth().east(), s2.n1.getEastNorth().north(),
                s2.n2.getEastNorth().east(), s2.n2.getEastNorth().north());
        }
    }
}
