// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.gui.preferences.ProjectionPreference;
import org.openstreetmap.josm.tools.Predicate;

/**
 * An component that can be navigated by a mapmover. Used as map view and for the
 * zoomer in the download dialog.
 *
 * @author imi
 */
public class NavigatableComponent extends JComponent implements Helpful {

    /**
     * Interface to notify listeners of the change of the zoom area.
     */
    public interface ZoomChangeListener {
        void zoomChanged();
    }

    public static final IntegerProperty PROP_SNAP_DISTANCE = new IntegerProperty("mappaint.node.snap-distance", 10);

    /**
     * the zoom listeners
     */
    private static final CopyOnWriteArrayList<ZoomChangeListener> zoomChangeListeners = new CopyOnWriteArrayList<ZoomChangeListener>();

    /**
     * Removes a zoom change listener
     *
     * @param listener the listener. Ignored if null or already absent
     */
    public static void removeZoomChangeListener(NavigatableComponent.ZoomChangeListener listener) {
        zoomChangeListeners.remove(listener);
    }

    /**
     * Adds a zoom change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     */
    public static void addZoomChangeListener(NavigatableComponent.ZoomChangeListener listener) {
        if (listener != null) {
            zoomChangeListeners.addIfAbsent(listener);
        }
    }

    protected static void fireZoomChanged() {
        for (ZoomChangeListener l : zoomChangeListeners) {
            l.zoomChanged();
        }
    }

    /**
     * The scale factor in x or y-units per pixel. This means, if scale = 10,
     * every physical pixel on screen are 10 x or 10 y units in the
     * northing/easting space of the projection.
     */
    private double scale = Main.proj.getDefaultZoomInPPD();
    /**
     * Center n/e coordinate of the desired screen center.
     */
    protected EastNorth center = calculateDefaultCenter();

    public NavigatableComponent() {
        setLayout(null);
    }

    protected DataSet getCurrentDataSet() {
        return Main.main.getCurrentDataSet();
    }

    private EastNorth calculateDefaultCenter() {
        Bounds b = Main.proj.getWorldBoundsLatLon();
        double lat = (b.getMax().lat() + b.getMin().lat())/2;
        double lon = (b.getMax().lon() + b.getMin().lon())/2;

        return Main.proj.latlon2eastNorth(new LatLon(lat, lon));
    }

    public static String getDistText(double dist) {
        return getSystemOfMeasurement().getDistText(dist);
    }

    public String getDist100PixelText()
    {
        return getDistText(getDist100Pixel());
    }

    public double getDist100Pixel()
    {
        int w = getWidth()/2;
        int h = getHeight()/2;
        LatLon ll1 = getLatLon(w-50,h);
        LatLon ll2 = getLatLon(w+50,h);
        return ll1.greatCircleDistance(ll2);
    }

    /**
     * @return Returns the center point. A copy is returned, so users cannot
     *      change the center by accessing the return value. Use zoomTo instead.
     */
    public EastNorth getCenter() {
        return center;
    }

    /**
     * @param x X-Pixelposition to get coordinate from
     * @param y Y-Pixelposition to get coordinate from
     *
     * @return Geographic coordinates from a specific pixel coordination
     *      on the screen.
     */
    public EastNorth getEastNorth(int x, int y) {
        return new EastNorth(
                center.east() + (x - getWidth()/2.0)*scale,
                center.north() - (y - getHeight()/2.0)*scale);
    }

    public ProjectionBounds getProjectionBounds() {
        return new ProjectionBounds(
                new EastNorth(
                        center.east() - getWidth()/2.0*scale,
                        center.north() - getHeight()/2.0*scale),
                        new EastNorth(
                                center.east() + getWidth()/2.0*scale,
                                center.north() + getHeight()/2.0*scale));
    }

    /* FIXME: replace with better method - used by MapSlider */
    public ProjectionBounds getMaxProjectionBounds() {
        Bounds b = getProjection().getWorldBoundsLatLon();
        return new ProjectionBounds(getProjection().latlon2eastNorth(b.getMin()),
                getProjection().latlon2eastNorth(b.getMax()));
    }

    /* FIXME: replace with better method - used by Main to reset Bounds when projection changes, don't use otherwise */
    public Bounds getRealBounds() {
        return new Bounds(
                getProjection().eastNorth2latlon(new EastNorth(
                        center.east() - getWidth()/2.0*scale,
                        center.north() - getHeight()/2.0*scale)),
                        getProjection().eastNorth2latlon(new EastNorth(
                                center.east() + getWidth()/2.0*scale,
                                center.north() + getHeight()/2.0*scale)));
    }

    /**
     * @param x X-Pixelposition to get coordinate from
     * @param y Y-Pixelposition to get coordinate from
     *
     * @return Geographic unprojected coordinates from a specific pixel coordination
     *      on the screen.
     */
    public LatLon getLatLon(int x, int y) {
        return getProjection().eastNorth2latlon(getEastNorth(x, y));
    }

    public LatLon getLatLon(double x, double y) {
        return getLatLon((int)x, (int)y);
    }

    /**
     * @param r
     * @return Minimum bounds that will cover rectangle
     */
    public Bounds getLatLonBounds(Rectangle r) {
        // TODO Maybe this should be (optional) method of Projection implementation
        EastNorth p1 = getEastNorth(r.x, r.y);
        EastNorth p2 = getEastNorth(r.x + r.width, r.y + r.height);

        Bounds result = new Bounds(Main.proj.eastNorth2latlon(p1));

        double eastMin = Math.min(p1.east(), p2.east());
        double eastMax = Math.max(p1.east(), p2.east());
        double northMin = Math.min(p1.north(), p2.north());
        double northMax = Math.max(p1.north(), p2.north());
        double deltaEast = (eastMax - eastMin) / 10;
        double deltaNorth = (northMax - northMin) / 10;

        for (int i=0; i < 10; i++) {
            result.extend(Main.proj.eastNorth2latlon(new EastNorth(eastMin + i * deltaEast, northMin)));
            result.extend(Main.proj.eastNorth2latlon(new EastNorth(eastMin + i * deltaEast, northMax)));
            result.extend(Main.proj.eastNorth2latlon(new EastNorth(eastMin, northMin  + i * deltaNorth)));
            result.extend(Main.proj.eastNorth2latlon(new EastNorth(eastMax, northMin  + i * deltaNorth)));
        }

        return result;
    }

    /**
     * Return the point on the screen where this Coordinate would be.
     * @param p The point, where this geopoint would be drawn.
     * @return The point on screen where "point" would be drawn, relative
     *      to the own top/left.
     */
    public Point2D getPoint2D(EastNorth p) {
        if (null == p)
            return new Point();
        double x = (p.east()-center.east())/scale + getWidth()/2;
        double y = (center.north()-p.north())/scale + getHeight()/2;
        return new Point2D.Double(x, y);
    }

    public Point2D getPoint2D(LatLon latlon) {
        if (latlon == null)
            return new Point();
        else if (latlon instanceof CachedLatLon)
            return getPoint2D(((CachedLatLon)latlon).getEastNorth());
        else
            return getPoint2D(getProjection().latlon2eastNorth(latlon));
    }
    public Point2D getPoint2D(Node n) {
        return getPoint2D(n.getEastNorth());
    }

    // looses precision, may overflow (depends on p and current scale)
    //@Deprecated
    public Point getPoint(EastNorth p) {
        Point2D d = getPoint2D(p);
        return new Point((int) d.getX(), (int) d.getY());
    }

    // looses precision, may overflow (depends on p and current scale)
    //@Deprecated
    public Point getPoint(LatLon latlon) {
        Point2D d = getPoint2D(latlon);
        return new Point((int) d.getX(), (int) d.getY());
    }

    // looses precision, may overflow (depends on p and current scale)
    //@Deprecated
    public Point getPoint(Node n) {
        Point2D d = getPoint2D(n);
        return new Point((int) d.getX(), (int) d.getY());
    }

    /**
     * Zoom to the given coordinate.
     * @param newCenter The center x-value (easting) to zoom to.
     * @param scale The scale to use.
     */
    private void zoomTo(EastNorth newCenter, double newScale) {
        Bounds b = getProjection().getWorldBoundsLatLon();
        CachedLatLon cl = new CachedLatLon(newCenter);
        boolean changed = false;
        double lat = cl.lat();
        double lon = cl.lon();
        if(lat < b.getMin().lat()) {changed = true; lat = b.getMin().lat(); }
        else if(lat > b.getMax().lat()) {changed = true; lat = b.getMax().lat(); }
        if(lon < b.getMin().lon()) {changed = true; lon = b.getMin().lon(); }
        else if(lon > b.getMax().lon()) {changed = true; lon = b.getMax().lon(); }
        if(changed) {
            newCenter = new CachedLatLon(lat, lon).getEastNorth();
        }
        int width = getWidth()/2;
        int height = getHeight()/2;
        LatLon l1 = new LatLon(b.getMin().lat(), lon);
        LatLon l2 = new LatLon(b.getMax().lat(), lon);
        EastNorth e1 = getProjection().latlon2eastNorth(l1);
        EastNorth e2 = getProjection().latlon2eastNorth(l2);
        double d = e2.north() - e1.north();
        if(d < height*newScale)
        {
            double newScaleH = d/height;
            e1 = getProjection().latlon2eastNorth(new LatLon(lat, b.getMin().lon()));
            e2 = getProjection().latlon2eastNorth(new LatLon(lat, b.getMax().lon()));
            d = e2.east() - e1.east();
            if(d < width*newScale) {
                newScale = Math.max(newScaleH, d/width);
            }
        }
        else
        {
            d = d/(l1.greatCircleDistance(l2)*height*10);
            if(newScale < d) {
                newScale = d;
            }
        }

        if (!newCenter.equals(center) || (scale != newScale)) {
            pushZoomUndo(center, scale);
            zoomNoUndoTo(newCenter, newScale);
        }
    }

    /**
     * Zoom to the given coordinate without adding to the zoom undo buffer.
     * @param newCenter The center x-value (easting) to zoom to.
     * @param scale The scale to use.
     */
    private void zoomNoUndoTo(EastNorth newCenter, double newScale) {
        if (!newCenter.equals(center)) {
            EastNorth oldCenter = center;
            center = newCenter;
            firePropertyChange("center", oldCenter, newCenter);
        }
        if (scale != newScale) {
            double oldScale = scale;
            scale = newScale;
            firePropertyChange("scale", oldScale, newScale);
        }

        repaint();
        fireZoomChanged();
    }

    public void zoomTo(EastNorth newCenter) {
        zoomTo(newCenter, scale);
    }

    public void zoomTo(LatLon newCenter) {
        if(newCenter instanceof CachedLatLon) {
            zoomTo(((CachedLatLon)newCenter).getEastNorth(), scale);
        } else {
            zoomTo(getProjection().latlon2eastNorth(newCenter), scale);
        }
    }

    public void zoomToFactor(double x, double y, double factor) {
        double newScale = scale*factor;
        // New center position so that point under the mouse pointer stays the same place as it was before zooming
        // You will get the formula by simplifying this expression: newCenter = oldCenter + mouseCoordinatesInNewZoom - mouseCoordinatesInOldZoom
        zoomTo(new EastNorth(
                center.east() - (x - getWidth()/2.0) * (newScale - scale),
                center.north() + (y - getHeight()/2.0) * (newScale - scale)),
                newScale);
    }

    public void zoomToFactor(EastNorth newCenter, double factor) {
        zoomTo(newCenter, scale*factor);
    }

    public void zoomToFactor(double factor) {
        zoomTo(center, scale*factor);
    }

    public void zoomTo(ProjectionBounds box) {
        // -20 to leave some border
        int w = getWidth()-20;
        if (w < 20) {
            w = 20;
        }
        int h = getHeight()-20;
        if (h < 20) {
            h = 20;
        }

        double scaleX = (box.max.east()-box.min.east())/w;
        double scaleY = (box.max.north()-box.min.north())/h;
        double newScale = Math.max(scaleX, scaleY);

        zoomTo(box.getCenter(), newScale);
    }

    public void zoomTo(Bounds box) {
        zoomTo(new ProjectionBounds(getProjection().latlon2eastNorth(box.getMin()),
                getProjection().latlon2eastNorth(box.getMax())));
    }

    private class ZoomData {
        LatLon center;
        double scale;

        public ZoomData(EastNorth center, double scale) {
            this.center = new CachedLatLon(center);
            this.scale = scale;
        }

        public EastNorth getCenterEastNorth() {
            return getProjection().latlon2eastNorth(center);
        }

        public double getScale() {
            return scale;
        }
    }

    private Stack<ZoomData> zoomUndoBuffer = new Stack<ZoomData>();
    private Stack<ZoomData> zoomRedoBuffer = new Stack<ZoomData>();
    private Date zoomTimestamp = new Date();

    private void pushZoomUndo(EastNorth center, double scale) {
        Date now = new Date();
        if ((now.getTime() - zoomTimestamp.getTime()) > (Main.pref.getDouble("zoom.undo.delay", 1.0) * 1000)) {
            zoomUndoBuffer.push(new ZoomData(center, scale));
            if (zoomUndoBuffer.size() > Main.pref.getInteger("zoom.undo.max", 50)) {
                zoomUndoBuffer.remove(0);
            }
            zoomRedoBuffer.clear();
        }
        zoomTimestamp = now;
    }

    public void zoomPrevious() {
        if (!zoomUndoBuffer.isEmpty()) {
            ZoomData zoom = zoomUndoBuffer.pop();
            zoomRedoBuffer.push(new ZoomData(center, scale));
            zoomNoUndoTo(zoom.getCenterEastNorth(), zoom.getScale());
        }
    }

    public void zoomNext() {
        if (!zoomRedoBuffer.isEmpty()) {
            ZoomData zoom = zoomRedoBuffer.pop();
            zoomUndoBuffer.push(new ZoomData(center, scale));
            zoomNoUndoTo(zoom.getCenterEastNorth(), zoom.getScale());
        }
    }

    public boolean hasZoomUndoEntries() {
        return !zoomUndoBuffer.isEmpty();
    }

    public boolean hasZoomRedoEntries() {
        return !zoomRedoBuffer.isEmpty();
    }

    private BBox getBBox(Point p, int snapDistance) {
        return new BBox(getLatLon(p.x - snapDistance, p.y - snapDistance),
                getLatLon(p.x + snapDistance, p.y + snapDistance));
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return a sorted map with the keys representing the distance of
     *      their associated nodes to point p.
     */
    private Map<Double, List<Node>> getNearestNodesImpl(Point p,
            Predicate<OsmPrimitive> predicate) {
        TreeMap<Double, List<Node>> nearestMap = new TreeMap<Double, List<Node>>();
        DataSet ds = getCurrentDataSet();

        if (ds != null) {
            double dist, snapDistanceSq = PROP_SNAP_DISTANCE.get();
            snapDistanceSq *= snapDistanceSq;

            for (Node n : ds.searchNodes(getBBox(p, PROP_SNAP_DISTANCE.get()))) {
                if (predicate.evaluate(n)
                        && (dist = getPoint2D(n).distanceSq(p)) < snapDistanceSq)
                {
                    List<Node> nlist;
                    if (nearestMap.containsKey(dist)) {
                        nlist = nearestMap.get(dist);
                    } else {
                        nlist = new LinkedList<Node>();
                        nearestMap.put(dist, nlist);
                    }
                    nlist.add(n);
                }
            }
        }

        return nearestMap;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return All nodes nearest to point p that are in a belt from
     *      dist(nearest) to dist(nearest)+4px around p and
     *      that are not in ignore.
     *
     * @param p the point for which to search the nearest segment.
     * @param ignore a collection of nodes which are not to be returned.
     * @param predicate the returned objects have to fulfill certain properties.
     */
    public final List<Node> getNearestNodes(Point p,
            Collection<Node> ignore, Predicate<OsmPrimitive> predicate) {
        List<Node> nearestList = Collections.emptyList();

        if (ignore == null) {
            ignore = Collections.emptySet();
        }

        Map<Double, List<Node>> nlists = getNearestNodesImpl(p, predicate);
        if (!nlists.isEmpty()) {
            Double minDistSq = null;
            List<Node> nlist;
            for (Double distSq : nlists.keySet()) {
                nlist = nlists.get(distSq);

                // filter nodes to be ignored before determining minDistSq..
                nlist.removeAll(ignore);
                if (minDistSq == null) {
                    if (!nlist.isEmpty()) {
                        minDistSq = distSq;
                        nearestList = new ArrayList<Node>();
                        nearestList.addAll(nlist);
                    }
                } else {
                    if (distSq-minDistSq < (4)*(4)) {
                        nearestList.addAll(nlist);
                    }
                }
            }
        }

        return nearestList;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return All nodes nearest to point p that are in a belt from
     *      dist(nearest) to dist(nearest)+4px around p.
     * @see #getNearestNodes(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned objects have to fulfill certain properties.
     */
    public final List<Node> getNearestNodes(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNodes(p, null, predicate);
    }

    /**
     * The *result* depends on the current map selection state.
     *
     * If more than one node within node.snap-distance pixels is found,
     * the nearest node selected is returned.
     *
     * If no such node is found, the nearest new/id=0 node within
     * about the same distance as the true nearest node is returned.
     *
     * If no such node is found either, the true nearest
     * node to p is returned.
     *
     * Finally, if a node is not found at all, return null.
     *
     * @return A node within snap-distance to point p,
     *      that is chosen by the algorithm described.
     *
     * @param p the screen point
     * @param predicate this parameter imposes a condition on the returned object, e.g.
     *        give the nearest node that is tagged.
     */
    public final Node getNearestNode(Point p, Predicate<OsmPrimitive> predicate) {
        Node n = null;

        Map<Double, List<Node>> nlists = getNearestNodesImpl(p, predicate);
        if (!nlists.isEmpty()) {
            Node ntsel = null, ntnew = null;
            double minDistSq = nlists.keySet().iterator().next();

            for (Double distSq : nlists.keySet()) {
                for (Node nd : nlists.get(distSq)) {
                    // find the nearest selected node
                    if (ntsel == null && nd.isSelected()) {
                        ntsel = nd;
                    }
                    // find the nearest newest node that is within about the same
                    // distance as the true nearest node
                    if (ntnew == null && nd.isNew() && (distSq-minDistSq < 1)) {
                        ntnew = nd;
                    }
                }
            }

            // take nearest selected, nearest new or true nearest node to p, in that order
            n = (ntsel != null) ? ntsel : ((ntnew != null) ? ntnew
                    : nlists.values().iterator().next().get(0));
        }

        return n;
    }

    @Deprecated
    public final Node getNearestNode(Point p) {
        return getNearestNode(p, OsmPrimitive.isUsablePredicate);
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return a sorted map with the keys representing the perpendicular
     *      distance of their associated way segments to point p.
     */
    private Map<Double, List<WaySegment>> getNearestWaySegmentsImpl(Point p,
            Predicate<OsmPrimitive> predicate) {
        Map<Double, List<WaySegment>> nearestMap = new TreeMap<Double, List<WaySegment>>();
        DataSet ds = getCurrentDataSet();

        if (ds != null) {
            double snapDistanceSq = Main.pref.getInteger("mappaint.segment.snap-distance", 10);
            snapDistanceSq *= snapDistanceSq;

            for (Way w : ds.searchWays(getBBox(p, Main.pref.getInteger("mappaint.segment.snap-distance", 10)))) {
                if (!predicate.evaluate(w)) {
                    continue;
                }
                Node lastN = null;
                int i = -2;
                for (Node n : w.getNodes()) {
                    i++;
                    if (n.isDeleted() || n.isIncomplete()) { //FIXME: This shouldn't happen, raise exception?
                        continue;
                    }
                    if (lastN == null) {
                        lastN = n;
                        continue;
                    }

                    Point2D A = getPoint2D(lastN);
                    Point2D B = getPoint2D(n);
                    double c = A.distanceSq(B);
                    double a = p.distanceSq(B);
                    double b = p.distanceSq(A);

                    /* perpendicular distance squared
                     * loose some precision to account for possible deviations in the calculation above
                     * e.g. if identical (A and B) come about reversed in another way, values may differ
                     * -- zero out least significant 32 dual digits of mantissa..
                     */
                    double perDistSq = Double.longBitsToDouble(
                            Double.doubleToLongBits( a - (a - b + c) * (a - b + c) / 4 / c )
                            >> 32 << 32); // resolution in numbers with large exponent not needed here..

                    if (perDistSq < snapDistanceSq && a < c + snapDistanceSq && b < c + snapDistanceSq) {
                        //System.err.println(Double.toHexString(perDistSq));

                        List<WaySegment> wslist;
                        if (nearestMap.containsKey(perDistSq)) {
                            wslist = nearestMap.get(perDistSq);
                        } else {
                            wslist = new LinkedList<WaySegment>();
                            nearestMap.put(perDistSq, wslist);
                        }
                        wslist.add(new WaySegment(w, i));
                    }

                    lastN = n;
                }
            }
        }

        return nearestMap;
    }

    /**
     * The result *order* depends on the current map selection state.
     * Segments within 10px of p are searched and sorted by their distance to @param p,
     * then, within groups of equally distant segments, prefer those that are selected.
     *
     * @return all segments within 10px of p that are not in ignore,
     *          sorted by their perpendicular distance.
     *
     * @param p the point for which to search the nearest segments.
     * @param ignore a collection of segments which are not to be returned.
     * @param predicate the returned objects have to fulfill certain properties.
     */
    public final List<WaySegment> getNearestWaySegments(Point p,
            Collection<WaySegment> ignore, Predicate<OsmPrimitive> predicate) {
        List<WaySegment> nearestList = new ArrayList<WaySegment>();
        List<WaySegment> unselected = new LinkedList<WaySegment>();

        for (List<WaySegment> wss : getNearestWaySegmentsImpl(p, predicate).values()) {
            // put selected waysegs within each distance group first
            // makes the order of nearestList dependent on current selection state
            for (WaySegment ws : wss) {
                (ws.way.isSelected() ? nearestList : unselected).add(ws);
            }
            nearestList.addAll(unselected);
            unselected.clear();
        }
        if (ignore != null) {
            nearestList.removeAll(ignore);
        }

        return nearestList;
    }

    /**
     * The result *order* depends on the current map selection state.
     *
     * @return all segments within 10px of p, sorted by their perpendicular distance.
     * @see #getNearestWaySegments(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest segments.
     * @param predicate the returned objects have to fulfill certain properties.
     */
    public final List<WaySegment> getNearestWaySegments(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestWaySegments(p, null, predicate);
    }

    /**
     * The *result* depends on the current map selection state.
     *
     * @return The nearest way segment to point p,
     *      prefer a nearest, selected way segment, if found.
     * @see #getNearestWaySegments(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final WaySegment getNearestWaySegment(Point p, Predicate<OsmPrimitive> predicate) {
        WaySegment wayseg = null, ntsel = null;

        for (List<WaySegment> wslist : getNearestWaySegmentsImpl(p, predicate).values()) {
            if (wayseg != null && ntsel != null) {
                break;
            }
            for (WaySegment ws : wslist) {
                if (wayseg == null) {
                    wayseg = ws;
                }
                if (ntsel == null && ws.way.isSelected()) {
                    ntsel = ws;
                }
            }
        }

        return (ntsel != null) ? ntsel : wayseg;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the perpendicular distance to point p.
     *
     * @return all nearest ways to the screen point given that are not in ignore.
     * @see #getNearestWaySegments(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest ways.
     * @param ignore a collection of ways which are not to be returned.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<Way> getNearestWays(Point p,
            Collection<Way> ignore, Predicate<OsmPrimitive> predicate) {
        List<Way> nearestList = new ArrayList<Way>();
        Set<Way> wset = new HashSet<Way>();

        for (List<WaySegment> wss : getNearestWaySegmentsImpl(p, predicate).values()) {
            for (WaySegment ws : wss) {
                if (wset.add(ws.way)) {
                    nearestList.add(ws.way);
                }
            }
        }
        if (ignore != null) {
            nearestList.removeAll(ignore);
        }

        return nearestList;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the perpendicular distance to point p.
     *
     * @return all nearest ways to the screen point given.
     * @see #getNearestWays(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest ways.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<Way> getNearestWays(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestWays(p, null, predicate);
    }

    /**
     * The *result* depends on the current map selection state.
     *
     * @return The nearest way to point p,
     *      prefer a selected way if there are multiple nearest.
     * @see #getNearestWaySegment(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final Way getNearestWay(Point p, Predicate<OsmPrimitive> predicate) {
        WaySegment nearestWaySeg = getNearestWaySegment(p, predicate);
        return (nearestWaySeg == null) ? null : nearestWaySeg.way;
    }

    @Deprecated
    public final Way getNearestWay(Point p) {
        return getNearestWay(p, OsmPrimitive.isUsablePredicate);
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * First, nodes will be searched. If there are nodes within BBox found,
     * return a collection of those nodes only.
     *
     * If no nodes are found, search for nearest ways. If there are ways
     * within BBox found, return a collection of those ways only.
     *
     * If nothing is found, return an empty collection.
     *
     * @return Primitives nearest to the given screen point that are not in ignore.
     * @see #getNearestNodes(Point, Collection, Predicate)
     * @see #getNearestWays(Point, Collection, Predicate)
     *
     * @param p The point on screen.
     * @param ignore a collection of ways which are not to be returned.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<OsmPrimitive> getNearestNodesOrWays(Point p,
            Collection<OsmPrimitive> ignore, Predicate<OsmPrimitive> predicate) {
        List<OsmPrimitive> nearestList = Collections.emptyList();
        OsmPrimitive osm = getNearestNodeOrWay(p, predicate, false);

        if (osm != null) {
            if (osm instanceof Node) {
                nearestList = new ArrayList<OsmPrimitive>(getNearestNodes(p, predicate));
            } else if (osm instanceof Way) {
                nearestList = new ArrayList<OsmPrimitive>(getNearestWays(p, predicate));
            }
            if (ignore != null) {
                nearestList.removeAll(ignore);
            }
        }

        return nearestList;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return Primitives nearest to the given screen point.
     * @see #getNearests(Point, Collection, Predicate)
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<OsmPrimitive> getNearestNodesOrWays(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNodesOrWays(p, null, predicate);
    }

    /**
     * This is used as a helper routine to {@link #getNearestNodeOrWay(Point, Predicate, boolean)}
     *
     * @return true, if the node fulfills certain properties wrt p and use_sel
     *
     * @param osm node to check
     * @param p point clicked
     * @param use_sel whether to prefer a selected node
     */
    private boolean isPrecedenceNode(Node osm, Point p, boolean use_selected) {
        boolean ret = false;

        if (osm != null) {
            ret |= !(p.distanceSq(getPoint2D(osm)) > (4)*(4));
            ret |= osm.isTagged();
            if (use_selected) {
                ret |= osm.isSelected();
            }
        }

        return ret;
    }

    /**
     * The *result* depends on the current map selection state IF use_selected is true.
     *
     * IF use_selected is true, use {@link #getNearestNode(Point, Predicate)} to find
     * the nearest, selected node.  If not found, try {@link #getNearestWaySegment(Point, Predicate)}
     * to find the nearest selected way.
     *
     * IF use_selected is false, or if no selected primitive was found, do the following.
     *
     * If the nearest node found is within 4px of p, simply take it.
     * Else, find the nearest way segment. Then, if p is closer to its
     * middle than to the node, take the way segment, else take the node.
     *
     * Finally, if no nearest primitive is found at all, return null.
     *
     * @return A primitive within snap-distance to point p,
     *      that is chosen by the algorithm described.
     * @see getNearestNode(Point, Predicate)
     * @see getNearestNodesImpl(Point, Predicate)
     * @see getNearestWay(Point, Predicate)
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     * @param use_selected whether to prefer primitives that are currently selected.
     */
    public final OsmPrimitive getNearestNodeOrWay(Point p, Predicate<OsmPrimitive> predicate, boolean use_selected) {
        OsmPrimitive osm = null;
        WaySegment ws = null;

        // find a nearest, selected primitive
        if (use_selected) {
            osm = getNearestNode(p, predicate);
            use_selected = isPrecedenceNode((Node)osm, p, use_selected);

            if (!use_selected) {
                ws = getNearestWaySegment(p, predicate);
                if (ws != null) {
                    if (ws.way.isSelected() || osm == null) {
                        // either no nearest nodes found or none were selected
                        use_selected = true;
                        osm = ws.way;
                    } // else { unselected node and wayseg found, do the stuff below }
                } else {
                    // no nearest wayseg found, osm is null or the nearest node
                    use_selected = true;
                }
            }
        }

        // no nearest, selected primitive was found or caller does not care about current selection
        if (!use_selected) {
            if (osm == null) {
                // get the true nearest node (if unselected found before, reuse it)
                for (List<Node> nlist : getNearestNodesImpl(p, predicate).values()) {
                    osm = nlist.get(0);
                    break;
                }
            }

            // there is no nearest node OR it does not fulfill criteria to simply be chosen
            if (osm == null || !isPrecedenceNode((Node)osm, p, use_selected)) {
                if (ws == null) {
                    // get the true nearest wayseg (if unselected found before, reuse it)
                    for (WaySegment wseg : getNearestWaySegments(p, predicate)) {
                        ws = wseg;
                        break;
                    }
                }
                if (ws != null) {
                    if (osm == null) {
                        // a nearest node was not found
                        osm = ws.way;
                    } else {
                        int maxWaySegLenSq = 3*PROP_SNAP_DISTANCE.get();
                        maxWaySegLenSq *= maxWaySegLenSq;

                        Point2D wp1 = getPoint2D(ws.way.getNode(ws.lowerIndex));
                        Point2D wp2 = getPoint2D(ws.way.getNode(ws.lowerIndex+1));

                        // is wayseg shorter than maxWaySegLenSq and
                        // is p closer to the middle of wayseg than to the nearest node?
                        if (wp1.distanceSq(wp2) < maxWaySegLenSq &&
                                p.distanceSq(project(0.5, wp1, wp2)) < p.distanceSq(getPoint2D((Node)osm))) {
                            osm = ws.way;
                        }
                    }
                }
            }
        }

        return osm;
    }

    /**
     * Convenience method to {@link #getNearestNodeOrWay(Point, Predicate, boolean)}.
     *
     * @return The nearest primitive to point p.
     */
    public final OsmPrimitive getNearestNodeOrWay(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNodeOrWay(p, predicate, false);
    }

    @Deprecated
    public final OsmPrimitive getNearest(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNodeOrWay(p, predicate, false);
    }

    @Deprecated
    public final Collection<OsmPrimitive> getNearestCollection(Point p, Predicate<OsmPrimitive> predicate) {
        return asColl(getNearest(p, predicate));
    }

    /**
     * @return o as collection of o's type.
     */
    public final static <T> Collection<T> asColl(T o) {
        if (o == null)
            return Collections.emptySet();
        return Collections.singleton(o);
    }

    public final static double perDist(Point2D pt, Point2D a, Point2D b) {
        if (pt != null && a != null && b != null) {
            double pd = (
                    (a.getX()-pt.getX())*(b.getX()-a.getX()) -
                    (a.getY()-pt.getY())*(b.getY()-a.getY()) );
            return Math.abs(pd) / a.distance(b);
        }
        return 0d;
    }

    /**
     *
     * @param pt point to project onto (ab)
     * @param a root of vector
     * @param b vector
     * @return point of intersection of line given by (ab)
     *      with its orthogonal line running through pt
     */
    public final static Point2D project(Point2D pt, Point2D a, Point2D b) {
        if (pt != null && a != null && b != null) {
            double r = ((
                    (pt.getX()-a.getX())*(b.getX()-a.getX()) +
                    (pt.getY()-a.getY())*(b.getY()-a.getY()) )
                    / a.distanceSq(b));
            return project(r, a, b);
        }
        return null;
    }

    /**
     * if r = 0 returns a, if r=1 returns b,
     * if r = 0.5 returns center between a and b, etc..
     *
     * @param r scale value
     * @param a root of vector
     * @param b vector
     * @return new point at a + r*(ab)
     */
    public final static Point2D project(double r, Point2D a, Point2D b) {
        Point2D ret = null;

        if (a != null && b != null) {
            ret = new Point2D.Double(a.getX() + r*(b.getX()-a.getX()),
                    a.getY() + r*(b.getY()-a.getY()));
        }
        return ret;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return a list of all objects that are nearest to point p and
     *          not in ignore or an empty list if nothing was found.
     *
     * @param p The point on screen.
     * @param ignore a collection of ways which are not to be returned.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<OsmPrimitive> getAllNearest(Point p,
            Collection<OsmPrimitive> ignore, Predicate<OsmPrimitive> predicate) {
        List<OsmPrimitive> nearestList = new ArrayList<OsmPrimitive>();
        Set<Way> wset = new HashSet<Way>();

        for (List<WaySegment> wss : getNearestWaySegmentsImpl(p, predicate).values()) {
            for (WaySegment ws : wss) {
                if (wset.add(ws.way)) {
                    nearestList.add(ws.way);
                }
            }
        }
        for (List<Node> nlist : getNearestNodesImpl(p, predicate).values()) {
            nearestList.addAll(nlist);
        }
        if (ignore != null) {
            nearestList.removeAll(ignore);
        }

        return nearestList;
    }

    /**
     * The *result* does not depend on the current map selection state,
     * neither does the result *order*.
     * It solely depends on the distance to point p.
     *
     * @return a list of all objects that are nearest to point p
     *          or an empty list if nothing was found.
     * @see #getAllNearest(Point, Collection, Predicate)
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<OsmPrimitive> getAllNearest(Point p, Predicate<OsmPrimitive> predicate) {
        return getAllNearest(p, null, predicate);
    }

    /**
     * @return The projection to be used in calculating stuff.
     */
    public Projection getProjection() {
        return Main.proj;
    }

    public String helpTopic() {
        String n = getClass().getName();
        return n.substring(n.lastIndexOf('.')+1);
    }

    /**
     * Return a ID which is unique as long as viewport dimensions are the same
     */
    public int getViewID() {
        String x = center.east() + "_" + center.north() + "_" + scale + "_" +
        getWidth() + "_" + getHeight() + "_" + getProjection().toString();
        java.util.zip.CRC32 id = new java.util.zip.CRC32();
        id.update(x.getBytes());
        return (int)id.getValue();
    }

    public static SystemOfMeasurement getSystemOfMeasurement() {
        SystemOfMeasurement som = SYSTEMS_OF_MEASUREMENT.get(ProjectionPreference.PROP_SYSTEM_OF_MEASUREMENT.get());
        if (som == null)
            return METRIC_SOM;
        return som;
    }

    public static class SystemOfMeasurement {
        public final double aValue;
        public final double bValue;
        public final String aName;
        public final String bName;

        /**
         * System of measurement. Currently covers only length units.
         *
         * If a quantity x is given in m (x_m) and in unit a (x_a) then it translates as
         * x_a == x_m / aValue
         */
        public SystemOfMeasurement(double aValue, String aName, double bValue, String bName) {
            this.aValue = aValue;
            this.aName = aName;
            this.bValue = bValue;
            this.bName = bName;
        }

        public String getDistText(double dist) {
            double a = dist / aValue;
            if (!Main.pref.getBoolean("system_of_measurement.use_only_lower_unit", false) && a > bValue / aValue) {
                double b = dist / bValue;
                return String.format(Locale.US, "%." + (b<10 ? 2 : 1) + "f %s", b, bName);
            } else if (a < 0.01)
                return "< 0.01 " + aName;
            else
                return String.format(Locale.US, "%." + (a<10 ? 2 : 1) + "f %s", a, aName);
        }
    }

    public static final SystemOfMeasurement METRIC_SOM = new SystemOfMeasurement(1, "m", 1000, "km");
    public static final SystemOfMeasurement CHINESE_SOM = new SystemOfMeasurement(1.0/3.0, "\u5e02\u5c3a" /* chi */, 500, "\u5e02\u91cc" /* li */);
    public static final SystemOfMeasurement IMPERIAL_SOM = new SystemOfMeasurement(0.3048, "ft", 1609.344, "mi");

    public static Map<String, SystemOfMeasurement> SYSTEMS_OF_MEASUREMENT;
    static {
        SYSTEMS_OF_MEASUREMENT = new LinkedHashMap<String, SystemOfMeasurement>();
        SYSTEMS_OF_MEASUREMENT.put(marktr("Metric"), METRIC_SOM);
        SYSTEMS_OF_MEASUREMENT.put(marktr("Chinese"), CHINESE_SOM);
        SYSTEMS_OF_MEASUREMENT.put(marktr("Imperial"), IMPERIAL_SOM);
    }
}
