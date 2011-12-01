// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.gui.NavigatableComponent;

public class Multipolygon {
    /** preference key for a collection of roles which indicate that the respective member belongs to an
     * <em>outer</em> polygon. Default is <tt>outer</tt>.
     */
    static public final String PREF_KEY_OUTER_ROLES = "mappaint.multipolygon.outer.roles";
    /** preference key for collection of role prefixes which indicate that the respective
     *  member belongs to an <em>outer</em> polygon. Default is empty.
     */
    static public final String PREF_KEY_OUTER_ROLE_PREFIXES = "mappaint.multipolygon.outer.role-prefixes";
    /** preference key for a collection of roles which indicate that the respective member belongs to an
     * <em>inner</em> polygon. Default is <tt>inner</tt>.
     */
    static public final String PREF_KEY_INNER_ROLES = "mappaint.multipolygon.inner.roles";
    /** preference key for collection of role prefixes which indicate that the respective
     *  member belongs to an <em>inner</em> polygon. Default is empty.
     */
    static public final String PREF_KEY_INNER_ROLE_PREFIXES = "mappaint.multipolygon.inner.role-prefixes";

    /**
     * <p>Kind of strategy object which is responsible for deciding whether a given
     * member role indicates that the member belongs to an <em>outer</em> or an
     * <em>inner</em> polygon.</p>
     * 
     * <p>The decision is taken based on preference settings, see the four preference keys
     * above.</p>
     * 
     */
    private static class MultipolygonRoleMatcher implements PreferenceChangedListener{
        private final List<String> outerExactRoles = new ArrayList<String>();
        private final List<String> outerRolePrefixes = new ArrayList<String>();
        private final List<String> innerExactRoles = new ArrayList<String>();
        private final List<String> innerRolePrefixes = new ArrayList<String>();

        private void initDefaults() {
            outerExactRoles.clear();
            outerRolePrefixes.clear();
            innerExactRoles.clear();
            innerRolePrefixes.clear();
            outerExactRoles.add("outer");
            innerExactRoles.add("inner");
        }

        private void setNormalized(Collection<String> literals, List<String> target){
            target.clear();
            for(String l: literals) {
                if (l == null) {
                    continue;
                }
                l = l.trim();
                if (!target.contains(l)) {
                    target.add(l);
                }
            }
        }

        private void initFromPreferences() {
            initDefaults();
            if (Main.pref == null) return;
            Collection<String> literals;
            literals = Main.pref.getCollection(PREF_KEY_OUTER_ROLES);
            if (literals != null && !literals.isEmpty()){
                setNormalized(literals, outerExactRoles);
            }
            literals = Main.pref.getCollection(PREF_KEY_OUTER_ROLE_PREFIXES);
            if (literals != null && !literals.isEmpty()){
                setNormalized(literals, outerRolePrefixes);
            }
            literals = Main.pref.getCollection(PREF_KEY_INNER_ROLES);
            if (literals != null && !literals.isEmpty()){
                setNormalized(literals, innerExactRoles);
            }
            literals = Main.pref.getCollection(PREF_KEY_INNER_ROLE_PREFIXES);
            if (literals != null && !literals.isEmpty()){
                setNormalized(literals, innerRolePrefixes);
            }
        }

        @Override
        public void preferenceChanged(PreferenceChangeEvent evt) {
            if (PREF_KEY_INNER_ROLE_PREFIXES.equals(evt.getKey()) ||
                    PREF_KEY_INNER_ROLES.equals(evt.getKey()) ||
                    PREF_KEY_OUTER_ROLE_PREFIXES.equals(evt.getKey()) ||
                    PREF_KEY_OUTER_ROLES.equals(evt.getKey())){
                initFromPreferences();
            }
        }

        public boolean isOuterRole(String role){
            if (role == null) return false;
            for (String candidate: outerExactRoles) {
                if (role.equals(candidate)) return true;
            }
            for (String candidate: outerRolePrefixes) {
                if (role.startsWith(candidate)) return true;
            }
            return false;
        }

        public boolean isInnerRole(String role){
            if (role == null) return false;
            for (String candidate: innerExactRoles) {
                if (role.equals(candidate)) return true;
            }
            for (String candidate: innerRolePrefixes) {
                if (role.startsWith(candidate)) return true;
            }
            return false;
        }
    }

    /*
     * Init a private global matcher object which will listen to preference
     * changes.
     */
    private static MultipolygonRoleMatcher roleMatcher;
    private static MultipolygonRoleMatcher getMultipolygonRoleMatcher() {
        if (roleMatcher == null) {
            roleMatcher = new MultipolygonRoleMatcher();
            if (Main.pref != null){
                roleMatcher.initFromPreferences();
                Main.pref.addPreferenceChangeListener(roleMatcher);
            }
        }
        return roleMatcher;
    }

    public static class JoinedWay {
        private final List<Node> nodes;
        private final boolean selected;

        public JoinedWay(List<Node> nodes, boolean selected) {
            this.nodes = nodes;
            this.selected = selected;
        }

        public List<Node> getNodes() {
            return nodes;
        }

        public boolean isSelected() {
            return selected;
        }

        public boolean isClosed() {
            return nodes.isEmpty() || nodes.get(nodes.size() - 1).equals(nodes.get(0));
        }
    }

    public static class PolyData {
        public enum Intersection {INSIDE, OUTSIDE, CROSSING}

        public Polygon poly = new Polygon();
        public final boolean selected;
        private Point lastP;
        private Rectangle bounds;

        public PolyData(NavigatableComponent nc, JoinedWay joinedWay) {
            this(nc, joinedWay.getNodes(), joinedWay.isSelected());
        }

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

    private final List<Way> innerWays = new ArrayList<Way>();
    private final List<Way> outerWays = new ArrayList<Way>();
    private final List<PolyData> innerPolygons = new ArrayList<PolyData>();
    private final List<PolyData> outerPolygons = new ArrayList<PolyData>();
    private final List<PolyData> combinedPolygons = new ArrayList<PolyData>();

    public Multipolygon(NavigatableComponent nc, Relation r) {
        load(r, nc);
    }

    private void load(Relation r, NavigatableComponent nc) {
        MultipolygonRoleMatcher matcher = getMultipolygonRoleMatcher();

        // Fill inner and outer list with valid ways
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isDrawable()) {
                if(m.isWay()) {
                    Way w = m.getWay();

                    if(w.getNodesCount() < 2) {
                        continue;
                    }

                    if(matcher.isInnerRole(m.getRole())) {
                        innerWays.add(w);
                    } else if(matcher.isOuterRole(m.getRole())) {
                        outerWays.add(w);
                    } else if (!m.hasRole()) {
                        outerWays.add(w);
                    } // Remaining roles ignored
                } // Non ways ignored
            }
        }

        createPolygons(nc, innerWays, innerPolygons);
        createPolygons(nc, outerWays, outerPolygons);
        if (!outerPolygons.isEmpty()) {
            addInnerToOuters();
        }
    }

    private void createPolygons(NavigatableComponent nc, List<Way> ways, List<PolyData> result) {
        List<Way> waysToJoin = new ArrayList<Way>();
        for (Way way: ways) {
            if (way.isClosed()) {
                result.add(new PolyData(nc, way.getNodes(), way.isSelected()));
            } else {
                waysToJoin.add(way);
            }
        }

        for (JoinedWay jw: joinWays(waysToJoin)) {
            result.add(new PolyData(nc, jw));
        }
    }

    public static Collection<JoinedWay> joinWays(Collection<Way> join)
    {
        Collection<JoinedWay> res = new ArrayList<JoinedWay>();
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

            res.add(new JoinedWay(n, selected));
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

}
