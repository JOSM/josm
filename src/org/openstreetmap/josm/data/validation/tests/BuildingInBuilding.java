// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;
import org.openstreetmap.josm.tools.Predicate;

public class BuildingInBuilding extends Test {

    protected static final int BUILDING_INSIDE_BUILDING = 2001;
    protected List<OsmPrimitive> primitivesToCheck = new LinkedList<OsmPrimitive>();
    protected QuadBuckets<Way> index = new QuadBuckets<Way>();

    public BuildingInBuilding() {
        super(tr("Building inside building"), tr("Checks for building areas inside of buildings."));
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

    @Override
    public void visit(Relation r) {
        if (r.isUsable() && r.isMultipolygon() && isBuilding(r)) {
            primitivesToCheck.add(r);
            for (RelationMember m : r.getMembers()) {
                if (m.getRole().equals("outer") && m.getType().equals(OsmPrimitiveType.WAY)) {
                    index.add(m.getWay());
                }
            }
        }
    }

    private static boolean isInPolygon(Node n, List<Node> polygon) {
        return Geometry.nodeInsidePolygon(n, polygon);
    }

    protected class MultiPolygonMembers {
        public final Set<Way> outers = new HashSet<Way>();
        public final Set<Way> inners = new HashSet<Way>();
        public MultiPolygonMembers(Relation multiPolygon) {
            for (RelationMember m : multiPolygon.getMembers()) {
                if (m.getType().equals(OsmPrimitiveType.WAY)) {
                    if (m.getRole().equals("outer")) {
                        outers.add(m.getWay());
                    } else if (m.getRole().equals("inner")) {
                        inners.add(m.getWay());
                    }
                }
            }
        }
    }

    protected boolean sameLayers(Way w1, Way w2) {
        String l1 = w1.get("layer") != null ? w1.get("layer") : "0";
        String l2 = w2.get("layer") != null ? w2.get("layer") : "0";
        return l1.equals(l2);
    }

    protected boolean isWayInsideMultiPolygon(Way object, Relation multiPolygon) {
        // Extract outer/inner members from multipolygon
        MultiPolygonMembers mpm = new MultiPolygonMembers(multiPolygon);
        // Test if object is inside an outer member
        for (Way out : mpm.outers) {
            PolygonIntersection inter = Geometry.polygonIntersection(object.getNodes(), out.getNodes());
            if (inter == PolygonIntersection.FIRST_INSIDE_SECOND || inter == PolygonIntersection.CROSSING) {
                boolean insideInner = false;
                // If inside an outer, check it is not inside an inner
                for (Way in : mpm.inners) {
                    if (Geometry.polygonIntersection(in.getNodes(), out.getNodes()) == PolygonIntersection.FIRST_INSIDE_SECOND &&
                        Geometry.polygonIntersection(object.getNodes(), in.getNodes()) == PolygonIntersection.FIRST_INSIDE_SECOND) {
                        insideInner = true;
                        break;
                    }
                }
                // Inside outer but not inside inner -> the building appears to be inside a buiding
                if (!insideInner) {
                    // Final check on "layer" tag. Buildings of different layers may be superposed
                    if (sameLayers(object, out)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public void endTest() {
        for (final OsmPrimitive p : primitivesToCheck) {
            Collection<Way> outers = new FilteredCollection<Way>(index.search(p.getBBox()), new Predicate<Way>() {

                protected boolean evaluateNode(Node n, Way object) {
                    return isInPolygon(n, object.getNodes()) || object.getNodes().contains(n);
                }

                protected boolean evaluateWay(Way w, Way object) {
                    if (w.equals(object)) return false;

                    // Get all multipolygons referencing object
                    Collection<OsmPrimitive> buildingMultiPolygons = new FilteredCollection<OsmPrimitive>(object.getReferrers(), new Predicate<OsmPrimitive>() {
                        @Override
                        public boolean evaluate(OsmPrimitive object) {
                            return primitivesToCheck.contains(object);
                        }
                    }) ;

                    // if there's none, test if w is inside object
                    if (buildingMultiPolygons.isEmpty()) {
                        PolygonIntersection inter = Geometry.polygonIntersection(w.getNodes(), object.getNodes());
                        // Final check on "layer" tag. Buildings of different layers may be superposed
                        return (inter == PolygonIntersection.FIRST_INSIDE_SECOND || inter == PolygonIntersection.CROSSING) && sameLayers(w, object);
                    } else {
                        // Else, test if w is inside one of the multipolygons
                        for (OsmPrimitive bmp : buildingMultiPolygons) {
                            if (bmp instanceof Relation) {
                                if (isWayInsideMultiPolygon(w, (Relation) bmp)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    }
                }

                protected boolean evaluateRelation(Relation r, Way object) {
                    MultiPolygonMembers mpm = new MultiPolygonMembers((Relation) p);
                    for (Way out : mpm.outers) {
                        if (evaluateWay(out, object)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public boolean evaluate(Way object) {
                    if (p.equals(object))
                        return false;
                    else if (p instanceof Node)
                        return evaluateNode((Node) p, object);
                    else if (p instanceof Way)
                        return evaluateWay((Way) p, object);
                    else if (p instanceof Relation)
                        return evaluateRelation((Relation) p, object);
                    return false;
                }
            });

            if (!outers.isEmpty()) {
                errors.add(new TestError(this, Severity.WARNING,
                        tr("Building inside building"), BUILDING_INSIDE_BUILDING, p));
            }
        }

        super.endTest();
    }
}
