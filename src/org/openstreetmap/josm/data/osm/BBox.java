// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.QuadTiling;
import org.openstreetmap.josm.tools.Utils;

public class BBox {

    private double xmin = Double.POSITIVE_INFINITY;
    private double xmax = Double.NEGATIVE_INFINITY;
    private double ymin = Double.POSITIVE_INFINITY;
    private double ymax = Double.NEGATIVE_INFINITY;

    /**
     * Constructs a new {@code BBox} defined by a single point.
     * 
     * @param x X coordinate
     * @param y Y coordinate
     * @since 6203
     */
    public BBox(final double x, final double y) {
        xmax = xmin = x;
        ymax = ymin = y;
        sanity();
    }

    /**
     * Constructs a new {@code BBox} defined by points <code>a</code> and <code>b</code>.
     * Result is minimal BBox containing both points.
     * 
     * @param a 
     * @param b
     */
    public BBox(LatLon a, LatLon b) {
        this(a.lon(), a.lat(), b.lon(), b.lat());
    }

    /**
     * Constructs a new {@code BBox} from another one.
     * 
     * @param copy the BBox to copy
     */
    public BBox(BBox copy) {
        this.xmin = copy.xmin;
        this.xmax = copy.xmax;
        this.ymin = copy.ymin;
        this.ymax = copy.ymax;
    }

    public BBox(double a_x, double a_y, double b_x, double b_y)  {
        
        if (a_x > b_x) {
            xmax = a_x;
            xmin = b_x;
        } else {
            xmax = b_x;
            xmin = a_x;
        }
        
        if (a_y > b_y) {
            ymax = a_y;
            ymin = b_y;
        } else {
            ymax = b_y;
            ymin = a_y;
        }
        
        sanity();
    }

    public BBox(Way w) {
        for (Node n : w.getNodes()) {
            LatLon coor = n.getCoor();
            if (coor == null) {
                continue;
            }
            add(coor);
        }
    }

    public BBox(Node n) {
        LatLon coor = n.getCoor();
        if (coor == null) {
            xmin = xmax = ymin = ymax = 0;
        } else {
            xmin = xmax = coor.lon();
            ymin = ymax = coor.lat();
        }
    }

    private void sanity()  {
        if (xmin < -180.0) {
            xmin = -180.0;
        }
        if (xmax >  180.0) {
            xmax =  180.0;
        }
        if (ymin <  -90.0) {
            ymin =  -90.0;
        }
        if (ymax >   90.0) {
            ymax =   90.0;
        }
    }

    public void add(LatLon c) {
        add(c.lon(), c.lat());
    }

    /**
     * Extends this bbox to include the point (x, y)
     */
    public void add(double x, double y) {
        xmin = Math.min(xmin, x);
        xmax = Math.max(xmax, x);
        ymin = Math.min(ymin, y);
        ymax = Math.max(ymax, y);
        sanity();
    }

    public void add(BBox box) {
        xmin = Math.min(xmin, box.xmin);
        xmax = Math.max(xmax, box.xmax);
        ymin = Math.min(ymin, box.ymin);
        ymax = Math.max(ymax, box.ymax);
        sanity();
    }

    public void addPrimitive(OsmPrimitive primitive, double extraSpace) {
        BBox primBbox = primitive.getBBox();
        add(primBbox.xmin - extraSpace, primBbox.ymin - extraSpace);
        add(primBbox.xmax + extraSpace, primBbox.ymax + extraSpace);
    }

    public double height() {
        return ymax-ymin;
    }

    public double width() {
        return xmax-xmin;
    }

    /**
     * Tests, weather the bbox b lies completely inside
     * this bbox.
     */
    public boolean bounds(BBox b) {
        if (!(xmin <= b.xmin) ||
                !(xmax >= b.xmax) ||
                !(ymin <= b.ymin) ||
                !(ymax >= b.ymax))
            return false;
        return true;
    }

    /**
     * Tests, weather the Point c lies within the bbox.
     */
    public boolean bounds(LatLon c) {
        if ((xmin <= c.lon()) &&
                (xmax >= c.lon()) &&
                (ymin <= c.lat()) &&
                (ymax >= c.lat()))
            return true;
        return false;
    }

    /**
     * Tests, weather two BBoxes intersect as an area.
     * I.e. whether there exists a point that lies in both of them.
     */
    public boolean intersects(BBox b) {
        if (xmin > b.xmax)
            return false;
        if (xmax < b.xmin)
            return false;
        if (ymin > b.ymax)
            return false;
        if (ymax < b.ymin)
            return false;
        return true;
    }

    /**
     * Returns the top-left point.
     * @return The top-left point
     */
    public LatLon getTopLeft() {
        return new LatLon(ymax, xmin);
    }

    /**
     * Returns the latitude of top-left point.
     * @return The latitude of top-left point
     * @since 6203
     */
    public double getTopLeftLat() {
        return ymax;
    }

    /**
     * Returns the longitude of top-left point.
     * @return The longitude of top-left point
     * @since 6203
     */
    public double getTopLeftLon() {
        return xmin;
    }

    /**
     * Returns the bottom-right point.
     * @return The bottom-right point
     */
    public LatLon getBottomRight() {
        return new LatLon(ymin, xmax);
    }

    /**
     * Returns the latitude of bottom-right point.
     * @return The latitude of bottom-right point
     * @since 6203
     */
    public double getBottomRightLat() {
        return ymin;
    }

    /**
     * Returns the longitude of bottom-right point.
     * @return The longitude of bottom-right point
     * @since 6203
     */
    public double getBottomRightLon() {
        return xmax;
    }

    public LatLon getCenter() {
        return new LatLon(ymin + (ymax-ymin)/2.0, xmin + (xmax-xmin)/2.0);
    }

    int getIndex(final int level) {

        int idx1 = QuadTiling.index(ymin, xmin, level);

        final int idx2 = QuadTiling.index(ymin, xmax, level);
        if (idx1 == -1) idx1 = idx2;
        else if (idx1 != idx2) return -1;

        final int idx3 = QuadTiling.index(ymax, xmin, level);
        if (idx1 == -1) idx1 = idx3;
        else if (idx1 != idx3) return -1;

        final int idx4 = QuadTiling.index(ymax, xmax, level);
        if (idx1 == -1) idx1 = idx4;
        else if (idx1 != idx4) return -1;

        return idx1;
    }

    @Override
    public int hashCode() {
        return (int)(ymin * xmin);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof BBox) {
            BBox b = (BBox)o;
            return b.xmax == xmax && b.ymax == ymax && b.xmin == xmin && b.ymin == ymin;
        } else
            return false;
    }

    @Override
    public String toString() {
        return "[ x: " + xmin + " -> " + xmax +
        ", y: " + ymin + " -> " + ymax + " ]";
    }

    public String toStringCSV(String separator) {
        return Utils.join(separator, Arrays.asList(
                LatLon.cDdFormatter.format(xmin),
                LatLon.cDdFormatter.format(ymin),
                LatLon.cDdFormatter.format(xmax),
                LatLon.cDdFormatter.format(ymax)));
    }
}
