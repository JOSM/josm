// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;

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
        if (!w.isUsable() || w.isClosed() || !w.hasKey(HIGHWAY)) {
            return;
        }

        List<OsmPrimitive> r = w.firstNode().getReferrers();
        boolean hasway = r.stream().anyMatch(p -> p != w && p.hasKey(HIGHWAY));
        if (!hasway) {
            for (OsmPrimitive p : r) {
                testForError(w, w.firstNode(), p);
            }
        }
        r = w.lastNode().getReferrers();
        hasway = r.stream().anyMatch(p -> p != w && p.hasKey(HIGHWAY));
        if (!hasway) {
            for (OsmPrimitive p : r) {
                testForError(w, w.lastNode(), p);
            }
        }
    }

    private void testForError(Way w, Node wayNode, OsmPrimitive p) {
        if (!wayNode.isOutsideDownloadArea()
                && wayNode.getReferrers().stream().noneMatch(p1 -> p1.hasTag("route", "ferry"))) {
            if (isArea(p)) {
                addPossibleError(w, wayNode, p, p);
            } else {
                p.referrers(Relation.class)
                        .filter(r -> r.isMultipolygon() && isArea(r))
                        .findFirst()
                        .ifPresent(r -> addPossibleError(w, wayNode, p, r));
            }
        }
    }

    private static boolean isArea(OsmPrimitive p) {
        return p.hasKey("landuse", "natural") && ElemStyles.hasAreaElemStyle(p, false);
    }

    private void addPossibleError(Way w, Node wayNode, OsmPrimitive p, OsmPrimitive area) {
        // Avoid "legal" cases (see #10655)
        if (w.hasKey(HIGHWAY) && wayNode.hasTag("leisure", "slipway") && area.hasTag("natural", "water")) {
            return;
        }
        if (wayNode.hasTag("noexit", "yes")) {
            // Avoid "legal" case (see #17036)
            return;
        }
        errors.add(TestError.builder(this, Severity.WARNING, 2301)
                .message(tr("Way terminates on Area"))
                .primitives(w, p)
                .highlight(wayNode)
                .build());
    }
}
