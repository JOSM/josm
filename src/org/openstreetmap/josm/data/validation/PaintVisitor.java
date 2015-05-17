// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.validation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.MapView;

/**
 * Visitor that highlights the primitives affected by an error
 * @author frsantos
 * @since 5671
 */
public class PaintVisitor extends AbstractVisitor implements ValidatorVisitor {
    /** The graphics */
    private final Graphics g;
    /** The MapView */
    private final MapView mv;

    /** The severity color */
    private Color color;
    /** Is the error selected ? */
    private boolean selected;

    private final Set<PaintedPoint> paintedPoints = new HashSet<>();
    private final Set<PaintedSegment> paintedSegments = new HashSet<>();

    /**
     * Constructor
     * @param g The graphics
     * @param mv The Mapview
     */
    public PaintVisitor(Graphics g, MapView mv) {
        this.g = g;
        this.mv = mv;
    }

    protected static class PaintedPoint {
        protected final LatLon p1;
        protected final Color color;

        public PaintedPoint(LatLon p1, Color color) {
            this.p1 = p1;
            this.color = color;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + p1.hashCode();
            result = prime * result + color.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PaintedPoint other = (PaintedPoint) obj;
            if (!p1.equals(other.p1))
                return false;
            if (!color.equals(other.color))
                return false;
            return true;
        }
    }

    protected static class PaintedSegment extends PaintedPoint {
        private final LatLon p2;

        public PaintedSegment(LatLon p1, LatLon p2, Color color) {
            super(p1, color);
            this.p2 = p2;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + p2.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj)) {
                return false;
            }
            PaintedSegment other = (PaintedSegment) obj;
            if (!p2.equals(other.p2))
                return false;
            return true;
        }
    }

    @Override
    public void visit(TestError error) {
        if (error != null && !error.isIgnored()) {
            color = error.getSeverity().getColor();
            selected = error.isSelected();
            error.visitHighlighted(this);
        }
    }

    @Override
    public void visit(OsmPrimitive p) {
        if (p.isUsable()) {
            p.accept(this);
        }
    }

    /**
     * Draws a circle around the node
     * @param n The node
     * @param color The circle color
     */
    protected void drawNode(Node n, Color color) {
        PaintedPoint pp = new PaintedPoint(n.getCoor(), color);

        if (!paintedPoints.contains(pp)) {
            Point p = mv.getPoint(n);
            g.setColor(color);

            if (selected) {
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
            } else {
                g.drawOval(p.x - 5, p.y - 5, 10, 10);
            }
            paintedPoints.add(pp);
        }
    }

    /**
     * Draws a line around the segment
     *
     * @param p1 The first point of segment
     * @param p2 The second point of segment
     * @param color The color
     */
    protected void drawSegment(Point p1, Point p2, Color color) {
        g.setColor(color);

        double t = Math.atan2(p2.x - p1.x, p2.y - p1.y);
        double cosT = 5 * Math.cos(t);
        double sinT = 5 * Math.sin(t);
        int deg = (int) Math.toDegrees(t);
        if (selected) {
            int[] x = new int[] { (int) (p1.x + cosT), (int) (p2.x + cosT),
                                  (int) (p2.x - cosT), (int) (p1.x - cosT) };
            int[] y = new int[] { (int) (p1.y - sinT), (int) (p2.y - sinT),
                                  (int) (p2.y + sinT), (int) (p1.y + sinT) };
            g.fillPolygon(x, y, 4);
            g.fillArc(p1.x - 5, p1.y - 5, 10, 10, deg,  180);
            g.fillArc(p2.x - 5, p2.y - 5, 10, 10, deg, -180);
        } else {
            g.drawLine((int) (p1.x + cosT), (int) (p1.y - sinT),
                       (int) (p2.x + cosT), (int) (p2.y - sinT));
            g.drawLine((int) (p1.x - cosT), (int) (p1.y + sinT),
                       (int) (p2.x - cosT), (int) (p2.y + sinT));
            g.drawArc(p1.x - 5, p1.y - 5, 10, 10, deg,  180);
            g.drawArc(p2.x - 5, p2.y - 5, 10, 10, deg, -180);
        }
    }

    /**
     * Draws a line around the segment
     *
     * @param n1 The first node of segment
     * @param n2 The second node of segment
     * @param color The color
     */
    protected void drawSegment(Node n1, Node n2, Color color) {
        if (n1.isDrawable() && n2.isDrawable() && isSegmentVisible(n1, n2)) {
            PaintedSegment ps = new PaintedSegment(n1.getCoor(), n2.getCoor(), color);
            if (!paintedSegments.contains(ps)) {
                drawSegment(mv.getPoint(n1), mv.getPoint(n2), color);
                paintedSegments.add(ps);
            }
        }
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    @Override
    public void visit(Node n) {
        if (n.isDrawable() && isNodeVisible(n)) {
            drawNode(n, color);
        }
    }

    @Override
    public void visit(Way w) {
        visit(w.getNodes());
    }

    @Override
    public void visit(WaySegment ws) {
        if (ws.lowerIndex < 0 || ws.lowerIndex + 1 >= ws.way.getNodesCount())
            return;
        Node a = ws.way.getNodes().get(ws.lowerIndex);
        Node b = ws.way.getNodes().get(ws.lowerIndex + 1);
        drawSegment(a, b, color);
    }

    @Override
    public void visit(Relation r) {
        /* No idea how to draw a relation. */
    }

    /**
     * Checks if the given node is in the visible area.
     * @param n The node to check for visibility
     * @return true if the node is visible
     */
    protected boolean isNodeVisible(Node n) {
        Point p = mv.getPoint(n);
        return !((p.x < 0) || (p.y < 0) || (p.x > mv.getWidth()) || (p.y > mv.getHeight()));
    }

    /**
     * Checks if the given segment is in the visible area.
     * NOTE: This will return true for a small number of non-visible
     *       segments.
     * @param n1 The first point of the segment to check
     * @param n2 The second point of the segment to check
     * @return {@code true} if the segment is visible
     */
    protected boolean isSegmentVisible(Node n1, Node n2) {
        Point p1 = mv.getPoint(n1);
        Point p2 = mv.getPoint(n2);
        if ((p1.x < 0) && (p2.x < 0))
            return false;
        if ((p1.y < 0) && (p2.y < 0))
            return false;
        if ((p1.x > mv.getWidth()) && (p2.x > mv.getWidth()))
            return false;
        if ((p1.y > mv.getHeight()) && (p2.y > mv.getHeight()))
            return false;
        return true;
    }

    @Override
    public void visit(List<Node> nodes) {
        Node lastN = null;
        for (Node n : nodes) {
            if (lastN == null) {
                lastN = n;
                continue;
            }
            drawSegment(lastN, n, color);
            lastN = n;
        }
    }

    /**
     * Clears the internal painted objects collections.
     */
    public void clearPaintedObjects() {
        paintedPoints.clear();
        paintedSegments.clear();
    }
}
