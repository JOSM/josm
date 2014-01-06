// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;

/**
 * Performs validation tests on barriers and entrances.
 * @since 6192
 */
public class BarriersEntrances extends Test {
    
    protected static final int BARRIER_ENTRANCE_WITHOUT_BARRIER = 2801;

    /**
     * Constructor
     */
    public BarriersEntrances() {
        super(tr("Barriers and entrances"), tr("Checks for errors in barriers and entrances."));
    }

    @Override
    public void visit(Node n) {
        if (n.hasTag("barrier", "entrance") && !n.isOutsideDownloadArea()) {
            for (OsmPrimitive p : n.getReferrers()) {
                if (p.hasKey("barrier")) {
                    return;
                }
            }
            errors.add(new TestError(this, Severity.WARNING, tr("Barrier entrance not set on a barrier"), BARRIER_ENTRANCE_WITHOUT_BARRIER, n));
        }
    }
}
