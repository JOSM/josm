// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.data.validation;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.gui.MapView;

/**
 * Validation error
 * @author frsantos
 */
public class TestError {
    /** is this error on the ignore list */
    private Boolean ignored = false;
    /** Severity */
    private Severity severity;
    /** The error message */
    private String message;
    /** Deeper error description */
    private String description;
    private String description_en;
    /** The affected primitives */
    private List<? extends OsmPrimitive> primitives;
    /** The primitives to be highlighted */
    private List<?> highlighted;
    /** The tester that raised this error */
    private Test tester;
    /** Internal code used by testers to classify errors */
    private int code;
    /** If this error is selected */
    private boolean selected;

    /**
     * Constructors
     * @param tester The tester
     * @param severity The severity of this error
     * @param message The error message
     * @param primitive The affected primitive
     * @param primitives The affected primitives
     * @param code The test error reference code
     */
    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, List<? extends OsmPrimitive> primitives, List<?> highlighted) {
        this.tester = tester;
        this.severity = severity;
        this.message = message;
        this.description = description;
        this.description_en = description_en;
        this.primitives = primitives;
        this.highlighted = highlighted;
        this.code = code;
    }

    public TestError(Test tester, Severity severity, String message, int code, List<? extends OsmPrimitive> primitives,
            List<?> highlighted) {
        this(tester, severity, message, null, null, code, primitives, highlighted);
    }

    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, List<? extends OsmPrimitive> primitives) {
        this(tester, severity, message, description, description_en, code, primitives, primitives);
    }

    public TestError(Test tester, Severity severity, String message, int code, List<? extends OsmPrimitive> primitives) {
        this(tester, severity, message, null, null, code, primitives, primitives);
    }

    public TestError(Test tester, Severity severity, String message, int code, OsmPrimitive primitive) {
        this(tester, severity, message, null, null, code, Collections.singletonList(primitive), Collections
                .singletonList(primitive));
    }

    public TestError(Test tester, Severity severity, String message, String description, String description_en,
            int code, OsmPrimitive primitive) {
        this(tester, severity, message, description, description_en, code, Collections.singletonList(primitive));
    }

    /**
     * Gets the error message
     * @return the error message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the error message
     * @return the error description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the error message
     * @param message The error message
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Gets the list of primitives affected by this error
     * @return the list of primitives affected by this error
     */
    public List<? extends OsmPrimitive> getPrimitives() {
        return primitives;
    }

    /**
     * Sets the list of primitives affected by this error
     * @param primitives the list of primitives affected by this error
     */

    public void setPrimitives(List<OsmPrimitive> primitives) {
        this.primitives = primitives;
    }

    /**
     * Gets the severity of this error
     * @return the severity of this error
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Sets the severity of this error
     * @param severity the severity of this error
     */
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    /**
     * Sets the ignore state for this error
     */
    public String getIgnoreState() {
        Collection<String> strings = new TreeSet<String>();
        String ignorestring = getIgnoreSubGroup();
        for (OsmPrimitive o : primitives) {
            // ignore data not yet uploaded
            if (o.isNew())
                return null;
            String type = "u";
            if (o instanceof Way) {
                type = "w";
            } else if (o instanceof Relation) {
                type = "r";
            } else if (o instanceof Node) {
                type = "n";
            }
            strings.add(type + "_" + o.getId());
        }
        for (String o : strings) {
            ignorestring += ":" + o;
        }
        return ignorestring;
    }

    public String getIgnoreSubGroup() {
        String ignorestring = getIgnoreGroup();
        if (description_en != null) {
            ignorestring += "_" + description_en;
        }
        return ignorestring;
    }

    public String getIgnoreGroup() {
        return Integer.toString(code);
    }

    public void setIgnored(boolean state) {
        ignored = state;
    }

    public Boolean getIgnored() {
        return ignored;
    }

    /**
     * Gets the tester that raised this error
     * @return the tester that raised this error
     */
    public Test getTester() {
        return tester;
    }

    /**
     * Gets the code
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns true if the error can be fixed automatically
     *
     * @return true if the error can be fixed
     */
    public boolean isFixable() {
        return tester != null && tester.isFixable(this);
    }

    /**
     * Fixes the error with the appropriate command
     *
     * @return The command to fix the error
     */
    public Command getFix() {
        if (tester == null)
            return null;

        return tester.fixError(this);
    }

    /**
     * Paints the error on affected primitives
     *
     * @param g The graphics
     * @param mv The MapView
     */
    public void paint(Graphics g, MapView mv) {
        if (!ignored) {
            PaintVisitor v = new PaintVisitor(g, mv);
            visitHighlighted(v);
        }
    }

    @SuppressWarnings("unchecked")
    public void visitHighlighted(ValidatorVisitor v) {
        for (Object o : highlighted) {
            if (o instanceof OsmPrimitive) {
                v.visit((OsmPrimitive) o);
            } else if (o instanceof WaySegment) {
                v.visit((WaySegment) o);
            } else if (o instanceof List<?>) {
                v.visit((List<Node>)o);
            }
        }
    }

    /**
     * Visitor that highlights the primitives affected by this error
     * @author frsantos
     */
    class PaintVisitor extends AbstractVisitor implements ValidatorVisitor {
        /** The graphics */
        private final Graphics g;
        /** The MapView */
        private final MapView mv;

        /**
         * Constructor
         * @param g The graphics
         * @param mv The Mapview
         */
        public PaintVisitor(Graphics g, MapView mv) {
            this.g = g;
            this.mv = mv;
        }

        @Override
        public void visit(OsmPrimitive p) {
            if (p.isUsable()) {
                p.visit(this);
            }
        }

        /**
         * Draws a circle around the node
         * @param n The node
         * @param color The circle color
         */
        public void drawNode(Node n, Color color) {
            Point p = mv.getPoint(n);
            g.setColor(color);
            if (selected) {
                g.fillOval(p.x - 5, p.y - 5, 10, 10);
            } else {
                g.drawOval(p.x - 5, p.y - 5, 10, 10);
            }
        }

        public void drawSegment(Point p1, Point p2, Color color) {
            g.setColor(color);

            double t = Math.atan2(p2.x - p1.x, p2.y - p1.y);
            double cosT = Math.cos(t);
            double sinT = Math.sin(t);
            int deg = (int) Math.toDegrees(t);
            if (selected) {
                int[] x = new int[] { (int) (p1.x + 5 * cosT), (int) (p2.x + 5 * cosT),
                                      (int) (p2.x - 5 * cosT), (int) (p1.x - 5 * cosT) };
                int[] y = new int[] { (int) (p1.y - 5 * sinT), (int) (p2.y - 5 * sinT),
                                      (int) (p2.y + 5 * sinT), (int) (p1.y + 5 * sinT) };
                g.fillPolygon(x, y, 4);
                g.fillArc(p1.x - 5, p1.y - 5, 10, 10, deg, 180);
                g.fillArc(p2.x - 5, p2.y - 5, 10, 10, deg, -180);
            } else {
                g.drawLine((int) (p1.x + 5 * cosT), (int) (p1.y - 5 * sinT),
                           (int) (p2.x + 5 * cosT), (int) (p2.y - 5 * sinT));
                g.drawLine((int) (p1.x - 5 * cosT), (int) (p1.y + 5 * sinT),
                           (int) (p2.x - 5 * cosT), (int) (p2.y + 5 * sinT));
                g.drawArc(p1.x - 5, p1.y - 5, 10, 10, deg, 180);
                g.drawArc(p2.x - 5, p2.y - 5, 10, 10, deg, -180);
            }
        }

        /**
         * Draws a line around the segment
         *
         * @param s The segment
         * @param color The color
         */
        public void drawSegment(Node n1, Node n2, Color color) {
            drawSegment(mv.getPoint(n1), mv.getPoint(n2), color);
        }

        /**
         * Draw a small rectangle.
         * White if selected (as always) or red otherwise.
         *
         * @param n The node to draw.
         */
        @Override
        public void visit(Node n) {
            if (isNodeVisible(n)) {
                drawNode(n, severity.getColor());
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
            Node a = ws.way.getNodes().get(ws.lowerIndex), b = ws.way.getNodes().get(ws.lowerIndex + 1);
            if (isSegmentVisible(a, b)) {
                drawSegment(a, b, severity.getColor());
            }
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
         * @param ls The segment to check
         * @return true if the segment is visible
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
                if (n.isDrawable() && isSegmentVisible(lastN, n)) {
                    drawSegment(lastN, n, severity.getColor());
                }
                lastN = n;
            }
        }
    }

    /**
     * Sets the selection flag of this error
     * @param selected if this error is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
