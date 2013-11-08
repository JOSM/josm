/* License: GPL. For details, see LICENSE file. */
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.gui.NavigatableComponent;

/**
 * A map renderer that paints a simple scheme of every primitive it visits to a
 * previous set graphic environment.
 */
public class WireframeMapRenderer extends AbstractMapRenderer implements Visitor {

    /** Color Preference for ways not matching any other group */
    protected Color dfltWayColor;
    /** Color Preference for relations */
    protected Color relationColor;
    /** Color Preference for untagged ways */
    protected Color untaggedWayColor;
    /** Color Preference for tagged nodes */
    protected Color taggedColor;
    /** Color Preference for multiply connected nodes */
    protected Color connectionColor;
    /** Color Preference for tagged and multiply connected nodes */
    protected Color taggedConnectionColor;
    /** Preference: should directional arrows be displayed */
    protected boolean showDirectionArrow;
    /** Preference: should arrows for oneways be displayed */
    protected boolean showOnewayArrow;
    /** Preference: should only the last arrow of a way be displayed */
    protected boolean showHeadArrowOnly;
    /** Preference: should the segement numbers of ways be displayed */
    protected boolean showOrderNumber;
    /** Preference: should selected nodes be filled */
    protected boolean fillSelectedNode;
    /** Preference: should unselected nodes be filled */
    protected boolean fillUnselectedNode;
    /** Preference: should tagged nodes be filled */
    protected boolean fillTaggedNode;
    /** Preference: should multiply connected nodes be filled */
    protected boolean fillConnectionNode;
    /** Preference: size of selected nodes */
    protected int selectedNodeSize;
    /** Preference: size of unselected nodes */
    protected int unselectedNodeSize;
    /** Preference: size of multiply connected nodes */
    protected int connectionNodeSize;
    /** Preference: size of tagged nodes */
    protected int taggedNodeSize;

    /** Color cache to draw subsequent segments of same color as one <code>Path</code>. */
    protected Color currentColor = null;
    /** Path store to draw subsequent segments of same color as one <code>Path</code>. */
    protected GeneralPath currentPath = new GeneralPath();
    /**
      * <code>DataSet</code> passed to the @{link render} function to overcome the argument
      * limitations of @{link Visitor} interface. Only valid until end of rendering call.
      */
    private DataSet ds;

    /** Helper variable for {@link #drawSegment} */
    private static final double PHI = Math.toRadians(20);
    /** Helper variable for {@link #drawSegment} */
    private static final double cosPHI = Math.cos(PHI);
    /** Helper variable for {@link #drawSegment} */
    private static final double sinPHI = Math.sin(PHI);

    /** Helper variable for {@link #visit(Relation)} */
    private Stroke relatedWayStroke = new BasicStroke(
            4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);

    /**
     * Creates an wireframe render
     *
     * @param g the graphics context. Must not be null.
     * @param nc the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException thrown if {@code g} is null
     * @throws IllegalArgumentException thrown if {@code nc} is null
     */
    public WireframeMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);
    }

    @Override
    public void getColors() {
        super.getColors();
        dfltWayColor = PaintColors.DEFAULT_WAY.get();
        relationColor = PaintColors.RELATION.get();
        untaggedWayColor = PaintColors.UNTAGGED_WAY.get();
        highlightColor = PaintColors.HIGHLIGHT_WIREFRAME.get();
        taggedColor = PaintColors.TAGGED.get();
        connectionColor = PaintColors.CONNECTION.get();

        if (taggedColor != nodeColor) {
            taggedConnectionColor = taggedColor;
        } else {
            taggedConnectionColor = connectionColor;
        }
    }

    @Override
    protected void getSettings(boolean virtual) {
        super.getSettings(virtual);
        MapPaintSettings settings = MapPaintSettings.INSTANCE;
        showDirectionArrow = settings.isShowDirectionArrow();
        showOnewayArrow = settings.isShowOnewayArrow();
        showHeadArrowOnly = settings.isShowHeadArrowOnly();
        showOrderNumber = settings.isShowOrderNumber();
        selectedNodeSize = settings.getSelectedNodeSize();
        unselectedNodeSize = settings.getUnselectedNodeSize();
        connectionNodeSize = settings.getConnectionNodeSize();
        taggedNodeSize = settings.getTaggedNodeSize();
        fillSelectedNode = settings.isFillSelectedNode();
        fillUnselectedNode = settings.isFillUnselectedNode();
        fillConnectionNode = settings.isFillConnectionNode();
        fillTaggedNode = settings.isFillTaggedNode();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Main.pref.getBoolean("mappaint.wireframe.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    /**
     * Renders the dataset for display.
     *
     * @param data <code>DataSet</code> to display
     * @param virtual <code>true</code> if virtual nodes are used
     * @param bounds display boundaries
     */
    @SuppressWarnings("unchecked")
    @Override
    public void render(DataSet data, boolean virtual, Bounds bounds) {
        BBox bbox = bounds.toBBox();
        this.ds = data;
        getSettings(virtual);

        for (final Relation rel : data.searchRelations(bbox)) {
            if (rel.isDrawable() && !ds.isSelected(rel) && !rel.isDisabledAndHidden()) {
                rel.accept(this);
            }
        }

        // draw tagged ways first, then untagged ways, then highlighted ways
        List<Way> highlightedWays = new ArrayList<Way>();
        List<Way> untaggedWays = new ArrayList<Way>();

        for (final Way way : data.searchWays(bbox)){
            if (way.isDrawable() && !ds.isSelected(way) && !way.isDisabledAndHidden()) {
                if (way.isHighlighted()) {
                    highlightedWays.add(way);
                } else if (!way.isTagged()) {
                    untaggedWays.add(way);
                } else {
                    way.accept(this);
                }
            }
        }
        displaySegments();

        // Display highlighted ways after the other ones (fix #8276)
        for (List<Way> specialWays : Arrays.asList(new List[]{untaggedWays, highlightedWays})) {
            for (final Way way : specialWays){
                way.accept(this);
            }
            specialWays.clear();
            displaySegments();
        }

        for (final OsmPrimitive osm : data.getSelected()) {
            if (osm.isDrawable()) {
                osm.accept(this);
            }
        }
        displaySegments();

        for (final OsmPrimitive osm: data.searchNodes(bbox)) {
            if (osm.isDrawable() && !ds.isSelected(osm) && !osm.isDisabledAndHidden())
            {
                osm.accept(this);
            }
        }
        drawVirtualNodes(data, bbox);

        // draw highlighted way segments over the already drawn ways. Otherwise each
        // way would have to be checked if it contains a way segment to highlight when
        // in most of the cases there won't be more than one segment. Since the wireframe
        // renderer does not feature any transparency there should be no visual difference.
        for (final WaySegment wseg : data.getHighlightedWaySegments()) {
            drawSegment(nc.getPoint(wseg.getFirstNode()), nc.getPoint(wseg.getSecondNode()), highlightColor, false);
        }
        displaySegments();
    }

    /**
     * Helper function to calculate maximum of 4 values.
     *
     * @param a First value
     * @param b Second value
     * @param c Third value
     * @param d Fourth value
     */
    private static final int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    @Override
    public void visit(Node n) {
        if (n.isIncomplete()) return;

        if (n.isHighlighted()) {
            drawNode(n, highlightColor, selectedNodeSize, fillSelectedNode);
        } else {
            Color color;

            if (isInactiveMode || n.isDisabled()) {
                color = inactiveColor;
            } else if (ds.isSelected(n)) {
                color = selectedColor;
            } else if (n.isConnectionNode()) {
                if (isNodeTagged(n)) {
                    color = taggedConnectionColor;
                } else {
                    color = connectionColor;
                }
            } else {
                if (isNodeTagged(n)) {
                    color = taggedColor;
                } else {
                    color = nodeColor;
                }
            }

            final int size = max((ds.isSelected(n) ? selectedNodeSize : 0),
                    (isNodeTagged(n) ? taggedNodeSize : 0),
                    (n.isConnectionNode() ? connectionNodeSize : 0),
                    unselectedNodeSize);

            final boolean fill = (ds.isSelected(n) && fillSelectedNode) ||
            (isNodeTagged(n) && fillTaggedNode) ||
            (n.isConnectionNode() && fillConnectionNode) ||
            fillUnselectedNode;

            drawNode(n, color, size, fill);
        }
    }

    private boolean isNodeTagged(Node n) {
        return n.isTagged() || n.isAnnotated();
    }

    /**
     * Draw a line for all way segments.
     * @param w The way to draw.
     */
    @Override
    public void visit(Way w) {
        if (w.isIncomplete() || w.getNodesCount() < 2)
            return;

        /* show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
           (even if the tag is negated as in oneway=false) or the way is selected */

        boolean showThisDirectionArrow = ds.isSelected(w) || showDirectionArrow;
        /* head only takes over control if the option is true,
           the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showThisDirectionArrow && !ds.isSelected(w) && showHeadArrowOnly;
        Color wayColor;

        if (isInactiveMode || w.isDisabled()) {
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
                if (showOrderNumber && !isInactiveMode) {
                    drawOrderNumber(lastP, p, orderNumber, g.getColor());
                }
                lastP = p;
            }
        }
    }

    /**
     * Draw objects used in relations.
     * @param r The relation to draw.
     */
    @Override
    public void visit(Relation r) {
        if (r.isIncomplete()) return;

        Color col;
        if (isInactiveMode || r.isDisabled()) {
            col = inactiveColor;
        } else if (ds.isSelected(r)) {
            col = selectedColor;
        } else {
            col = relationColor;
        }
        g.setColor(col);

        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete() || !m.getMember().isDrawable()) {
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
                    if (!n.isDrawable()) {
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
     * Visitor for changesets not used in this class
     * @param cs The changeset for inspection.
     */
    @Override
    public void visit(Changeset cs) {/* ignore */}

    @Override
    public void drawNode(Node n, Color color, int size, boolean fill) {
        if (size > 1) {
            int radius = size / 2;
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

    /**
     * Draw a line with the given color.
     *
     * @param path The path to append this segment.
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param showDirection <code>true</code> if segment direction should be indicated
     */
    protected void drawSegment(GeneralPath path, Point p1, Point p2, boolean showDirection) {
        Rectangle bounds = g.getClipBounds();
        bounds.grow(100, 100);                  // avoid arrow heads at the border
        LineClip clip = new LineClip(p1, p2, bounds);
        if (clip.execute()) {
            p1 = clip.getP1();
            p2 = clip.getP2();
            path.moveTo(p1.x, p1.y);
            path.lineTo(p2.x, p2.y);

            if (showDirection) {
                final double l =  10. / p1.distance(p2);

                final double sx = l * (p1.x - p2.x);
                final double sy = l * (p1.y - p2.y);

                path.lineTo (p2.x + (int) Math.round(cosPHI * sx - sinPHI * sy), p2.y + (int) Math.round(sinPHI * sx + cosPHI * sy));
                path.moveTo (p2.x + (int) Math.round(cosPHI * sx + sinPHI * sy), p2.y + (int) Math.round(- sinPHI * sx + cosPHI * sy));
                path.lineTo(p2.x, p2.y);
            }
        }
    }

    /**
     * Draw a line with the given color.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param col The color to use for drawing line.
     * @param showDirection <code>true</code> if segment direction should be indicated.
     */
    protected void drawSegment(Point p1, Point p2, Color col, boolean showDirection) {
        if (col != currentColor) {
            displaySegments(col);
        }
        drawSegment(currentPath, p1, p2, showDirection);
    }

    /**
     * Checks if a polygon is visible in display.
     *
     * @param polygon The polygon to check.
     * @return <code>true</code> if polygon is visible.
     */
    protected boolean isPolygonVisible(Polygon polygon) {
        Rectangle bounds = polygon.getBounds();
        if (bounds.width == 0 && bounds.height == 0) return false;
        if (bounds.x > nc.getWidth()) return false;
        if (bounds.y > nc.getHeight()) return false;
        if (bounds.x + bounds.width < 0) return false;
        if (bounds.y + bounds.height < 0) return false;
        return true;
    }

    /**
     * Finally display all segments in currect path.
     */
    protected void displaySegments() {
        displaySegments(null);
    }

    /**
     * Finally display all segments in currect path.
     *
     * @param newColor This color is set after the path is drawn.
     */
    protected void displaySegments(Color newColor) {
        if (currentPath != null) {
            g.setColor(currentColor);
            g.draw(currentPath);
            currentPath = new GeneralPath();
            currentColor = newColor;
        }
    }
}
