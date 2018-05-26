// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.Geometry;

/**
 * Check cyclic ways for errors
 *
 * @author jrreid
 */
public class WronglyOrderedWays extends Test {

    // CHECKSTYLE.OFF: SingleSpaceSeparator
    protected static final int WRONGLY_ORDERED_COAST = 1001;
    protected static final int WRONGLY_ORDERED_LAND  = 1003;
    // CHECKSTYLE.ON: SingleSpaceSeparator

    /**
     * Constructor
     */
    public WronglyOrderedWays() {
        super(tr("Wrongly Ordered Ways"),
                tr("This test checks the direction of water, land and coastline ways."));
    }

    @Override
    public void visit(Way w) {

        if (!w.isUsable() || !w.isClosed())
            return;

        String natural = w.get("natural");
        if (natural == null) {
            return;
        } else if ("coastline".equals(natural) && Geometry.isClockwise(w)) {
            reportError(w, tr("Reversed coastline: land not on left side"), WRONGLY_ORDERED_COAST);
        } else if ("land".equals(natural) && Geometry.isClockwise(w)) {
            reportError(w, tr("Reversed land: land not on left side"), WRONGLY_ORDERED_LAND);
        }
    }

    private void reportError(Way w, String msg, int type) {
        errors.add(TestError.builder(this, Severity.WARNING, type)
                .message(msg)
                .primitives(w)
                .build());
    }
}
