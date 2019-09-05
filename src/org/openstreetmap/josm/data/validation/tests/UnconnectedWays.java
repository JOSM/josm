// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmDataManager;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.QuadBuckets;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.projection.Ellipsoid;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;

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
            return p.hasKey(RAILWAY) && !p.hasTag(RAILWAY, "abandoned");
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
            return p.hasKey("natural", "landuse") && !p.hasTag("natural", "tree_row", "cliff");
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

    protected Map<Node, MyWaySegment> getWayEndNodesNearOtherHighway() {
        Map<Node, MyWaySegment> map = new HashMap<>();
        for (MyWaySegment s : waySegments) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node en : s.nearbyNodes(mindist)) {
                if (en.hasTag(HIGHWAY, "turning_circle", "bus_stop")
                        || en.hasTag("amenity", "parking_entrance")
                        || en.hasTag(RAILWAY, "buffer_stop")
                        || en.isKeyTrue("noexit")
                        || en.hasKey("entrance", "barrier")) {
                    continue;
                }
                // to handle intersections of 't' shapes and similar
                if (!en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                    addIfNewOrCloser(map, en, s);
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
                for (Node en : s.nearbyNodes(mindist)) {
                    if (!en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                        addIfNewOrCloser(map, en, s);
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
                if (!en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                    addIfNewOrCloser(map, en, s);
                }
            }
        }
        return map;
    }

    private void addIfNewOrCloser(Map<Node, MyWaySegment> map, Node node, MyWaySegment ws) {
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
            if (partialSelection && !nodesToTest.contains(node) && !waysToTest.contains(ws.w))
                continue;
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
            if (w.isUsable() && isCandidate(w) && w.getRealNodesCount() > 1
                    // don't complain about highways ending near platforms
                    && !w.hasTag(HIGHWAY, "platform") && !w.hasTag(RAILWAY, "platform", "platform_edge")
                    ) {
                waySegments.addAll(getWaySegments(w));
                addNode(w.firstNode(), endnodes);
                addNode(w.lastNode(), endnodes);
            }
        }
        searchNodes = new QuadBuckets<>();
        searchNodes.addAll(endnodes);
        if (isHighwayTest) {
            addErrors(Severity.WARNING, getWayEndNodesNearOtherHighway(), tr("Way end node near other highway"));
        } else {
            addErrors(Severity.WARNING, getWayEndNodesNearOtherWay(), tr("Way end node near other way"));
        }

        /* the following two use a shorter distance */
        boolean includeOther = isBeforeUpload ? ValidatorPrefHelper.PREF_OTHER_UPLOAD.get() : ValidatorPrefHelper.PREF_OTHER.get();
        if (minmiddledist > 0.0 && includeOther) {
            searchNodes.clear();
            searchNodes.addAll(middlenodes);
            addErrors(Severity.OTHER, getWayNodesNearOtherWay(), tr("Way node near other way"));
            searchNodes.clear();
            searchNodes.addAll(othernodes);
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

        double getDist(Node n) {
            EastNorth coord = n.getEastNorth();
            if (coord == null)
                return Double.NaN;
            EastNorth en1 = n1.getEastNorth();
            EastNorth en2 = n2.getEastNorth();
            return Line2D.ptSegDist(en1.getX(), en1.getY(), en2.getX(), en2.getY(), coord.getX(), coord.getY());

        }

        boolean nearby(Node n, double dist) {
            if (w.containsNode(n))
                return false;
            if (n.isKeyTrue("noexit"))
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
                if (!nearby(n, dist) || !n.getCoor().isIn(dsArea)) {
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
