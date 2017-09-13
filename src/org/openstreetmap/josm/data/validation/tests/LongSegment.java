// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Checks for very long segments.
 *
 * @since 8320
 */
public class LongSegment extends Test {

    /** Long segment error */
    protected static final int LONG_SEGMENT = 3501;
    /** Maximum segment length for this test */
    protected int maxlength;
    /** set of visited ways. Tracking this increases performance when checking single nodes. */
    private Set<Way> visitedWays;

    /** set of way segments that have been reported */
    protected Set<WaySegment> reported;

    /**
     * Constructor
     */
    public LongSegment() {
        super(tr("Long segments"),
              tr("This tests for long way segments, which are usually errors."));
    }

    @Override
    public void visit(Node n) {
        // Test all way segments around this node.
        // If there is an error in the unchanged part of the way, we do not need to warn the user about it.
        for (Way way : n.getParentWays()) {
                if (ignoreWay(way)) {
                    continue;
                }
                // Do not simply use index of - a node may be in a way multiple times
                for (int i = 0; i < way.getNodesCount(); i++) {
                    if (n == way.getNode(i)) {
                        if (i > 0) {
                            visitWaySegment(way, i - 1);
                        }
                        if (i < way.getNodesCount() - 1) {
                            visitWaySegment(way, i);
                        }
                    }
                }
            }
    }

    @Override
    public void visit(Way w) {
        if (ignoreWay(w)) {
            return;
        }
        visitedWays.add(w);

        testWay(w);
    }

    private void testWay(Way w) {
        for (int i = 0; i < w.getNodesCount() - 1; i++) {
            visitWaySegment(w, i);
        }
    }

    private boolean ignoreWay(Way w) {
        return visitedWays.contains(w) || w.hasTag("route", "ferry");
    }

    private void visitWaySegment(Way w, int i) {
        LatLon coor1 = w.getNode(i).getCoor();
        LatLon coor2 = w.getNode(i + 1).getCoor();

        if (coor1 != null && coor2 != null) {
            Double length = coor1.greatCircleDistance(coor2);
            if (length > maxlength) {
                addErrorForSegment(new WaySegment(w, i), length / 1000.0);
            }
        }
    }

    private void addErrorForSegment(WaySegment waySegment, Double length) {
        if (reported.add(waySegment)) {
            errors.add(TestError.builder(this, Severity.WARNING, LONG_SEGMENT)
                    .message(tr("Long segments"), marktr("Very long segment of {0} kilometers"), length.intValue())
                    .primitives(waySegment.way)
                    .highlightWaySegments(Collections.singleton(waySegment))
                    .build());
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        maxlength = Config.getPref().getInt("validator.maximum.segment.length", 15_000);
        reported = new HashSet<>();
        visitedWays = new HashSet<>();
    }

    @Override
    public void endTest() {
        super.endTest();
        // free memory
        visitedWays = null;
        reported = null;
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return p.isUsable() && (isUsableWay(p) || isUsableNode(p));
    }

    private static boolean isUsableNode(OsmPrimitive p) {
        // test changed nodes - ways referred by them may not be checked automatically.
        return p instanceof Node && p.isDrawable();
    }

    private static boolean isUsableWay(OsmPrimitive p) {
        // test only Ways with at least 2 nodes
        return p instanceof Way && ((Way) p).getNodesCount() > 1;
    }
}
