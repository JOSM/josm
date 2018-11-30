// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation.tests;

import static org.openstreetmap.josm.data.validation.tests.CrossingWays.HIGHWAY;
import static org.openstreetmap.josm.data.validation.tests.CrossingWays.RAILWAY;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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

    /**
     * Unconnected highways test.
     */
    public static class UnconnectedHighways extends UnconnectedWays {
        static final int UNCONNECTED_HIGHWAYS = 1311;

        /**
         * Constructs a new {@code UnconnectedHighways} test.
         */
        public UnconnectedHighways() {
            super(tr("Unconnected highways"), UNCONNECTED_HIGHWAYS);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey(HIGHWAY);
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
            super(tr("Unconnected railways"), UNCONNECTED_RAILWAYS);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("railway");
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
            super(tr("Unconnected waterways"), UNCONNECTED_WATERWAYS);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("waterway");
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
            super(tr("Unconnected natural lands and landuses"), UNCONNECTED_NATURAL_OR_LANDUSE);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasKey("natural", "landuse");
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
            super(tr("Unconnected power ways"), UNCONNECTED_POWER);
        }

        @Override
        public boolean isPrimitiveUsable(OsmPrimitive p) {
            return super.isPrimitiveUsable(p) && p.hasTag("power", "line", "minor_line", "cable");
        }
    }

    protected static final int UNCONNECTED_WAYS = 1301;
    protected static final String PREFIX = ValidatorPrefHelper.PREFIX + "." + UnconnectedWays.class.getSimpleName();

    private Set<MyWaySegment> ways;
    private QuadBuckets<Node> endnodes; // nodes at end of way
    private QuadBuckets<Node> endnodesHighway; // nodes at end of way
    private QuadBuckets<Node> middlenodes; // nodes in middle of way
    private Set<Node> othernodes; // nodes appearing at least twice
    private Area dsArea;

    private double mindist;
    private double minmiddledist;

    /**
     * Constructs a new {@code UnconnectedWays} test.
     * @param title The test title
     * @since 6691
     */
    public UnconnectedWays(String title) {
        this(title, UNCONNECTED_WAYS);

    }

    /**
     * Constructs a new {@code UnconnectedWays} test with the given code.
     * @param title The test title
     * @param code The test code
     * @since 14468
     */
    public UnconnectedWays(String title, int code) {
        super(title, tr("This test checks if a way has an endpoint very near to another way."));
        this.code = code;
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        ways = new HashSet<>();
        endnodes = new QuadBuckets<>();
        endnodesHighway = new QuadBuckets<>();
        middlenodes = new QuadBuckets<>();
        othernodes = new HashSet<>();
        mindist = Config.getPref().getDouble(PREFIX + ".node_way_distance", 10.0);
        minmiddledist = Config.getPref().getDouble(PREFIX + ".way_way_distance", 0.0);
        DataSet dataSet = OsmDataManager.getInstance().getEditDataSet();
        dsArea = dataSet == null ? null : dataSet.getDataSourceArea();
    }

    protected Map<Node, Way> getWayEndNodesNearOtherHighway() {
        Map<Node, Way> map = new HashMap<>();
        for (int iter = 0; iter < 1; iter++) {
            for (MyWaySegment s : ways) {
                if (isCanceled()) {
                    map.clear();
                    return map;
                }
                if (s.highway) {
                    for (Node en : s.nearbyNodes(mindist)) {
                        if (en == null || !endnodesHighway.contains(en)) {
                            continue;
                        }
                        if (en.hasTag(HIGHWAY, "turning_circle", "bus_stop")
                                || en.hasTag("amenity", "parking_entrance")
                                || en.hasTag(RAILWAY, "buffer_stop")
                                || en.isKeyTrue("noexit")
                                || en.hasKey("entrance", "barrier")) {
                            continue;
                        }
                        // to handle intersections of 't' shapes and similar
                        if (en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                            continue;
                        }
                        map.put(en, s.w);
                    }
                }
            }
        }
        return map;
    }

    protected Map<Node, Way> getWayEndNodesNearOtherWay() {
        Map<Node, Way> map = new HashMap<>();
        for (MyWaySegment s : ways) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node en : s.nearbyNodes(mindist)) {
                if (en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                    continue;
                }
                if (!s.highway && endnodesHighway.contains(en) && !s.w.concernsArea()) {
                    map.put(en, s.w);
                } else if (endnodes.contains(en) && !s.w.concernsArea()) {
                    map.put(en, s.w);
                }
            }
        }
        return map;
    }

    protected Map<Node, Way> getWayNodesNearOtherWay() {
        Map<Node, Way> map = new HashMap<>();
        for (MyWaySegment s : ways) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node en : s.nearbyNodes(minmiddledist)) {
                if (en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                    continue;
                }
                if (!middlenodes.contains(en)) {
                    continue;
                }
                map.put(en, s.w);
            }
        }
        return map;
    }

    protected Map<Node, Way> getConnectedWayEndNodesNearOtherWay() {
        Map<Node, Way> map = new HashMap<>();
        for (MyWaySegment s : ways) {
            if (isCanceled()) {
                map.clear();
                return map;
            }
            for (Node en : s.nearbyNodes(minmiddledist)) {
                if (en.isConnectedTo(s.w.getNodes(), 3 /* hops */, null)) {
                    continue;
                }
                if (!othernodes.contains(en)) {
                    continue;
                }
                map.put(en, s.w);
            }
        }
        return map;
    }

    protected final void addErrors(Severity severity, Map<Node, Way> errorMap, String message) {
        for (Map.Entry<Node, Way> error : errorMap.entrySet()) {
            errors.add(TestError.builder(this, severity, code)
                    .message(message)
                    .primitives(error.getKey(), error.getValue())
                    .highlight(error.getKey())
                    .build());
        }
    }

    @Override
    public void endTest() {
        addErrors(Severity.WARNING, getWayEndNodesNearOtherHighway(), tr("Way end node near other highway"));
        addErrors(Severity.WARNING, getWayEndNodesNearOtherWay(), tr("Way end node near other way"));
        /* the following two use a shorter distance */
        if (minmiddledist > 0.0) {
            addErrors(Severity.OTHER, getWayNodesNearOtherWay(), tr("Way node near other way"));
            addErrors(Severity.OTHER, getConnectedWayEndNodesNearOtherWay(), tr("Connected way end node near other way"));
        }
        ways = null;
        endnodes = null;
        endnodesHighway = null;
        middlenodes = null;
        othernodes = null;
        dsArea = null;
        super.endTest();
    }

    private class MyWaySegment {
        private final Line2D line;
        public final Way w;
        public final boolean isAbandoned;
        public final boolean isBoundary;
        public final boolean highway;
        private final double len;
        private Set<Node> nearbyNodeCache;
        private double nearbyNodeCacheDist = -1.0;
        private final Node n1;
        private final Node n2;

        MyWaySegment(Way w, Node n1, Node n2) {
            this.w = w;
            String railway = w.get(RAILWAY);
            String highway = w.get(HIGHWAY);
            this.isAbandoned = "abandoned".equals(railway) || w.isKeyTrue("disused");
            this.highway = (highway != null || railway != null) && !isAbandoned;
            this.isBoundary = !this.highway && w.hasTag("boundary", "administrative");
            line = new Line2D.Double(n1.getEastNorth().east(), n1.getEastNorth().north(),
                    n2.getEastNorth().east(), n2.getEastNorth().north());
            len = line.getP1().distance(line.getP2());
            this.n1 = n1;
            this.n2 = n2;
        }

        public boolean nearby(Node n, double dist) {
            if (w == null) {
                Logging.debug("way null");
                return false;
            }
            if (w.containsNode(n))
                return false;
            if (n.isKeyTrue("noexit"))
                return false;
            EastNorth coord = n.getEastNorth();
            if (coord == null)
                return false;
            Point2D p = new Point2D.Double(coord.east(), coord.north());
            if (line.getP1().distance(p) > len+dist)
                return false;
            if (line.getP2().distance(p) > len+dist)
                return false;
            return line.ptSegDist(p) < dist;
        }

        public List<LatLon> getBounds(double fudge) {
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
            List<LatLon> ret = new ArrayList<>(2);
            ret.add(topLeft);
            ret.add(botRight);
            return ret;
        }

        public Collection<Node> nearbyNodes(double dist) {
            // If you're looking for nodes that are farther away that we looked for last time,
            // the cached result is no good
            if (dist > nearbyNodeCacheDist) {
                nearbyNodeCache = null;
            }
            if (nearbyNodeCache != null) {
                // If we've cached an area greater than the
                // one now being asked for...
                if (nearbyNodeCacheDist > dist) {
                    // Used the cached result and trim out
                    // the nodes that are not in the smaller
                    // area, but keep the old larger cache.
                    Set<Node> trimmed = new HashSet<>(nearbyNodeCache);
                    Set<Node> initial = new HashSet<>(nearbyNodeCache);
                    for (Node n : initial) {
                        if (!nearby(n, dist)) {
                            trimmed.remove(n);
                        }
                    }
                    return trimmed;
                }
                return nearbyNodeCache;
            }
            /*
             * We know that any point near the line must be at
             * least as close as the other end of the line, plus
             * a little fudge for the distance away ('dist').
             */

            // This needs to be a hash set because the searches
            // overlap a bit and can return duplicate nodes.
            nearbyNodeCache = null;
            List<LatLon> bounds = this.getBounds(dist * (360.0d / (Ellipsoid.WGS84.a * 2 * Math.PI)));
            List<Node> foundNodes = endnodesHighway.search(new BBox(bounds.get(0), bounds.get(1)));
            foundNodes.addAll(endnodes.search(new BBox(bounds.get(0), bounds.get(1))));

            for (Node n : foundNodes) {
                if (!nearby(n, dist) || !n.getCoor().isIn(dsArea)) {
                    continue;
                }
                // It is actually very rare for us to find a node
                // so defer as much of the work as possible, like
                // allocating the hash set
                if (nearbyNodeCache == null) {
                    nearbyNodeCache = new HashSet<>();
                }
                nearbyNodeCache.add(n);
            }
            nearbyNodeCacheDist = dist;
            if (nearbyNodeCache == null) {
                nearbyNodeCache = Collections.emptySet();
            }
            return nearbyNodeCache;
        }
    }

    List<MyWaySegment> getWaySegments(Way w) {
        List<MyWaySegment> ret = new ArrayList<>();
        if (!w.isUsable()
                || w.hasKey("barrier")
                || w.hasTag("natural", "cliff"))
            return ret;

        int size = w.getNodesCount();
        if (size < 2)
            return ret;
        for (int i = 1; i < size; ++i) {
            if (i < size-1) {
                addNode(w.getNode(i), middlenodes);
            }
            Node a = w.getNode(i-1);
            Node b = w.getNode(i);
            if (a.isDrawable() && b.isDrawable()) {
                MyWaySegment ws = new MyWaySegment(w, a, b);
                if (ws.isBoundary || ws.isAbandoned) {
                    continue;
                }
                ret.add(ws);
            }
        }
        return ret;
    }

    @Override
    public void visit(Way w) {
        // do not consider empty ways
        if (w.getNodesCount() > 0
                // ignore addr:interpolation ways as they are not physical features and most of
                // the time very near the associated highway, which is perfectly normal, see #9332
                && !w.hasKey("addr:interpolation")
                // similarly for public transport platforms, tree rows
                && !w.hasTag(HIGHWAY, "platform") && !w.hasTag(RAILWAY, "platform", "platform_edge") && !w.hasTag("natural", "tree_row")
                ) {
            ways.addAll(getWaySegments(w));
            QuadBuckets<Node> set = endnodes;
            if (w.hasKey(HIGHWAY, RAILWAY)) {
                set = endnodesHighway;
            }
            addNode(w.firstNode(), set);
            addNode(w.lastNode(), set);
        }
    }

    private void addNode(Node n, QuadBuckets<Node> s) {
        boolean m = middlenodes.contains(n);
        boolean e = endnodes.contains(n);
        boolean eh = endnodesHighway.contains(n);
        boolean o = othernodes.contains(n);
        if (!m && !e && !o && !eh) {
            s.add(n);
        } else if (!o) {
            othernodes.add(n);
            if (e) {
                endnodes.remove(n);
            } else if (eh) {
                endnodesHighway.remove(n);
            } else {
                middlenodes.remove(n);
            }
        }
    }
}
