// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.data.projection.Ellipsoid.WGS84;

import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.IndexCondition;
import org.openstreetmap.josm.gui.mappaint.mapcss.ConditionFactory.OpenEndPseudoClassCondition;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.CompositeList;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.PolygonIntersection;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * MapCSS selector.
 *
 * A rule has two parts, a selector and a declaration block
 * e.g.
 * <pre>
 * way[highway=residential]
 * { width: 10; color: blue; }
 * </pre>
 *
 * The selector decides, if the declaration block gets applied or not.
 *
 * All implementing classes of Selector are immutable.
 */
public interface Selector {

    /** selector base that matches anything. */
    String BASE_ANY = "*";

    /** selector base that matches on OSM object node. */
    String BASE_NODE = "node";

    /** selector base that matches on OSM object way. */
    String BASE_WAY = "way";

    /** selector base that matches on OSM object relation. */
    String BASE_RELATION = "relation";

    /** selector base that matches with any area regardless of whether the area border is only modelled with a single way or with
     * a set of ways glued together with a relation.*/
    String BASE_AREA = "area";

    /** selector base for special rules containing meta information. */
    String BASE_META = "meta";

    /** selector base for style information not specific to nodes, ways or relations. */
    String BASE_CANVAS = "canvas";

    /** selector base for artificial bases created to use preferences. */
    String BASE_SETTING = "setting";

    /** selector base for grouping settings. */
    String BASE_SETTINGS = "settings";

    /**
     * Apply the selector to the primitive and check if it matches.
     *
     * @param env the Environment. env.mc and env.layer are read-only when matching a selector.
     * env.source is not needed. This method will set the matchingReferrers field of env as
     * a side effect! Make sure to clear it before invoking this method.
     * @return true, if the selector applies
     */
    boolean matches(Environment env);

    /**
     * Returns the subpart, if supported. A subpart identifies different rendering layers (<code>::subpart</code> syntax).
     * @return the subpart, if supported
     * @throws UnsupportedOperationException if not supported
     */
    Subpart getSubpart();

    /**
     * Returns the scale range, an interval of the form "lower &lt; x &lt;= upper" where 0 &lt;= lower &lt; upper.
     * @return the scale range, if supported
     * @throws UnsupportedOperationException if not supported
     */
    Range getRange();

    String getBase();

    /**
     * Returns the list of conditions.
     * @return the list of conditions
     */
    List<Condition> getConditions();

    /**
     * The type of child of parent selector.
     * @see ChildOrParentSelector
     */
    enum ChildOrParentSelectorType {
        CHILD, PARENT, SUBSET_OR_EQUAL, NOT_SUBSET_OR_EQUAL, SUPERSET_OR_EQUAL, NOT_SUPERSET_OR_EQUAL, CROSSING, SIBLING,
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
     * <p>Child: see <a href="https://josm.openstreetmap.de/wiki/Help/Styles/MapCSSImplementation#Childselector">wiki</a>
     * <br>Parent: see <a href="https://josm.openstreetmap.de/wiki/Help/Styles/MapCSSImplementation#Parentselector">wiki</a></p>
     */
    class ChildOrParentSelector implements Selector {
        public final Selector left;
        public final LinkSelector link;
        public final Selector right;
        public final ChildOrParentSelectorType type;

        /**
         * Constructs a new {@code ChildOrParentSelector}.
         * @param a the first selector
         * @param link link
         * @param b the second selector
         * @param type the selector type
         */
        public ChildOrParentSelector(Selector a, LinkSelector link, Selector b, ChildOrParentSelectorType type) {
            CheckParameterUtil.ensureParameterNotNull(a, "a");
            CheckParameterUtil.ensureParameterNotNull(b, "b");
            CheckParameterUtil.ensureParameterNotNull(link, "link");
            CheckParameterUtil.ensureParameterNotNull(type, "type");
            this.left = a;
            this.link = link;
            this.right = b;
            this.type = type;
        }

        @Override
        public String getBase() {
            // take the base from the rightmost selector
            return right.getBase();
        }

        @Override
        public List<Condition> getConditions() {
            return new CompositeList<>(left.getConditions(), right.getConditions());
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
        private class MatchingReferrerFinder implements PrimitiveVisitor {
            private final Environment e;

            /**
             * Constructor
             * @param e the environment against which we match
             */
            MatchingReferrerFinder(Environment e) {
                this.e = e;
            }

            @Override
            public void visit(INode n) {
                // node should never be a referrer
                throw new AssertionError();
            }

            private <T extends IPrimitive> void doVisit(T parent, IntSupplier counter, IntFunction<IPrimitive> getter) {
                // If e.parent is already set to the first matching referrer.
                // We skip any following referrer injected into the visitor.
                if (e.parent != null) return;

                if (!left.matches(e.withPrimitive(parent)))
                    return;
                int count = counter.getAsInt();
                if (link.conds == null || link.conds.isEmpty()) {
                    // index is not needed, we can avoid the sequential search below
                    e.parent = parent;
                    e.count = count;
                    return;
                }
                // see #18964
                int step = firstAndLastOnly() ? count - 1 : 1;
                for (int i = 0; i < count; i += step) {
                    if (getter.apply(i).equals(e.osm)
                            && link.matches(e.withParentAndIndexAndLinkContext(parent, i, count))) {
                        e.parent = parent;
                        e.index = i;
                        e.count = count;
                        return;
                    }
                }
            }

            private boolean firstAndLastOnly() {
                for (Condition c : link.conds) {
                    if (!(c instanceof IndexCondition) || !((IndexCondition) c).isFirstOrLast) {
                        return false;
                    }
                }
                return true;
            }

            @Override
            public void visit(IWay<?> w) {
                doVisit(w, w::getNodesCount, w::getNode);
            }

            @Override
            public void visit(IRelation<?> r) {
                doVisit(r, r::getMembersCount, i -> r.getMember(i).getMember());
            }
        }

        private abstract static class AbstractFinder implements PrimitiveVisitor {
            protected final Environment e;

            protected AbstractFinder(Environment e) {
                this.e = e;
            }

            @Override
            public void visit(INode n) {
            }

            @Override
            public void visit(IWay<?> w) {
            }

            @Override
            public void visit(IRelation<?> r) {
            }

            public void visit(Collection<? extends IPrimitive> primitives) {
                for (IPrimitive p : primitives) {
                    if (e.child != null) {
                        // abort if first match has been found
                        break;
                    } else if (isPrimitiveUsable(p)) {
                        p.accept(this);
                    }
                }
            }

            public boolean isPrimitiveUsable(IPrimitive p) {
                return !e.osm.equals(p) && p.isUsable();
            }

            protected void addToChildren(Environment e, IPrimitive p) {
                if (e.children == null) {
                    e.children = new LinkedHashSet<>();
                }
                e.children.add(p);
            }
        }

        private class MultipolygonOpenEndFinder extends AbstractFinder {

            @Override
            public void visit(IWay<?> w) {
                w.visitReferrers(innerVisitor);
            }

            MultipolygonOpenEndFinder(Environment e) {
                super(e);
            }

            private final PrimitiveVisitor innerVisitor = new AbstractFinder(e) {
                @Override
                public void visit(IRelation<?> r) {
                    if (r instanceof Relation && left.matches(e.withPrimitive(r))) {
                        final List<?> openEnds = MultipolygonCache.getInstance().get((Relation) r).getOpenEnds();
                        final int openEndIndex = openEnds.indexOf(e.osm);
                        if (openEndIndex >= 0) {
                            e.parent = r;
                            e.index = openEndIndex;
                            e.count = openEnds.size();
                        }
                    }
                }
            };
        }

        private final class CrossingFinder extends AbstractFinder {

            private final String layer;
            private Area area;
            /** Will contain all way segments, grouped by cells */
            Map<Point2D, List<WaySegment>> cellSegments;

            private CrossingFinder(Environment e) {
                super(e);
                CheckParameterUtil.ensureThat(isArea(e.osm), "Only areas are supported");
                layer = OsmUtils.getLayer(e.osm);
            }

            private Area getAreaEastNorth(IPrimitive p, Environment e) {
                if (e.mpAreaCache != null && p.isMultipolygon()) {
                    Area a = e.mpAreaCache.get(p);
                    if (a == null) {
                        a = Geometry.getAreaEastNorth(p);
                        e.mpAreaCache.put(p, a);
                    }
                    return a;
                }
                return Geometry.getAreaEastNorth(p);
            }

            private Map<List<Way>, List<WaySegment>> findCrossings(IPrimitive area,
                    Map<Point2D, List<WaySegment>> cellSegments) {
                /** The detected crossing ways */
                Map<List<Way>, List<WaySegment>> crossingWays = new HashMap<>(50);
                if (area instanceof Way) {
                    CrossingWays.findIntersectingWay((Way) area, cellSegments, crossingWays, false);
                } else if (area instanceof Relation && area.isMultipolygon()) {
                    Relation r = (Relation) area;
                    for (Way w : r.getMemberPrimitives(Way.class)) {
                        if (!w.hasIncompleteNodes()) {
                            CrossingWays.findIntersectingWay(w, cellSegments, crossingWays, false);
                        }
                    }
                }
                return crossingWays;
            }

            @Override
            public void visit(Collection<? extends IPrimitive> primitives) {
                List<? extends IPrimitive> toIgnore;
                if (e.osm instanceof Relation) {
                    toIgnore = ((IRelation<?>) e.osm).getMemberPrimitivesList();
                } else {
                    toIgnore = null;
                }

                for (IPrimitive p : primitives) {
                    if (isPrimitiveUsable(p) && Objects.equals(layer, OsmUtils.getLayer(p))
                            && left.matches(new Environment(p).withParent(e.osm)) && isArea(p)
                            && (toIgnore == null || !toIgnore.contains(p))) {
                        visitArea(p);
                    }
                }
            }

            private void visitArea(IPrimitive p) {
                if (area == null) {
                    area = getAreaEastNorth(e.osm, e);
                }
                Area otherArea = getAreaEastNorth(p, e);
                if (area.isEmpty() || otherArea.isEmpty()) {
                    useFindCrossings(p);
                } else {
                    // we have complete data. This allows to find intersections with shared nodes
                    // See #16707
                    Pair<PolygonIntersection, Area> is = Geometry.polygonIntersectionResult(
                            otherArea, area, Geometry.INTERSECTION_EPS_EAST_NORTH);
                    if (Geometry.PolygonIntersection.CROSSING == is.a) {
                        addToChildren(e, p);
                        // store intersection area to improve highlight and zoom to problem
                        if (e.intersections == null) {
                            e.intersections = new HashMap<>();
                        }
                        e.intersections.put(p, is.b);
                    }
                }

            }

            private void useFindCrossings(IPrimitive p) {
                if (cellSegments == null) {
                    // lazy initialisation
                    cellSegments = new HashMap<>();
                    findCrossings(e.osm, cellSegments); // ignore self intersections etc. here
                }
                // need a copy
                final Map<Point2D, List<WaySegment>> tmpCellSegments = new HashMap<>(cellSegments);
                // calculate all crossings between e.osm and p
                Map<List<Way>, List<WaySegment>> crossingWays = findCrossings(p, tmpCellSegments);
                if (!crossingWays.isEmpty()) {
                    addToChildren(e, p);
                    if (e.crossingWaysMap == null) {
                        e.crossingWaysMap = new HashMap<>();
                    }
                    e.crossingWaysMap.put(p, crossingWays);
                }
            }
        }

        /**
         * Finds elements which are inside the right element, collects those in {@code children}
         */
        private class ContainsFinder extends AbstractFinder {
            protected List<IPrimitive> toCheck;

            protected ContainsFinder(Environment e) {
                super(e);
                CheckParameterUtil.ensureThat(!(e.osm instanceof INode), "Nodes not supported");
            }

            @Override
            public void visit(Collection<? extends IPrimitive> primitives) {
                for (IPrimitive p : primitives) {
                    if (p != e.osm && isPrimitiveUsable(p) && left.matches(new Environment(p).withParent(e.osm))) {
                        if (toCheck == null) {
                            toCheck = new ArrayList<>();
                        }
                        toCheck.add(p);
                    }
                }
            }

            void execGeometryTests() {
                if (toCheck == null || toCheck.isEmpty())
                    return;
                for (IPrimitive p : Geometry.filterInsideAnyPolygon(toCheck, e.osm)) {
                    addToChildren(e, p);
                }
            }
        }

        /**
         * Finds elements which are inside the left element, or in other words, it finds elements enclosing e.osm.
         * The found enclosing elements are collected in {@code e.children}.
         */
        private class InsideOrEqualFinder extends AbstractFinder {

            protected InsideOrEqualFinder(Environment e) {
                super(e);
            }

            @Override
            public void visit(IWay<?> w) {
                if (left.matches(new Environment(w).withParent(e.osm))
                        && w.getBBox().bounds(e.osm.getBBox())
                        && !Geometry.filterInsidePolygon(Collections.singletonList(e.osm), w).isEmpty()) {
                    addToChildren(e, w);
                }
            }

            @Override
            public void visit(IRelation<?> r) {
                if (r instanceof Relation && r.isMultipolygon() && r.getBBox().bounds(e.osm.getBBox())
                        && left.matches(new Environment(r).withParent(e.osm))
                        && !Geometry.filterInsideMultipolygon(Collections.singletonList(e.osm), (Relation) r).isEmpty()) {
                    addToChildren(e, r);
                }
            }
        }

        private void visitBBox(Environment e, AbstractFinder finder) {
            boolean withNodes = finder instanceof ContainsFinder;
            if (e.osm.getDataSet() == null) {
                // do nothing
            } else if (left instanceof GeneralSelector) {
                if (withNodes && ((GeneralSelector) left).matchesBase(OsmPrimitiveType.NODE)) {
                    finder.visit(e.osm.getDataSet().searchNodes(e.osm.getBBox()));
                }
                if (((GeneralSelector) left).matchesBase(OsmPrimitiveType.WAY)) {
                    finder.visit(e.osm.getDataSet().searchWays(e.osm.getBBox()));
                }
                if (((GeneralSelector) left).matchesBase(OsmPrimitiveType.RELATION)) {
                    finder.visit(e.osm.getDataSet().searchRelations(e.osm.getBBox()));
                }
            } else {
                if (withNodes) {
                    finder.visit(e.osm.getDataSet().searchNodes(e.osm.getBBox()));
                }
                finder.visit(e.osm.getDataSet().searchWays(e.osm.getBBox()));
                finder.visit(e.osm.getDataSet().searchRelations(e.osm.getBBox()));
            }
        }

        private static boolean isArea(IPrimitive p) {
            return (p instanceof IWay && ((IWay<?>) p).isClosed() && ((IWay<?>) p).getNodesCount() >= 4)
                    || (p instanceof IRelation && p.isMultipolygon() && !p.isIncomplete());
        }

        @Override
        public boolean matches(Environment e) {

            if (!right.matches(e))
                return false;

            if (ChildOrParentSelectorType.SUBSET_OR_EQUAL == type || ChildOrParentSelectorType.NOT_SUBSET_OR_EQUAL == type) {

                if (e.osm.getDataSet() == null || !isArea(e.osm)) {
                    // only areas can contain elements
                    return ChildOrParentSelectorType.NOT_SUBSET_OR_EQUAL == type;
                }
                ContainsFinder containsFinder = new ContainsFinder(e);
                e.parent = e.osm;

                visitBBox(e, containsFinder);
                containsFinder.execGeometryTests();
                return ChildOrParentSelectorType.SUBSET_OR_EQUAL == type ? e.children != null : e.children == null;

            } else if (ChildOrParentSelectorType.SUPERSET_OR_EQUAL == type || ChildOrParentSelectorType.NOT_SUPERSET_OR_EQUAL == type) {

                if (e.osm.getDataSet() == null || (e.osm instanceof INode && ((INode) e.osm).getCoor() == null)
                        || (!(e.osm instanceof INode) && !isArea(e.osm))) {
                    return ChildOrParentSelectorType.NOT_SUPERSET_OR_EQUAL == type;
                }

                InsideOrEqualFinder insideOrEqualFinder = new InsideOrEqualFinder(e);
                e.parent = e.osm;

                visitBBox(e, insideOrEqualFinder);
                return ChildOrParentSelectorType.SUPERSET_OR_EQUAL == type ? e.children != null : e.children == null;

            } else if (ChildOrParentSelectorType.CROSSING == type) {
                e.parent = e.osm;
                if (e.osm.getDataSet() != null && isArea(e.osm)) {
                    final CrossingFinder crossingFinder = new CrossingFinder(e);
                    visitBBox(e, crossingFinder);
                    return e.children != null;
                }
                return e.children != null;
            } else if (ChildOrParentSelectorType.SIBLING == type) {
                if (e.osm instanceof INode) {
                    for (IPrimitive ref : e.osm.getReferrers(true)) {
                        if (ref instanceof IWay) {
                            IWay<?> w = (IWay<?>) ref;
                            final int i = w.getNodes().indexOf(e.osm);
                            if (i - 1 >= 0) {
                                final INode n = w.getNode(i - 1);
                                final Environment e2 = e.withPrimitive(n).withParent(w).withChild(e.osm);
                                if (left.matches(e2) && link.matches(e2.withLinkContext())) {
                                    e.child = n;
                                    e.index = i;
                                    e.count = w.getNodesCount();
                                    e.parent = w;
                                    return true;
                                }
                            }
                        }
                    }
                }
            } else if (ChildOrParentSelectorType.CHILD == type
                    && link.conds != null && !link.conds.isEmpty()
                    && link.conds.get(0) instanceof OpenEndPseudoClassCondition) {
                if (e.osm instanceof INode) {
                    e.osm.visitReferrers(new MultipolygonOpenEndFinder(e));
                    return e.parent != null;
                }
            } else if (ChildOrParentSelectorType.CHILD == type) {
                MatchingReferrerFinder collector = new MatchingReferrerFinder(e);
                e.osm.visitReferrers(collector);
                if (e.parent != null)
                    return true;
            } else if (ChildOrParentSelectorType.PARENT == type) {
                if (e.osm instanceof IWay) {
                    List<? extends INode> wayNodes = ((IWay<?>) e.osm).getNodes();
                    for (int i = 0; i < wayNodes.size(); i++) {
                        INode n = wayNodes.get(i);
                        if (left.matches(e.withPrimitive(n))
                            && link.matches(e.withChildAndIndexAndLinkContext(n, i, wayNodes.size()))) {
                            e.child = n;
                            e.index = i;
                            e.count = wayNodes.size();
                            return true;
                        }
                    }
                } else if (e.osm instanceof IRelation) {
                    List<? extends IRelationMember<?>> members = ((IRelation<?>) e.osm).getMembers();
                    for (int i = 0; i < members.size(); i++) {
                        IPrimitive member = members.get(i).getMember();
                        if (left.matches(e.withPrimitive(member))
                            && link.matches(e.withChildAndIndexAndLinkContext(member, i, members.size()))) {
                            e.child = member;
                            e.index = i;
                            e.count = members.size();
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        @Override
        public Subpart getSubpart() {
            return right.getSubpart();
        }

        @Override
        public Range getRange() {
            return right.getRange();
        }

        @Override
        public String toString() {
            return left.toString() + ' ' + (ChildOrParentSelectorType.PARENT == type ? '<' : '>') + link + ' ' + right;
        }
    }

    /**
     * Super class of {@link org.openstreetmap.josm.gui.mappaint.mapcss.Selector.GeneralSelector} and
     * {@link org.openstreetmap.josm.gui.mappaint.mapcss.Selector.LinkSelector}.
     * @since 5841
     */
    abstract class AbstractSelector implements Selector {

        protected final List<Condition> conds;

        protected AbstractSelector(List<Condition> conditions) {
            this.conds = Utils.toUnmodifiableList(conditions);
        }

        /**
         * Determines if all conditions match the given environment.
         * @param env The environment to check
         * @return {@code true} if all conditions apply, false otherwise.
         */
        @Override
        public boolean matches(Environment env) {
            CheckParameterUtil.ensureParameterNotNull(env, "env");
            for (Condition c : conds) {
                try {
                    if (!c.applies(env)) return false;
                } catch (PatternSyntaxException e) {
                    Logging.log(Logging.LEVEL_ERROR, "PatternSyntaxException while applying condition" + c + ':', e);
                    return false;
                }
            }
            return true;
        }

        @Override
        public List<Condition> getConditions() {
            return conds;
        }
    }

    /**
     * In a child selector, conditions on the link between a parent and a child object.
     * See <a href="https://josm.openstreetmap.de/wiki/Help/Styles/MapCSSImplementation#Linkselector">wiki</a>
     */
    class LinkSelector extends AbstractSelector {

        public LinkSelector(List<Condition> conditions) {
            super(conditions);
        }

        @Override
        public boolean matches(Environment env) {
            Utils.ensure(env.isLinkContext(), "Requires LINK context in environment, got ''{0}''", env.getContext());
            return super.matches(env);
        }

        @Override
        public String getBase() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Subpart getSubpart() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Range getRange() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return "LinkSelector{conditions=" + conds + '}';
        }
    }

    /**
     * General selector. See <a href="https://josm.openstreetmap.de/wiki/Help/Styles/MapCSSImplementation#Selectors">wiki</a>
     */
    class GeneralSelector extends AbstractSelector {

        public final String base;
        public final Range range;
        public final Subpart subpart;

        public GeneralSelector(String base, Range range, List<Condition> conds, Subpart subpart) {
            super(conds);
            this.base = checkBase(base);
            this.range = Objects.requireNonNull(range, "range");
            this.subpart = subpart != null ? subpart : Subpart.DEFAULT_SUBPART;
        }

        @Override
        public Subpart getSubpart() {
            return subpart;
        }

        @Override
        public Range getRange() {
            return range;
        }

        public boolean matchesConditions(Environment e) {
            return super.matches(e);
        }

        @Override
        public boolean matches(Environment e) {
            return matchesBase(e) && super.matches(e);
        }

        /**
         * Set base and check if this is a known value.
         * @param base value for base
         * @return the matching String constant for a known value
         * @throws IllegalArgumentException if value is not knwon
         */
        private static String checkBase(String base) {
            switch(base) {
            case "*": return BASE_ANY;
            case "node": return BASE_NODE;
            case "way": return BASE_WAY;
            case "relation": return BASE_RELATION;
            case "area": return BASE_AREA;
            case "meta": return BASE_META;
            case "canvas": return BASE_CANVAS;
            case "setting": return BASE_SETTING;
            case "settings": return BASE_SETTINGS;
            default:
                throw new IllegalArgumentException(MessageFormat.format("Unknown MapCSS base selector {0}", base));
            }
        }

        @Override
        public String getBase() {
            return base;
        }

        public boolean matchesBase(OsmPrimitiveType type) {
            if (BASE_ANY.equals(base)) {
                return true;
            } else if (OsmPrimitiveType.NODE == type) {
                return BASE_NODE.equals(base);
            } else if (OsmPrimitiveType.WAY == type) {
                return BASE_WAY.equals(base) || BASE_AREA.equals(base);
            } else if (OsmPrimitiveType.RELATION == type) {
                return BASE_AREA.equals(base) || BASE_RELATION.equals(base) || BASE_CANVAS.equals(base);
            }
            return false;
        }

        public boolean matchesBase(IPrimitive p) {
            if (!matchesBase(p.getType())) {
                return false;
            } else {
                if (p instanceof IRelation) {
                    if (BASE_AREA.equals(base)) {
                        return ((IRelation<?>) p).isMultipolygon();
                    } else if (BASE_CANVAS.equals(base)) {
                        return p.get("#canvas") != null;
                    }
                }
                return true;
            }
        }

        public boolean matchesBase(Environment e) {
            return matchesBase(e.osm);
        }

        public static Range fromLevel(int a, int b) {
            // for input validation in Range constructor below
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

        public static double level2scale(int lvl) {
            if (lvl < 0)
                throw new IllegalArgumentException("lvl must be >= 0 but is "+lvl);
            // preliminary formula - map such that mapnik imagery tiles of the same
            // or similar level are displayed at the given scale
            return 2.0 * Math.PI * WGS84.a / Math.pow(2.0, lvl) / 2.56;
        }

        public static int scale2level(double scale) {
            if (scale < 0)
                throw new IllegalArgumentException("scale must be >= 0 but is "+scale);
            return (int) Math.floor(Math.log(2 * Math.PI * WGS84.a / 2.56 / scale) / Math.log(2));
        }

        @Override
        public String toString() {
            return base
                    + (Range.ZERO_TO_INFINITY.equals(range) ? "" : range)
                    + (conds != null ? conds.stream().map(String::valueOf).collect(Collectors.joining("")) : "")
                    + (subpart != null && subpart != Subpart.DEFAULT_SUBPART ? ("::" + subpart) : "");
        }
    }
}
