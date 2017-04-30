// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.HashSet;
import java.util.Set;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

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
    /** set of visited ways */
    private final Set<Way> visitedWays = new HashSet<>();

    /**
     * Constructor
     */
    public LongSegment() {
        super(tr("Long segments"),
              tr("This tests for long way segments, which are usually errors."));
    }

    @Override
    public void visit(Node n) {
        for (Way w : n.getParentWays()) {
            if (isPrimitiveUsable(w)) {
                testWay(w);
            }
        }
    }

    @Override
    public void visit(Way w) {
        testWay(w);
    }

    private void testWay(Way w) {
        if (visitedWays.contains(w) || w.hasTag("route", "ferry")) {
            return;
        }
        visitedWays.add(w);
        Double length = w.getLongestSegmentLength();
        if (length > maxlength) {
            length /= 1000.0;
            errors.add(TestError.builder(this, Severity.WARNING, LONG_SEGMENT)
                    .message(tr("Long segments"), marktr("Very long segment of {0} kilometers"), length.intValue())
                    .primitives(w)
                    .build());
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        maxlength = Main.pref.getInteger("validator.maximum.segment.length", 15_000);
        visitedWays.clear();
    }

    @Override
    public void endTest() {
        visitedWays.clear();
        super.endTest();
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        // test only nodes and Ways with at least 2 nodes
        return p.isUsable() && ((p instanceof Node && p.isDrawable()) || (p instanceof Way && ((Way) p).getNodesCount() > 1));
    }
}
