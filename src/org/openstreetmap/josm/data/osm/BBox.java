package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

public class BBox {

    private double xmin = Double.POSITIVE_INFINITY;
    private double xmax = Double.NEGATIVE_INFINITY;
    private double ymin = Double.POSITIVE_INFINITY;
    private double ymax = Double.NEGATIVE_INFINITY;

    public BBox(Bounds bounds) {
        add(bounds.getMin());
        add(bounds.getMax());
    }

    public BBox(LatLon a, LatLon b) {
        add(a);
        add(b);
    }

    public BBox(BBox copy) {
        this.xmin = copy.xmin;
        this.xmax = copy.xmax;
        this.ymin = copy.ymin;
        this.ymax = copy.ymax;
    }

    public BBox(double a_x, double a_y, double b_x, double b_y)  {
        xmin = Math.min(a_x, b_x);
        xmax = Math.max(a_x, b_x);
        ymin = Math.min(a_y, b_y);
        ymax = Math.max(a_y, b_y);
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

    public void add(double x, double y) {
        xmin = Math.min(xmin, x);
        xmax = Math.max(xmax, x);
        ymin = Math.min(ymin, y);
        ymax = Math.max(ymax, y);
        sanity();
    }

    public void add(BBox box) {
        add(box.getTopLeft());
        add(box.getBottomRight());
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

    public boolean bounds(BBox b) {
        if (!(xmin <= b.xmin) ||
                !(xmax >= b.xmax) ||
                !(ymin <= b.ymin) ||
                !(ymax >= b.ymax))
            return false;
        return true;
    }

    public boolean bounds(LatLon c) {
        if ((xmin <= c.lon()) &&
                (xmax >= c.lon()) &&
                (ymin <= c.lat()) &&
                (ymax >= c.lat()))
            return true;
        return false;
    }

    public boolean inside(BBox b) {
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

    public boolean intersects(BBox b) {
        return this.inside(b) || b.inside(this);
    }

    public List<LatLon> points()  {
        LatLon p1 = new LatLon(ymin, xmin);
        LatLon p2 = new LatLon(ymin, xmax);
        LatLon p3 = new LatLon(ymax, xmin);
        LatLon p4 = new LatLon(ymax, xmax);
        List<LatLon> ret = new ArrayList<LatLon>(4);
        ret.add(p1);
        ret.add(p2);
        ret.add(p3);
        ret.add(p4);
        return ret;
    }

    public LatLon getTopLeft() {
        return new LatLon(ymax, xmin);
    }

    public LatLon getBottomRight() {
        return new LatLon(ymin, xmax);
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
}
