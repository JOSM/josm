package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.tools.FilteredCollection;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Predicate;

public class BuildingInBuilding extends Test {

    protected static int BUILDING_INSIDE_BUILDING = 2001;
    protected List<OsmPrimitive> primitivesToCheck = new LinkedList<OsmPrimitive>();
    protected QuadBuckets<Way> index = new QuadBuckets<Way>();

    public BuildingInBuilding() {
        super(tr("Building inside building"));
    }

    @Override
    public void visit(Node n) {
        if (n.isUsable() && isBuilding(n)) {
            primitivesToCheck.add(n);
        }
    }

    @Override
    public void visit(Way w) {
        if (w.isUsable() && w.isClosed() && isBuilding(w)) {
            primitivesToCheck.add(w);
            index.add(w);
        }
    }

    private static boolean isInPolygon(Node n, List<Node> polygon) {
        return Geometry.nodeInsidePolygon(n, polygon);
    }

    private static boolean isInPolygon(Way w, List<Node> polygon) {
        for (Node n : w.getNodes()) {
            if (!isInPolygon(n, polygon)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void endTest() {
        for (final OsmPrimitive p : primitivesToCheck) {
            Collection<Way> outer = index.search(p.getBBox());
            outer = new FilteredCollection<Way>(outer, new Predicate<Way>() {

                @Override
                public boolean evaluate(Way object) {
                    if (p.equals(object)) {
                        return false;
                    } else if (p instanceof Node) {
                        return isInPolygon((Node) p, object.getNodes()) || object.getNodes().contains((Node) p);
                    } else if (p instanceof Way) {
                        return isInPolygon((Way) p, object.getNodes());
                    } else {
                        return false;
                    }
                }
            });
            if (!outer.isEmpty()) {
                errors.add(new TestError(this, Severity.WARNING,
                        tr("Building inside building"), BUILDING_INSIDE_BUILDING, p));
            }
        }
    }

    private static boolean isBuilding(OsmPrimitive p) {
        return "yes".equals(p.get("building"));
    }
}
