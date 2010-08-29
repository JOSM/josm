// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Point;
import java.awt.Rectangle;
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

    public static final int snapDistance = Main.pref.getInteger("node.snap-distance", 10);
    public static final int snapDistanceSq = sqr(snapDistance);

    private static int sqr(int a) { return a*a;}
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
    public Point getPoint(EastNorth p) {
        if (null == p)
            return new Point();
        double x = (p.east()-center.east())/scale + getWidth()/2;
        double y = (center.north()-p.north())/scale + getHeight()/2;
        return new Point((int)x,(int)y);
    }

    public Point getPoint(LatLon latlon) {
        if (latlon == null)
            return new Point();
        else if (latlon instanceof CachedLatLon)
            return getPoint(((CachedLatLon)latlon).getEastNorth());
        else
            return getPoint(getProjection().latlon2eastNorth(latlon));
    }
    public Point getPoint(Node n) {
        return getPoint(n.getEastNorth());
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

    private BBox getSnapDistanceBBox(Point p) {
        return new BBox(getLatLon(p.x - snapDistance, p.y - snapDistance),
                getLatLon(p.x + snapDistance, p.y + snapDistance));
    }

    @Deprecated
    public final Node getNearestNode(Point p) {
        return getNearestNode(p, OsmPrimitive.isUsablePredicate);
    }

    /**
     * Return the nearest node to the screen point given.
     * If more then one node within snapDistance pixel is found,
     * the nearest node is returned.
     * @param p the screen point
     * @param predicate this parameter imposes a condition on the returned object, e.g.
     *        give the nearest node that is tagged.
     */
    public final Node getNearestNode(Point p, Predicate<OsmPrimitive> predicate) {
        DataSet ds = getCurrentDataSet();
        if (ds == null)
            return null;

        double minDistanceSq = snapDistanceSq;
        Node minPrimitive = null;
        for (Node n : ds.searchNodes(getSnapDistanceBBox(p))) {
            if (! predicate.evaluate(n))
                continue;
            Point sp = getPoint(n);
            double dist = p.distanceSq(sp);
            if (dist < minDistanceSq) {
                minDistanceSq = dist;
                minPrimitive = n;
            }
            // when multiple nodes on one point, prefer new or selected nodes
            else if (dist == minDistanceSq && minPrimitive != null
                    && ((n.isNew() && ds.isSelected(n))
                            || (!ds.isSelected(minPrimitive) && (ds.isSelected(n) || n.isNew())))) {
                minPrimitive = n;
            }
        }
        return minPrimitive;
    }

    /**
     * @return all way segments within 10px of p, sorted by their
     * perpendicular distance.
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned objects have to fulfill certain properties.
     */
    public final List<WaySegment> getNearestWaySegments(Point p, Predicate<OsmPrimitive> predicate) {
        TreeMap<Double, List<WaySegment>> nearest = new TreeMap<Double, List<WaySegment>>();
        DataSet ds = getCurrentDataSet();
        if (ds == null)
            return null;

        for (Way w : ds.searchWays(getSnapDistanceBBox(p))) {
            if (!predicate.evaluate(w))
                continue;
            Node lastN = null;
            int i = -2;
            for (Node n : w.getNodes()) {
                i++;
                if (n.isDeleted() || n.isIncomplete()) {//FIXME: This shouldn't happen, raise exception?
                    continue;
                }
                if (lastN == null) {
                    lastN = n;
                    continue;
                }

                Point A = getPoint(lastN);
                Point B = getPoint(n);
                double c = A.distanceSq(B);
                double a = p.distanceSq(B);
                double b = p.distanceSq(A);
                double perDist = a - (a - b + c) * (a - b + c) / 4 / c; // perpendicular distance squared
                if (perDist < snapDistanceSq && a < c + snapDistanceSq && b < c + snapDistanceSq) {
                    if (ds.isSelected(w)) {
                        perDist -= 0.00001;
                    }
                    List<WaySegment> l;
                    if (nearest.containsKey(perDist)) {
                        l = nearest.get(perDist);
                    } else {
                        l = new LinkedList<WaySegment>();
                        nearest.put(perDist, l);
                    }
                    l.add(new WaySegment(w, i));
                }

                lastN = n;
            }
        }
        ArrayList<WaySegment> nearestList = new ArrayList<WaySegment>();
        for (List<WaySegment> wss : nearest.values()) {
            nearestList.addAll(wss);
        }
        return nearestList;
    }

    /**
     * @return the nearest way segment to the screen point given that is not
     * in ignore.
     *
     * @param p the point for which to search the nearest segment.
     * @param ignore a collection of segments which are not to be returned.
     * @param predicate the returned object has to fulfill certain properties.
     * May be null.
     */
    public final WaySegment getNearestWaySegment
                                    (Point p, Collection<WaySegment> ignore, Predicate<OsmPrimitive> predicate) {
        List<WaySegment> nearest = getNearestWaySegments(p, predicate);
        if(nearest == null)
            return null;
        if (ignore != null) {
            nearest.removeAll(ignore);
        }
        return nearest.isEmpty() ? null : nearest.get(0);
    }

    /**
     * @return the nearest way segment to the screen point given.
     */
    public final WaySegment getNearestWaySegment(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestWaySegment(p, null, predicate);
    }

    @Deprecated
    public final Way getNearestWay(Point p) {
        return getNearestWay(p, OsmPrimitive.isUsablePredicate);
    }

    /**
     * @return the nearest way to the screen point given.
     */
    public final Way getNearestWay(Point p, Predicate<OsmPrimitive> predicate) {
        WaySegment nearestWaySeg = getNearestWaySegment(p, predicate);
        return nearestWaySeg == null ? null : nearestWaySeg.way;
    }

    /**
     * Return the object, that is nearest to the given screen point.
     *
     * First, a node will be searched. If a node within 10 pixel is found, the
     * nearest node is returned.
     *
     * If no node is found, search for near ways.
     *
     * If nothing is found, return <code>null</code>.
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     * @return  The primitive that is nearest to the point p.
     */
    public OsmPrimitive getNearest(Point p, Predicate<OsmPrimitive> predicate) {
        OsmPrimitive osm = getNearestNode(p, predicate);
        if (osm == null)
        {
            osm = getNearestWay(p, predicate);
        }
        return osm;
    }

    /**
     * Returns a singleton of the nearest object, or else an empty collection.
     */
    public Collection<OsmPrimitive> getNearestCollection(Point p, Predicate<OsmPrimitive> predicate) {
        OsmPrimitive osm = getNearest(p, predicate);
        if (osm == null)
            return Collections.emptySet();
        return Collections.singleton(osm);
    }

    /**
     * @return A list of all objects that are nearest to
     * the mouse.
     *
     * @return A collection of all items or <code>null</code>
     *      if no item under or near the point. The returned
     *      list is never empty.
     */
    public Collection<OsmPrimitive> getAllNearest(Point p, Predicate<OsmPrimitive> predicate) {
        Collection<OsmPrimitive> nearest = new HashSet<OsmPrimitive>();
        DataSet ds = getCurrentDataSet();
        if (ds == null)
            return null;
        for (Way w : ds.searchWays(getSnapDistanceBBox(p))) {
            if (!predicate.evaluate(w))
                continue;
            Node lastN = null;
            for (Node n : w.getNodes()) {
                if (!predicate.evaluate(n))
                    continue;
                if (lastN == null) {
                    lastN = n;
                    continue;
                }
                Point A = getPoint(lastN);
                Point B = getPoint(n);
                double c = A.distanceSq(B);
                double a = p.distanceSq(B);
                double b = p.distanceSq(A);
                double perDist = a - (a - b + c) * (a - b + c) / 4 / c; // perpendicular distance squared
                if (perDist < snapDistanceSq && a < c + snapDistanceSq && b < c + snapDistanceSq) {
                    nearest.add(w);
                    break;
                }
                lastN = n;
            }
        }
        for (Node n : ds.searchNodes(getSnapDistanceBBox(p))) {
            if (n.isUsable()
                    && getPoint(n).distanceSq(p) < snapDistanceSq) {
                nearest.add(n);
            }
        }
        return nearest.isEmpty() ? null : nearest;
    }

    /**
     * @return A list of all nodes that are nearest to
     * the mouse.
     *
     * @return A collection of all nodes or <code>null</code>
     *      if no node under or near the point. The returned
     *      list is never empty.
     */
    public Collection<Node> getNearestNodes(Point p, Predicate<OsmPrimitive> predicate) {
        Collection<Node> nearest = new HashSet<Node>();
        DataSet ds = getCurrentDataSet();
        if (ds == null)
            return null;

        for (Node n : ds.searchNodes(getSnapDistanceBBox(p))) {
            if (!predicate.evaluate(n))
                continue;
            if (getPoint(n).distanceSq(p) < snapDistanceSq) {
                nearest.add(n);
            }
        }
        return nearest.isEmpty() ? null : nearest;
    }

    /**
     * @return the nearest nodes to the screen point given that is not
     * in ignore.
     *
     * @param p the point for which to search the nearest segment.
     * @param ignore a collection of nodes which are not to be returned.
     * @param predicate the returned objects have to fulfill certain properties.
     * May be null.
     */
    public final Collection<Node> getNearestNodes(Point p, Collection<Node> ignore, Predicate<OsmPrimitive> predicate) {
        Collection<Node> nearest = getNearestNodes(p, predicate);
        if (nearest == null) return null;
        if (ignore != null) {
            nearest.removeAll(ignore);
        }
        return nearest.isEmpty() ? null : nearest;
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
