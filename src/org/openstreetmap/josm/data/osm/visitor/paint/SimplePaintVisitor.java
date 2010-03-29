/* License: GPL. Copyright 2007 by Immanuel Scholz and others */
package org.openstreetmap.josm.data.osm.visitor.paint;

/* To enable debugging or profiling remove the double / signs */

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.Collection;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.AbstractVisitor;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * A visitor that paints a simple scheme of every primitive it visits to a
 * previous set graphic environment.
 *
 * @author imi
 */
public class SimplePaintVisitor extends AbstractVisitor implements PaintVisitor {
    /**
     * The environment to paint to.
     */
    protected Graphics2D g;
    /**
     * MapView to get screen coordinates.
     */
    protected NavigatableComponent nc;

    public boolean inactive;

    protected static final double PHI = Math.toRadians(20);

    /**
     * Preferences
     */
    protected Color inactiveColor;
    protected Color selectedColor;
    protected Color nodeColor;
    protected Color dfltWayColor;
    protected Color relationColor;
    protected Color untaggedWayColor;
    protected Color incompleteColor;
    protected Color backgroundColor;
    protected Color highlightColor;
    protected Color taggedColor;
    protected boolean showDirectionArrow;
    protected boolean showRelevantDirectionsOnly;
    protected boolean showHeadArrowOnly;
    protected boolean showOrderNumber;
    protected boolean fillSelectedNode;
    protected boolean fillUnselectedNode;
    protected boolean fillTaggedNode;
    protected int selectedNodeSize;
    protected int selectedNodeRadius;
    protected int unselectedNodeSize;
    protected int unselectedNodeRadius;
    protected int junctionNodeSize;
    protected int junctionNodeRadius;
    protected int defaultSegmentWidth;
    protected int virtualNodeSize;
    protected int virtualNodeSpace;
    protected int segmentNumberSpace;

    /**
     * Draw subsequent segments of same color as one Path
     */
    protected Color currentColor = null;
    protected GeneralPath currentPath = new GeneralPath();

    public void getColors()
    {
        inactiveColor = PaintColors.INACTIVE.get();
        selectedColor = PaintColors.SELECTED.get();
        nodeColor = PaintColors.NODE.get();
        dfltWayColor = PaintColors.DEFAULT_WAY.get();
        relationColor = PaintColors.RELATION.get();
        untaggedWayColor = PaintColors.UNTAGGED_WAY.get();
        incompleteColor = PaintColors.INCOMPLETE_WAY.get();
        backgroundColor = PaintColors.BACKGROUND.get();
        highlightColor = PaintColors.HIGHLIGHT.get();
        taggedColor = PaintColors.TAGGED.get();
    }

    protected void getSettings(boolean virtual) {
        MapPaintSettings settings = MapPaintSettings.INSTANCE;
        showDirectionArrow = settings.isShowDirectionArrow();
        showRelevantDirectionsOnly = settings.isShowRelevantDirectionsOnly();
        showHeadArrowOnly = settings.isShowHeadArrowOnly();
        showOrderNumber = settings.isShowOrderNumber();
        selectedNodeSize = settings.getSelectedNodeSize();
        selectedNodeRadius = selectedNodeSize / 2;
        unselectedNodeSize = settings.getUnselectedNodeSize();
        unselectedNodeRadius = unselectedNodeSize / 2;
        junctionNodeSize = settings.getJunctionNodeSize();
        junctionNodeRadius = junctionNodeSize / 2;
        defaultSegmentWidth = settings.getDefaultSegmentWidth();
        fillSelectedNode = settings.isFillSelectedNode();
        fillUnselectedNode = settings.isFillUnselectedNode();
        fillTaggedNode = settings.isFillTaggedNode();
        virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 8) / 2 : 0;
        virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);
        getColors();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    DataSet ds;
    public void visitAll(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = new BBox(bounds);
        this.ds = data;
        //boolean profiler = Main.pref.getBoolean("simplepaint.profiler",false);
        //long profilerStart = java.lang.System.currentTimeMillis();
        //long profilerLast = profilerStart;
        //int profilerN = 0;
        //if(profiler)
        //    System.out.println("Simplepaint Profiler");

        getSettings(virtual);

        //if(profiler)
        //{
        //    System.out.format("Prepare  : %4dms\n", (java.lang.System.currentTimeMillis()-profilerLast));
        //    profilerLast = java.lang.System.currentTimeMillis();
        //}

        /* draw tagged ways first, then untagged ways. takes
           time to iterate through list twice, OTOH does not
           require changing the colour while painting... */
        //profilerN = 0;
        for (final OsmPrimitive osm: data.searchRelations(bbox)) {
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isFiltered()) {
                osm.visit(this);
                //        profilerN++;
            }
        }

        //if(profiler)
        //{
        //    System.out.format("Relations: %4dms, n=%5d\n", (java.lang.System.currentTimeMillis()-profilerLast), profilerN);
        //    profilerLast = java.lang.System.currentTimeMillis();
        //}

        //profilerN = 0;
        for (final OsmPrimitive osm:data.searchWays(bbox)){
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isFiltered() && osm.isTagged()) {
                osm.visit(this);
                //        profilerN++;
            }
        }
        displaySegments();

        for (final OsmPrimitive osm:data.searchWays(bbox)){
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isFiltered() && !osm.isTagged()) {
                osm.visit(this);
                //        profilerN++;
            }
        }
        displaySegments();

        //if(profiler)
        //{
        //    System.out.format("Ways     : %4dms, n=%5d\n",
        //        (java.lang.System.currentTimeMillis()-profilerLast), profilerN);
        //    profilerLast = java.lang.System.currentTimeMillis();
        //}

        //profilerN = 0;
        for (final OsmPrimitive osm : data.getSelected())
            if (!osm.isDeleted()) {
                osm.visit(this);
                //        profilerN++;
            }
        displaySegments();

        //if(profiler)
        //{
        //    System.out.format("Selected : %4dms, n=%5d\n", (java.lang.System.currentTimeMillis()-profilerLast), profilerN);
        //    profilerLast = java.lang.System.currentTimeMillis();
        //}

        //profilerN = 0;
        for (final OsmPrimitive osm: data.searchNodes(bbox)) {
            if (!osm.isDeleted() && !ds.isSelected(osm) && !osm.isFiltered())
            {
                osm.visit(this);
                //        profilerN++;
            }
        }

        //if(profiler)
        //{
        //    System.out.format("Nodes    : %4dms, n=%5d\n",
        //        (java.lang.System.currentTimeMillis()-profilerLast), profilerN);
        //    profilerLast = java.lang.System.currentTimeMillis();
        //}

        drawVirtualNodes(data.searchWays(bbox));

        //if(profiler)
        //{
        //    System.out.format("All      : %4dms\n", (profilerLast-profilerStart));
        //}
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    public void visit(Node n) {
        if (n.isIncomplete()) return;

        if (n.isHighlighted()) {
            drawNode(n, highlightColor, selectedNodeSize, unselectedNodeRadius, fillSelectedNode);
        } else if (ds.isSelected(n)) {
            drawNode(n, selectedColor, selectedNodeSize, selectedNodeRadius, fillSelectedNode);
        } else {
            boolean junction = n.isJunctionNode();
            drawNode(
                n,
                (inactive || n.isDisabled()) ? inactiveColor : 
                    (n.isTagged() ? taggedColor : nodeColor),
                junction ? junctionNodeSize : unselectedNodeSize,
                junction ? junctionNodeRadius : unselectedNodeRadius,
                n.isTagged() ? fillTaggedNode : fillUnselectedNode);
        }
    }

    public static boolean isLargeSegment(Point p1, Point p2, int space)
    {
        int xd = p1.x-p2.x; if(xd < 0) {
            xd = -xd;
        }
        int yd = p1.y-p2.y; if(yd < 0) {
            yd = -yd;
        }
        return (xd+yd > space);
    }

    public void drawVirtualNodes(Collection<Way> ways) {

        if (virtualNodeSize != 0) {
            GeneralPath path = new GeneralPath();
            for (Way osm: ways){
                if (osm.isUsable() && !osm.isFiltered()) {
                    visitVirtual(path, osm);
                }
            }
            g.setColor(nodeColor);
            g.draw(path);
        }
    }

    public void visitVirtual(GeneralPath path, Way w) {
        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            while(it.hasNext())
            {
                Point p = nc.getPoint(it.next());
                if(isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace))
                {
                    int x = (p.x+lastP.x)/2;
                    int y = (p.y+lastP.y)/2;
                    path.moveTo(x-virtualNodeSize, y);
                    path.lineTo(x+virtualNodeSize, y);
                    path.moveTo(x, y-virtualNodeSize);
                    path.lineTo(x, y+virtualNodeSize);
                }
                lastP = p;
            }
        }
    }

    /**
     * Draw a darkblue line for all segments.
     * @param w The way to draw.
     */
    public void visit(Way w) {
        if (w.isIncomplete() || w.getNodesCount() < 2)
            return;

        /* show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
           (even if the tag is negated as in oneway=false) or the way is selected */

        boolean showThisDirectionArrow = ds.isSelected(w)
        || (showDirectionArrow && (!showRelevantDirectionsOnly || w.hasDirectionKeys()));
        /* head only takes over control if the option is true,
           the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showThisDirectionArrow && !ds.isSelected(w) && showHeadArrowOnly;
        Color wayColor;

        if (inactive || w.isDisabled()) {
            wayColor = inactiveColor;
        } else if(w.isHighlighted()) {
            wayColor = highlightColor;
        } else if(ds.isSelected(w)) {
            wayColor = selectedColor;
        } else if (!w.isTagged()) {
            wayColor = untaggedWayColor;
        } else {
            wayColor = dfltWayColor;
        }

        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            for (int orderNumber = 1; it.hasNext(); orderNumber++) {
                Point p = nc.getPoint(it.next());
                drawSegment(lastP, p, wayColor,
                        showOnlyHeadArrowOnly ? !it.hasNext() : showThisDirectionArrow);
                if (showOrderNumber) {
                    drawOrderNumber(lastP, p, orderNumber);
                }
                lastP = p;
            }
        }
    }

    private Stroke relatedWayStroke = new BasicStroke(
            4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    public void visit(Relation r) {
        if (r.isIncomplete()) return;

        Color col;
        if (inactive || r.isDisabled()) {
            col = inactiveColor;
        } else if (ds.isSelected(r)) {
            col = selectedColor;
        } else {
            col = relationColor;
        }
        g.setColor(col);

        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete() || m.getMember().isDeleted()) {
                continue;
            }

            if (m.isNode()) {
                Point p = nc.getPoint(m.getNode());
                if (p.x < 0 || p.y < 0
                        || p.x > nc.getWidth() || p.y > nc.getHeight()) {
                    continue;
                }

                g.drawOval(p.x-3, p.y-3, 6, 6);
            } else if (m.isWay()) {
                GeneralPath path = new GeneralPath();

                boolean first = true;
                for (Node n : m.getWay().getNodes()) {
                    if (n.isIncomplete() || n.isDeleted()) {
                        continue;
                    }
                    Point p = nc.getPoint(n);
                    if (first) {
                        path.moveTo(p.x, p.y);
                        first = false;
                    } else {
                        path.lineTo(p.x, p.y);
                    }
                }

                g.draw(relatedWayStroke.createStrokedShape(path));
            }
        }
    }

    /**
     * Draw an number of the order of the two consecutive nodes within the
     * parents way
     */
    protected void drawOrderNumber(Point p1, Point p2, int orderNumber) {
        if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
            String on = Integer.toString(orderNumber);
            int strlen = on.length();
            int x = (p1.x+p2.x)/2 - 4*strlen;
            int y = (p1.y+p2.y)/2 + 4;

            if(virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace))
            {
                y = (p1.y+p2.y)/2 - virtualNodeSize - 3;
            }

            displaySegments(); /* draw nodes on top! */
            Color c = g.getColor();
            g.setColor(backgroundColor);
            g.fillRect(x-1, y-12, 8*strlen+1, 14);
            g.setColor(c);
            g.drawString(on, x, y);
        }
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n     The node to draw.
     * @param color The color of the node.
     */
    public void drawNode(Node n, Color color, int size, int radius, boolean fill) {
        if (size > 1) {
            Point p = nc.getPoint(n);
            if ((p.x < 0) || (p.y < 0) || (p.x > nc.getWidth())
                    || (p.y > nc.getHeight()))
                return;
            g.setColor(color);
            if (fill) {
                g.fillRect(p.x - radius, p.y - radius, size, size);
                g.drawRect(p.x - radius, p.y - radius, size, size);
            } else {
                g.drawRect(p.x - radius, p.y - radius, size, size);
            }
        }
    }

    protected void drawSegment(GeneralPath path, Point p1, Point p2, boolean showDirection) {
        if (isSegmentVisible(p1, p2)) {
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);

            if (showDirection) {
                double t = Math.atan2(p2.y-p1.y, p2.x-p1.x) + Math.PI;
                path.lineTo((int)(p2.x + 10*Math.cos(t-PHI)), (int)(p2.y + 10*Math.sin(t-PHI)));
                path.moveTo((int)(p2.x + 10*Math.cos(t+PHI)), (int)(p2.y + 10*Math.sin(t+PHI)));
                path.lineTo(p2.x, p2.y);
            }
        }
    }

    /**
     * Draw a line with the given color.
     */
    protected void drawSegment(Point p1, Point p2, Color col, boolean showDirection) {
        if (col != currentColor) {
            displaySegments(col);
        }
        drawSegment(currentPath, p1, p2, showDirection);
    }

    protected boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }

    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    public void setGraphics(Graphics2D g) {
        this.g = g;
    }

    public void setNavigatableComponent(NavigatableComponent nc) {
        this.nc = nc;
    }

    protected void displaySegments() {
        displaySegments(null);
    }
    protected void displaySegments(Color newColor) {
        if (currentPath != null) {
            g.setColor(currentColor);
            g.draw(currentPath);
            currentPath = new GeneralPath();
            currentColor = newColor;
        }
    }

    public void setInactive(boolean inactive) {
        this.inactive = inactive;
    }
}
