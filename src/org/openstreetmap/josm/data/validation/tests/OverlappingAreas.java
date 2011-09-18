package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.Collections;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.mappaint.AreaElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyle;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

public class OverlappingAreas extends Test {

    protected static int OVERLAPPING_AREAS = 2201;
    protected QuadBuckets<Way> index = new QuadBuckets<Way>();
    private static ElemStyles styles = MapPaintStyles.getStyles();

    public OverlappingAreas() {
        super(tr("Overlapping Areas"));
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable() && w.isClosed() && hasAreaElemStyle(w)) {
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

    private boolean hasAreaElemStyle(OsmPrimitive p) {
        for (ElemStyle s : styles.generateStyles(p, 1.0, null, false).a) {
            if (s instanceof AreaElemStyle) {
                return true;
            }
        }
        return false;
    }
}
