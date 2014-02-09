// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public interface Selector {

    /**
     * Apply the selector to the primitive and check if it matches.
     *
     * @param env the Environment. env.mc and env.layer are read-only when matching a selector.
     * env.source is not needed. This method will set the matchingReferrers field of env as
     * a side effect! Make sure to clear it before invoking this method.
     * @return true, if the selector applies
     */
    public boolean matches(Environment env);

    public String getSubpart();

    public Range getRange();

    public static enum ChildOrParentSelectorType {
        CHILD, PARENT, ELEMENT_OF, CROSSING
    }

    /**
     * <p>Represents a child selector or a parent selector.</p>
     *
     * <p>In addition to the standard CSS notation for child selectors, JOSM also supports
     * an "inverse" notation:</p>
     * <pre>
     *    selector_a &gt; selector_b { ... }       // the standard notation (child selector)
     *    relation[type=route] &gt; way { ... }    // example (all ways of a route)
     *
     *    selector_a &lt; selector_b { ... }       // the inverse notation (parent selector)
     *    node[traffic_calming] &lt; way { ... }   // example (way that has a traffic calming node)
     * </pre>
     *
     */
    public static class ChildOrParentSelector implements Selector {
        public final Selector left;
        public final LinkSelector link;
        public final Selector right;
        public final ChildOrParentSelectorType type;

        /**
         *
         * @param a the first selector
         * @param b the second selector
         * @param type the selector type
         */
        public ChildOrParentSelector(Selector a, LinkSelector link, Selector b, ChildOrParentSelectorType type) {
            this.left = a;
            this.link = link;
            this.right = b;
            this.type = type;
        }

        /**
         * <p>Finds the first referrer matching {@link #left}</p>
         *
         * <p>The visitor works on an environment and it saves the matching
         * referrer in {@code e.parent} and its relative position in the
         * list referrers "child list" in {@code e.index}.</p>
         *
         * <p>If after execution {@code e.parent} is null, no matching
         * referrer was found.</p>
         *
         */
        private  class MatchingReferrerFinder extends AbstractVisitor{
            private Environment e;

            /**
             * Constructor
             * @param e the environment against which we match
             */
            public MatchingReferrerFinder(Environment e){
                this.e = e;
            }

            @Override
            public void visit(Node n) {
                // node should never be a referrer
                throw new AssertionError();
            }

            @Override
            public void visit(Way w) {
                /*
                 * If e.parent is already set to the first matching referrer. We skip any following
                 * referrer injected into the visitor.
                 */
                if (e.parent != null) return;

                if (!left.matches(e.withPrimitive(w)))
                    return;
                for (int i=0; i<w.getNodesCount(); i++) {
                    Node n = w.getNode(i);
                    if (n.equals(e.osm)) {
                        if (link.matches(e.withParentAndIndexAndLinkContext(w, i))) {
                            e.parent = w;
                            e.index = i;
                            return;
                        }
                    }
                }
            }

            @Override
            public void visit(Relation r) {
                /*
                 * If e.parent is already set to the first matching referrer. We skip any following
                 * referrer injected into the visitor.
                 */
                if (e.parent != null) return;

                if (!left.matches(e.withPrimitive(r)))
                    return;
                for (int i=0; i < r.getMembersCount(); i++) {
                    RelationMember m = r.getMember(i);
                    if (m.getMember().equals(e.osm)) {
                        if (link.matches(e.withParentAndIndexAndLinkContext(r, i))) {
                            e.parent = r;
                            e.index = i;
                            return;
                        }
                    }
                }
            }
        }

        private abstract class AbstractFinder extends AbstractVisitor {
            protected final Environment e;

            protected AbstractFinder(Environment e) {
                this.e = e;
            }

            @Override
            public void visit(Node n) {
            }

            @Override
            public void visit(Way w) {
            }

            @Override
            public void visit(Relation r) {
            }

            public void visit(Collection<? extends OsmPrimitive> primitives) {
                for (OsmPrimitive p : primitives) {
                    if (e.child != null) {
                        // abort if first match has been found
                        break;
                    } else if (!e.osm.equals(p) && p.isUsable()) {
                        p.accept(this);
                    }
                }
            }
        }

        private final class CrossingFinder extends AbstractFinder {
            private CrossingFinder(Environment e) {
                super(e);
                CheckParameterUtil.ensureThat(e.osm instanceof Way, "Only ways are supported");
            }

            @Override
            public void visit(Way w) {
                if (e.child == null && left.matches(e.withPrimitive(w))) {
                    if (e.osm instanceof Way && Geometry.PolygonIntersection.CROSSING.equals(Geometry.polygonIntersection(w.getNodes(), ((Way) e.osm).getNodes()))) {
                        e.child = w;
                    }
                }
            }
        }

        private final class ContainsFinder extends AbstractFinder {
            private ContainsFinder(Environment e) {
                super(e);
                CheckParameterUtil.ensureThat(!(e.osm instanceof Node), "Nodes not supported");
            }

            @Override
            public void visit(Node n) {
                if (e.child == null && left.matches(e.withPrimitive(n))) {
                    if (e.osm instanceof Way && Geometry.nodeInsidePolygon(n, ((Way) e.osm).getNodes())
                            || e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon() && Geometry.isNodeInsideMultiPolygon(n, (Relation) e.osm, null)) {
                        e.child = n;
                    }
                }
            }

            @Override
            public void visit(Way w) {
                if (e.child == null && left.matches(e.withPrimitive(w))) {
                    if (e.osm instanceof Way && Geometry.PolygonIntersection.FIRST_INSIDE_SECOND.equals(Geometry.polygonIntersection(w.getNodes(), ((Way) e.osm).getNodes()))
                            || e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon() && Geometry.isPolygonInsideMultiPolygon(w.getNodes(), (Relation) e.osm, null)) {
                        e.child = w;
                    }
                }
            }
        }

        @Override
        public boolean matches(Environment e) {

            if (!right.matches(e))
                return false;

            if (ChildOrParentSelectorType.ELEMENT_OF.equals(type)) {

                if (e.osm instanceof Node || e.osm.getDataSet() == null) {
                    // nodes cannot contain elements
                    return false;
                }
                e.parent = e.osm;

                final ContainsFinder containsFinder = new ContainsFinder(e);
                if (right instanceof GeneralSelector) {
                    if (((GeneralSelector) right).matchesBase(OsmPrimitiveType.NODE)) {
                        containsFinder.visit(e.osm.getDataSet().searchNodes(e.osm.getBBox()));
                    }
                    if (((GeneralSelector) right).matchesBase(OsmPrimitiveType.WAY)) {
                        containsFinder.visit(e.osm.getDataSet().searchWays(e.osm.getBBox()));
                    }
                } else {
                    // use slow test
                    containsFinder.visit(e.osm.getDataSet().allPrimitives());
                }

                return e.child != null;

            } else if (ChildOrParentSelectorType.CROSSING.equals(type) && e.osm instanceof Way) {
                e.parent = e.osm;
                final CrossingFinder crossingFinder = new CrossingFinder(e);
                if (((GeneralSelector) right).matchesBase(OsmPrimitiveType.WAY)) {
                    crossingFinder.visit(e.osm.getDataSet().searchWays(e.osm.getBBox()));
                }
                return e.child != null;
            } else if (ChildOrParentSelectorType.CHILD.equals(type)) {
                MatchingReferrerFinder collector = new MatchingReferrerFinder(e);
                e.osm.visitReferrers(collector);
                if (e.parent != null)
                    return true;
            } else if (ChildOrParentSelectorType.PARENT.equals(type)) {
                if (e.osm instanceof Way) {
                    List<Node> wayNodes = ((Way) e.osm).getNodes();
                    for (int i=0; i<wayNodes.size(); i++) {
                        Node n = wayNodes.get(i);
                        if (left.matches(e.withPrimitive(n))) {
                            if (link.matches(e.withChildAndIndexAndLinkContext(n, i))) {
                                e.child = n;
                                e.index = i;
                                return true;
                            }
                        }
                    }
                }
                else if (e.osm instanceof Relation) {
                    List<RelationMember> members = ((Relation) e.osm).getMembers();
                    for (int i=0; i<members.size(); i++) {
                        OsmPrimitive member = members.get(i).getMember();
                        if (left.matches(e.withPrimitive(member))) {
                            if (link.matches(e.withChildAndIndexAndLinkContext(member, i))) {
                                e.child = member;
                                e.index = i;
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public String getSubpart() {
            return right.getSubpart();
        }

        @Override
        public Range getRange() {
            return right.getRange();
        }

        @Override
        public String toString() {
            return left + " " + (ChildOrParentSelectorType.PARENT.equals(type) ? "<" : ">") + link + " " + right;
        }
    }

    /**
     * Super class of {@link GeneralSelector} and {@link LinkSelector}
     * @since 5841
     */
    public static abstract class AbstractSelector implements Selector {

        protected final List<Condition> conds;

        protected AbstractSelector(List<Condition> conditions) {
            if (conditions == null || conditions.isEmpty()) {
                this.conds = null;
            } else {
                this.conds = conditions;
            }
        }

        /**
         * Determines if all conditions match the given environment.
         * @param env The environment to check
         * @return {@code true} if all conditions apply, false otherwise.
         */
        public final boolean matchesConditions(Environment env) {
            if (conds == null) return true;
            for (Condition c : conds) {
                try {
                    if (!c.applies(env)) return false;
                } catch (PatternSyntaxException e) {
                    Main.error("PatternSyntaxException while applying condition" + c +": "+e.getMessage());
                    return false;
                }
            }
            return true;
        }

        public List<Condition> getConditions() {
            if (conds == null) {
                return Collections.emptyList();
            }
            return Collections.unmodifiableList(conds);
        }
    }

    public static class LinkSelector extends AbstractSelector {

        public LinkSelector(List<Condition> conditions) {
            super(conditions);
        }

        @Override
        public boolean matches(Environment env) {
            Utils.ensure(env.isLinkContext(), "Requires LINK context in environment, got ''{0}''", env.getContext());
            return matchesConditions(env);
        }

        @Override
        public String getSubpart() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Range getRange() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public String toString() {
            return "LinkSelector{" + "conditions=" + conds + '}';
        }
    }

    public static class GeneralSelector extends AbstractSelector {
        public final String base;
        public final Range range;
        public final String subpart;

        public GeneralSelector(String base, Pair<Integer, Integer> zoom, List<Condition> conds, String subpart) {
            super(conds);
            this.base = base;
            if (zoom != null) {
                int a = zoom.a == null ? 0 : zoom.a;
                int b = zoom.b == null ? Integer.MAX_VALUE : zoom.b;
                if (a <= b) {
                    range = fromLevel(a, b);
                } else {
                    range = Range.ZERO_TO_INFINITY;
                }
            } else {
                range = Range.ZERO_TO_INFINITY;
            }
            this.subpart = subpart;
        }

        @Override
        public String getSubpart() {
            return subpart;
        }
        @Override
        public Range getRange() {
            return range;
        }

        public boolean matchesBase(OsmPrimitiveType type) {
            if (base.equals("*")) {
                return true;
            } else if (OsmPrimitiveType.NODE.equals(type)) {
                return base.equals("node");
            } else if (OsmPrimitiveType.WAY.equals(type)) {
                return base.equals("way") || base.equals("area");
            } else if (OsmPrimitiveType.RELATION.equals(type)) {
                return base.equals("area") || base.equals("relation") || base.equals("canvas");
            }
            return false;
        }

        public boolean matchesBase(OsmPrimitive p) {
            if (!matchesBase(p.getType())) {
                return false;
            } else {
                if (p instanceof Relation) {
                    if (base.equals("area")) {
                        return ((Relation) p).isMultipolygon();
                    } else if (base.equals("canvas")) {
                        return p.get("#canvas") != null;
                    }
                }
                return true;
            }
        }

        public boolean matchesBase(Environment e) {
            return matchesBase(e.osm);
        }

        @Override
        public boolean matches(Environment e) {
            return matchesBase(e) && matchesConditions(e);
        }

        public String getBase() {
            return base;
        }

        public static Range fromLevel(int a, int b) {
            if (a > b)
                throw new AssertionError();
            double lower = 0;
            double upper = Double.POSITIVE_INFINITY;
            if (b != Integer.MAX_VALUE) {
                lower = level2scale(b + 1);
            }
            if (a != 0) {
                upper = level2scale(a);
            }
            return new Range(lower, upper);
        }

        final static double R = 6378135;

        public static double level2scale(int lvl) {
            if (lvl < 0)
                throw new IllegalArgumentException();
            // preliminary formula - map such that mapnik imagery tiles of the same
            // or similar level are displayed at the given scale
            return 2.0 * Math.PI * R / Math.pow(2.0, lvl) / 2.56;
        }

        @Override
        public String toString() {
            return base + (Range.ZERO_TO_INFINITY.equals(range) ? "" : range) + Utils.join("", conds) + (subpart != null ? ("::" + subpart) : "");
        }
    }
}
