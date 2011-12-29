package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
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

    /**
     * Return true if w is in polygon.
     */
    private static boolean isInPolygon(Way w, List<Node> polygon) {
        // Check that all nodes of w are in polygon
        for (Node n : w.getNodes()) {
            if (!isInPolygon(n, polygon)) {
                return false;
            }
        }
        // All nodes can be inside polygon and still, w outside:
        //            +-------------+
        //           /|             |
        //          / |             |
        //         /  |             |
        //        / w |             |
        //  +----+----+             |
        //  |       polygon         |
        //  |_______________________|
        //
        for (int i=1; i<w.getNodesCount(); i++) {
            LatLon center = w.getNode(i).getCoor().getCenter(w.getNode(i-1).getCoor());
            if (center != null && !isInPolygon(new Node(center), polygon)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void endTest() {
        for (final OsmPrimitive p : primitivesToCheck) {
            Collection<Way> outers = new FilteredCollection<Way>(index.search(p.getBBox()), new Predicate<Way>() {
                @Override
                public boolean evaluate(Way object) {
                    if (p.equals(object)) {
                        return false;
                    } else if (p instanceof Node) {
                        return isInPolygon((Node) p, object.getNodes()) || object.getNodes().contains(p);
                    } else if (p instanceof Way) {
                        return isInPolygon((Way) p, object.getNodes()) && !isInInnerWay((Way)p, object);
                    } else {
                        return false;
                    }
                }
            });
            if (!outers.isEmpty()) {
                errors.add(new TestError(this, Severity.WARNING,
                        tr("Building inside building"), BUILDING_INSIDE_BUILDING, p));
            }
        }
    }
    
    private boolean isInInnerWay(Way w, Way outer) {
        for (OsmPrimitive r : outer.getReferrers()) {
            if (r instanceof Relation && ((Relation)r).isMultipolygon()) {
                for (RelationMember m : ((Relation)r).getMembers()) {
                    if (m.hasRole() && m.getRole().equals("inner") && m.getType().equals(OsmPrimitiveType.WAY)) {
                        // Only check inner ways actually inside the current outer
                        Way inner = m.getWay();
                        if (isInPolygon(inner, outer.getNodes())) {
                            // If the tested way is inside this inner, outer is a false positive
                            if (isInPolygon(w, inner.getNodes())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isBuilding(OsmPrimitive p) {
        return "yes".equals(p.get("building"));
    }
}
