// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

/**
 * Checks for untagged ways
 *
 * @since 8320
 */
public class LongSegment extends Test {

    /** Long segment error */
    protected static final int LONG_SEGMENT    = 3501;
    /** Maximum segment length for this test */
    protected int maxlength;

    /**
     * Constructor
     */
    public LongSegment() {
        super(tr("Long segments"),
              tr("This tests for long way segments, which are usually errors."));
    }

    @Override
    public void visit(Way w) {
        Double length = w.getLongestSegmentLength();
        if(length > maxlength) {
            length /= 1000.0;
            errors.add(new TestError(this, Severity.WARNING, tr("Long segments"),
                    tr("Very long segment of {0} kilometers", length.intValue()),
                    String.format("Very long segment of %d kilometers", length.intValue()),
                    LONG_SEGMENT, w));
        }
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        maxlength = Main.pref.getInteger("validator.maximum.segment.length", 15000);
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return p.isUsable() && p instanceof Way && ((Way) p).getNodesCount() > 1; // test only Ways with at least 2 nodes
    }
}
