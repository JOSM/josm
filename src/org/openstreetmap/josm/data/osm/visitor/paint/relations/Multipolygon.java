// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.gui.NavigatableComponent;

public class Multipolygon {

    public static class PolyData {
        public enum Intersection {INSIDE, OUTSIDE, CROSSING}

        public Polygon poly = new Polygon();
        public final boolean selected;
        private Point lastP;
        private Rectangle bounds;

        public PolyData(NavigatableComponent nc, List<Node> nodes, boolean selected) {
            this.selected = selected;
            Point p = null;
            for (Node n : nodes)
            {
                p = nc.getPoint(n);
                poly.addPoint(p.x,p.y);
            }
            if (!nodes.get(0).equals(nodes.get(nodes.size() - 1))) {
                p = nc.getPoint(nodes.get(0));
                poly.addPoint(p.x, p.y);
            }
            lastP = p;
        }

        public PolyData(PolyData copy) {
            poly = new Polygon(copy.poly.xpoints, copy.poly.ypoints, copy.poly.npoints);
            this.selected = copy.selected;
            lastP = copy.lastP;
        }

        public Intersection contains(Polygon p) {
            int contains = p.npoints;
            for(int i = 0; i < p.npoints; ++i)
            {
                if(poly.contains(p.xpoints[i],p.ypoints[i])) {
                    --contains;
                }
            }
            if(contains == 0) return Intersection.INSIDE;
            if(contains == p.npoints) return Intersection.OUTSIDE;
            return Intersection.CROSSING;
        }

        public void addInner(Polygon p) {
            for(int i = 0; i < p.npoints; ++i) {
                poly.addPoint(p.xpoints[i],p.ypoints[i]);
            }
            poly.addPoint(lastP.x, lastP.y);
        }

        public Polygon get() {
            return poly;
        }

        public Rectangle getBounds() {
            if (bounds == null) {
                bounds = poly.getBounds();
            }
            return bounds;
        }

        @Override
        public String toString() {
            return "Points: " + poly.npoints + " Selected: " + selected;
        }
    }

    private final NavigatableComponent nc;

    private final List<Way> innerWays = new ArrayList<Way>();
    private final List<Way> outerWays = new ArrayList<Way>();
    private final List<PolyData> innerPolygons = new ArrayList<PolyData>();
    private final List<PolyData> outerPolygons = new ArrayList<PolyData>();
    private final List<PolyData> combinedPolygons = new ArrayList<PolyData>();
    private boolean hasNonClosedWays;

    public Multipolygon(NavigatableComponent nc) {
        this.nc = nc;
    }

    public void load(Relation r) {
        // Fill inner and outer list with valid ways
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isDrawable()) {
                if(m.isWay()) {
                    Way w = m.getWay();

                    if(w.getNodesCount() < 2) {
                        continue;
                    }

                    if("inner".equals(m.getRole())) {
                        getInnerWays().add(w);
                    } else if("outer".equals(m.getRole())) {
                        getOuterWays().add(w);
                    } else if (!m.hasRole()) {
                        getOuterWays().add(w);
                    } // Remaining roles ignored
                } // Non ways ignored
            }
        }

        createPolygons(innerWays, innerPolygons);
        createPolygons(outerWays, outerPolygons);
        if (!outerPolygons.isEmpty()) {
            addInnerToOuters();
        }
    }

    private void createPolygons(List<Way> ways, List<PolyData> result) {
        List<Way> waysToJoin = new ArrayList<Way>();
        for (Way way: ways) {
            if (way.isClosed()) {
                result.add(new PolyData(nc, way.getNodes(), way.isSelected()));
            } else {
                waysToJoin.add(way);
            }
        }

        result.addAll(joinWays(waysToJoin));
    }

    public Collection<PolyData> joinWays(Collection<Way> join)
    {
        Collection<PolyData> res = new LinkedList<PolyData>();
        Way[] joinArray = join.toArray(new Way[join.size()]);
        int left = join.size();
        while(left != 0)
        {
            Way w = null;
            boolean selected = false;
            List<Node> n = null;
            boolean joined = true;
            while(joined && left != 0)
            {
                joined = false;
                for(int i = 0; i < joinArray.length && left != 0; ++i)
                {
                    if(joinArray[i] != null)
                    {
                        Way c = joinArray[i];
                        if(w == null)
                        { w = c; selected = w.isSelected(); joinArray[i] = null; --left; }
                        else
                        {
                            int mode = 0;
                            int cl = c.getNodesCount()-1;
                            int nl;
                            if(n == null)
                            {
                                nl = w.getNodesCount()-1;
                                if(w.getNode(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(w.getNode(nl) == c.getNode(cl)) {
                                    mode = 22;
                                } else if(w.getNode(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(w.getNode(0) == c.getNode(cl)) {
                                    mode = 12;
                                }
                            }
                            else
                            {
                                nl = n.size()-1;
                                if(n.get(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if(n.get(0) == c.getNode(cl)) {
                                    mode = 12;
                                } else if(n.get(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if(n.get(nl) == c.getNode(cl)) {
                                    mode = 22;
                                }
                            }
                            if(mode != 0)
                            {
                                joinArray[i] = null;
                                joined = true;
                                if(c.isSelected()) {
                                    selected = true;
                                }
                                --left;
                                if(n == null) {
                                    n = w.getNodes();
                                }
                                n.remove((mode == 21 || mode == 22) ? nl : 0);
                                if(mode == 21) {
                                    n.addAll(c.getNodes());
                                } else if(mode == 12) {
                                    n.addAll(0, c.getNodes());
                                } else if(mode == 22)
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(nl, node);
                                    }
                                }
                                else /* mode == 11 */
                                {
                                    for(Node node : c.getNodes()) {
                                        n.add(0, node);
                                    }
                                }
                            }
                        }
                    }
                } /* for(i = ... */
            } /* while(joined) */

            if (n == null) {
                n = w.getNodes();
            }

            if(!n.isEmpty() && !n.get(n.size() - 1).equals(n.get(0))) {
                hasNonClosedWays = true;
            }
            PolyData pd = new PolyData(nc, n, selected);
            res.add(pd);
        } /* while(left != 0) */

        return res;
    }

    public PolyData findOuterPolygon(PolyData inner, List<PolyData> outerPolygons) {
        PolyData result = null;

        {// First try to test only bbox, use precise testing only if we don't get unique result
            Rectangle innerBox = inner.getBounds();
            PolyData insidePolygon = null;
            PolyData intersectingPolygon = null;
            int insideCount = 0;
            int intersectingCount = 0;

            for (PolyData outer: outerPolygons) {
                if (outer.getBounds().contains(innerBox)) {
                    insidePolygon = outer;
                    insideCount++;
                } else if (outer.getBounds().intersects(innerBox)) {
                    intersectingPolygon = outer;
                    intersectingCount++;
                }
            }

            if (insideCount == 1)
                return insidePolygon;
            else if (intersectingCount == 1)
                return intersectingPolygon;
        }

        for (PolyData combined : outerPolygons) {
            Intersection c = combined.contains(inner.poly);
            if(c != Intersection.OUTSIDE)
            {
                if(result == null || result.contains(combined.poly) != Intersection.INSIDE) {
                    result = combined;
                }
            }
        }
        return result;
    }

    private void addInnerToOuters()  {

        if (innerPolygons.isEmpty()) {
            combinedPolygons.addAll(outerPolygons);
        } else if (outerPolygons.size() == 1) {
            PolyData combinedOuter = new PolyData(outerPolygons.get(0));
            for (PolyData inner: innerPolygons) {
                combinedOuter.addInner(inner.poly);
            }
            combinedPolygons.add(combinedOuter);
        } else {
            for (PolyData outer: outerPolygons) {
                combinedPolygons.add(new PolyData(outer));
            }

            for (PolyData pdInner: innerPolygons) {
                PolyData o = findOuterPolygon(pdInner, combinedPolygons);
                if(o == null) {
                    o = outerPolygons.get(0);
                }
                o.addInner(pdInner.poly);
            }
        }
    }

    public List<Way> getOuterWays() {
        return outerWays;
    }

    public List<Way> getInnerWays() {
        return innerWays;
    }

    public List<PolyData> getInnerPolygons() {
        return innerPolygons;
    }

    public List<PolyData> getOuterPolygons() {
        return outerPolygons;
    }

    public List<PolyData> getCombinedPolygons() {
        return combinedPolygons;
    }

    public boolean hasNonClosedWays() {
        return hasNonClosedWays;
    }

}
