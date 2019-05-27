// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint.relations;

import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.event.NodeMovedEvent;
import org.openstreetmap.josm.data.osm.event.WayNodesChangedEvent;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData.Intersection;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.AreaAndPerimeter;
import org.openstreetmap.josm.tools.Logging;

/**
 * Multipolygon data used to represent complex areas, see <a href="https://wiki.openstreetmap.org/wiki/Relation:multipolygon">wiki</a>.
 * @since 2788
 */
public class Multipolygon {

    /** preference key for a collection of roles which indicate that the respective member belongs to an
     * <em>outer</em> polygon. Default is <code>outer</code>.
     */
    public static final String PREF_KEY_OUTER_ROLES = "mappaint.multipolygon.outer.roles";

    /** preference key for collection of role prefixes which indicate that the respective
     *  member belongs to an <em>outer</em> polygon. Default is empty.
     */
    public static final String PREF_KEY_OUTER_ROLE_PREFIXES = "mappaint.multipolygon.outer.role-prefixes";

    /** preference key for a collection of roles which indicate that the respective member belongs to an
     * <em>inner</em> polygon. Default is <code>inner</code>.
     */
    public static final String PREF_KEY_INNER_ROLES = "mappaint.multipolygon.inner.roles";

    /** preference key for collection of role prefixes which indicate that the respective
     *  member belongs to an <em>inner</em> polygon. Default is empty.
     */
    public static final String PREF_KEY_INNER_ROLE_PREFIXES = "mappaint.multipolygon.inner.role-prefixes";

    /**
     * <p>Kind of strategy object which is responsible for deciding whether a given
     * member role indicates that the member belongs to an <em>outer</em> or an
     * <em>inner</em> polygon.</p>
     *
     * <p>The decision is taken based on preference settings, see the four preference keys
     * above.</p>
     */
    private static class MultipolygonRoleMatcher implements PreferenceChangedListener {
        private final List<String> outerExactRoles = new ArrayList<>();
        private final List<String> outerRolePrefixes = new ArrayList<>();
        private final List<String> innerExactRoles = new ArrayList<>();
        private final List<String> innerRolePrefixes = new ArrayList<>();

        private void initDefaults() {
            outerExactRoles.clear();
            outerRolePrefixes.clear();
            innerExactRoles.clear();
            innerRolePrefixes.clear();
            outerExactRoles.add("outer");
            innerExactRoles.add("inner");
        }

        private static void setNormalized(Collection<String> literals, List<String> target) {
            target.clear();
            for (String l: literals) {
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
            if (Config.getPref() == null) return;
            Collection<String> literals;
            literals = Config.getPref().getList(PREF_KEY_OUTER_ROLES);
            if (literals != null && !literals.isEmpty()) {
                setNormalized(literals, outerExactRoles);
            }
            literals = Config.getPref().getList(PREF_KEY_OUTER_ROLE_PREFIXES);
            if (literals != null && !literals.isEmpty()) {
                setNormalized(literals, outerRolePrefixes);
            }
            literals = Config.getPref().getList(PREF_KEY_INNER_ROLES);
            if (literals != null && !literals.isEmpty()) {
                setNormalized(literals, innerExactRoles);
            }
            literals = Config.getPref().getList(PREF_KEY_INNER_ROLE_PREFIXES);
            if (literals != null && !literals.isEmpty()) {
                setNormalized(literals, innerRolePrefixes);
            }
        }

        @Override
        public void preferenceChanged(PreferenceChangeEvent evt) {
            if (PREF_KEY_INNER_ROLE_PREFIXES.equals(evt.getKey()) ||
                    PREF_KEY_INNER_ROLES.equals(evt.getKey()) ||
                    PREF_KEY_OUTER_ROLE_PREFIXES.equals(evt.getKey()) ||
                    PREF_KEY_OUTER_ROLES.equals(evt.getKey())) {
                initFromPreferences();
            }
        }

        boolean isOuterRole(String role) {
            if (role == null) return false;
            for (String candidate: outerExactRoles) {
                if (role.equals(candidate)) return true;
            }
            for (String candidate: outerRolePrefixes) {
                if (role.startsWith(candidate)) return true;
            }
            return false;
        }

        boolean isInnerRole(String role) {
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
     * Init a private global matcher object which will listen to preference changes.
     */
    private static MultipolygonRoleMatcher roleMatcher;

    private static synchronized MultipolygonRoleMatcher getMultipolygonRoleMatcher() {
        if (roleMatcher == null) {
            roleMatcher = new MultipolygonRoleMatcher();
            if (Config.getPref() != null) {
                roleMatcher.initFromPreferences();
                Config.getPref().addPreferenceChangeListener(roleMatcher);
            }
        }
        return roleMatcher;
    }

    /**
     * Class representing a string of ways.
     *
     * The last node of one way is the first way of the next one.
     * The string may or may not be closed.
     */
    public static class JoinedWay {
        protected final List<Node> nodes;
        protected final Collection<Long> wayIds;
        protected boolean selected;

        /**
         * Constructs a new {@code JoinedWay}.
         * @param nodes list of nodes - must not be null
         * @param wayIds list of way IDs - must not be null
         * @param selected whether joined way is selected or not
         */
        public JoinedWay(List<Node> nodes, Collection<Long> wayIds, boolean selected) {
            this.nodes = new ArrayList<>(nodes);
            this.wayIds = new ArrayList<>(wayIds);
            this.selected = selected;
        }

        /**
         * Replies the list of nodes.
         * @return the list of nodes
         */
        public List<Node> getNodes() {
            return Collections.unmodifiableList(nodes);
        }

        /**
         * Replies the list of way IDs.
         * @return the list of way IDs
         */
        public Collection<Long> getWayIds() {
            return Collections.unmodifiableCollection(wayIds);
        }

        /**
         * Determines if this is selected.
         * @return {@code true} if this is selected
         */
        public final boolean isSelected() {
            return selected;
        }

        /**
         * Sets whether this is selected
         * @param selected {@code true} if this is selected
         * @since 10312
         */
        public final void setSelected(boolean selected) {
            this.selected = selected;
        }

        /**
         * Determines if this joined way is closed.
         * @return {@code true} if this joined way is closed
         */
        public boolean isClosed() {
            return nodes.isEmpty() || getLastNode().equals(getFirstNode());
        }

        /**
         * Returns the first node.
         * @return the first node
         * @since 10312
         */
        public Node getFirstNode() {
            return nodes.get(0);
        }

        /**
         * Returns the last node.
         * @return the last node
         * @since 10312
         */
        public Node getLastNode() {
            return nodes.get(nodes.size() - 1);
        }
    }

    /**
     * The polygon data for a multipolygon part.
     * It contains the outline of this polygon in east/north space.
     */
    public static class PolyData extends JoinedWay {
        /**
         * The intersection type used for {@link PolyData#contains(java.awt.geom.Path2D.Double)}
         */
        public enum Intersection {
            /**
             * The polygon is completely inside this PolyData
             */
            INSIDE,
            /**
             * The polygon is completely outside of this PolyData
             */
            OUTSIDE,
            /**
             * The polygon is partially inside and outside of this PolyData
             */
            CROSSING
        }

        private final Path2D.Double poly;
        private Rectangle2D bounds;
        private final List<PolyData> inners;

        /**
         * Constructs a new {@code PolyData} from a closed way.
         * @param closedWay closed way
         */
        public PolyData(Way closedWay) {
            this(closedWay.getNodes(), closedWay.isSelected(), Collections.singleton(closedWay.getUniqueId()));
        }

        /**
         * Constructs a new {@code PolyData} from a {@link JoinedWay}.
         * @param joinedWay joined way
         */
        public PolyData(JoinedWay joinedWay) {
            this(joinedWay.nodes, joinedWay.selected, joinedWay.wayIds);
        }

        private PolyData(List<Node> nodes, boolean selected, Collection<Long> wayIds) {
            super(nodes, wayIds, selected);
            this.inners = new ArrayList<>();
            this.poly = new Path2D.Double();
            this.poly.setWindingRule(Path2D.WIND_EVEN_ODD);
            buildPoly();
        }

        /**
         * Constructs a new {@code PolyData} from an existing {@code PolyData}.
         * @param copy existing instance
         */
        public PolyData(PolyData copy) {
            super(copy.nodes, copy.wayIds, copy.selected);
            this.poly = (Path2D.Double) copy.poly.clone();
            this.inners = new ArrayList<>(copy.inners);
        }

        private void buildPoly() {
            boolean initial = true;
            for (Node n : nodes) {
                EastNorth p = n.getEastNorth();
                if (p != null) {
                    if (initial) {
                        poly.moveTo(p.getX(), p.getY());
                        initial = false;
                    } else {
                        poly.lineTo(p.getX(), p.getY());
                    }
                }
            }
            if (nodes.size() >= 3 && nodes.get(0) == nodes.get(nodes.size() - 1)) {
                poly.closePath();
            }
            for (PolyData inner : inners) {
                appendInner(inner.poly);
            }
        }

        /**
         * Checks if this multipolygon contains or crosses an other polygon. This is a quick+lazy test which assumes
         * that a polygon is inside when all points are inside. It will fail when the polygon encloses a hole or crosses
         * the edges of poly so that both end points are inside poly (think of a square overlapping a U-shape).
         * @param p The path to check. Needs to be in east/north space.
         * @return a {@link Intersection} constant
         */
        public Intersection contains(Path2D.Double p) {
            int contains = 0;
            int total = 0;
            double[] coords = new double[6];
            for (PathIterator it = p.getPathIterator(null); !it.isDone(); it.next()) {
                switch (it.currentSegment(coords)) {
                    case PathIterator.SEG_MOVETO:
                    case PathIterator.SEG_LINETO:
                        if (poly.contains(coords[0], coords[1])) {
                            contains++;
                        }
                        total++;
                        break;
                    default: // Do nothing
                }
            }
            if (contains == total) return Intersection.INSIDE;
            if (contains == 0) return Intersection.OUTSIDE;
            return Intersection.CROSSING;
        }

        /**
         * Adds an inner polygon
         * @param inner The polygon to add as inner polygon.
         */
        public void addInner(PolyData inner) {
            inners.add(inner);
            appendInner(inner.poly);
        }

        private void appendInner(Path2D.Double inner) {
            poly.append(inner.getPathIterator(null), false);
        }

        /**
         * Gets the polygon outline and interior as java path
         * @return The path in east/north space.
         */
        public Path2D.Double get() {
            return poly;
        }

        /**
         * Gets the bounds as {@link Rectangle2D} in east/north space.
         * @return The bounds
         */
        public Rectangle2D getBounds() {
            if (bounds == null) {
                bounds = poly.getBounds2D();
            }
            return bounds;
        }

        /**
         * Gets a list of all inner polygons.
         * @return The inner polygons.
         */
        public List<PolyData> getInners() {
            return Collections.unmodifiableList(inners);
        }

        private void resetNodes(DataSet dataSet) {
            if (!nodes.isEmpty()) {
                DataSet ds = dataSet;
                // Find DataSet (can be null for several nodes when undoing nodes creation, see #7162)
                for (Iterator<Node> it = nodes.iterator(); it.hasNext() && ds == null;) {
                    ds = it.next().getDataSet();
                }
                nodes.clear();
                if (ds == null) {
                    // DataSet still not found. This should not happen, but a warning does no harm
                    Logging.warn("DataSet not found while resetting nodes in Multipolygon. " +
                            "This should not happen, you may report it to JOSM developers.");
                } else if (wayIds.size() == 1) {
                    Way w = (Way) ds.getPrimitiveById(wayIds.iterator().next(), OsmPrimitiveType.WAY);
                    nodes.addAll(w.getNodes());
                } else if (!wayIds.isEmpty()) {
                    List<Way> waysToJoin = new ArrayList<>();
                    for (Long wayId : wayIds) {
                        Way w = (Way) ds.getPrimitiveById(wayId, OsmPrimitiveType.WAY);
                        if (w != null && w.getNodesCount() > 0) { // fix #7173 (empty ways on purge)
                            waysToJoin.add(w);
                        }
                    }
                    if (!waysToJoin.isEmpty()) {
                        nodes.addAll(joinWays(waysToJoin).iterator().next().getNodes());
                    }
                }
                resetPoly();
            }
        }

        private void resetPoly() {
            poly.reset();
            buildPoly();
            bounds = null;
        }

        /**
         * Check if this polygon was changed by a node move
         * @param event The node move event
         */
        public void nodeMoved(NodeMovedEvent event) {
            final Node n = event.getNode();
            boolean innerChanged = false;
            for (PolyData inner : inners) {
                if (inner.nodes.contains(n)) {
                    inner.resetPoly();
                    innerChanged = true;
                }
            }
            if (nodes.contains(n) || innerChanged) {
                resetPoly();
            }
        }

        /**
         * Check if this polygon was affected by a way change
         * @param event The way event
         */
        public void wayNodesChanged(WayNodesChangedEvent event) {
            final Long wayId = event.getChangedWay().getUniqueId();
            boolean innerChanged = false;
            for (PolyData inner : inners) {
                if (inner.wayIds.contains(wayId)) {
                    inner.resetNodes(event.getDataset());
                    innerChanged = true;
                }
            }
            if (wayIds.contains(wayId) || innerChanged) {
                resetNodes(event.getDataset());
            }
        }

        @Override
        public boolean isClosed() {
            if (nodes.size() < 3 || !getFirstNode().equals(getLastNode()))
                return false;
            for (PolyData inner : inners) {
                if (!inner.isClosed())
                    return false;
            }
            return true;
        }

        /**
         * Calculate area and perimeter length in the given projection.
         *
         * @param projection the projection to use for the calculation, {@code null} defaults to {@link ProjectionRegistry#getProjection()}
         * @return area and perimeter
         */
        public AreaAndPerimeter getAreaAndPerimeter(Projection projection) {
            AreaAndPerimeter ap = Geometry.getAreaAndPerimeter(nodes, projection);
            double area = ap.getArea();
            double perimeter = ap.getPerimeter();
            for (PolyData inner : inners) {
                AreaAndPerimeter apInner = inner.getAreaAndPerimeter(projection);
                area -= apInner.getArea();
                perimeter += apInner.getPerimeter();
            }
            return new AreaAndPerimeter(area, perimeter);
        }
    }

    private final List<Way> innerWays = new ArrayList<>();
    private final List<Way> outerWays = new ArrayList<>();
    private final List<PolyData> combinedPolygons = new ArrayList<>();
    private final List<Node> openEnds = new ArrayList<>();

    private boolean incomplete;

    /**
     * Constructs a new {@code Multipolygon} from a relation.
     * @param r relation
     */
    public Multipolygon(Relation r) {
        load(r);
    }

    private void load(Relation r) {
        MultipolygonRoleMatcher matcher = getMultipolygonRoleMatcher();

        // Fill inner and outer list with valid ways
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete()) {
                this.incomplete = true;
            } else if (m.getMember().isDrawable() && m.isWay()) {
                Way w = m.getWay();

                if (w.getNodesCount() < 2) {
                    continue;
                }

                if (matcher.isInnerRole(m.getRole())) {
                    innerWays.add(w);
                } else if (!m.hasRole() || matcher.isOuterRole(m.getRole())) {
                    outerWays.add(w);
                } // Remaining roles ignored
            } // Non ways ignored
        }

        final List<PolyData> innerPolygons = new ArrayList<>();
        final List<PolyData> outerPolygons = new ArrayList<>();
        createPolygons(innerWays, innerPolygons);
        createPolygons(outerWays, outerPolygons);
        if (!outerPolygons.isEmpty()) {
            addInnerToOuters(innerPolygons, outerPolygons);
        }
    }

    /**
     * Determines if this multipolygon is incomplete.
     * @return {@code true} if this multipolygon is incomplete
     */
    public final boolean isIncomplete() {
        return incomplete;
    }

    private void createPolygons(List<Way> ways, List<PolyData> result) {
        List<Way> waysToJoin = new ArrayList<>();
        for (Way way: ways) {
            if (way.isClosed()) {
                result.add(new PolyData(way));
            } else {
                waysToJoin.add(way);
            }
        }

        for (JoinedWay jw: joinWays(waysToJoin)) {
            result.add(new PolyData(jw));
            if (!jw.isClosed()) {
                openEnds.add(jw.getFirstNode());
                openEnds.add(jw.getLastNode());
            }
        }
    }

    /**
     * Attempt to combine the ways in the list if they share common end nodes
     * @param waysToJoin The ways to join
     * @return A collection of {@link JoinedWay} objects indicating the possible join of those ways
     */
    public static Collection<JoinedWay> joinWays(Collection<Way> waysToJoin) {
        final Collection<JoinedWay> result = new ArrayList<>();
        final Way[] joinArray = waysToJoin.toArray(new Way[0]);
        int left = waysToJoin.size();
        while (left > 0) {
            Way w = null;
            boolean selected = false;
            List<Node> nodes = null;
            Set<Long> wayIds = new HashSet<>();
            boolean joined = true;
            while (joined && left > 0) {
                joined = false;
                for (int i = 0; i < joinArray.length && left != 0; ++i) {
                    if (joinArray[i] != null) {
                        Way c = joinArray[i];
                        if (c.getNodesCount() == 0) {
                            continue;
                        }
                        if (w == null) {
                            w = c;
                            selected = w.isSelected();
                            joinArray[i] = null;
                            --left;
                        } else {
                            int mode = 0;
                            int cl = c.getNodesCount()-1;
                            int nl;
                            if (nodes == null) {
                                nl = w.getNodesCount()-1;
                                if (w.getNode(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if (w.getNode(nl) == c.getNode(cl)) {
                                    mode = 22;
                                } else if (w.getNode(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if (w.getNode(0) == c.getNode(cl)) {
                                    mode = 12;
                                }
                            } else {
                                nl = nodes.size()-1;
                                if (nodes.get(nl) == c.getNode(0)) {
                                    mode = 21;
                                } else if (nodes.get(0) == c.getNode(cl)) {
                                    mode = 12;
                                } else if (nodes.get(0) == c.getNode(0)) {
                                    mode = 11;
                                } else if (nodes.get(nl) == c.getNode(cl)) {
                                    mode = 22;
                                }
                            }
                            if (mode != 0) {
                                joinArray[i] = null;
                                joined = true;
                                if (c.isSelected()) {
                                    selected = true;
                                }
                                --left;
                                if (nodes == null) {
                                    nodes = w.getNodes();
                                    wayIds.add(w.getUniqueId());
                                }
                                nodes.remove((mode == 21 || mode == 22) ? nl : 0);
                                if (mode == 21) {
                                    nodes.addAll(c.getNodes());
                                } else if (mode == 12) {
                                    nodes.addAll(0, c.getNodes());
                                } else if (mode == 22) {
                                    for (Node node : c.getNodes()) {
                                        nodes.add(nl, node);
                                    }
                                } else /* mode == 11 */ {
                                    for (Node node : c.getNodes()) {
                                        nodes.add(0, node);
                                    }
                                }
                                wayIds.add(c.getUniqueId());
                            }
                        }
                    }
                }
            }

            if (nodes == null && w != null) {
                nodes = w.getNodes();
                wayIds.add(w.getUniqueId());
            }

            if (nodes != null) {
                result.add(new JoinedWay(nodes, wayIds, selected));
            }
        }

        return result;
    }

    /**
     * Find a matching outer polygon for the inner one
     * @param inner The inner polygon to search the outer for
     * @param outerPolygons The possible outer polygons
     * @return The outer polygon that was found or <code>null</code> if none was found.
     */
    public PolyData findOuterPolygon(PolyData inner, List<PolyData> outerPolygons) {
        // First try to test only bbox, use precise testing only if we don't get unique result
        Rectangle2D innerBox = inner.getBounds();
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

        PolyData result = null;
        for (PolyData combined : outerPolygons) {
            if (combined.contains(inner.poly) != Intersection.OUTSIDE
                    && (result == null || result.contains(combined.poly) == Intersection.INSIDE)) {
                result = combined;
            }
        }
        return result;
    }

    private void addInnerToOuters(List<PolyData> innerPolygons, List<PolyData> outerPolygons) {
        if (innerPolygons.isEmpty()) {
            combinedPolygons.addAll(outerPolygons);
        } else if (outerPolygons.size() == 1) {
            PolyData combinedOuter = new PolyData(outerPolygons.get(0));
            for (PolyData inner: innerPolygons) {
                combinedOuter.addInner(inner);
            }
            combinedPolygons.add(combinedOuter);
        } else {
            for (PolyData outer: outerPolygons) {
                combinedPolygons.add(new PolyData(outer));
            }

            for (PolyData pdInner: innerPolygons) {
                Optional.ofNullable(findOuterPolygon(pdInner, combinedPolygons)).orElseGet(() -> outerPolygons.get(0))
                    .addInner(pdInner);
            }
        }
    }

    /**
     * Replies the list of outer ways.
     * @return the list of outer ways
     */
    public List<Way> getOuterWays() {
        return Collections.unmodifiableList(outerWays);
    }

    /**
     * Replies the list of inner ways.
     * @return the list of inner ways
     */
    public List<Way> getInnerWays() {
        return Collections.unmodifiableList(innerWays);
    }

    /**
     * Replies the list of combined polygons.
     * @return the list of combined polygons
     */
    public List<PolyData> getCombinedPolygons() {
        return Collections.unmodifiableList(combinedPolygons);
    }

    /**
     * Replies the list of inner polygons.
     * @return the list of inner polygons
     */
    public List<PolyData> getInnerPolygons() {
        final List<PolyData> innerPolygons = new ArrayList<>();
        createPolygons(innerWays, innerPolygons);
        return innerPolygons;
    }

    /**
     * Replies the list of outer polygons.
     * @return the list of outer polygons
     */
    public List<PolyData> getOuterPolygons() {
        final List<PolyData> outerPolygons = new ArrayList<>();
        createPolygons(outerWays, outerPolygons);
        return outerPolygons;
    }

    /**
     * Returns the start and end node of non-closed rings.
     * @return the start and end node of non-closed rings.
     */
    public List<Node> getOpenEnds() {
        return Collections.unmodifiableList(openEnds);
    }
}
