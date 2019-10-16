// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;

/**
 * Checks if a way has an endpoint very near to another way.
 * <br>
 * This class is abstract since highway/railway/waterway/… ways must be handled separately.
 * An actual implementation must override {@link #isPrimitiveUsable(OsmPrimitive)}
 * to denote which kind of primitives can be handled.
 *
 * @author frsantos
 */
public abstract class UnconnectedWays extends Test {
    private final int code;
    private final boolean isHighwayTest;

    protected abstract boolean isCandidate(OsmPrimitive p);

    protected boolean isWantedWay(Way w) {
        return w.isUsable() && isCandidate(w);
    }

    /**
     * Check if unconnected end node should be ignored.
     * @param n the node
     * @return true if node should be ignored
     */
    protected boolean ignoreUnconnectedEndNode(Node n) {
        return false;
    }

    @Override
    public boolean isPrimitiveUsable(OsmPrimitive p) {
        return super.isPrimitiveUsable(p) && ((partialSelection && p instanceof Node) || isCandidate(p));
    }

    /**
     * Unconnected highways test.
     */
    public static class UnconnectedHighways extends UnconnectedWays {
        static final int UNCONNECTED_HIGHWAYS = 1311;

        /**
         * Constructs a new {@code UnconnectedHighways} test.
         */
        public UnconnectedHighways() {
            super(tr("Unconnected highways"), UNCONNECTED_HIGHWAYS, true);
        }

        @Override
        protected boolean isCandidate(OsmPrimitive p) {
            return p.hasKey(HIGHWAY);
        }

        @Override
        protected boolean ignoreUnconnectedEndNode(Node n) {
            return n.hasTag(HIGHWAY, "turning_circle", "bus_stop")
                    || n.hasTag("amenity", "parking_entrance")
                    || n.isKeyTrue("noexit")
                    || n.hasKey("entrance", "barrier")
                    || n.getParentWays().stream().anyMatch(Test::isBuilding);
        }
    }

    /**
     * Unconnected railways test.
     */
    public static class UnconnectedRailways extends UnconnectedWays {
        static final int UNCONNECTED_RAILWAYS = 1321;
        /**
         * Constructs a new {@code UnconnectedRailways} test.
         */
        public UnconnectedRailways() {
            super(tr("Unconnected railways"), UNCONNECTED_RAILWAYS, false);
        }

        @Override
        protected boolean isCandidate(OsmPrimitive p) {
            return p.hasTagDifferent(RAILWAY, "abandoned");
        }

        @Override
        protected boolean ignoreUnconnectedEndNode(Node n) {
            return n.hasTag(RAILWAY, "buffer_stop");
        }
    }

    /**
     * Unconnected waterways test.
     */
    public static class UnconnectedWaterways extends UnconnectedWays {
        static final int UNCONNECTED_WATERWAYS = 1331;
        /**
         * Constructs a new {@code UnconnectedWaterways} test.
         */
        public UnconnectedWaterways() {
            super(tr("Unconnected waterways"), UNCONNECTED_WATERWAYS, false);
        }

        @Override
        protected boolean isCandidate(OsmPrimitive p) {
            return p.hasKey("waterway");
        }
    }

    /**
     * Unconnected natural/landuse test.
     */
    public static class UnconnectedNaturalOrLanduse extends UnconnectedWays {
        static final int UNCONNECTED_NATURAL_OR_LANDUSE = 1341;
        /**
         * Constructs a new {@code UnconnectedNaturalOrLanduse} test.
         */
        public UnconnectedNaturalOrLanduse() {
            super(tr("Unconnected natural lands and landuses"), UNCONNECTED_NATURAL_OR_LANDUSE, false);
        }

        @Override
        protected boolean isCandidate(OsmPrimitive p) {
            return p.hasKey("landuse") || p.hasTagDifferent("natural", "tree_row", "cliff");
        }
    }

    /**
     * Unconnected power ways test.
     */
    public static class UnconnectedPower extends UnconnectedWays {
        static final int UNCONNECTED_POWER = 1351;
        /**
         * Constructs a new {@code UnconnectedPower} test.
         */
        public UnconnectedPower() {
            super(tr("Unconnected power ways"), UNCONNECTED_POWER, false);
        }

        @Override
        protected boolean isCandidate(OsmPrimitive p) {
            return p.hasTag("power", "line", "minor_line", "cable");
        }

        @Override
        protected boolean ignoreUnconnectedEndNode(Node n) {
            return n.hasTag("power", "terminal");
        }
    }

    protected static final int UNCONNECTED_WAYS = 1301;
    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + UnconnectedWays.class.getSimpleName();

    private List<MyWaySegment> waySegments;
    private Set<Node> endnodes; // nodes at end of way
    private Set<Node> middlenodes; // nodes in middle of way
    private Set<Node> othernodes; // nodes appearing at least twice
    private QuadBuckets<Node> searchNodes;
    private Set<Way> waysToTest;
    private Set<Node> nodesToTest;
    private Area dsArea;

    private double mindist;
    private double minmiddledist;
    private double maxLen; // maximum length of allowed detour to reach the unconnected node
    private DataSet ds;

    /**
     * Constructs a new {@code UnconnectedWays} test.
     * @param title The test title
     * @since 6691
     */
    public UnconnectedWays(String title) {
        this(title, UNCONNECTED_WAYS, false);

    }

    /**
     * Constructs a new {@code UnconnectedWays} test with the given code.
     * @param title The test title
     * @param code The test code
     * @param isHighwayTest use {@code true} if test concerns highways or railways
     * @since 14468
     */
    public UnconnectedWays(String title, int code, boolean isHighwayTest) {
        super(title, tr("This test checks if a way has an endpoint very near to another way."));
        this.code = code;
        this.isHighwayTest = isHighwayTest;
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        waySegments = new ArrayList<>();
        searchNodes = new QuadBuckets<>();
        waysToTest = new HashSet<>();
        nodesToTest = new HashSet<>();
        endnodes = new HashSet<>();
        middlenodes = new HashSet<>();
        othernodes = new HashSet<>();
        mindist = Config.getPref().getDouble(PREFIX + ".node_way_distance", 10.0);
        minmiddledist = Config.getPref().getDouble(PREFIX + ".way_way_distance", 0.0);
        ds = OsmDataManager.getInstance().getEditDataSet();
        dsArea = ds == null ? null : ds.getDataSourceArea();
    }

    protected Map<Node, MyWaySegment> getHighwayEndNodesNearOtherHighway() {
        Map<Node, MyWaySegment> map = new HashMap<>();
        for (MyWaySegment s : waySegments) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node endnode : s.nearbyNodes(mindist)) {
                Way parentWay = getWantedParentWay(endnode);
                if (parentWay != null) {
                    if (!Objects.equals(OsmUtils.getLayer(s.w), OsmUtils.getLayer(parentWay))) {
                        continue; // ignore ways with different layer tag
                    }

                    // to handle intersections of 't' shapes and similar
                    if (!s.isConnectedTo(endnode)) {
                        if (parentWay.hasTag(HIGHWAY, "platform") || s.w.hasTag(HIGHWAY, "platform")
                                || s.barrierBetween(endnode)) {
                            continue;
                        }
                        addIfNewOrCloser(map, endnode, s);
                    }
                }
            }
        }
        return map;
    }

    protected Map<Node, MyWaySegment> getWayEndNodesNearOtherWay() {
        Map<Node, MyWaySegment> map = new HashMap<>();

        for (MyWaySegment s : waySegments) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            if (!s.concernsArea) {
                for (Node endnode : s.nearbyNodes(mindist)) {
                    if (!s.isConnectedTo(endnode)) {
                        if (s.w.hasTag("power")) {
                            boolean badConnection = false;
                            Way otherWay = getWantedParentWay(endnode);
                            if (otherWay != null) {
                                for (String key : Arrays.asList("voltage", "frequency")) {
                                    String v1 = s.w.get(key);
                                    String v2 = otherWay.get(key);
                                    if (v1 != null && v2 != null && !v1.equals(v2)) {
                                        badConnection = true;
                                    }
                                }
                            }
                            if (badConnection)
                                continue;
                        }
                        addIfNewOrCloser(map, endnode, s);
                    }
                }
            }
        }
        return map;
    }

    protected Map<Node, MyWaySegment> getWayNodesNearOtherWay() {
        Map<Node, MyWaySegment> map = new HashMap<>();
        for (MyWaySegment s : waySegments) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node en : s.nearbyNodes(minmiddledist)) {
                if (!s.isConnectedTo(en)) {
                    addIfNewOrCloser(map, en, s);
                }
            }
        }
        return map;
    }

    /**
     * An unconnected node might have multiple parent ways, e.g. a highway and a landuse way.
     * Make sure we get the one that was analysed before.
     * @param endnode the node which is known to be an end node of the wanted way
     * @return the wanted way
     */
    private Way getWantedParentWay(Node endnode) {
        for (Way w : endnode.getParentWays()) {
            if (isWantedWay(w))
                return w;
        }
        Logging.error("end node without matching parent way");
        return null;
    }

    private void addIfNewOrCloser(Map<Node, MyWaySegment> map, Node node, MyWaySegment ws) {
        if (partialSelection && !nodesToTest.contains(node) && !waysToTest.contains(ws.w))
            return;
        MyWaySegment old = map.get(node);
        if (old != null) {
            double d1 = ws.getDist(node);
            double d2 = old.getDist(node);
            if (d1 > d2) {
                // keep old value
                return;
            }
        }
        map.put(node, ws);
    }

    protected final void addErrors(Severity severity, Map<Node, MyWaySegment> errorMap, String message) {
        for (Entry<Node, MyWaySegment> error : errorMap.entrySet()) {
            Node node = error.getKey();
            MyWaySegment ws = error.getValue();
            errors.add(TestError.builder(this, severity, code)
                    .message(message)
                    .primitives(node, ws.w)
                    .highlight(node)
                    .build());
        }
    }

    @Override
    public void endTest() {
        if (ds == null)
            return;

        for (Way w : ds.getWays()) {
            if (isWantedWay(w) && w.getRealNodesCount() > 1) {
                waySegments.addAll(getWaySegments(w));
                addNode(w.firstNode(), endnodes);
                addNode(w.lastNode(), endnodes);
            }
        }
        fillSearchNodes(endnodes);
        if (!searchNodes.isEmpty()) {
            maxLen = 4 * mindist;
            if (isHighwayTest) {
                addErrors(Severity.WARNING, getHighwayEndNodesNearOtherHighway(), tr("Way end node near other highway"));
            } else {
                addErrors(Severity.WARNING, getWayEndNodesNearOtherWay(), tr("Way end node near other way"));
            }
        }

        /* the following two should use a shorter distance */
        boolean includeOther = isBeforeUpload ? ValidatorPrefHelper.PREF_OTHER_UPLOAD.get() : ValidatorPrefHelper.PREF_OTHER.get();
        if (minmiddledist > 0.0 && includeOther) {
            maxLen = 4 * minmiddledist;
            fillSearchNodes(middlenodes);
            addErrors(Severity.OTHER, getWayNodesNearOtherWay(), tr("Way node near other way"));
            fillSearchNodes(othernodes);
            addErrors(Severity.OTHER, getWayNodesNearOtherWay(), tr("Connected way end node near other way"));
        }

        waySegments = null;
        endnodes = null;
        middlenodes = null;
        othernodes = null;
        searchNodes = null;
        dsArea = null;
        ds = null;
        super.endTest();
    }

    private void fillSearchNodes(Collection<Node> nodes) {
        searchNodes.clear();
        for (Node n : nodes) {
            if (!ignoreUnconnectedEndNode(n) && n.getCoor().isIn(dsArea)) {
                searchNodes.add(n);
            }
        }
    }

    private class MyWaySegment {
        public final Way w;
        private final Node n1;
        private final Node n2;
        private final boolean concernsArea;

        MyWaySegment(Way w, Node n1, Node n2, boolean concersArea) {
            this.w = w;
            this.n1 = n1;
            this.n2 = n2;
            this.concernsArea = concersArea;
        }

        /**
         * Check if the given node is connected to this segment using a reasonable short way.
         * @param startNode the node
         * @return true if a reasonable connection was found
         */
        boolean isConnectedTo(Node startNode) {
            return isConnectedTo(startNode, null, new HashSet<>(), 0);
        }

        /**
         * Check if the given node is connected to this segment using a reasonable short way.
         * @param node the given node
         * @param startWay previously visited way or null if first
         * @param visited set of visited nodes
         * @param len length of the travelled route
         * @return true if a reasonable connection was found
         */
        boolean isConnectedTo(Node node, Way startWay, Set<Node> visited, double len) {
            if (n1 == node || n2 == node) {
                return true;
            }
            if (len > maxLen) {
                return false;
            }
            if (visited != null) {
                visited.add(node);
                for (final Way way : node.getParentWays()) {
                    if (isWantedWay(way)) {
                        List<Node> nextNodes = new ArrayList<>();
                        int pos = way.getNodes().indexOf(node);
                        if (pos > 0) {
                            nextNodes.add(way.getNode(pos - 1));
                        }
                        if (pos + 1 < way.getNodesCount()) {
                            nextNodes.add(way.getNode(pos + 1));
                        }
                        for (Node next : nextNodes) {
                            final boolean containsN = visited.contains(next);
                            visited.add(next);
                            if (!containsN && isConnectedTo(next, way, visited,
                                    len + node.getCoor().greatCircleDistance(next.getCoor()))) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }

        double getDist(Node n) {
            EastNorth coord = n.getEastNorth();
            if (coord == null)
                return Double.NaN;
            EastNorth closest = Geometry.closestPointToSegment(n1.getEastNorth(), n2.getEastNorth(), coord);
            Node x = new Node();
            x.setEastNorth(closest);
            return x.getCoor().greatCircleDistance(n.getCoor());

        }

        boolean nearby(Node n, double dist) {
            if (w.containsNode(n))
                return false;
            double d = getDist(n);
            return !Double.isNaN(d) && d < dist;
        }

        BBox getBounds(double fudge) {
            double x1 = n1.getCoor().lon();
            double x2 = n2.getCoor().lon();
            if (x1 > x2) {
                double tmpx = x1;
                x1 = x2;
                x2 = tmpx;
            }
            double y1 = n1.getCoor().lat();
            double y2 = n2.getCoor().lat();
            if (y1 > y2) {
                double tmpy = y1;
                y1 = y2;
                y2 = tmpy;
            }
            LatLon topLeft = new LatLon(y2+fudge, x1-fudge);
            LatLon botRight = new LatLon(y1-fudge, x2+fudge);
            return new BBox(topLeft, botRight);
        }

        Collection<Node> nearbyNodes(double dist) {
            /*
             * We know that any point near the line segment must be at
             * least as close as the other end of the line, plus
             * a little fudge for the distance away ('dist').
             */

            BBox bounds = this.getBounds(dist * (360.0d / (Ellipsoid.WGS84.a * 2 * Math.PI)));
            List<Node> result = null;
            List<Node> foundNodes = searchNodes.search(bounds);
            for (Node n : foundNodes) {
                if (!nearby(n, dist)) {
                    continue;
                }
                // It is actually very rare for us to find a node
                // so defer as much of the work as possible, like
                // allocating the hash set
                if (result == null) {
                    result = new ArrayList<>();
                }
                result.add(n);
            }
            return result == null ? Collections.emptyList() : result;
        }

        private boolean barrierBetween(Node endnode) {
            EastNorth en = endnode.getEastNorth();
            EastNorth closest = Geometry.closestPointToSegment(n1.getEastNorth(), n2.getEastNorth(), en);
            Node x = new Node(closest);
            BBox bbox = new BBox(endnode.getCoor(), x.getCoor());
            for (Way nearbyWay : ds.searchWays(bbox)) {
                if (nearbyWay != w && nearbyWay.isUsable() && nearbyWay.hasTag("barrier")
                        && !endnode.getParentWays().contains(nearbyWay)) {
                    //make sure that the barrier is really between endnode and the highway segment, not just close to or around them
                    Iterator<Node> iter = nearbyWay.getNodes().iterator();
                    EastNorth prev = iter.next().getEastNorth();
                    while (iter.hasNext()) {
                        EastNorth curr = iter.next().getEastNorth();
                        if (Geometry.getSegmentSegmentIntersection(closest, en, prev, curr) != null) {
                            return true;
                        }
                        prev = curr;
                    }
                }
            }
            return false;
        }

    }

    List<MyWaySegment> getWaySegments(Way w) {
        List<MyWaySegment> ret = new ArrayList<>();
        if (!w.isUsable() || w.isKeyTrue("disused"))
            return ret;

        int size = w.getNodesCount();
        boolean concersArea = w.concernsArea();
        for (int i = 1; i < size; ++i) {
            if (i < size-1) {
                addNode(w.getNode(i), middlenodes);
            }
            Node a = w.getNode(i-1);
            Node b = w.getNode(i);
            if (a.isDrawable() && b.isDrawable()) {
                MyWaySegment ws = new MyWaySegment(w, a, b, concersArea);
                ret.add(ws);
            }
        }
        return ret;
    }

    @Override
    public void visit(Way w) {
        if (partialSelection) {
            waysToTest.add(w);
        }
    }

    @Override
    public void visit(Node n) {
        if (partialSelection) {
            nodesToTest.add(n);
        }
    }

    private void addNode(Node n, Set<Node> s) {
        boolean m = middlenodes.contains(n);
        boolean e = endnodes.contains(n);
        boolean o = othernodes.contains(n);
        if (!m && !e && !o) {
            s.add(n);
        } else if (!o) {
            othernodes.add(n);
            if (e) {
                endnodes.remove(n);
            } else {
                middlenodes.remove(n);
            }
        }
    }
}
