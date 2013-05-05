// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
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

    protected static final int OVERLAPPING_AREAS = 2201;
    protected QuadBuckets<Way> index = new QuadBuckets<Way>();

    public OverlappingAreas() {
        super(tr("Overlapping Areas"), tr("This test checks if areas overlap."));
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable() && w.isArea() && ElemStyles.hasAreaElemStyle(w, false)) {
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
                            if (w.equals(wi))
                                return false;
                            else
                                return Geometry.polygonIntersection(w.getNodes(), wi.getNodes())
                                        == Geometry.PolygonIntersection.CROSSING;
                        }
                    });
            if (!overlaps.isEmpty()) {
                Collection<Way> overlapsWater = new ArrayList<Way>();
                Collection<Way> overlapsOther = new ArrayList<Way>();

                String natural1 = w.get("natural");
                String landuse1 = w.get("landuse");
                boolean isWaterArea = "water".equals(natural1) || "wetland".equals(natural1) || "coastline".equals(natural1) || "reservoir".equals(landuse1);
                boolean isWaterArea2 = false;

                for (Way wayOther : overlaps) {
                    String natural2 = wayOther.get("natural");
                    String landuse2 = wayOther.get("landuse");
                    boolean isWaterAreaTest = "water".equals(natural2) || "wetland".equals(natural2) || "coastline".equals(natural2) || "reservoir".equals(landuse2);

                    if (!isWaterArea2) {
                        isWaterArea2 = isWaterAreaTest;
                    }

                    if (isWaterArea && isWaterAreaTest) {
                        overlapsWater.add(wayOther);
                    } else {
                        overlapsOther.add(wayOther);
                    }
                }

                if (!overlapsWater.isEmpty()) {
                    errors.add(new TestError(this, Severity.WARNING, tr("Overlapping Water Areas"),
                            OVERLAPPING_AREAS, Collections.singletonList(w), overlapsWater));
                }

                if (!overlapsOther.isEmpty()) {
                    errors.add(new TestError(this, Severity.OTHER, tr("Overlapping Areas"),
                            OVERLAPPING_AREAS, Collections.singletonList(w), overlapsOther));
                }
            }
        }

        super.endTest();
    }

}
