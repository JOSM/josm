// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.LineMetrics;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.swing.AbstractButton;
import javax.swing.FocusManager;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.Changeset;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.StyleElementList;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.mappaint.styleelement.AreaElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.HorizontalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.VerticalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.MapImage;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.RepeatImageElement.LineImageAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.Symbol;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel;
import org.openstreetmap.josm.tools.CompositeList;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.AreaAndPerimeter;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * A map renderer which renders a map according to style rules in a set of style sheets.
 * @since 486
 */
public class StyledMapRenderer extends AbstractMapRenderer {

    private static final ForkJoinPool THREAD_POOL =
            Utils.newForkJoinPool("mappaint.StyledMapRenderer.style_creation.numberOfThreads", "styled-map-renderer-%d", Thread.NORM_PRIORITY);

    /**
     * Iterates over a list of Way Nodes and returns screen coordinates that
     * represent a line that is shifted by a certain offset perpendicular
     * to the way direction.
     *
     * There is no intention, to handle consecutive duplicate Nodes in a
     * perfect way, but it should not throw an exception.
     */
    private class OffsetIterator implements Iterator<MapViewPoint> {

        private final List<Node> nodes;
        private final double offset;
        private int idx;

        private MapViewPoint prev;
        /* 'prev0' is a point that has distance 'offset' from 'prev' and the
         * line from 'prev' to 'prev0' is perpendicular to the way segment from
         * 'prev' to the current point.
         */
        private double xPrev0;
        private double yPrev0;

        OffsetIterator(List<Node> nodes, double offset) {
            this.nodes = nodes;
            this.offset = offset;
            idx = 0;
        }

        @Override
        public boolean hasNext() {
            return idx < nodes.size();
        }

        @Override
        public MapViewPoint next() {
            if (!hasNext())
                throw new NoSuchElementException();

            MapViewPoint current = getForIndex(idx);

            if (Math.abs(offset) < 0.1d) {
                idx++;
                return current;
            }

            double xCurrent = current.getInViewX();
            double yCurrent = current.getInViewY();
            if (idx == nodes.size() - 1) {
                ++idx;
                if (prev != null) {
                    return mapState.getForView(xPrev0 + xCurrent - prev.getInViewX(),
                                               yPrev0 + yCurrent - prev.getInViewY());
                } else {
                    return current;
                }
            }

            MapViewPoint next = getForIndex(idx + 1);
            double dxNext = next.getInViewX() - xCurrent;
            double dyNext = next.getInViewY() - yCurrent;
            double lenNext = Math.sqrt(dxNext*dxNext + dyNext*dyNext);

            if (lenNext < 1e-11) {
                lenNext = 1; // value does not matter, because dy_next and dx_next is 0
            }

            // calculate the position of the translated current point
            double om = offset / lenNext;
            double xCurrent0 = xCurrent + om * dyNext;
            double yCurrent0 = yCurrent - om * dxNext;

            if (idx == 0) {
                ++idx;
                prev = current;
                xPrev0 = xCurrent0;
                yPrev0 = yCurrent0;
                return mapState.getForView(xCurrent0, yCurrent0);
            } else {
                double dxPrev = xCurrent - prev.getInViewX();
                double dyPrev = yCurrent - prev.getInViewY();
                // determine intersection of the lines parallel to the two segments
                double det = dxNext*dyPrev - dxPrev*dyNext;
                double m = dxNext*(yCurrent0 - yPrev0) - dyNext*(xCurrent0 - xPrev0);

                if (Utils.equalsEpsilon(det, 0) || Math.signum(det) != Math.signum(m)) {
                    ++idx;
                    prev = current;
                    xPrev0 = xCurrent0;
                    yPrev0 = yCurrent0;
                    return mapState.getForView(xCurrent0, yCurrent0);
                }

                double f = m / det;
                if (f < 0) {
                    ++idx;
                    prev = current;
                    xPrev0 = xCurrent0;
                    yPrev0 = yCurrent0;
                    return mapState.getForView(xCurrent0, yCurrent0);
                }
                // the position of the intersection or intermittent point
                double cx = xPrev0 + f * dxPrev;
                double cy = yPrev0 + f * dyPrev;

                if (f > 1) {
                    // check if the intersection point is too far away, this will happen for sharp angles
                    double dxI = cx - xCurrent;
                    double dyI = cy - yCurrent;
                    double lenISq = dxI * dxI + dyI * dyI;

                    if (lenISq > Math.abs(2 * offset * offset)) {
                        // intersection point is too far away, calculate intermittent points for capping
                        double dxPrev0 = xCurrent0 - xPrev0;
                        double dyPrev0 = yCurrent0 - yPrev0;
                        double lenPrev0 = Math.sqrt(dxPrev0 * dxPrev0 + dyPrev0 * dyPrev0);
                        f = 1 + Math.abs(offset / lenPrev0);
                        double cxCap = xPrev0 + f * dxPrev;
                        double cyCap = yPrev0 + f * dyPrev;
                        xPrev0 = cxCap;
                        yPrev0 = cyCap;
                        // calculate a virtual prev point which lies on a line that goes through current and
                        // is perpendicular to the line that goes through current and the intersection
                        // so that the next capping point is calculated with it.
                        double lenI = Math.sqrt(lenISq);
                        double xv = xCurrent + dyI / lenI;
                        double yv = yCurrent - dxI / lenI;

                        prev = mapState.getForView(xv, yv);
                        return mapState.getForView(cxCap, cyCap);
                    }
                }
                ++idx;
                prev = current;
                xPrev0 = xCurrent0;
                yPrev0 = yCurrent0;
                return mapState.getForView(cx, cy);
            }
        }

        private MapViewPoint getForIndex(int i) {
            return mapState.getPointFor(nodes.get(i));
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * This stores a style and a primitive that should be painted with that style.
     */
    public static class StyleRecord implements Comparable<StyleRecord> {
        private final StyleElement style;
        private final OsmPrimitive osm;
        private final int flags;

        StyleRecord(StyleElement style, OsmPrimitive osm, int flags) {
            this.style = style;
            this.osm = osm;
            this.flags = flags;
        }

        @Override
        public int compareTo(StyleRecord other) {
            if ((this.flags & FLAG_DISABLED) != 0 && (other.flags & FLAG_DISABLED) == 0)
                return -1;
            if ((this.flags & FLAG_DISABLED) == 0 && (other.flags & FLAG_DISABLED) != 0)
                return 1;

            int d0 = Float.compare(this.style.majorZIndex, other.style.majorZIndex);
            if (d0 != 0)
                return d0;

            // selected on top of member of selected on top of unselected
            // FLAG_DISABLED bit is the same at this point
            if (this.flags > other.flags)
                return 1;
            if (this.flags < other.flags)
                return -1;

            int dz = Float.compare(this.style.zIndex, other.style.zIndex);
            if (dz != 0)
                return dz;

            // simple node on top of icons and shapes
            if (NodeElement.SIMPLE_NODE_ELEMSTYLE.equals(this.style) && !NodeElement.SIMPLE_NODE_ELEMSTYLE.equals(other.style))
                return 1;
            if (!NodeElement.SIMPLE_NODE_ELEMSTYLE.equals(this.style) && NodeElement.SIMPLE_NODE_ELEMSTYLE.equals(other.style))
                return -1;

            // newer primitives to the front
            long id = this.osm.getUniqueId() - other.osm.getUniqueId();
            if (id > 0)
                return 1;
            if (id < 0)
                return -1;

            return Float.compare(this.style.objectZIndex, other.style.objectZIndex);
        }

        /**
         * Get the style for this style element.
         * @return The style
         */
        public StyleElement getStyle() {
            return style;
        }

        /**
         * Paints the primitive with the style.
         * @param paintSettings The settings to use.
         * @param painter The painter to paint the style.
         */
        public void paintPrimitive(MapPaintSettings paintSettings, StyledMapRenderer painter) {
            style.paintPrimitive(
                    osm,
                    paintSettings,
                    painter,
                    (flags & FLAG_SELECTED) != 0,
                    (flags & FLAG_OUTERMEMBER_OF_SELECTED) != 0,
                    (flags & FLAG_MEMBER_OF_SELECTED) != 0
            );
        }

        @Override
        public String toString() {
            return "StyleRecord [style=" + style + ", osm=" + osm + ", flags=" + flags + "]";
        }
    }

    private static Map<Font, Boolean> IS_GLYPH_VECTOR_DOUBLE_TRANSLATION_BUG = new HashMap<>();

    /**
     * Check, if this System has the GlyphVector double translation bug.
     *
     * With this bug, <code>gv.setGlyphTransform(i, trfm)</code> has a different
     * effect than on most other systems, namely the translation components
     * ("m02" &amp; "m12", {@link AffineTransform}) appear to be twice as large, as
     * they actually are. The rotation is unaffected (scale &amp; shear not tested
     * so far).
     *
     * This bug has only been observed on Mac OS X, see #7841.
     *
     * After switch to Java 7, this test is a false positive on Mac OS X (see #10446),
     * i.e. it returns true, but the real rendering code does not require any special
     * handling.
     * It hasn't been further investigated why the test reports a wrong result in
     * this case, but the method has been changed to simply return false by default.
     * (This can be changed with a setting in the advanced preferences.)
     *
     * @param font The font to check.
     * @return false by default, but depends on the value of the advanced
     * preference glyph-bug=false|true|auto, where auto is the automatic detection
     * method which apparently no longer gives a useful result for Java 7.
     */
    public static boolean isGlyphVectorDoubleTranslationBug(Font font) {
        Boolean cached = IS_GLYPH_VECTOR_DOUBLE_TRANSLATION_BUG.get(font);
        if (cached != null)
            return cached;
        String overridePref = Main.pref.get("glyph-bug", "auto");
        if ("auto".equals(overridePref)) {
            FontRenderContext frc = new FontRenderContext(null, false, false);
            GlyphVector gv = font.createGlyphVector(frc, "x");
            gv.setGlyphTransform(0, AffineTransform.getTranslateInstance(1000, 1000));
            Shape shape = gv.getGlyphOutline(0);
            if (Main.isTraceEnabled()) {
                Main.trace("#10446: shape: "+shape.getBounds());
            }
            // x is about 1000 on normal stystems and about 2000 when the bug occurs
            int x = shape.getBounds().x;
            boolean isBug = x > 1500;
            IS_GLYPH_VECTOR_DOUBLE_TRANSLATION_BUG.put(font, isBug);
            return isBug;
        } else {
            boolean override = Boolean.parseBoolean(overridePref);
            IS_GLYPH_VECTOR_DOUBLE_TRANSLATION_BUG.put(font, override);
            return override;
        }
    }

    private double circum;
    private double scale;

    private MapPaintSettings paintSettings;

    private Color highlightColorTransparent;

    /**
     * Flags used to store the primitive state along with the style. This is the normal style.
     * <p>
     * Not used in any public interfaces.
     */
    private static final int FLAG_NORMAL = 0;
    /**
     * A primitive with {@link OsmPrimitive#isDisabled()}
     */
    private static final int FLAG_DISABLED = 1;
    /**
     * A primitive with {@link OsmPrimitive#isMemberOfSelected()}
     */
    private static final int FLAG_MEMBER_OF_SELECTED = 2;
    /**
     * A primitive with {@link OsmPrimitive#isSelected()}
     */
    private static final int FLAG_SELECTED = 4;
    /**
     * A primitive with {@link OsmPrimitive#isOuterMemberOfSelected()}
     */
    private static final int FLAG_OUTERMEMBER_OF_SELECTED = 8;

    private static final double PHI = Math.toRadians(20);
    private static final double cosPHI = Math.cos(PHI);
    private static final double sinPHI = Math.sin(PHI);

    private Collection<WaySegment> highlightWaySegments;

    // highlight customization fields
    private int highlightLineWidth;
    private int highlightPointRadius;
    private int widerHighlight;
    private int highlightStep;

    //flag that activate wider highlight mode
    private boolean useWiderHighlight;

    private boolean useStrokes;
    private boolean showNames;
    private boolean showIcons;
    private boolean isOutlineOnly;

    private Font orderFont;

    private boolean leftHandTraffic;
    private Object antialiasing;

    private Supplier<RenderBenchmarkCollector> benchmarkFactory = RenderBenchmarkCollector.defaultBenchmarkSupplier();

    /**
     * Constructs a new {@code StyledMapRenderer}.
     *
     * @param g the graphics context. Must not be null.
     * @param nc the map viewport. Must not be null.
     * @param isInactiveMode if true, the paint visitor shall render OSM objects such that they
     * look inactive. Example: rendering of data in an inactive layer using light gray as color only.
     * @throws IllegalArgumentException if {@code g} is null
     * @throws IllegalArgumentException if {@code nc} is null
     */
    public StyledMapRenderer(Graphics2D g, NavigatableComponent nc, boolean isInactiveMode) {
        super(g, nc, isInactiveMode);

        if (nc != null) {
            Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
            useWiderHighlight = !(focusOwner instanceof AbstractButton || focusOwner == nc);
        }
    }

    private void displaySegments(MapViewPath path, Path2D orientationArrows, Path2D onewayArrows, Path2D onewayArrowsCasing,
            Color color, BasicStroke line, BasicStroke dashes, Color dashedColor) {
        g.setColor(isInactiveMode ? inactiveColor : color);
        if (useStrokes) {
            g.setStroke(line);
        }
        g.draw(path.computeClippedLine(g.getStroke()));

        if (!isInactiveMode && useStrokes && dashes != null) {
            g.setColor(dashedColor);
            g.setStroke(dashes);
            g.draw(path.computeClippedLine(dashes));
        }

        if (orientationArrows != null) {
            g.setColor(isInactiveMode ? inactiveColor : color);
            g.setStroke(new BasicStroke(line.getLineWidth(), line.getEndCap(), BasicStroke.JOIN_MITER, line.getMiterLimit()));
            g.draw(orientationArrows);
        }

        if (onewayArrows != null) {
            g.setStroke(new BasicStroke(1, line.getEndCap(), BasicStroke.JOIN_MITER, line.getMiterLimit()));
            g.fill(onewayArrowsCasing);
            g.setColor(isInactiveMode ? inactiveColor : backgroundColor);
            g.fill(onewayArrows);
        }

        if (useStrokes) {
            g.setStroke(new BasicStroke());
        }
    }

    /**
     * Displays text at specified position including its halo, if applicable.
     *
     * @param gv Text's glyphs to display. If {@code null}, use text from {@code s} instead.
     * @param s text to display if {@code gv} is {@code null}
     * @param x X position
     * @param y Y position
     * @param disabled {@code true} if element is disabled (filtered out)
     * @param text text style to use
     */
    private void displayText(GlyphVector gv, String s, int x, int y, boolean disabled, TextLabel text) {
        if (gv == null && s.isEmpty()) return;
        if (isInactiveMode || disabled) {
            g.setColor(inactiveColor);
            if (gv != null) {
                g.drawGlyphVector(gv, x, y);
            } else {
                g.setFont(text.font);
                g.drawString(s, x, y);
            }
        } else if (text.haloRadius != null) {
            g.setStroke(new BasicStroke(2*text.haloRadius, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(text.haloColor);
            Shape textOutline;
            if (gv == null) {
                FontRenderContext frc = g.getFontRenderContext();
                TextLayout tl = new TextLayout(s, text.font, frc);
                textOutline = tl.getOutline(AffineTransform.getTranslateInstance(x, y));
            } else {
                textOutline = gv.getOutline(x, y);
            }
            g.draw(textOutline);
            g.setStroke(new BasicStroke());
            g.setColor(text.color);
            g.fill(textOutline);
        } else {
            g.setColor(text.color);
            if (gv != null) {
                g.drawGlyphVector(gv, x, y);
            } else {
                g.setFont(text.font);
                g.drawString(s, x, y);
            }
        }
    }

    /**
     * Worker function for drawing areas.
     *
     * @param osm the primitive
     * @param path the path object for the area that should be drawn; in case
     * of multipolygons, this can path can be a complex shape with one outer
     * polygon and one or more inner polygons
     * @param color The color to fill the area with.
     * @param fillImage The image to fill the area with. Overrides color.
     * @param extent if not null, area will be filled partially; specifies, how
     * far to fill from the boundary towards the center of the area;
     * if null, area will be filled completely
     * @param pfClip clipping area for partial fill (only needed for unclosed
     * polygons)
     * @param disabled If this should be drawn with a special disabled style.
     * @param text The text to write on the area.
     */
    protected void drawArea(OsmPrimitive osm, Path2D.Double path, Color color,
            MapImage fillImage, Float extent, Path2D.Double pfClip, boolean disabled, TextLabel text) {

        Shape area = path.createTransformedShape(mapState.getAffineTransform());

        if (!isOutlineOnly) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            if (fillImage == null) {
                if (isInactiveMode) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.33f));
                }
                g.setColor(color);
                if (extent == null) {
                    g.fill(area);
                } else {
                    Shape oldClip = g.getClip();
                    Shape clip = area;
                    if (pfClip != null) {
                        clip = pfClip.createTransformedShape(mapState.getAffineTransform());
                    }
                    g.clip(clip);
                    g.setStroke(new BasicStroke(2 * extent, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 4));
                    g.draw(area);
                    g.setClip(oldClip);
                }
            } else {
                TexturePaint texture = new TexturePaint(fillImage.getImage(disabled),
                        new Rectangle(0, 0, fillImage.getWidth(), fillImage.getHeight()));
                g.setPaint(texture);
                Float alpha = fillImage.getAlphaFloat();
                if (!Utils.equalsEpsilon(alpha, 1f)) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                }
                if (extent == null) {
                    g.fill(area);
                } else {
                    Shape oldClip = g.getClip();
                    BasicStroke stroke = new BasicStroke(2 * extent, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER);
                    g.clip(stroke.createStrokedShape(area));
                    Shape fill = area;
                    if (pfClip != null) {
                        fill = pfClip.createTransformedShape(mapState.getAffineTransform());
                    }
                    g.fill(fill);
                    g.setClip(oldClip);
                }
                g.setPaintMode();
            }
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);
        }

        drawAreaText(osm, text, area);
    }

    private void drawAreaText(OsmPrimitive osm, TextLabel text, Shape area) {
        if (text != null && isShowNames()) {
            // abort if we can't compose the label to be rendered
            if (text.labelCompositionStrategy == null) return;
            String name = text.labelCompositionStrategy.compose(osm);
            if (name == null) return;

            Rectangle pb = area.getBounds();
            FontMetrics fontMetrics = g.getFontMetrics(orderFont); // if slow, use cache
            Rectangle2D nb = fontMetrics.getStringBounds(name, g); // if slow, approximate by strlen()*maxcharbounds(font)

            // Using the Centroid is Nicer for buildings like: +--------+
            // but this needs to be fast.  As most houses are  |   42   |
            // boxes anyway, the center of the bounding box    +---++---+
            // will have to do.                                    ++
            // Centroids are not optimal either, just imagine a U-shaped house.

            // quick check to see if label box is smaller than primitive box
            if (pb.width >= nb.getWidth() && pb.height >= nb.getHeight()) {

                final double w = pb.width - nb.getWidth();
                final double h = pb.height - nb.getHeight();

                final int x2 = pb.x + (int) (w/2.0);
                final int y2 = pb.y + (int) (h/2.0);

                final int nbw = (int) nb.getWidth();
                final int nbh = (int) nb.getHeight();

                Rectangle centeredNBounds = new Rectangle(x2, y2, nbw, nbh);

                // slower check to see if label is displayed inside primitive shape
                boolean labelOK = area.contains(centeredNBounds);
                if (!labelOK) {
                    // if center position (C) is not inside osm shape, try naively some other positions as follows:
                    // CHECKSTYLE.OFF: SingleSpaceSeparator
                    final int x1 = pb.x + (int)   (w/4.0);
                    final int x3 = pb.x + (int) (3*w/4.0);
                    final int y1 = pb.y + (int)   (h/4.0);
                    final int y3 = pb.y + (int) (3*h/4.0);
                    // CHECKSTYLE.ON: SingleSpaceSeparator
                    // +-----------+
                    // |  5  1  6  |
                    // |  4  C  2  |
                    // |  8  3  7  |
                    // +-----------+
                    Rectangle[] candidates = new Rectangle[] {
                            new Rectangle(x2, y1, nbw, nbh),
                            new Rectangle(x3, y2, nbw, nbh),
                            new Rectangle(x2, y3, nbw, nbh),
                            new Rectangle(x1, y2, nbw, nbh),
                            new Rectangle(x1, y1, nbw, nbh),
                            new Rectangle(x3, y1, nbw, nbh),
                            new Rectangle(x3, y3, nbw, nbh),
                            new Rectangle(x1, y3, nbw, nbh)
                    };
                    // Dumb algorithm to find a better placement. We could surely find a smarter one but it should
                    // solve most of building issues with only few calculations (8 at most)
                    for (int i = 0; i < candidates.length && !labelOK; i++) {
                        centeredNBounds = candidates[i];
                        labelOK = area.contains(centeredNBounds);
                    }
                }
                if (labelOK) {
                    Font defaultFont = g.getFont();
                    int x = (int) (centeredNBounds.getMinX() - nb.getMinX());
                    int y = (int) (centeredNBounds.getMinY() - nb.getMinY());
                    displayText(null, name, x, y, osm.isDisabled(), text);
                    g.setFont(defaultFont);
                } else if (Main.isTraceEnabled()) {
                    Main.trace("Couldn't find a correct label placement for "+osm+" / "+name);
                }
            }
        }
    }

    /**
     * Draws a multipolygon area.
     * @param r The multipolygon relation
     * @param color The color to fill the area with.
     * @param fillImage The image to fill the area with. Overrides color.
     * @param extent if not null, area will be filled partially; specifies, how
     * far to fill from the boundary towards the center of the area;
     * if null, area will be filled completely
     * @param extentThreshold if not null, determines if the partial filled should
     * be replaced by plain fill, when it covers a certain fraction of the total area
     * @param disabled If this should be drawn with a special disabled style.
     * @param text The text to write on the area.
     */
    public void drawArea(Relation r, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled, TextLabel text) {
        Multipolygon multipolygon = MultipolygonCache.getInstance().get(nc, r);
        if (!r.isDisabled() && !multipolygon.getOuterWays().isEmpty()) {
            for (PolyData pd : multipolygon.getCombinedPolygons()) {
                Path2D.Double p = pd.get();
                Path2D.Double pfClip = null;
                if (!isAreaVisible(p)) {
                    continue;
                }
                if (extent != null) {
                    if (!usePartialFill(pd.getAreaAndPerimeter(null), extent, extentThreshold)) {
                        extent = null;
                    } else if (!pd.isClosed()) {
                        pfClip = getPFClip(pd, extent * scale);
                    }
                }
                drawArea(r, p,
                        pd.isSelected() ? paintSettings.getRelationSelectedColor(color.getAlpha()) : color,
                        fillImage, extent, pfClip, disabled, text);
            }
        }
    }

    /**
     * Draws an area defined by a way. They way does not need to be closed, but it should.
     * @param w The way.
     * @param color The color to fill the area with.
     * @param fillImage The image to fill the area with. Overrides color.
     * @param extent if not null, area will be filled partially; specifies, how
     * far to fill from the boundary towards the center of the area;
     * if null, area will be filled completely
     * @param extentThreshold if not null, determines if the partial filled should
     * be replaced by plain fill, when it covers a certain fraction of the total area
     * @param disabled If this should be drawn with a special disabled style.
     * @param text The text to write on the area.
     */
    public void drawArea(Way w, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled, TextLabel text) {
        Path2D.Double pfClip = null;
        if (extent != null) {
            if (!usePartialFill(Geometry.getAreaAndPerimeter(w.getNodes()), extent, extentThreshold)) {
                extent = null;
            } else if (!w.isClosed()) {
                pfClip = getPFClip(w, extent * scale);
            }
        }
        drawArea(w, getPath(w), color, fillImage, extent, pfClip, disabled, text);
    }

    /**
     * Determine, if partial fill should be turned off for this object, because
     * only a small unfilled gap in the center of the area would be left.
     *
     * This is used to get a cleaner look for urban regions with many small
     * areas like buildings, etc.
     * @param ap the area and the perimeter of the object
     * @param extent the "width" of partial fill
     * @param threshold when the partial fill covers that much of the total
     * area, the partial fill is turned off; can be greater than 100% as the
     * covered area is estimated as <code>perimeter * extent</code>
     * @return true, if the partial fill should be used, false otherwise
     */
    private boolean usePartialFill(AreaAndPerimeter ap, float extent, Float threshold) {
        if (threshold == null) return true;
        return ap.getPerimeter() * extent * scale < threshold * ap.getArea();
    }

    public void drawBoxText(Node n, BoxTextElement bs) {
        if (!isShowNames() || bs == null)
            return;

        MapViewPoint p = mapState.getPointFor(n);
        TextLabel text = bs.text;
        String s = text.labelCompositionStrategy.compose(n);
        if (s == null) return;

        Font defaultFont = g.getFont();
        g.setFont(text.font);

        int x = (int) (Math.round(p.getInViewX()) + text.xOffset);
        int y = (int) (Math.round(p.getInViewY()) + text.yOffset);
        /**
         *
         *       left-above __center-above___ right-above
         *         left-top|                 |right-top
         *                 |                 |
         *      left-center|  center-center  |right-center
         *                 |                 |
         *      left-bottom|_________________|right-bottom
         *       left-below   center-below    right-below
         *
         */
        Rectangle box = bs.getBox();
        if (bs.hAlign == HorizontalTextAlignment.RIGHT) {
            x += box.x + box.width + 2;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            Rectangle2D bounds = text.font.getStringBounds(s, frc);
            int textWidth = (int) bounds.getWidth();
            if (bs.hAlign == HorizontalTextAlignment.CENTER) {
                x -= textWidth / 2;
            } else if (bs.hAlign == HorizontalTextAlignment.LEFT) {
                x -= -box.x + 4 + textWidth;
            } else throw new AssertionError();
        }

        if (bs.vAlign == VerticalTextAlignment.BOTTOM) {
            y += box.y + box.height;
        } else {
            FontRenderContext frc = g.getFontRenderContext();
            LineMetrics metrics = text.font.getLineMetrics(s, frc);
            if (bs.vAlign == VerticalTextAlignment.ABOVE) {
                y -= -box.y + metrics.getDescent();
            } else if (bs.vAlign == VerticalTextAlignment.TOP) {
                y -= -box.y - metrics.getAscent();
            } else if (bs.vAlign == VerticalTextAlignment.CENTER) {
                y += (metrics.getAscent() - metrics.getDescent()) / 2;
            } else if (bs.vAlign == VerticalTextAlignment.BELOW) {
                y += box.y + box.height + metrics.getAscent() + 2;
            } else throw new AssertionError();
        }
        displayText(null, s, x, y, n.isDisabled(), text);
        g.setFont(defaultFont);
    }

    /**
     * Draw an image along a way repeatedly.
     *
     * @param way the way
     * @param pattern the image
     * @param disabled If this should be drawn with a special disabled style.
     * @param offset offset from the way
     * @param spacing spacing between two images
     * @param phase initial spacing
     * @param align alignment of the image. The top, center or bottom edge can be aligned with the way.
     */
    public void drawRepeatImage(Way way, MapImage pattern, boolean disabled, double offset, double spacing, double phase,
            LineImageAlignment align) {
        final int imgWidth = pattern.getWidth();
        final double repeat = imgWidth + spacing;
        final int imgHeight = pattern.getHeight();

        int dy1 = (int) ((align.getAlignmentOffset() - .5) * imgHeight);
        int dy2 = dy1 + imgHeight;

        OffsetIterator it = new OffsetIterator(way.getNodes(), offset);
        MapViewPath path = new MapViewPath(mapState);
        if (it.hasNext()) {
            path.moveTo(it.next());
        }
        while (it.hasNext()) {
            path.lineTo(it.next());
        }

        double startOffset = phase % repeat;
        if (startOffset < 0) {
            startOffset += repeat;
        }

        BufferedImage image = pattern.getImage(disabled);

        path.visitClippedLine(startOffset, repeat, (inLineOffset, start, end, startIsOldEnd) -> {
            final double segmentLength = start.distanceToInView(end);
            if (segmentLength < 0.1) {
                // avoid odd patterns when zoomed out.
                return;
            }
            if (segmentLength > repeat * 500) {
                // simply skip drawing so many images - something must be wrong.
                return;
            }
            AffineTransform saveTransform = g.getTransform();
            g.translate(start.getInViewX(), start.getInViewY());
            double dx = end.getInViewX() - start.getInViewX();
            double dy = end.getInViewY() - start.getInViewY();
            g.rotate(Math.atan2(dy, dx));

            // The start of the next image
            double imageStart = -(inLineOffset % repeat);

            while (imageStart < segmentLength) {
                int x = (int) imageStart;
                int sx1 = Math.max(0, -x);
                int sx2 = imgWidth - Math.max(0, x + imgWidth - (int) Math.ceil(segmentLength));
                g.drawImage(image, x + sx1, dy1, x + sx2, dy2, sx1, 0, sx2, imgHeight, null);
                imageStart += repeat;
            }

            g.setTransform(saveTransform);
        });
    }

    @Override
    public void drawNode(Node n, Color color, int size, boolean fill) {
        if (size <= 0 && !n.isHighlighted())
            return;

        MapViewPoint p = mapState.getPointFor(n);

        if (n.isHighlighted()) {
            drawPointHighlight(p.getInView(), size);
        }

        if (size > 1 && p.isInView()) {
            int radius = size / 2;

            if (isInactiveMode || n.isDisabled()) {
                g.setColor(inactiveColor);
            } else {
                g.setColor(color);
            }
            Rectangle2D rect = new Rectangle2D.Double(p.getInViewX()-radius-1, p.getInViewY()-radius-1, size + 1, size + 1);
            if (fill) {
                g.fill(rect);
            } else {
                g.draw(rect);
            }
        }
    }

    /**
     * Draw the icon for a given node.
     * @param n The node
     * @param img The icon to draw at the node position
     * @param disabled {@code} true to render disabled version, {@code false} for the standard version
     * @param selected {@code} true to render it as selected, {@code false} otherwise
     * @param member {@code} true to render it as a relation member, {@code false} otherwise
     * @param theta the angle of rotation in radians
     */
    public void drawNodeIcon(Node n, MapImage img, boolean disabled, boolean selected, boolean member, double theta) {
        MapViewPoint p = mapState.getPointFor(n);

        int w = img.getWidth();
        int h = img.getHeight();
        if (n.isHighlighted()) {
            drawPointHighlight(p.getInView(), Math.max(w, h));
        }

        float alpha = img.getAlphaFloat();

        Graphics2D temporaryGraphics = (Graphics2D) g.create();
        if (!Utils.equalsEpsilon(alpha, 1f)) {
            temporaryGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        double x = Math.round(p.getInViewX());
        double y = Math.round(p.getInViewY());
        temporaryGraphics.translate(x, y);
        temporaryGraphics.rotate(theta);
        int drawX = -w/2 + img.offsetX;
        int drawY = -h/2 + img.offsetY;
        temporaryGraphics.drawImage(img.getImage(disabled), drawX, drawY, nc);
        if (selected || member) {
            Color color;
            if (disabled) {
                color = inactiveColor;
            } else if (selected) {
                color = selectedColor;
            } else {
                color = relationSelectedColor;
            }
            temporaryGraphics.setColor(color);
            temporaryGraphics.draw(new Rectangle2D.Double(drawX - 2, drawY - 2, w + 4, h + 4));
        }
    }

    /**
     * Draw the symbol and possibly a highlight marking on a given node.
     * @param n The position to draw the symbol on
     * @param s The symbol to draw
     * @param fillColor The color to fill the symbol with
     * @param strokeColor The color to use for the outer corner of the symbol
     */
    public void drawNodeSymbol(Node n, Symbol s, Color fillColor, Color strokeColor) {
        MapViewPoint p = mapState.getPointFor(n);

        if (n.isHighlighted()) {
            drawPointHighlight(p.getInView(), s.size);
        }

        if (fillColor != null || strokeColor != null) {
            Shape shape = s.buildShapeAround(p.getInViewX(), p.getInViewY());

            if (fillColor != null) {
                g.setColor(fillColor);
                g.fill(shape);
            }
            if (s.stroke != null) {
                g.setStroke(s.stroke);
                g.setColor(strokeColor);
                g.draw(shape);
                g.setStroke(new BasicStroke());
            }
        }
    }

    /**
     * Draw a number of the order of the two consecutive nodes within the
     * parents way
     *
     * @param n1 First node of the way segment.
     * @param n2 Second node of the way segment.
     * @param orderNumber The number of the segment in the way.
     * @param clr The color to use for drawing the text.
     */
    public void drawOrderNumber(Node n1, Node n2, int orderNumber, Color clr) {
        MapViewPoint p1 = mapState.getPointFor(n1);
        MapViewPoint p2 = mapState.getPointFor(n2);
        drawOrderNumber(p1, p2, orderNumber, clr);
    }

    /**
     * highlights a given GeneralPath using the settings from BasicStroke to match the line's
     * style. Width of the highlight is hard coded.
     * @param path path to draw
     * @param line line style
     */
    private void drawPathHighlight(MapViewPath path, BasicStroke line) {
        if (path == null)
            return;
        g.setColor(highlightColorTransparent);
        float w = line.getLineWidth() + highlightLineWidth;
        if (useWiderHighlight) w += widerHighlight;
        while (w >= line.getLineWidth()) {
            g.setStroke(new BasicStroke(w, line.getEndCap(), line.getLineJoin(), line.getMiterLimit()));
            g.draw(path);
            w -= highlightStep;
        }
    }

    /**
     * highlights a given point by drawing a rounded rectangle around it. Give the
     * size of the object you want to be highlighted, width is added automatically.
     * @param p point
     * @param size highlight size
     */
    private void drawPointHighlight(Point2D p, int size) {
        g.setColor(highlightColorTransparent);
        int s = size + highlightPointRadius;
        if (useWiderHighlight) s += widerHighlight;
        while (s >= size) {
            int r = (int) Math.floor(s/2d);
            g.fill(new RoundRectangle2D.Double(p.getX()-r, p.getY()-r, s, s, r, r));
            s -= highlightStep;
        }
    }

    public void drawRestriction(Image img, Point pVia, double vx, double vx2, double vy, double vy2, double angle, boolean selected) {
        // rotate image with direction last node in from to, and scale down image to 16*16 pixels
        Image smallImg = ImageProvider.createRotatedImage(img, angle, new Dimension(16, 16));
        int w = smallImg.getWidth(null), h = smallImg.getHeight(null);
        g.drawImage(smallImg, (int) (pVia.x+vx+vx2)-w/2, (int) (pVia.y+vy+vy2)-h/2, nc);

        if (selected) {
            g.setColor(isInactiveMode ? inactiveColor : relationSelectedColor);
            g.drawRect((int) (pVia.x+vx+vx2)-w/2-2, (int) (pVia.y+vy+vy2)-h/2-2, w+4, h+4);
        }
    }

    public void drawRestriction(Relation r, MapImage icon, boolean disabled) {
        Way fromWay = null;
        Way toWay = null;
        OsmPrimitive via = null;

        /* find the "from", "via" and "to" elements */
        for (RelationMember m : r.getMembers()) {
            if (m.getMember().isIncomplete())
                return;
            else {
                if (m.isWay()) {
                    Way w = m.getWay();
                    if (w.getNodesCount() < 2) {
                        continue;
                    }

                    switch(m.getRole()) {
                    case "from":
                        if (fromWay == null) {
                            fromWay = w;
                        }
                        break;
                    case "to":
                        if (toWay == null) {
                            toWay = w;
                        }
                        break;
                    case "via":
                        if (via == null) {
                            via = w;
                        }
                        break;
                    default: // Do nothing
                    }
                } else if (m.isNode()) {
                    Node n = m.getNode();
                    if ("via".equals(m.getRole()) && via == null) {
                        via = n;
                    }
                }
            }
        }

        if (fromWay == null || toWay == null || via == null)
            return;

        Node viaNode;
        if (via instanceof Node) {
            viaNode = (Node) via;
            if (!fromWay.isFirstLastNode(viaNode))
                return;
        } else {
            Way viaWay = (Way) via;
            Node firstNode = viaWay.firstNode();
            Node lastNode = viaWay.lastNode();
            Boolean onewayvia = Boolean.FALSE;

            String onewayviastr = viaWay.get("oneway");
            if (onewayviastr != null) {
                if ("-1".equals(onewayviastr)) {
                    onewayvia = Boolean.TRUE;
                    Node tmp = firstNode;
                    firstNode = lastNode;
                    lastNode = tmp;
                } else {
                    onewayvia = OsmUtils.getOsmBoolean(onewayviastr);
                    if (onewayvia == null) {
                        onewayvia = Boolean.FALSE;
                    }
                }
            }

            if (fromWay.isFirstLastNode(firstNode)) {
                viaNode = firstNode;
            } else if (!onewayvia && fromWay.isFirstLastNode(lastNode)) {
                viaNode = lastNode;
            } else
                return;
        }

        /* find the "direct" nodes before the via node */
        Node fromNode;
        if (fromWay.firstNode() == via) {
            fromNode = fromWay.getNode(1);
        } else {
            fromNode = fromWay.getNode(fromWay.getNodesCount()-2);
        }

        Point pFrom = nc.getPoint(fromNode);
        Point pVia = nc.getPoint(viaNode);

        /* starting from via, go back the "from" way a few pixels
           (calculate the vector vx/vy with the specified length and the direction
           away from the "via" node along the first segment of the "from" way)
         */
        double distanceFromVia = 14;
        double dx = pFrom.x >= pVia.x ? pFrom.x - pVia.x : pVia.x - pFrom.x;
        double dy = pFrom.y >= pVia.y ? pFrom.y - pVia.y : pVia.y - pFrom.y;

        double fromAngle;
        if (dx == 0) {
            fromAngle = Math.PI/2;
        } else {
            fromAngle = Math.atan(dy / dx);
        }
        double fromAngleDeg = Math.toDegrees(fromAngle);

        double vx = distanceFromVia * Math.cos(fromAngle);
        double vy = distanceFromVia * Math.sin(fromAngle);

        if (pFrom.x < pVia.x) {
            vx = -vx;
        }
        if (pFrom.y < pVia.y) {
            vy = -vy;
        }

        /* go a few pixels away from the way (in a right angle)
           (calculate the vx2/vy2 vector with the specified length and the direction
           90degrees away from the first segment of the "from" way)
         */
        double distanceFromWay = 10;
        double vx2 = 0;
        double vy2 = 0;
        double iconAngle = 0;

        if (pFrom.x >= pVia.x && pFrom.y >= pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            }
            iconAngle = 270+fromAngleDeg;
        }
        if (pFrom.x < pVia.x && pFrom.y >= pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            }
            iconAngle = 90-fromAngleDeg;
        }
        if (pFrom.x < pVia.x && pFrom.y < pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg - 90));
            }
            iconAngle = 90+fromAngleDeg;
        }
        if (pFrom.x >= pVia.x && pFrom.y < pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg + 180));
            } else {
                vx2 = distanceFromWay * Math.sin(Math.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Math.toRadians(fromAngleDeg));
            }
            iconAngle = 270-fromAngleDeg;
        }

        drawRestriction(icon.getImage(disabled),
                pVia, vx, vx2, vy, vy2, iconAngle, r.isSelected());
    }

    /**
     * A half segment that can be used to place text on it. Used in the drawTextOnPath algorithm.
     * @author Michael Zangl
     */
    private static class HalfSegment {
        /**
         * start point of half segment (as length along the way)
         */
        final double start;
        /**
         * end point of half segment (as length along the way)
         */
        final double end;
        /**
         * quality factor (off screen / partly on screen / fully on screen)
         */
        final double quality;

        /**
         * Create a new half segment
         * @param start The start along the way
         * @param end The end of the segment
         * @param quality A quality factor.
         */
        HalfSegment(double start, double end, double quality) {
            super();
            this.start = start;
            this.end = end;
            this.quality = quality;
        }

        @Override
        public String toString() {
            return "HalfSegment [start=" + start + ", end=" + end + ", quality=" + quality + "]";
        }
    }

    /**
     * Draws a text along a given way.
     * @param way The way to draw the text on.
     * @param text The text definition (font/.../text content) to draw.
     */
    public void drawTextOnPath(Way way, TextLabel text) {
        if (way == null || text == null)
            return;
        String name = text.getString(way);
        if (name == null || name.isEmpty())
            return;

        FontMetrics fontMetrics = g.getFontMetrics(text.font);
        Rectangle2D rec = fontMetrics.getStringBounds(name, g);

        Rectangle bounds = g.getClipBounds();

        List<MapViewPoint> points = way.getNodes().stream().map(mapState::getPointFor).collect(Collectors.toList());

        // find half segments that are long enough to draw text on (don't draw text over the cross hair in the center of each segment)
        List<HalfSegment> longHalfSegment = new ArrayList<>();

        double pathLength = computePath(2 * (rec.getWidth() + 4), bounds, points, longHalfSegment);

        if (rec.getWidth() > pathLength)
            return;

        double t1, t2;

        if (!longHalfSegment.isEmpty()) {
            // find the segment with the best quality. If there are several with best quality, the one close to the center is prefered.
            Optional<HalfSegment> besto = longHalfSegment.stream().max(
                    Comparator.comparingDouble(segment ->
                        segment.quality - 1e-5 * Math.abs(0.5 * (segment.end + segment.start) - 0.5 * pathLength)
                    ));
            if (!besto.isPresent())
                throw new IllegalStateException("Unable to find the segment with the best quality for " + way);
            HalfSegment best = besto.get();
            double remaining = best.end - best.start - rec.getWidth(); // total space left and right from the text
            // The space left and right of the text should be distributed 20% - 80% (towards the center),
            // but the smaller space should not be less than 7 px.
            // However, if the total remaining space is less than 14 px, then distribute it evenly.
            double smallerSpace = Math.min(Math.max(0.2 * remaining, 7), 0.5 * remaining);
            if ((best.end + best.start)/2 < pathLength/2) {
                t2 = best.end - smallerSpace;
                t1 = t2 - rec.getWidth();
            } else {
                t1 = best.start + smallerSpace;
                t2 = t1 + rec.getWidth();
            }
        } else {
            // doesn't fit into one half-segment -> just put it in the center of the way
            t1 = pathLength/2 - rec.getWidth()/2;
            t2 = pathLength/2 + rec.getWidth()/2;
        }
        t1 /= pathLength;
        t2 /= pathLength;

        double[] p1 = pointAt(t1, points, pathLength);
        double[] p2 = pointAt(t2, points, pathLength);

        if (p1 == null || p2 == null)
            return;

        double angleOffset;
        double offsetSign;
        double tStart;

        if (p1[0] < p2[0] &&
                p1[2] < Math.PI/2 &&
                p1[2] > -Math.PI/2) {
            angleOffset = 0;
            offsetSign = 1;
            tStart = t1;
        } else {
            angleOffset = Math.PI;
            offsetSign = -1;
            tStart = t2;
        }

        List<GlyphVector> gvs = Utils.getGlyphVectorsBidi(name, text.font, g.getFontRenderContext());
        double gvOffset = 0;
        for (GlyphVector gv : gvs) {
            double gvWidth = gv.getLogicalBounds().getBounds2D().getWidth();
            for (int i = 0; i < gv.getNumGlyphs(); ++i) {
                Rectangle2D rect = gv.getGlyphLogicalBounds(i).getBounds2D();
                double t = tStart + offsetSign * (gvOffset + rect.getX() + rect.getWidth()/2) / pathLength;
                double[] p = pointAt(t, points, pathLength);
                if (p != null) {
                    AffineTransform trfm = AffineTransform.getTranslateInstance(p[0] - rect.getX(), p[1]);
                    trfm.rotate(p[2]+angleOffset);
                    double off = -rect.getY() - rect.getHeight()/2 + text.yOffset;
                    trfm.translate(-rect.getWidth()/2, off);
                    if (isGlyphVectorDoubleTranslationBug(text.font)) {
                        // scale the translation components by one half
                        AffineTransform tmp = AffineTransform.getTranslateInstance(-0.5 * trfm.getTranslateX(), -0.5 * trfm.getTranslateY());
                        tmp.concatenate(trfm);
                        trfm = tmp;
                    }
                    gv.setGlyphTransform(i, trfm);
                }
            }
            displayText(gv, null, 0, 0, way.isDisabled(), text);
            gvOffset += gvWidth;
        }
    }

    private static double computePath(double minSegmentLength, Rectangle bounds, List<MapViewPoint> points,
            List<HalfSegment> longHalfSegment) {
        MapViewPoint lastPoint = points.get(0);
        double pathLength = 0;
        for (MapViewPoint p : points.subList(1, points.size())) {
            double segmentLength = p.distanceToInView(lastPoint);
            if (segmentLength > minSegmentLength) {
                Point2D center = new Point2D.Double((lastPoint.getInViewX() + p.getInViewX())/2, (lastPoint.getInViewY() + p.getInViewY())/2);
                double q = computeQuality(bounds, lastPoint, center);
                // prefer the first one for quality equality.
                longHalfSegment.add(new HalfSegment(pathLength, pathLength + segmentLength / 2, q));

                q = 0;
                if (bounds != null) {
                    if (bounds.contains(center) && bounds.contains(p.getInView())) {
                        q = 2;
                    } else if (bounds.contains(center) || bounds.contains(p.getInView())) {
                        q = 1;
                    }
                }
                longHalfSegment.add(new HalfSegment(pathLength + segmentLength / 2, pathLength + segmentLength, q));
            }
            pathLength += segmentLength;
            lastPoint = p;
        }
        return pathLength;
    }

    private static double computeQuality(Rectangle bounds, MapViewPoint p1, Point2D p2) {
        double q = 0;
        if (bounds != null) {
            if (bounds.contains(p1.getInView())) {
                q += 1;
            }
            if (bounds.contains(p2)) {
                q += 1;
            }
        }
        return q;
    }

    /**
     * draw way. This method allows for two draw styles (line using color, dashes using dashedColor) to be passed.
     * @param way The way to draw
     * @param color The base color to draw the way in
     * @param line The line style to use. This is drawn using color.
     * @param dashes The dash style to use. This is drawn using dashedColor. <code>null</code> if unused.
     * @param dashedColor The color of the dashes.
     * @param offset The offset
     * @param showOrientation show arrows that indicate the technical orientation of
     *              the way (defined by order of nodes)
     * @param showHeadArrowOnly True if only the arrow at the end of the line but not those on the segments should be displayed.
     * @param showOneway show symbols that indicate the direction of the feature,
     *              e.g. oneway street or waterway
     * @param onewayReversed for oneway=-1 and similar
     */
    public void drawWay(Way way, Color color, BasicStroke line, BasicStroke dashes, Color dashedColor, float offset,
            boolean showOrientation, boolean showHeadArrowOnly,
            boolean showOneway, boolean onewayReversed) {

        MapViewPath path = new MapViewPath(mapState);
        MapViewPath orientationArrows = showOrientation ? new MapViewPath(mapState) : null;
        MapViewPath onewayArrows;
        MapViewPath onewayArrowsCasing;
        Rectangle bounds = g.getClipBounds();
        if (bounds != null) {
            // avoid arrow heads at the border
            bounds.grow(100, 100);
        }

        List<Node> wayNodes = way.getNodes();
        if (wayNodes.size() < 2) return;

        // only highlight the segment if the way itself is not highlighted
        if (!way.isHighlighted() && highlightWaySegments != null) {
            MapViewPath highlightSegs = null;
            for (WaySegment ws : highlightWaySegments) {
                if (ws.way != way || ws.lowerIndex < offset) {
                    continue;
                }
                if (highlightSegs == null) {
                    highlightSegs = new MapViewPath(mapState);
                }

                highlightSegs.moveTo(ws.getFirstNode());
                highlightSegs.lineTo(ws.getSecondNode());
            }

            drawPathHighlight(highlightSegs, line);
        }

        MapViewPoint lastPoint = null;
        double wayLength = 0;
        Iterator<MapViewPoint> it = new OffsetIterator(wayNodes, offset);
        boolean initialMoveToNeeded = true;
        while (it.hasNext()) {
            MapViewPoint p = it.next();
            if (lastPoint != null) {
                MapViewPoint p1 = lastPoint;
                MapViewPoint p2 = p;

                if (initialMoveToNeeded) {
                    initialMoveToNeeded = false;
                    path.moveTo(p1);
                }
                path.lineTo(p2);

                /* draw arrow */
                if (showHeadArrowOnly ? !it.hasNext() : showOrientation) {
                    //TODO: Cache
                    ArrowPaintHelper drawHelper = new ArrowPaintHelper(PHI, 10 + line.getLineWidth());
                    drawHelper.paintArrowAt(orientationArrows, p2, p1);
                }
                if (showOneway) {
                    final double segmentLength = p1.distanceToInView(p2);
                    if (segmentLength != 0) {
                    }
                    wayLength += segmentLength;
                }
            }
            lastPoint = p;
        }
        if (showOneway) {
            onewayArrows = new MapViewPath(mapState);
            onewayArrowsCasing = new MapViewPath(mapState);
            double interval = 60;

            path.visitClippedLine(0, 60, (inLineOffset, start, end, startIsOldEnd) -> {
                double segmentLength = start.distanceToInView(end);
                if (segmentLength > 0.001) {
                    final double nx = (end.getInViewX() - start.getInViewX()) / segmentLength;
                    final double ny = (end.getInViewY() - start.getInViewY()) / segmentLength;

                    // distance from p1
                    double dist = interval - (inLineOffset % interval);

                    while (dist < segmentLength) {
                        appenOnewayPath(onewayReversed, start, nx, ny, dist, 3d, onewayArrowsCasing);
                        appenOnewayPath(onewayReversed, start, nx, ny, dist, 2d, onewayArrows);
                        dist += interval;
                    }
                }
            });
        } else {
            onewayArrows = null;
            onewayArrowsCasing = null;
        }

        if (way.isHighlighted()) {
            drawPathHighlight(path, line);
        }
        displaySegments(path, orientationArrows, onewayArrows, onewayArrowsCasing, color, line, dashes, dashedColor);
    }

    private static void appenOnewayPath(boolean onewayReversed, MapViewPoint p1, double nx, double ny, double dist,
            double onewaySize, Path2D onewayPath) {
        // scale such that border is 1 px
        final double fac = -(onewayReversed ? -1 : 1) * onewaySize * (1 + sinPHI) / (sinPHI * cosPHI);
        final double sx = nx * fac;
        final double sy = ny * fac;

        // Attach the triangle at the incenter and not at the tip.
        // Makes the border even at all sides.
        final double x = p1.getInViewX() + nx * (dist + (onewayReversed ? -1 : 1) * (onewaySize / sinPHI));
        final double y = p1.getInViewY() + ny * (dist + (onewayReversed ? -1 : 1) * (onewaySize / sinPHI));

        onewayPath.moveTo(x, y);
        onewayPath.lineTo(x + cosPHI * sx - sinPHI * sy, y + sinPHI * sx + cosPHI * sy);
        onewayPath.lineTo(x + cosPHI * sx + sinPHI * sy, y - sinPHI * sx + cosPHI * sy);
        onewayPath.lineTo(x, y);
    }

    /**
     * Gets the "circum". This is the distance on the map in meters that 100 screen pixels represent.
     * @return The "circum"
     */
    public double getCircum() {
        return circum;
    }

    @Override
    public void getColors() {
        super.getColors();
        this.highlightColorTransparent = new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 100);
        this.backgroundColor = PaintColors.getBackgroundColor();
    }

    @Override
    public void getSettings(boolean virtual) {
        super.getSettings(virtual);
        paintSettings = MapPaintSettings.INSTANCE;

        circum = nc.getDist100Pixel();
        scale = nc.getScale();

        leftHandTraffic = Main.pref.getBoolean("mappaint.lefthandtraffic", false);

        useStrokes = paintSettings.getUseStrokesDistance() > circum;
        showNames = paintSettings.getShowNamesDistance() > circum;
        showIcons = paintSettings.getShowIconsDistance() > circum;
        isOutlineOnly = paintSettings.isOutlineOnly();
        orderFont = new Font(Main.pref.get("mappaint.font", "Droid Sans"), Font.PLAIN, Main.pref.getInteger("mappaint.fontsize", 8));

        antialiasing = Main.pref.getBoolean("mappaint.use-antialiasing", true) ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);

        Object textAntialiasing;
        switch (Main.pref.get("mappaint.text-antialiasing", "default")) {
            case "on":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_ON;
                break;
            case "off":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_OFF;
                break;
            case "gasp":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_GASP;
                break;
            case "lcd-hrgb":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
                break;
            case "lcd-hbgr":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HBGR;
                break;
            case "lcd-vrgb":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VRGB;
                break;
            case "lcd-vbgr":
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_VBGR;
                break;
            default:
                textAntialiasing = RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, textAntialiasing);

        highlightLineWidth = Main.pref.getInteger("mappaint.highlight.width", 4);
        highlightPointRadius = Main.pref.getInteger("mappaint.highlight.radius", 7);
        widerHighlight = Main.pref.getInteger("mappaint.highlight.bigger-increment", 5);
        highlightStep = Main.pref.getInteger("mappaint.highlight.step", 4);
    }

    private static Path2D.Double getPath(Way w) {
        Path2D.Double path = new Path2D.Double();
        boolean initial = true;
        for (Node n : w.getNodes()) {
            EastNorth p = n.getEastNorth();
            if (p != null) {
                if (initial) {
                    path.moveTo(p.getX(), p.getY());
                    initial = false;
                } else {
                    path.lineTo(p.getX(), p.getY());
                }
            }
        }
        if (w.isClosed()) {
            path.closePath();
        }
        return path;
    }

    private static Path2D.Double getPFClip(Way w, double extent) {
        Path2D.Double clip = new Path2D.Double();
        buildPFClip(clip, w.getNodes(), extent);
        return clip;
    }

    private static Path2D.Double getPFClip(PolyData pd, double extent) {
        Path2D.Double clip = new Path2D.Double();
        clip.setWindingRule(Path2D.WIND_EVEN_ODD);
        buildPFClip(clip, pd.getNodes(), extent);
        for (PolyData pdInner : pd.getInners()) {
            buildPFClip(clip, pdInner.getNodes(), extent);
        }
        return clip;
    }

    /**
     * Fix the clipping area of unclosed polygons for partial fill.
     *
     * The current algorithm for partial fill simply strokes the polygon with a
     * large stroke width after masking the outside with a clipping area.
     * This works, but for unclosed polygons, the mask can crop the corners at
     * both ends (see #12104).
     *
     * This method fixes the clipping area by sort of adding the corners to the
     * clip outline.
     *
     * @param clip the clipping area to modify (initially empty)
     * @param nodes nodes of the polygon
     * @param extent the extent
     */
    private static void buildPFClip(Path2D.Double clip, List<Node> nodes, double extent) {
        boolean initial = true;
        for (Node n : nodes) {
            EastNorth p = n.getEastNorth();
            if (p != null) {
                if (initial) {
                    clip.moveTo(p.getX(), p.getY());
                    initial = false;
                } else {
                    clip.lineTo(p.getX(), p.getY());
                }
            }
        }
        if (nodes.size() >= 3) {
            EastNorth fst = nodes.get(0).getEastNorth();
            EastNorth snd = nodes.get(1).getEastNorth();
            EastNorth lst = nodes.get(nodes.size() - 1).getEastNorth();
            EastNorth lbo = nodes.get(nodes.size() - 2).getEastNorth();

            EastNorth cLst = getPFDisplacedEndPoint(lbo, lst, fst, extent);
            EastNorth cFst = getPFDisplacedEndPoint(snd, fst, cLst != null ? cLst : lst, extent);
            if (cLst == null && cFst != null) {
                cLst = getPFDisplacedEndPoint(lbo, lst, cFst, extent);
            }
            if (cLst != null) {
                clip.lineTo(cLst.getX(), cLst.getY());
            }
            if (cFst != null) {
                clip.lineTo(cFst.getX(), cFst.getY());
            }
        }
    }

    /**
     * Get the point to add to the clipping area for partial fill of unclosed polygons.
     *
     * <code>(p1,p2)</code> is the first or last way segment and <code>p3</code> the
     * opposite endpoint.
     *
     * @param p1 1st point
     * @param p2 2nd point
     * @param p3 3rd point
     * @param extent the extent
     * @return a point q, such that p1,p2,q form a right angle
     * and the distance of q to p2 is <code>extent</code>. The point q lies on
     * the same side of the line p1,p2 as the point p3.
     * Returns null if p1,p2,p3 forms an angle greater 90 degrees. (In this case
     * the corner of the partial fill would not be cut off by the mask, so an
     * additional point is not necessary.)
     */
    private static EastNorth getPFDisplacedEndPoint(EastNorth p1, EastNorth p2, EastNorth p3, double extent) {
        double dx1 = p2.getX() - p1.getX();
        double dy1 = p2.getY() - p1.getY();
        double dx2 = p3.getX() - p2.getX();
        double dy2 = p3.getY() - p2.getY();
        if (dx1 * dx2 + dy1 * dy2 < 0) {
            double len = Math.sqrt(dx1 * dx1 + dy1 * dy1);
            if (len == 0) return null;
            double dxm = -dy1 * extent / len;
            double dym = dx1 * extent / len;
            if (dx1 * dy2 - dx2 * dy1 < 0) {
                dxm = -dxm;
                dym = -dym;
            }
            return new EastNorth(p2.getX() + dxm, p2.getY() + dym);
        }
        return null;
    }

    /**
     * Test if the area is visible
     * @param area The area, interpreted in east/north space.
     * @return true if it is visible.
     */
    private boolean isAreaVisible(Path2D.Double area) {
        Rectangle2D bounds = area.getBounds2D();
        if (bounds.isEmpty()) return false;
        MapViewPoint p = mapState.getPointFor(new EastNorth(bounds.getX(), bounds.getY()));
        if (p.getInViewX() > mapState.getViewWidth()) return false;
        if (p.getInViewY() < 0) return false;
        p = mapState.getPointFor(new EastNorth(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight()));
        if (p.getInViewX() < 0) return false;
        if (p.getInViewY() > mapState.getViewHeight()) return false;
        return true;
    }

    public boolean isInactiveMode() {
        return isInactiveMode;
    }

    public boolean isShowIcons() {
        return showIcons;
    }

    public boolean isShowNames() {
        return showNames;
    }

    private static double[] pointAt(double t, List<MapViewPoint> poly, double pathLength) {
        double totalLen = t * pathLength;
        double curLen = 0;
        double dx, dy;
        double segLen;

        // Yes, it is inefficient to iterate from the beginning for each glyph.
        // Can be optimized if it turns out to be slow.
        for (int i = 1; i < poly.size(); ++i) {
            dx = poly.get(i).getInViewX() - poly.get(i - 1).getInViewX();
            dy = poly.get(i).getInViewY() - poly.get(i - 1).getInViewY();
            segLen = Math.sqrt(dx*dx + dy*dy);
            if (totalLen > curLen + segLen) {
                curLen += segLen;
                continue;
            }
            return new double[] {
                    poly.get(i - 1).getInViewX() + (totalLen - curLen) / segLen * dx,
                    poly.get(i - 1).getInViewY() + (totalLen - curLen) / segLen * dy,
                    Math.atan2(dy, dx)};
        }
        return null;
    }

    /**
     * Computes the flags for a given OSM primitive.
     * @param primitive The primititve to compute the flags for.
     * @param checkOuterMember <code>true</code> if we should also add {@link #FLAG_OUTERMEMBER_OF_SELECTED}
     * @return The flag.
     */
    public static int computeFlags(OsmPrimitive primitive, boolean checkOuterMember) {
        if (primitive.isDisabled()) {
            return FLAG_DISABLED;
        } else if (primitive.isSelected()) {
            return FLAG_SELECTED;
        } else if (checkOuterMember && primitive.isOuterMemberOfSelected()) {
            return FLAG_OUTERMEMBER_OF_SELECTED;
        } else if (primitive.isMemberOfSelected()) {
            return FLAG_MEMBER_OF_SELECTED;
        } else {
            return FLAG_NORMAL;
        }
    }

    private class ComputeStyleListWorker extends RecursiveTask<List<StyleRecord>> implements Visitor {
        private final transient List<? extends OsmPrimitive> input;
        private final transient List<StyleRecord> output;

        private final transient ElemStyles styles = MapPaintStyles.getStyles();
        private final int directExecutionTaskSize;

        private final boolean drawArea = circum <= Main.pref.getInteger("mappaint.fillareas", 10_000_000);
        private final boolean drawMultipolygon = drawArea && Main.pref.getBoolean("mappaint.multipolygon", true);
        private final boolean drawRestriction = Main.pref.getBoolean("mappaint.restriction", true);

        /**
         * Constructs a new {@code ComputeStyleListWorker}.
         * @param input the primitives to process
         * @param output the list of styles to which styles will be added
         * @param directExecutionTaskSize the threshold deciding whether to subdivide the tasks
         */
        ComputeStyleListWorker(final List<? extends OsmPrimitive> input, List<StyleRecord> output, int directExecutionTaskSize) {
            this.input = input;
            this.output = output;
            this.directExecutionTaskSize = directExecutionTaskSize;
            this.styles.setDrawMultipolygon(drawMultipolygon);
        }

        @Override
        protected List<StyleRecord> compute() {
            if (input.size() <= directExecutionTaskSize) {
                return computeDirectly();
            } else {
                final Collection<ForkJoinTask<List<StyleRecord>>> tasks = new ArrayList<>();
                for (int fromIndex = 0; fromIndex < input.size(); fromIndex += directExecutionTaskSize) {
                    final int toIndex = Math.min(fromIndex + directExecutionTaskSize, input.size());
                    final List<StyleRecord> output = new ArrayList<>(directExecutionTaskSize);
                    tasks.add(new ComputeStyleListWorker(input.subList(fromIndex, toIndex), output, directExecutionTaskSize).fork());
                }
                for (ForkJoinTask<List<StyleRecord>> task : tasks) {
                    output.addAll(task.join());
                }
                return output;
            }
        }

        public List<StyleRecord> computeDirectly() {
            MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().lock();
            try {
                for (final OsmPrimitive osm : input) {
                    acceptDrawable(osm);
                }
                return output;
            } catch (RuntimeException e) {
                throw BugReport.intercept(e).put("input-size", input.size()).put("output-size", output.size());
            } finally {
                MapCSSStyleSource.STYLE_SOURCE_LOCK.readLock().unlock();
            }
        }

        private void acceptDrawable(final OsmPrimitive osm) {
            try {
                if (osm.isDrawable()) {
                    osm.accept(this);
                }
            } catch (RuntimeException e) {
                throw BugReport.intercept(e).put("osm", osm);
            }
        }

        @Override
        public void visit(Node n) {
            add(n, computeFlags(n, false));
        }

        @Override
        public void visit(Way w) {
            add(w, computeFlags(w, true));
        }

        @Override
        public void visit(Relation r) {
            add(r, computeFlags(r, true));
        }

        @Override
        public void visit(Changeset cs) {
            throw new UnsupportedOperationException();
        }

        public void add(Node osm, int flags) {
            StyleElementList sl = styles.get(osm, circum, nc);
            for (StyleElement s : sl) {
                output.add(new StyleRecord(s, osm, flags));
            }
        }

        public void add(Relation osm, int flags) {
            StyleElementList sl = styles.get(osm, circum, nc);
            for (StyleElement s : sl) {
                if (drawMultipolygon && drawArea && s instanceof AreaElement && (flags & FLAG_DISABLED) == 0) {
                    output.add(new StyleRecord(s, osm, flags));
                } else if (drawRestriction && s instanceof NodeElement) {
                    output.add(new StyleRecord(s, osm, flags));
                }
            }
        }

        public void add(Way osm, int flags) {
            StyleElementList sl = styles.get(osm, circum, nc);
            for (StyleElement s : sl) {
                if (!(drawArea && (flags & FLAG_DISABLED) == 0) && s instanceof AreaElement) {
                    continue;
                }
                output.add(new StyleRecord(s, osm, flags));
            }
        }
    }

    /**
     * Sets the factory that creates the benchmark data receivers.
     * @param benchmarkFactory The factory.
     * @since 10697
     */
    public void setBenchmarkFactory(Supplier<RenderBenchmarkCollector> benchmarkFactory) {
        this.benchmarkFactory = benchmarkFactory;
    }

    @Override
    public void render(final DataSet data, boolean renderVirtualNodes, Bounds bounds) {
        RenderBenchmarkCollector benchmark = benchmarkFactory.get();
        BBox bbox = bounds.toBBox();
        getSettings(renderVirtualNodes);

        data.getReadLock().lock();
        try {
            highlightWaySegments = data.getHighlightedWaySegments();

            benchmark.renderStart(circum);

            List<Node> nodes = data.searchNodes(bbox);
            List<Way> ways = data.searchWays(bbox);
            List<Relation> relations = data.searchRelations(bbox);

            final List<StyleRecord> allStyleElems = new ArrayList<>(nodes.size()+ways.size()+relations.size());

            // Need to process all relations first.
            // Reason: Make sure, ElemStyles.getStyleCacheWithRange is
            // not called for the same primitive in parallel threads.
            // (Could be synchronized, but try to avoid this for
            // performance reasons.)
            THREAD_POOL.invoke(new ComputeStyleListWorker(relations, allStyleElems,
                    Math.max(20, relations.size() / THREAD_POOL.getParallelism() / 3)));
            THREAD_POOL.invoke(new ComputeStyleListWorker(new CompositeList<>(nodes, ways), allStyleElems,
                    Math.max(100, (nodes.size() + ways.size()) / THREAD_POOL.getParallelism() / 3)));

            if (!benchmark.renderSort()) {
                return;
            }

            Collections.sort(allStyleElems); // TODO: try parallel sort when switching to Java 8

            if (!benchmark.renderDraw(allStyleElems)) {
                return;
            }

            for (StyleRecord record : allStyleElems) {
                paintRecord(record);
            }

            drawVirtualNodes(data, bbox);

            benchmark.renderDone();
        } catch (RuntimeException e) {
            throw BugReport.intercept(e)
                    .put("data", data)
                    .put("circum", circum)
                    .put("scale", scale)
                    .put("paintSettings", paintSettings)
                    .put("renderVirtualNodes", renderVirtualNodes);
        } finally {
            data.getReadLock().unlock();
        }
    }

    private void paintRecord(StyleRecord record) {
        try {
            record.paintPrimitive(paintSettings, this);
        } catch (RuntimeException e) {
            throw BugReport.intercept(e).put("record", record);
        }
    }
}
