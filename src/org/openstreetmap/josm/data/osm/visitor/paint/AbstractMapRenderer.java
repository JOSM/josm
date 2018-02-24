// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapViewState;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Logging;

/**
 * <p>Abstract common superclass for {@link Rendering} implementations.</p>
 * @since 4087
 */
public abstract class AbstractMapRenderer implements Rendering {

    /** the graphics context to which the visitor renders OSM objects */
    protected final Graphics2D g;
    /** the map viewport - provides projection and hit detection functionality */
    protected final NavigatableComponent nc;

    /**
     * The {@link MapViewState} to use to convert between coordinates.
     */
    protected final MapViewState mapState;

    /** if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only. */
    protected boolean isInactiveMode;
    /** Color Preference for background */
    protected Color backgroundColor;
    /** Color Preference for inactive objects */
    protected Color inactiveColor;
    /** Color Preference for selected objects */
    protected Color selectedColor;
    /** Color Preference for members of selected relations */
    protected Color relationSelectedColor;
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
     * @throws IllegalArgumentException if {@code g} is null
     * @throws IllegalArgumentException if {@code nc} is null
     */
    public AbstractMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        CheckParameterUtil.ensureParameterNotNull(g);
        CheckParameterUtil.ensureParameterNotNull(nc);
        this.g = g;
        this.nc = nc;
        this.mapState = nc.getState();
        this.isInactiveMode = isInactiveMode;
    }

    /**
     * Draw the node as small square with the given color.
     *
     * @param n  The node to draw.
     * @param color The color of the node.
     * @param size size in pixels
     * @param fill determines if the square mmust be filled
     */
    public abstract void drawNode(Node n, Color color, int size, boolean fill);

    /**
     * Draw an number of the order of the two consecutive nodes within the
     * parents way
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param orderNumber The number of the segment in the way.
     * @param clr The color to use for drawing the text.
     * @since 10827
     */
    protected void drawOrderNumber(MapViewPoint p1, MapViewPoint p2, int orderNumber, Color clr) {
        if (isSegmentVisible(p1, p2) && isLargeSegment(p1, p2, segmentNumberSpace)) {
            String on = Integer.toString(orderNumber);
            int strlen = on.length();
            double centerX = (p1.getInViewX()+p2.getInViewX())/2;
            double centerY = (p1.getInViewY()+p2.getInViewY())/2;
            double x = centerX - 4*strlen;
            double y = centerY + 4;

            if (virtualNodeSize != 0 && isLargeSegment(p1, p2, virtualNodeSpace)) {
                y = centerY - virtualNodeSize - 3;
            }

            g.setColor(backgroundColor);
            g.fill(new Rectangle2D.Double(x-1, y-12, 8*strlen+1, 14));
            g.setColor(clr);
            g.drawString(on, (int) x, (int) y);
        }
    }

    /**
     * Draws virtual nodes.
     *
     * @param data The data set being rendered.
     * @param bbox The bounding box being displayed.
     */
    public void drawVirtualNodes(DataSet data, BBox bbox) {
        if (virtualNodeSize == 0 || data == null || bbox == null || data.isLocked())
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
            // drawing them over the existing ones works fine (at least in their current simple style)
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
            // Idea from bastiK:
            // avoid the WaySegment class and add another data class with { Way way; Node firstNode, secondNode; int firstIdx; }.
            // On read, it would first check, if the way still has firstIdx+2 nodes, then check if the corresponding way nodes are still
            // the same and report changes in a more controlled manner.
            Logging.trace(e);
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
        this.relationSelectedColor = PaintColors.RELATIONSELECTED.get();
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
        this.virtualNodeSize = virtual ? Config.getPref().getInt("mappaint.node.virtual-size", 8) / 2 : 0;
        this.virtualNodeSpace = Config.getPref().getInt("mappaint.node.virtual-space", 70);
        this.segmentNumberSpace = Config.getPref().getInt("mappaint.segmentnumber.space", 40);
        getColors();
    }

    /**
     * Checks if a way segemnt is large enough for additional information display.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param space The free space to check against.
     * @return <code>true</code> if segment is larger than required space
     * @since 10827
     */
    public static boolean isLargeSegment(MapViewPoint p1, MapViewPoint p2, int space) {
        return p1.oneNormInView(p2) > space;
    }

    /**
     * Checks if segment is visible in display.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @return <code>true</code> if segment may be visible.
     * @since 10827
     */
    protected boolean isSegmentVisible(MapViewPoint p1, MapViewPoint p2) {
        MapViewRectangle view = mapState.getViewArea();
        // not outside in the same direction
        return (p1.getOutsideRectangleFlags(view) & p2.getOutsideRectangleFlags(view)) == 0;
    }

    /**
     * Creates path for drawing virtual nodes for one way.
     *
     * @param path The path to append drawing to.
     * @param w The ways to draw node for.
     * @since 10827
     */
    public void visitVirtual(Path2D path, Way w) {
        Iterator<Node> it = w.getNodes().iterator();
        MapViewPoint lastP = null;
        while (it.hasNext()) {
            Node n = it.next();
            if (n.isLatLonKnown()) {
                MapViewPoint p = mapState.getPointFor(n);
                if (lastP != null && isSegmentVisible(lastP, p) && isLargeSegment(lastP, p, virtualNodeSpace)) {
                    double x = (p.getInViewX()+lastP.getInViewX())/2;
                    double y = (p.getInViewY()+lastP.getInViewY())/2;
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
