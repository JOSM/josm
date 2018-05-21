// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.OsmData;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.PrimitiveVisitor;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.MapViewState.MapViewRectangle;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.draw.MapPath2D;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Utils;

/**
 * A map renderer that paints a simple scheme of every primitive it visits to a
 * previous set graphic environment.
 * @since 23
 */
public class WireframeMapRenderer extends AbstractMapRenderer implements PrimitiveVisitor {

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
    /** Preference: should the segment numbers of ways be displayed */
    protected boolean showOrderNumber;
    /** Preference: should the segment numbers of the selected be displayed */
    protected boolean showOrderNumberOnSelectedWay;
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
    protected Color currentColor;
    /** Path store to draw subsequent segments of same color as one <code>Path</code>. */
    protected MapPath2D currentPath = new MapPath2D();

    /** Helper variable for {@link #drawSegment} */
    private static final ArrowPaintHelper ARROW_PAINT_HELPER = new ArrowPaintHelper(Utils.toRadians(20), 10);

    /** Helper variable for {@link #visit(IRelation)} */
    private final Stroke relatedWayStroke = new BasicStroke(
            4, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_BEVEL);
    private MapViewRectangle viewClip;

    /**
     * Creates an wireframe render
     *
     * @param g the graphics context. Must not be null.
     * @param nc the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException if {@code g} is null
     * @throws IllegalArgumentException if {@code nc} is null
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

        if (!taggedColor.equals(nodeColor)) {
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
        showOrderNumberOnSelectedWay = settings.isShowOrderNumberOnSelectedWay();
        selectedNodeSize = settings.getSelectedNodeSize();
        unselectedNodeSize = settings.getUnselectedNodeSize();
        connectionNodeSize = settings.getConnectionNodeSize();
        taggedNodeSize = settings.getTaggedNodeSize();
        fillSelectedNode = settings.isFillSelectedNode();
        fillUnselectedNode = settings.isFillUnselectedNode();
        fillConnectionNode = settings.isFillConnectionNode();
        fillTaggedNode = settings.isFillTaggedNode();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                Config.getPref().getBoolean("mappaint.wireframe.use-antialiasing", false) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public void render(OsmData<?, ?, ?, ?> data, boolean virtual, Bounds bounds) {
        BBox bbox = bounds.toBBox();
        Rectangle clip = g.getClipBounds();
        clip.grow(50, 50);
        viewClip = mapState.getViewArea(clip);
        getSettings(virtual);

        for (final IRelation<?> rel : data.searchRelations(bbox)) {
            if (rel.isDrawable() && !rel.isSelected() && !rel.isDisabledAndHidden()) {
                rel.accept(this);
            }
        }

        // draw tagged ways first, then untagged ways, then highlighted ways
        List<IWay<?>> highlightedWays = new ArrayList<>();
        List<IWay<?>> untaggedWays = new ArrayList<>();

        for (final IWay<?> way : data.searchWays(bbox)) {
            if (way.isDrawable() && !way.isSelected() && !way.isDisabledAndHidden()) {
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
        List<IWay<?>> specialWays = new ArrayList<>(untaggedWays);
        specialWays.addAll(highlightedWays);
        for (final IWay<?> way : specialWays) {
            way.accept(this);
        }
        specialWays.clear();
        displaySegments();

        for (final IPrimitive osm : data.getSelected()) {
            if (osm.isDrawable()) {
                osm.accept(this);
            }
        }
        displaySegments();

        for (final INode osm: data.searchNodes(bbox)) {
            if (osm.isDrawable() && !osm.isSelected() && !osm.isDisabledAndHidden()) {
                osm.accept(this);
            }
        }
        drawVirtualNodes(data, bbox);

        // draw highlighted way segments over the already drawn ways. Otherwise each
        // way would have to be checked if it contains a way segment to highlight when
        // in most of the cases there won't be more than one segment. Since the wireframe
        // renderer does not feature any transparency there should be no visual difference.
        for (final WaySegment wseg : data.getHighlightedWaySegments()) {
            drawSegment(mapState.getPointFor(wseg.getFirstNode()), mapState.getPointFor(wseg.getSecondNode()), highlightColor, false);
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
     * @return maximumof {@code a}, {@code b}, {@code c}, {@code d}
     */
    private static int max(int a, int b, int c, int d) {
        return Math.max(Math.max(a, b), Math.max(c, d));
    }

    /**
     * Draw a small rectangle.
     * White if selected (as always) or red otherwise.
     *
     * @param n The node to draw.
     */
    @Override
    public void visit(INode n) {
        if (n.isIncomplete()) return;

        if (n.isHighlighted()) {
            drawNode(n, highlightColor, selectedNodeSize, fillSelectedNode);
        } else {
            Color color;

            if (isInactiveMode || n.isDisabled()) {
                color = inactiveColor;
            } else if (n.isSelected()) {
                color = selectedColor;
            } else if (n.isMemberOfSelected()) {
                color = relationSelectedColor;
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

            final int size = max(n.isSelected() ? selectedNodeSize : 0,
                    isNodeTagged(n) ? taggedNodeSize : 0,
                    n.isConnectionNode() ? connectionNodeSize : 0,
                    unselectedNodeSize);

            final boolean fill = (n.isSelected() && fillSelectedNode) ||
            (isNodeTagged(n) && fillTaggedNode) ||
            (n.isConnectionNode() && fillConnectionNode) ||
            fillUnselectedNode;

            drawNode(n, color, size, fill);
        }
    }

    private static boolean isNodeTagged(INode n) {
        return n.isTagged() || n.isAnnotated();
    }

    /**
     * Draw a line for all way segments.
     * @param w The way to draw.
     */
    @Override
    public void visit(IWay<?> w) {
        if (w.isIncomplete() || w.getNodesCount() < 2)
            return;

        /* show direction arrows, if draw.segment.relevant_directions_only is not set, the way is tagged with a direction key
           (even if the tag is negated as in oneway=false) or the way is selected */

        boolean showThisDirectionArrow = w.isSelected() || showDirectionArrow;
        /* head only takes over control if the option is true,
           the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showThisDirectionArrow && showHeadArrowOnly && !w.isSelected();
        Color wayColor;

        if (isInactiveMode || w.isDisabled()) {
            wayColor = inactiveColor;
        } else if (w.isHighlighted()) {
            wayColor = highlightColor;
        } else if (w.isSelected()) {
            wayColor = selectedColor;
        } else if (w.isMemberOfSelected()) {
            wayColor = relationSelectedColor;
        } else if (!w.isTagged()) {
            wayColor = untaggedWayColor;
        } else {
            wayColor = dfltWayColor;
        }

        Iterator<? extends INode> it = w.getNodes().iterator();
        if (it.hasNext()) {
            MapViewPoint lastP = mapState.getPointFor(it.next());
            int lastPOutside = lastP.getOutsideRectangleFlags(viewClip);
            for (int orderNumber = 1; it.hasNext(); orderNumber++) {
                MapViewPoint p = mapState.getPointFor(it.next());
                int pOutside = p.getOutsideRectangleFlags(viewClip);
                if ((pOutside & lastPOutside) == 0) {
                    drawSegment(lastP, p, wayColor,
                            showOnlyHeadArrowOnly ? !it.hasNext() : showThisDirectionArrow);
                    if ((showOrderNumber || (showOrderNumberOnSelectedWay && w.isSelected())) && !isInactiveMode) {
                        drawOrderNumber(lastP, p, orderNumber, g.getColor());
                    }
                }
                lastP = p;
                lastPOutside = pOutside;
            }
        }
    }

    /**
     * Draw objects used in relations.
     * @param r The relation to draw.
     */
    @Override
    public void visit(IRelation<?> r) {
        if (r.isIncomplete()) return;

        Color col;
        if (isInactiveMode || r.isDisabled()) {
            col = inactiveColor;
        } else if (r.isSelected()) {
            col = selectedColor;
        } else if (r.isMultipolygon() && r.isMemberOfSelected()) {
            col = relationSelectedColor;
        } else {
            col = relationColor;
        }
        g.setColor(col);

        for (IRelationMember<?> m : r.getMembers()) {
            if (m.getMember().isIncomplete() || !m.getMember().isDrawable()) {
                continue;
            }

            if (m.isNode()) {
                MapViewPoint p = mapState.getPointFor((INode) m.getMember());
                if (p.isInView()) {
                    g.draw(new Ellipse2D.Double(p.getInViewX()-4, p.getInViewY()-4, 9, 9));
                }

            } else if (m.isWay()) {
                GeneralPath path = new GeneralPath();

                boolean first = true;
                for (INode n : ((IWay<?>) m.getMember()).getNodes()) {
                    if (!n.isDrawable()) {
                        continue;
                    }
                    MapViewPoint p = mapState.getPointFor(n);
                    if (first) {
                        path.moveTo(p.getInViewX(), p.getInViewY());
                        first = false;
                    } else {
                        path.lineTo(p.getInViewX(), p.getInViewY());
                    }
                }

                g.draw(relatedWayStroke.createStrokedShape(path));
            }
        }
    }

    @Override
    public void drawNode(INode n, Color color, int size, boolean fill) {
        if (size > 1) {
            MapViewPoint p = mapState.getPointFor(n);
            if (!p.isInView())
                return;
            int radius = size / 2;
            Double shape = new Rectangle2D.Double(p.getInViewX() - radius, p.getInViewY() - radius, size, size);
            g.setColor(color);
            if (fill) {
                g.fill(shape);
            }
            g.draw(shape);
        }
    }

    /**
     * Draw a line with the given color.
     *
     * @param path The path to append this segment.
     * @param mv1 First point of the way segment.
     * @param mv2 Second point of the way segment.
     * @param showDirection <code>true</code> if segment direction should be indicated
     * @since 10827
     */
    protected void drawSegment(MapPath2D path, MapViewPoint mv1, MapViewPoint mv2, boolean showDirection) {
        path.moveTo(mv1);
        path.lineTo(mv2);
        if (showDirection) {
            ARROW_PAINT_HELPER.paintArrowAt(path, mv2, mv1);
        }
    }

    /**
     * Draw a line with the given color.
     *
     * @param p1 First point of the way segment.
     * @param p2 Second point of the way segment.
     * @param col The color to use for drawing line.
     * @param showDirection <code>true</code> if segment direction should be indicated.
     * @since 10827
     */
    protected void drawSegment(MapViewPoint p1, MapViewPoint p2, Color col, boolean showDirection) {
        if (!col.equals(currentColor)) {
            displaySegments(col);
        }
        drawSegment(currentPath, p1, p2, showDirection);
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
            currentPath = new MapPath2D();
            currentColor = newColor;
        }
    }
}
