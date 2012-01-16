package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

public class OverlappingAreas extends Test {

    protected static int OVERLAPPING_AREAS = 2201;
    protected QuadBuckets<Way> index = new QuadBuckets<Way>();

    public OverlappingAreas() {
        super(tr("Overlapping Areas"), tr("This test checks if areas overlap."));
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable() && w.isClosed() && ElemStyles.hasAreaElemStyle(w, false)) {
            index.add(w);
        }
    }

    @Override
    public void endTest() {
        for (final Way w : index) {
            Collection<Way> overlaps = Utils.filter(
                    index.search(w.getBBox()),
                    new Predicate<Way>() {

                        @Override
                        public boolean evaluate(Way wi) {
                            if (w.equals(wi)) {
                                return false;
                            } else {
                                return Geometry.polygonIntersection(w.getNodes(), wi.getNodes())
                                        == Geometry.PolygonIntersection.CROSSING;
                            }
                        }
                    });
            if (!overlaps.isEmpty()) {
                errors.add(new TestError(this, Severity.OTHER, tr("Overlapping Areas"),
                        OVERLAPPING_AREAS, Collections.singletonList(w), overlaps));
            }
        }
    }

}
