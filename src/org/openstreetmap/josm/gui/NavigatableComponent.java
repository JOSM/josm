// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.NumberFormat;
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
import java.util.Map.Entry;
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
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.gui.download.DownloadDialog;
import org.openstreetmap.josm.gui.help.Helpful;
import org.openstreetmap.josm.gui.preferences.projection.ProjectionPreference;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

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
     * Interface to notify listeners of the change of the system of measurement.
     * @since 6056
     */
    public interface SoMChangeListener {
        /**
         * The current SoM has changed.
         * @param oldSoM The old system of measurement
         * @param newSoM The new (current) system of measurement
         */
        void systemOfMeasurementChanged(String oldSoM, String newSoM);
    }

    /**
     * Simple data class that keeps map center and scale in one object.
     */
    public static class ViewportData {
        private EastNorth center;
        private Double scale;

        public ViewportData(EastNorth center, Double scale) {
            this.center = center;
            this.scale = scale;
        }

        /**
         * Return the projected coordinates of the map center
         * @return the center
         */
        public EastNorth getCenter() {
            return center;
        }

        /**
         * Return the scale factor in east-/north-units per pixel.
         * @return the scale
         */
        public Double getScale() {
            return scale;
        }
    }

    public static final IntegerProperty PROP_SNAP_DISTANCE = new IntegerProperty("mappaint.node.snap-distance", 10);

    public static final String PROPNAME_CENTER = "center";
    public static final String PROPNAME_SCALE  = "scale";

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
     * the SoM listeners
     */
    private static final CopyOnWriteArrayList<SoMChangeListener> somChangeListeners = new CopyOnWriteArrayList<SoMChangeListener>();

    /**
     * Removes a SoM change listener
     *
     * @param listener the listener. Ignored if null or already absent
     * @since 6056
     */
    public static void removeSoMChangeListener(NavigatableComponent.SoMChangeListener listener) {
        somChangeListeners.remove(listener);
    }

    /**
     * Adds a SoM change listener
     *
     * @param listener the listener. Ignored if null or already registered.
     * @since 6056
     */
    public static void addSoMChangeListener(NavigatableComponent.SoMChangeListener listener) {
        if (listener != null) {
            somChangeListeners.addIfAbsent(listener);
        }
    }

    protected static void fireSoMChanged(String oldSoM, String newSoM) {
        for (SoMChangeListener l : somChangeListeners) {
            l.systemOfMeasurementChanged(oldSoM, newSoM);
        }
    }

    /**
     * The scale factor in x or y-units per pixel. This means, if scale = 10,
     * every physical pixel on screen are 10 x or 10 y units in the
     * northing/easting space of the projection.
     */
    private double scale = Main.getProjection().getDefaultZoomInPPD();
    /**
     * Center n/e coordinate of the desired screen center.
     */
    protected EastNorth center = calculateDefaultCenter();

    private final Object paintRequestLock = new Object();
    private Rectangle paintRect = null;
    private Polygon paintPoly = null;

    /**
     * Constructs a new {@code NavigatableComponent}.
     */
    public NavigatableComponent() {
        setLayout(null);
    }

    protected DataSet getCurrentDataSet() {
        return Main.main.getCurrentDataSet();
    }

    private EastNorth calculateDefaultCenter() {
        Bounds b = DownloadDialog.getSavedDownloadBounds();
        if (b == null) {
            b = Main.getProjection().getWorldBoundsLatLon();
        }
        return Main.getProjection().latlon2eastNorth(b.getCenter());
    }

    /**
     * Returns the text describing the given distance in the current system of measurement.
     * @param dist The distance in metres.
     * @return the text describing the given distance in the current system of measurement.
     * @since 3406
     */
    public static String getDistText(double dist) {
        return getSystemOfMeasurement().getDistText(dist);
    }

    /**
     * Returns the text describing the given area in the current system of measurement.
     * @param area The distance in square metres.
     * @return the text describing the given area in the current system of measurement.
     * @since 5560
     */
    public static String getAreaText(double area) {
        return getSystemOfMeasurement().getAreaText(area);
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

    public double getScale() {
        return scale;
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

        Bounds result = new Bounds(Main.getProjection().eastNorth2latlon(p1));

        double eastMin = Math.min(p1.east(), p2.east());
        double eastMax = Math.max(p1.east(), p2.east());
        double northMin = Math.min(p1.north(), p2.north());
        double northMax = Math.max(p1.north(), p2.north());
        double deltaEast = (eastMax - eastMin) / 10;
        double deltaNorth = (northMax - northMin) / 10;

        for (int i=0; i < 10; i++) {
            result.extend(Main.getProjection().eastNorth2latlon(new EastNorth(eastMin + i * deltaEast, northMin)));
            result.extend(Main.getProjection().eastNorth2latlon(new EastNorth(eastMin + i * deltaEast, northMax)));
            result.extend(Main.getProjection().eastNorth2latlon(new EastNorth(eastMin, northMin  + i * deltaNorth)));
            result.extend(Main.getProjection().eastNorth2latlon(new EastNorth(eastMax, northMin  + i * deltaNorth)));
        }

        return result;
    }

    public AffineTransform getAffineTransform() {
        return new AffineTransform(
                1.0/scale, 0.0, 0.0, -1.0/scale, getWidth()/2.0 - center.east()/scale, getHeight()/2.0 + center.north()/scale);
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
     * @param newScale The scale to use.
     */
    public void zoomTo(EastNorth newCenter, double newScale) {
        Bounds b = getProjection().getWorldBoundsLatLon();
        LatLon cl = Projections.inverseProject(newCenter);
        boolean changed = false;
        double lat = cl.lat();
        double lon = cl.lon();
        if(lat < b.getMinLat()) {changed = true; lat = b.getMinLat(); }
        else if(lat > b.getMaxLat()) {changed = true; lat = b.getMaxLat(); }
        if(lon < b.getMinLon()) {changed = true; lon = b.getMinLon(); }
        else if(lon > b.getMaxLon()) {changed = true; lon = b.getMaxLon(); }
        if(changed) {
            newCenter = Projections.project(new LatLon(lat,lon));
        }
        int width = getWidth()/2;
        int height = getHeight()/2;
        if (width == 0 || height == 0) {
            throw new IllegalStateException("Cannot zoom into undimensioned NavigatableComponent");
        }
        LatLon l1 = new LatLon(b.getMinLat(), lon);
        LatLon l2 = new LatLon(b.getMaxLat(), lon);
        EastNorth e1 = getProjection().latlon2eastNorth(l1);
        EastNorth e2 = getProjection().latlon2eastNorth(l2);
        double d = e2.north() - e1.north();
        if(d < height*newScale)
        {
            double newScaleH = d/height;
            e1 = getProjection().latlon2eastNorth(new LatLon(lat, b.getMinLon()));
            e2 = getProjection().latlon2eastNorth(new LatLon(lat, b.getMaxLon()));
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
     * @param newScale The scale to use.
     */
    private void zoomNoUndoTo(EastNorth newCenter, double newScale) {
        if (!newCenter.equals(center)) {
            EastNorth oldCenter = center;
            center = newCenter;
            firePropertyChange(PROPNAME_CENTER, oldCenter, newCenter);
        }
        if (scale != newScale) {
            double oldScale = scale;
            scale = newScale;
            firePropertyChange(PROPNAME_SCALE, oldScale, newScale);
        }

        repaint();
        fireZoomChanged();
    }

    public void zoomTo(EastNorth newCenter) {
        zoomTo(newCenter, scale);
    }

    public void zoomTo(LatLon newCenter) {
        zoomTo(Projections.project(newCenter));
    }

    public void smoothScrollTo(LatLon newCenter) {
        smoothScrollTo(Projections.project(newCenter));
    }

    /**
     * Create a thread that moves the viewport to the given center in an
     * animated fashion.
     */
    public void smoothScrollTo(EastNorth newCenter) {
        // FIXME make these configurable.
        final int fps = 20;     // animation frames per second
        final int speed = 1500; // milliseconds for full-screen-width pan
        if (!newCenter.equals(center)) {
            final EastNorth oldCenter = center;
            final double distance = newCenter.distance(oldCenter) / scale;
            final double milliseconds = distance / getWidth() * speed;
            final double frames = milliseconds * fps / 1000;
            final EastNorth finalNewCenter = newCenter;

            new Thread(){
                @Override
                public void run() {
                    for (int i=0; i<frames; i++) {
                        // FIXME - not use zoom history here
                        zoomTo(oldCenter.interpolate(finalNewCenter, (i+1) / frames));
                        try {
                            Thread.sleep(1000 / fps);
                        } catch (InterruptedException ex) {
                            Main.warn("InterruptedException in "+NavigatableComponent.class.getSimpleName()+" during smooth scrolling");
                        }
                    }
                }
            }.start();
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

        double scaleX = (box.maxEast-box.minEast)/w;
        double scaleY = (box.maxNorth-box.minNorth)/h;
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
            this.center = Projections.inverseProject(center);
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
            for (Entry<Double, List<Node>> entry : nlists.entrySet()) {
                Double distSq = entry.getKey();
                List<Node> nlist = entry.getValue();

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
     * The *result* depends on the current map selection state IF use_selected is true.
     *
     * If more than one node within node.snap-distance pixels is found,
     * the nearest node selected is returned IF use_selected is true.
     *
     * Else the nearest new/id=0 node within about the same distance
     * as the true nearest node is returned.
     *
     * If no such node is found either, the true nearest
     * node to p is returned.
     *
     * Finally, if a node is not found at all, null is returned.
     *
     * @return A node within snap-distance to point p,
     *      that is chosen by the algorithm described.
     *
     * @param p the screen point
     * @param predicate this parameter imposes a condition on the returned object, e.g.
     *        give the nearest node that is tagged.
     */
    public final Node getNearestNode(Point p, Predicate<OsmPrimitive> predicate, boolean use_selected) {
        return getNearestNode(p, predicate, use_selected, null);
    }

    /**
     * The *result* depends on the current map selection state IF use_selected is true
     *
     * If more than one node within node.snap-distance pixels is found,
     * the nearest node selected is returned IF use_selected is true.
     *
     * If there are no selected nodes near that point, the node that is related to some of the preferredRefs
     *
     * Else the nearest new/id=0 node within about the same distance
     * as the true nearest node is returned.
     *
     * If no such node is found either, the true nearest
     * node to p is returned.
     *
     * Finally, if a node is not found at all, null is returned.
     * @since 6065
     * @return A node within snap-distance to point p,
     *      that is chosen by the algorithm described.
     *
     * @param p the screen point
     * @param predicate this parameter imposes a condition on the returned object, e.g.
     *        give the nearest node that is tagged.
     * @param preferredRefs primitives, whose nodes we prefer
     */
    public final Node getNearestNode(Point p, Predicate<OsmPrimitive> predicate,
            boolean useSelected, Collection<OsmPrimitive> preferredRefs) {

        Map<Double, List<Node>> nlists = getNearestNodesImpl(p, predicate);
        if (nlists.isEmpty()) return null;

        if (preferredRefs != null && preferredRefs.isEmpty()) preferredRefs = null;
        Node ntsel = null, ntnew = null, ntref = null;
        boolean useNtsel = useSelected;
        double minDistSq = nlists.keySet().iterator().next();

        for (Entry<Double, List<Node>> entry : nlists.entrySet()) {
            Double distSq = entry.getKey();
            for (Node nd : entry.getValue()) {
                // find the nearest selected node
                if (ntsel == null && nd.isSelected()) {
                    ntsel = nd;
                    // if there are multiple nearest nodes, prefer the one
                    // that is selected. This is required in order to drag
                    // the selected node if multiple nodes have the same
                    // coordinates (e.g. after unglue)
                    useNtsel |= (distSq == minDistSq);
                }
                if (ntref == null && preferredRefs != null && distSq == minDistSq) {
                    List<OsmPrimitive> ndRefs = nd.getReferrers();
                    for (OsmPrimitive ref: preferredRefs) {
                        if (ndRefs.contains(ref)) {
                            ntref = nd;
                            break;
                        }
                    }
                }
                // find the nearest newest node that is within about the same
                // distance as the true nearest node
                if (ntnew == null && nd.isNew() && (distSq-minDistSq < 1)) {
                    ntnew = nd;
                }
            }
        }

        // take nearest selected, nearest new or true nearest node to p, in that order
        if (ntsel != null && useNtsel)
            return ntsel;
        if (ntref != null)
            return ntref;
        if (ntnew != null)
            return ntnew;
        return nlists.values().iterator().next().get(0);
    }

    /**
     * Convenience method to {@link #getNearestNode(Point, Predicate, boolean)}.
     *
     * @return The nearest node to point p.
     */
    public final Node getNearestNode(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNode(p, predicate, true);
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
     * The *result* depends on the current map selection state IF use_selected is true.
     *
     * @return The nearest way segment to point p,
     *      and, depending on use_selected, prefers a selected way segment, if found.
     * @see #getNearestWaySegments(Point, Collection, Predicate)
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned object has to fulfill certain properties.
     * @param use_selected whether selected way segments should be preferred.
     */
    public final WaySegment getNearestWaySegment(Point p, Predicate<OsmPrimitive> predicate, boolean use_selected) {
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

        return (ntsel != null && use_selected) ? ntsel : wayseg;
    }

     /**
     * The *result* depends on the current map selection state IF use_selected is true.
     *
     * @return The nearest way segment to point p,
     *      and, depending on use_selected, prefers a selected way segment, if found.
     * Also prefers segments of ways that are related to one of preferredRefs primitives
     * @see #getNearestWaySegments(Point, Collection, Predicate)
     * @since 6065
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned object has to fulfill certain properties.
     * @param use_selected whether selected way segments should be preferred.
     * @param preferredRefs - prefer segments related to these primitives, may be null
     */
    public final WaySegment getNearestWaySegment(Point p, Predicate<OsmPrimitive> predicate,
            boolean use_selected,  Collection<OsmPrimitive> preferredRefs) {
        WaySegment wayseg = null, ntsel = null, ntref = null;
        if (preferredRefs != null && preferredRefs.isEmpty()) preferredRefs = null;

        searchLoop: for (List<WaySegment> wslist : getNearestWaySegmentsImpl(p, predicate).values()) {
            for (WaySegment ws : wslist) {
                if (wayseg == null) {
                    wayseg = ws;
                }
                if (ntsel == null && ws.way.isSelected()) {
                    ntsel = ws;
                    break searchLoop;
                }
                if (ntref == null && preferredRefs != null) {
                    // prefer ways containing given nodes
                    for (Node nd: ws.way.getNodes()) {
                        if (preferredRefs.contains(nd)) {
                            ntref = ws;
                            break searchLoop;
                        }
                    }
                    Collection<OsmPrimitive> wayRefs = ws.way.getReferrers();
                    // prefer member of the given relations
                    for (OsmPrimitive ref: preferredRefs) {
                        if (ref instanceof Relation && wayRefs.contains(ref)) {
                            ntref = ws;
                            break searchLoop;
                        }
                    }
                }
            }
        }
        if (ntsel != null && use_selected)
            return ntsel;
        if (ntref != null)
            return ntref;
        return wayseg;
    }

    /**
     * Convenience method to {@link #getNearestWaySegment(Point, Predicate, boolean)}.
     *
     * @return The nearest way segment to point p.
     */
    public final WaySegment getNearestWaySegment(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestWaySegment(p, predicate, true);
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
     * @see #getNearestWaySegment(Point, Predicate)
     *
     * @param p the point for which to search the nearest segment.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final Way getNearestWay(Point p, Predicate<OsmPrimitive> predicate) {
        WaySegment nearestWaySeg = getNearestWaySegment(p, predicate);
        return (nearestWaySeg == null) ? null : nearestWaySeg.way;
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
     * @see #getNearestNodesOrWays(Point, Collection, Predicate)
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     */
    public final List<OsmPrimitive> getNearestNodesOrWays(Point p, Predicate<OsmPrimitive> predicate) {
        return getNearestNodesOrWays(p, null, predicate);
    }

    /**
     * This is used as a helper routine to {@link #getNearestNodeOrWay(Point, Predicate, boolean)}
     * It decides, whether to yield the node to be tested or look for further (way) candidates.
     *
     * @return true, if the node fulfills the properties of the function body
     *
     * @param osm node to check
     * @param p point clicked
     * @param use_selected whether to prefer selected nodes
     */
    private boolean isPrecedenceNode(Node osm, Point p, boolean use_selected) {
        if (osm != null) {
            if (!(p.distanceSq(getPoint2D(osm)) > (4)*(4))) return true;
            if (osm.isTagged()) return true;
            if (use_selected && osm.isSelected()) return true;
        }
        return false;
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
     * @see #getNearestNode(Point, Predicate)
     * @see #getNearestNodesImpl(Point, Predicate)
     * @see #getNearestWay(Point, Predicate)
     *
     * @param p The point on screen.
     * @param predicate the returned object has to fulfill certain properties.
     * @param use_selected whether to prefer primitives that are currently selected or referred by selected primitives
     */
    public final OsmPrimitive getNearestNodeOrWay(Point p, Predicate<OsmPrimitive> predicate, boolean use_selected) {
        Collection<OsmPrimitive> sel;
        DataSet ds = getCurrentDataSet();
        if (use_selected && ds!=null) {
            sel = ds.getSelected();
        } else {
            sel = null;
        }
        OsmPrimitive osm = getNearestNode(p, predicate, use_selected, sel);

        if (isPrecedenceNode((Node)osm, p, use_selected)) return osm;
        WaySegment ws;
        if (use_selected) {
            ws = getNearestWaySegment(p, predicate, use_selected, sel);
        } else {
            ws = getNearestWaySegment(p, predicate, use_selected);
        }
        if (ws == null) return osm;

        if ((ws.way.isSelected() && use_selected) || osm == null) {
            // either (no _selected_ nearest node found, if desired) or no nearest node was found
            osm = ws.way;
        } else {
            int maxWaySegLenSq = 3*PROP_SNAP_DISTANCE.get();
            maxWaySegLenSq *= maxWaySegLenSq;

            Point2D wp1 = getPoint2D(ws.way.getNode(ws.lowerIndex));
            Point2D wp2 = getPoint2D(ws.way.getNode(ws.lowerIndex+1));

            // is wayseg shorter than maxWaySegLenSq and
            // is p closer to the middle of wayseg  than  to the nearest node?
            if (wp1.distanceSq(wp2) < maxWaySegLenSq &&
                    p.distanceSq(project(0.5, wp1, wp2)) < p.distanceSq(getPoint2D((Node)osm))) {
                osm = ws.way;
            }
        }
        return osm;
    }

    /**
     * @return o as collection of o's type.
     */
    public static <T> Collection<T> asColl(T o) {
        if (o == null)
            return Collections.emptySet();
        return Collections.singleton(o);
    }

    public static double perDist(Point2D pt, Point2D a, Point2D b) {
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
    public static Point2D project(Point2D pt, Point2D a, Point2D b) {
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
    public static Point2D project(double r, Point2D a, Point2D b) {
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

        // add nearby ways
        for (List<WaySegment> wss : getNearestWaySegmentsImpl(p, predicate).values()) {
            for (WaySegment ws : wss) {
                if (wset.add(ws.way)) {
                    nearestList.add(ws.way);
                }
            }
        }

        // add nearby nodes
        for (List<Node> nlist : getNearestNodesImpl(p, predicate).values()) {
            nearestList.addAll(nlist);
        }

        // add parent relations of nearby nodes and ways
        Set<OsmPrimitive> parentRelations = new HashSet<OsmPrimitive>();
        for (OsmPrimitive o : nearestList) {
            for (OsmPrimitive r : o.getReferrers()) {
                if (r instanceof Relation && predicate.evaluate(r)) {
                    parentRelations.add(r);
                }
            }
        }
        nearestList.addAll(parentRelations);

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
        return Main.getProjection();
    }

    @Override
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

    /**
     * Returns the current system of measurement.
     * @return The current system of measurement (metric system by default).
     * @since 3490
     */
    public static SystemOfMeasurement getSystemOfMeasurement() {
        SystemOfMeasurement som = SYSTEMS_OF_MEASUREMENT.get(ProjectionPreference.PROP_SYSTEM_OF_MEASUREMENT.get());
        if (som == null)
            return METRIC_SOM;
        return som;
    }

    /**
     * Sets the current system of measurement.
     * @param somKey The system of measurement key. Must be defined in {@link NavigatableComponent#SYSTEMS_OF_MEASUREMENT}.
     * @since 6056
     * @throws IllegalArgumentException if {@code somKey} is not known
     */
    public static void setSystemOfMeasurement(String somKey) {
        if (!SYSTEMS_OF_MEASUREMENT.containsKey(somKey)) {
            throw new IllegalArgumentException("Invalid system of measurement: "+somKey);
        }
        String oldKey = ProjectionPreference.PROP_SYSTEM_OF_MEASUREMENT.get();
        if (ProjectionPreference.PROP_SYSTEM_OF_MEASUREMENT.put(somKey)) {
            fireSoMChanged(oldKey, somKey);
        }
    }

    /**
     * A system of units used to express length and area measurements.
     * @since 3406
     */
    public static class SystemOfMeasurement {

        /** First value, in meters, used to translate unit according to above formula. */
        public final double aValue;
        /** Second value, in meters, used to translate unit according to above formula. */
        public final double bValue;
        /** First unit used to format text. */
        public final String aName;
        /** Second unit used to format text. */
        public final String bName;
        /** Specific optional area value, in squared meters, between {@code aValue*aValue} and {@code bValue*bValue}. Set to {@code -1} if not used.
         *  @since 5870 */
        public final double areaCustomValue;
        /** Specific optional area unit. Set to {@code null} if not used.
         *  @since 5870 */
        public final String areaCustomName;

        /**
         * System of measurement. Currently covers only length (and area) units.
         *
         * If a quantity x is given in m (x_m) and in unit a (x_a) then it translates as
         * x_a == x_m / aValue
         *
         * @param aValue First value, in meters, used to translate unit according to above formula.
         * @param aName First unit used to format text.
         * @param bValue Second value, in meters, used to translate unit according to above formula.
         * @param bName Second unit used to format text.
         */
        public SystemOfMeasurement(double aValue, String aName, double bValue, String bName) {
            this(aValue, aName, bValue, bName, -1, null);
        }

        /**
         * System of measurement. Currently covers only length (and area) units.
         *
         * If a quantity x is given in m (x_m) and in unit a (x_a) then it translates as
         * x_a == x_m / aValue
         *
         * @param aValue First value, in meters, used to translate unit according to above formula.
         * @param aName First unit used to format text.
         * @param bValue Second value, in meters, used to translate unit according to above formula.
         * @param bName Second unit used to format text.
         * @param areaCustomValue Specific optional area value, in squared meters, between {@code aValue*aValue} and {@code bValue*bValue}.
         *                        Set to {@code -1} if not used.
         * @param areaCustomName Specific optional area unit. Set to {@code null} if not used.
         *
         * @since 5870
         */
        public SystemOfMeasurement(double aValue, String aName, double bValue, String bName, double areaCustomValue, String areaCustomName) {
            this.aValue = aValue;
            this.aName = aName;
            this.bValue = bValue;
            this.bName = bName;
            this.areaCustomValue = areaCustomValue;
            this.areaCustomName = areaCustomName;
        }

        /**
         * Returns the text describing the given distance in this system of measurement.
         * @param dist The distance in metres
         * @return The text describing the given distance in this system of measurement.
         */
        public String getDistText(double dist) {
            return getDistText(dist, null, 0.01);
        }

        /**
         * Returns the text describing the given distance in this system of measurement.
         * @param dist The distance in metres
         * @param format A {@link NumberFormat} to format the area value
         * @param threshold Values lower than this {@code threshold} are displayed as {@code "< [threshold]"}
         * @return The text describing the given distance in this system of measurement.
         * @since 6422
         */
        public String getDistText(final double dist, final NumberFormat format, final double threshold) {
            double a = dist / aValue;
            if (!Main.pref.getBoolean("system_of_measurement.use_only_lower_unit", false) && a > bValue / aValue)
                return formatText(dist / bValue, bName, format);
            else if (a < threshold)
                return "< " + formatText(threshold, aName, format);
            else
                return formatText(a, aName, format);
        }

        /**
         * Returns the text describing the given area in this system of measurement.
         * @param area The area in square metres
         * @return The text describing the given area in this system of measurement.
         * @since 5560
         */
        public String getAreaText(double area) {
            return getAreaText(area, null, 0.01);
        }

        /**
         * Returns the text describing the given area in this system of measurement.
         * @param area The area in square metres
         * @param format A {@link NumberFormat} to format the area value
         * @param threshold Values lower than this {@code threshold} are displayed as {@code "< [threshold]"}
         * @return The text describing the given area in this system of measurement.
         * @since 6422
         */
        public String getAreaText(final double area, final NumberFormat format, final double threshold) {
            double a = area / (aValue*aValue);
            boolean lowerOnly = Main.pref.getBoolean("system_of_measurement.use_only_lower_unit", false);
            boolean customAreaOnly = Main.pref.getBoolean("system_of_measurement.use_only_custom_area_unit", false);
            if ((!lowerOnly && areaCustomValue > 0 && a > areaCustomValue / (aValue*aValue) && a < (bValue*bValue) / (aValue*aValue)) || customAreaOnly)
                return formatText(area / areaCustomValue, areaCustomName, format);
            else if (!lowerOnly && a >= (bValue*bValue) / (aValue*aValue))
                return formatText(area / (bValue * bValue), bName + "\u00b2", format);
            else if (a < threshold)
                return "< " + formatText(threshold, aName + "\u00b2", format);
            else
                return formatText(a, aName + "\u00b2", format);
        }

        private static String formatText(double v, String unit, NumberFormat format) {
            if (format != null) {
                return format.format(v) + " " + unit;
            }
            return String.format(Locale.US, "%." + (v<9.999999 ? 2 : 1) + "f %s", v, unit);
        }
    }

    /**
     * Metric system (international standard).
     * @since 3406
     */
    public static final SystemOfMeasurement METRIC_SOM = new SystemOfMeasurement(1, "m", 1000, "km", 10000, "ha");

    /**
     * Chinese system.
     * @since 3406
     */
    public static final SystemOfMeasurement CHINESE_SOM = new SystemOfMeasurement(1.0/3.0, "\u5e02\u5c3a" /* chi */, 500, "\u5e02\u91cc" /* li */);

    /**
     * Imperial system (British Commonwealth and former British Empire).
     * @since 3406
     */
    public static final SystemOfMeasurement IMPERIAL_SOM = new SystemOfMeasurement(0.3048, "ft", 1609.344, "mi", 4046.86, "ac");

    /**
     * Nautical mile system (navigation, polar exploration).
     * @since 5549
     */
    public static final SystemOfMeasurement NAUTICAL_MILE_SOM = new SystemOfMeasurement(185.2, "kbl", 1852, "NM");

    /**
     * Known systems of measurement.
     * @since 3406
     */
    public static final Map<String, SystemOfMeasurement> SYSTEMS_OF_MEASUREMENT;
    static {
        SYSTEMS_OF_MEASUREMENT = new LinkedHashMap<String, SystemOfMeasurement>();
        SYSTEMS_OF_MEASUREMENT.put(marktr("Metric"), METRIC_SOM);
        SYSTEMS_OF_MEASUREMENT.put(marktr("Chinese"), CHINESE_SOM);
        SYSTEMS_OF_MEASUREMENT.put(marktr("Imperial"), IMPERIAL_SOM);
        SYSTEMS_OF_MEASUREMENT.put(marktr("Nautical Mile"), NAUTICAL_MILE_SOM);
    }

    private static class CursorInfo {
        public Cursor cursor;
        public Object object;
        public CursorInfo(Cursor c, Object o) {
            cursor = c;
            object = o;
        }
    }

    private LinkedList<CursorInfo> cursors = new LinkedList<CursorInfo>();
    
    /**
     * Set new cursor.
     */
    public void setNewCursor(Cursor cursor, Object reference) {
        if (!cursors.isEmpty()) {
            CursorInfo l = cursors.getLast();
            if(l != null && l.cursor == cursor && l.object == reference)
                return;
            stripCursors(reference);
        }
        cursors.add(new CursorInfo(cursor, reference));
        setCursor(cursor);
    }
    
    public void setNewCursor(int cursor, Object reference) {
        setNewCursor(Cursor.getPredefinedCursor(cursor), reference);
    }
    
    /**
     * Remove the new cursor and reset to previous
     */
    public void resetCursor(Object reference) {
        if (cursors.isEmpty()) {
            setCursor(null);
            return;
        }
        CursorInfo l = cursors.getLast();
        stripCursors(reference);
        if (l != null && l.object == reference) {
            if (cursors.isEmpty()) {
                setCursor(null);
            } else {
                setCursor(cursors.getLast().cursor);
            }
        }
    }

    private void stripCursors(Object reference) {
        LinkedList<CursorInfo> c = new LinkedList<CursorInfo>();
        for(CursorInfo i : cursors) {
            if(i.object != reference) {
                c.add(i);
            }
        }
        cursors = c;
    }

    @Override
    public void paint(Graphics g) {
        synchronized (paintRequestLock) {
            if (paintRect != null) {
                Graphics g2 = g.create();
                g2.setColor(Utils.complement(PaintColors.getBackgroundColor()));
                g2.drawRect(paintRect.x, paintRect.y, paintRect.width, paintRect.height);
                g2.dispose();
            }
            if (paintPoly != null) {
                Graphics g2 = g.create();
                g2.setColor(Utils.complement(PaintColors.getBackgroundColor()));
                g2.drawPolyline(paintPoly.xpoints, paintPoly.ypoints, paintPoly.npoints);
                g2.dispose();
            }
        }
        super.paint(g);
    }

    /**
     * Requests to paint the given {@code Rectangle}.
     * @param r The Rectangle to draw
     * @see #requestClearRect
     * @since 5500
     */
    public void requestPaintRect(Rectangle r) {
        if (r != null) {
            synchronized (paintRequestLock) {
                paintRect = r;
            }
            repaint();
        }
    }

    /**
     * Requests to paint the given {@code Polygon} as a polyline (unclosed polygon).
     * @param p The Polygon to draw
     * @see #requestClearPoly
     * @since 5500
     */
    public void requestPaintPoly(Polygon p) {
        if (p != null) {
            synchronized (paintRequestLock) {
                paintPoly = p;
            }
            repaint();
        }
    }

    /**
     * Requests to clear the rectangled previously drawn.
     * @see #requestPaintRect
     * @since 5500
     */
    public void requestClearRect() {
        synchronized (paintRequestLock) {
            paintRect = null;
        }
        repaint();
    }

    /**
     * Requests to clear the polyline previously drawn.
     * @see #requestPaintPoly
     * @since 5500
     */
    public void requestClearPoly() {
        synchronized (paintRequestLock) {
            paintPoly = null;
        }
        repaint();
    }
}
