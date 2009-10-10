// License: GPL. See LICENSE file for details.

package org.openstreetmap.josm.gui;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JComponent;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.CachedLatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.help.Helpful;

/**
 * An component that can be navigated by a mapmover. Used as map view and for the
 * zoomer in the download dialog.
 *
 * @author imi
 */
public class NavigatableComponent extends JComponent implements Helpful {

    public static final int snapDistance = sqr(Main.pref.getInteger("node.snap-distance", 10));

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
        double lat = (b.max.lat() + b.min.lat())/2;
        double lon = (b.max.lon() + b.min.lon())/2;

        return Main.proj.latlon2eastNorth(new LatLon(lat, lon));
    }

    /**
     * Return a ID which is unique as long as viewport dimensions are the same
     */
    public Integer getViewID()
    {
        String x = center.east() + "_" + center.north() + "_" + scale + "_" +
        getWidth() + "_" + getHeight() + "_" + getProjection().toString();
        java.util.zip.CRC32 id = new java.util.zip.CRC32();
        id.update(x.getBytes());
        return new Long(id.getValue()).intValue();
    }

    public String getDist100PixelText()
    {
        double dist = getDist100Pixel();
        return dist >= 2000 ? Math.round(dist/100)/10 +" km" : (dist >= 1
                ? Math.round(dist*10)/10 +" m" : "< 1 m");
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
        return new ProjectionBounds(getProjection().latlon2eastNorth(b.min),
                getProjection().latlon2eastNorth(b.max));
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
        boolean rep = false;

        Bounds b = getProjection().getWorldBoundsLatLon();
        CachedLatLon cl = new CachedLatLon(newCenter);
        boolean changed = false;
        double lat = cl.lat();
        double lon = cl.lon();
        if(lat < b.min.lat()) {changed = true; lat = b.min.lat(); }
        else if(lat > b.max.lat()) {changed = true; lat = b.max.lat(); }
        if(lon < b.min.lon()) {changed = true; lon = b.min.lon(); }
        else if(lon > b.max.lon()) {changed = true; lon = b.max.lon(); }
        if(changed) {
            newCenter = new CachedLatLon(lat, lon).getEastNorth();
        }
        if (!newCenter.equals(center)) {
            EastNorth oldCenter = center;
            center = newCenter;
            rep = true;
            firePropertyChange("center", oldCenter, newCenter);
        }

        int width = getWidth()/2;
        int height = getHeight()/2;
        LatLon l1 = new LatLon(b.min.lat(), lon);
        LatLon l2 = new LatLon(b.max.lat(), lon);
        EastNorth e1 = getProjection().latlon2eastNorth(l1);
        EastNorth e2 = getProjection().latlon2eastNorth(l2);
        double d = e2.north() - e1.north();
        if(d < height*newScale)
        {
            double newScaleH = d/height;
            e1 = getProjection().latlon2eastNorth(new LatLon(lat, b.min.lon()));
            e2 = getProjection().latlon2eastNorth(new LatLon(lat, b.max.lon()));
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
        if (scale != newScale) {
            double oldScale = scale;
            scale = newScale;
            rep = true;
            firePropertyChange("scale", oldScale, newScale);
        }

        if(rep) {
            repaint();
        }
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
        zoomTo(new ProjectionBounds(getProjection().latlon2eastNorth(box.min),
                getProjection().latlon2eastNorth(box.max)));
    }

    /**
     * Return the nearest point to the screen point given.
     * If a node within snapDistance pixel is found, the nearest node is returned.
     */
    public final Node getNearestNode(Point p) {
        double minDistanceSq = snapDistance;
        Node minPrimitive = null;
        DataSet ds = getCurrentDataSet();
        if(ds == null)
            return null;
        for (Node n : ds.nodes) {
            if (!n.isUsable()) {
                continue;
            }
            Point sp = getPoint(n);
            double dist = p.distanceSq(sp);
            if (dist < minDistanceSq) {
                minDistanceSq = dist;
                minPrimitive = n;
            }
            // when multiple nodes on one point, prefer new or selected nodes
            else if(dist == minDistanceSq && minPrimitive != null
                    && ((n.getId() == 0 && ds.isSelected(n))
                            || (!ds.isSelected(minPrimitive) && (ds.isSelected(n) || n.getId() == 0)))) {
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
     */
    public final List<WaySegment> getNearestWaySegments(Point p) {
        TreeMap<Double, List<WaySegment>> nearest = new TreeMap<Double, List<WaySegment>>();
        DataSet ds = getCurrentDataSet();
        if(ds == null)
            return null;
        for (Way w : ds.ways) {
            if (!w.isUsable()) {
                continue;
            }
            Node lastN = null;
            int i = -2;
            for (Node n : w.getNodes()) {
                i++;
                if (n.isDeleted() || n.incomplete) {
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
                double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
                if (perDist < snapDistance && a < c+snapDistance && b < c+snapDistance) {
                    if(ds.isSelected(w)) {
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
     * May be null.
     */
    public final WaySegment getNearestWaySegment(Point p, Collection<WaySegment> ignore) {
        List<WaySegment> nearest = getNearestWaySegments(p);
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
    public final WaySegment getNearestWaySegment(Point p) {
        return getNearestWaySegment(p, null);
    }

    /**
     * @return the nearest way to the screen point given.
     */
    public final Way getNearestWay(Point p) {
        WaySegment nearestWaySeg = getNearestWaySegment(p);
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
     * @return  The primitive that is nearest to the point p.
     */
    public OsmPrimitive getNearest(Point p) {
        OsmPrimitive osm = getNearestNode(p);
        if (osm == null)
        {
            osm = getNearestWay(p);
        }
        return osm;
    }

    /**
     * Returns a singleton of the nearest object, or else an empty collection.
     */
    public Collection<OsmPrimitive> getNearestCollection(Point p) {
        OsmPrimitive osm = getNearest(p);
        if (osm == null)
            return Collections.emptySet();
        return Collections.singleton(osm);
    }

    /**
     * @return A list of all objects that are nearest to
     * the mouse.  Does a simple sequential scan on all the data.
     *
     * @return A collection of all items or <code>null</code>
     *      if no item under or near the point. The returned
     *      list is never empty.
     */
    public Collection<OsmPrimitive> getAllNearest(Point p) {
        Collection<OsmPrimitive> nearest = new HashSet<OsmPrimitive>();
        DataSet ds = getCurrentDataSet();
        if(ds == null)
            return null;
        for (Way w : ds.ways) {
            if (!w.isUsable()) {
                continue;
            }
            Node lastN = null;
            for (Node n : w.getNodes()) {
                if (!n.isUsable()) {
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
                double perDist = a-(a-b+c)*(a-b+c)/4/c; // perpendicular distance squared
                if (perDist < snapDistance && a < c+snapDistance && b < c+snapDistance) {
                    nearest.add(w);
                    break;
                }
                lastN = n;
            }
        }
        for (Node n : ds.nodes) {
            if (n.isUsable()
                    && getPoint(n).distanceSq(p) < snapDistance) {
                nearest.add(n);
            }
        }
        return nearest.isEmpty() ? null : nearest;
    }

    /**
     * @return A list of all nodes that are nearest to
     * the mouse.  Does a simple sequential scan on all the data.
     *
     * @return A collection of all nodes or <code>null</code>
     *      if no node under or near the point. The returned
     *      list is never empty.
     */
    public Collection<Node> getNearestNodes(Point p) {
        Collection<Node> nearest = new HashSet<Node>();
        DataSet ds = getCurrentDataSet();
        if(ds == null)
            return null;
        for (Node n : ds.nodes) {
            if (n.isUsable()
                    && getPoint(n).distanceSq(p) < snapDistance) {
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
     * May be null.
     */
    public final Collection<Node> getNearestNodes(Point p, Collection<Node> ignore) {
        Collection<Node> nearest = getNearestNodes(p);
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
}
