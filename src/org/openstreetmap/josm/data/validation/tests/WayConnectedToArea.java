// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.Utils;

/**
 * Checks for ways connected to areas.
 * @since 4682
 */
public class WayConnectedToArea extends Test {

    /**
     * Constructs a new {@code WayConnectedToArea} test.
     */
    public WayConnectedToArea() {
        super(tr("Way connected to Area"), tr("Checks for ways connected to areas."));
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable() || w.isClosed() || !w.hasKey("highway")) {
            return;
        }

        boolean hasway = false;
        List<OsmPrimitive> r = w.firstNode().getReferrers();
        for (OsmPrimitive p : r) {
            if(p != w && p.hasKey("highway")) {
                hasway = true;
                break;
            }
        }
        if (!hasway) {
            for (OsmPrimitive p : r) {
                testForError(w, w.firstNode(), p);
            }
        }
        hasway = false;
        r = w.lastNode().getReferrers();
        for (OsmPrimitive p : r) {
            if(p != w && p.hasKey("highway")) {
                hasway = true;
                break;
            }
        }
        if (!hasway) {
            for (OsmPrimitive p : r) {
                testForError(w, w.lastNode(), p);
            }
        }
    }

    private void testForError(Way w, Node wayNode, OsmPrimitive p) {
        if (wayNode.isOutsideDownloadArea()) {
            return;
        } else if (Utils.exists(wayNode.getReferrers(), Predicates.hasTag("route", "ferry"))) {
            return;
        } else if (isArea(p)) {
            addError(w, wayNode, p);
        } else {
            for (OsmPrimitive r : p.getReferrers()) {
                if (r instanceof Relation
                        && r.hasTag("type", "multipolygon")
                        && isArea(r)) {
                    addError(w, wayNode, p);
                    break;
                }
            }
        }
    }

    private boolean isArea(OsmPrimitive p) {
        return (p.hasKey("landuse") || p.hasKey("natural"))
                && ElemStyles.hasAreaElemStyle(p, false);
    }

    private void addError(Way w, Node wayNode, OsmPrimitive p) {
        errors.add(new TestError(this, Severity.WARNING,
                tr("Way terminates on Area"), 2301,
                Arrays.asList(w, p),
                Arrays.asList(wayNode)));
    }
}
