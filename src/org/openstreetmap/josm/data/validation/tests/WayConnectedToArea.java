package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;

public class WayConnectedToArea extends Test {

    public WayConnectedToArea() {
        super(tr("Way connected to Area"), tr("Checks for ways connected to areas."));
    }

    @Override
    public void visit(Way w) {
        if (!w.isUsable() || w.isClosed() || !w.hasKey("highway")) {
            return;
        }

        for (OsmPrimitive p : w.firstNode().getReferrers()) {
            testForError(w, w.firstNode(), p);
        }
        for (OsmPrimitive p : w.lastNode().getReferrers()) {
            testForError(w, w.lastNode(), p);
        }

    }

    private void testForError(Way w, Node wayNode, OsmPrimitive p) {
        if (isArea(p)) {
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
