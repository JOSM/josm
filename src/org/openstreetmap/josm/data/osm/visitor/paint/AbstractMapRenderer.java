// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * <p>Abstract common superclass for {@link Rendering} implementations.</p>
 *
 */
public abstract class AbstractMapRenderer implements Rendering {

    /** the graphics context to which the visitor renders OSM objects */
    protected Graphics2D g;
    /** the map viewport - provides projection and hit detection functionality */
    protected NavigatableComponent nc;

    /** if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only. */
    protected boolean isInactiveMode;
    /** Color Preference for background */
    protected Color backgroundColor;
    /** Color Preference for inactive objects */
    protected Color inactiveColor;
    /** Color Preference for selected objects */
    protected Color selectedColor;
    /** Color Preference for nodes */
    protected Color nodeColor;

    /** Color Preference for hightlighted objects */
    protected Color highlightColor;
    /** Preference: size of virtual nodes (0 displayes display) */
    protected int virtualNodeSize;
    /** Preference: minimum space (displayed way length) to display virtual nodes */
    protected int virtualNodeSpace;

    /** Preference: minimum space (displayed way length) to display segment numbers */
    protected int segmentNumberSpace;

    /**
     * <p>Creates an abstract paint visitor</p>
     *
     * @param g the graphics context. Must not be null.
     * @param nc the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException thrown if {@code g} is null
     * @throws IllegalArgumentException thrown if {@code nc} is null
     */
    public AbstractMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(g);
        CheckParameterUtil.ensureParameterNotNull(nc);
        this.g = g;
        this.nc = nc;
        this.isInactiveMode = isInactiveMode;
    }

    /**
     * Draw the node as small rectangle with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     */
    public abstract void drawNode(Node n, Color color, int size, boolean fill);

    /**
     * Draw an number of the order of the two consecutive nodes within the
     * parents way
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param orderNumber The number of the segment in the way.
     */
    protected void drawOrderNumber(Point p1, Point p2, int orderNumber, Color clr) {
        if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
            String on = Integer.toString(orderNumber);
            int strlen = on.length();
            int x = (p1.x+p2.x)/2 - 4*strlen;
            int y = (p1.y+p2.y)/2 + 4;

            if (virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace)) {
                y = (p1.y+p2.y)/2 - virtualNodeSize - 3;
            }

            g.setColor(backgroundColor);
            g.fillRect(x-1, y-12, 8*strlen+1, 14);
            g.setColor(clr);
            g.drawString(on, x, y);
        }
    }

    /**
     * Draws virtual nodes.
     *
     * @param data The data set being rendered.
     * @param bbox The bounding box being displayed.
     */
    public void drawVirtualNodes(DataSet data, BBox bbox) {
        if (virtualNodeSize == 0 || data == null || bbox == null)
            return;
        // print normal virtual nodes
        GeneralPath path = new GeneralPath();
        for (Way osm : data.searchWays(bbox)) {
            if (osm.isUsable() && !osm.isDisabledAndHidden() && !osm.isDisabled()) {
                visitVirtual(path, osm);
            }
        }
        g.setColor(nodeColor);
        g.draw(path);
        try {
            // print highlighted virtual nodes. Since only the color changes, simply
            // drawing them over the existing ones works fine (at least in their current
            // simple style)
            path = new GeneralPath();
            for (WaySegment wseg: data.getHighlightedVirtualNodes()) {
                if (wseg.way.isUsable() && !wseg.way.isDisabled()) {
                    visitVirtual(path, wseg.toWay());
                }
            }
            g.setColor(highlightColor);
            g.draw(path);
        } catch (ArrayIndexOutOfBoundsException e) {
            // Silently ignore any ArrayIndexOutOfBoundsException that may be raised
            // if the way has changed while being rendered (fix #7979)
            // TODO: proper solution ?
            // Idea from bastiK: avoid the WaySegment class and add another data class with { Way way; Node firstNode, secondNode; int firstIdx; }.
            // On read, it would first check, if the way still has firstIdx+2 nodes, then check if the corresponding way nodes are still the same
            // and report changes in a more controlled manner.
        }
    }

    /**
     * Reads the color definitions from preferences. This function is <code>public</code>, so that
     * color names in preferences can be displayed even without calling the wireframe display before.
     */
    public void getColors() {
        this.backgroundColor = PaintColors.BACKGROUND.get();
        this.inactiveColor = PaintColors.INACTIVE.get();
        this.selectedColor = PaintColors.SELECTED.get();
        this.nodeColor = PaintColors.NODE.get();
        this.highlightColor = PaintColors.HIGHLIGHT.get();
    }

    /**
     * Reads all the settings from preferences. Calls the @{link #getColors}
     * function.
     *
     * @param virtual <code>true</code> if virtual nodes are used
     */
    protected void getSettings(boolean virtual) {
        this.virtualNodeSize = virtual ? Main.pref.getInteger("mappaint.node.virtual-size", 8) / 2 : 0;
        this.virtualNodeSpace = Main.pref.getInteger("mappaint.node.virtual-space", 70);
        this.segmentNumberSpace = Main.pref.getInteger("mappaint.segmentnumber.space", 40);
        getColors();
    }

    /**
     * Checks if a way segemnt is large enough for additional information display.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param space The free space to check against.
     * @return <code>true</code> if segment is larger than required space
     */
    public static boolean isLargeSegment(Point2D p1, Point2D p2, int space) {
        double xd = Math.abs(p1.getX()-p2.getX());
        double yd = Math.abs(p1.getY()-p2.getY());
        return (xd+yd > space);
    }

    /**
     * Checks if segment is visible in display.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @return <code>true</code> if segment is visible.
     */
    protected boolean isSegmentVisible(Point p1, Point p2) {
        if ((p1.x < 0) && (p2.x < 0)) return false;
        if ((p1.y < 0) && (p2.y < 0)) return false;
        if ((p1.x > nc.getWidth()) && (p2.x > nc.getWidth())) return false;
        if ((p1.y > nc.getHeight()) && (p2.y > nc.getHeight())) return false;
        return true;
    }

    /**
     * Creates path for drawing virtual nodes for one way.
     *
     * @param path The path to append drawing to.
     * @param w The ways to draw node for.
     */
    public void visitVirtual(GeneralPath path, Way w) {
        Iterator<Node> it = w.getNodes().iterator();
        if (it.hasNext()) {
            Point lastP = nc.getPoint(it.next());
            while (it.hasNext())
            {
                Point p = nc.getPoint(it.next());
                if (isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace))
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
}
