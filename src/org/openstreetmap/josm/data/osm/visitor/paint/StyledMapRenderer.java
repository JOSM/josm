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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.FocusManager;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.PolyData;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.data.preferences.AbstractProperty;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;
import org.openstreetmap.josm.gui.NavigatableComponent;
import org.openstreetmap.josm.gui.draw.MapViewPath;
import org.openstreetmap.josm.gui.draw.MapViewPositionAndRotation;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.HorizontalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.BoxTextElement.VerticalTextAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.MapImage;
import org.openstreetmap.josm.gui.mappaint.styleelement.NodeElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.RepeatImageElement.LineImageAlignment;
import org.openstreetmap.josm.gui.mappaint.styleelement.StyleElement;
import org.openstreetmap.josm.gui.mappaint.styleelement.Symbol;
import org.openstreetmap.josm.gui.mappaint.styleelement.TextLabel;
import org.openstreetmap.josm.gui.mappaint.styleelement.placement.PositionForAreaStrategy;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CompositeList;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Geometry.AreaAndPerimeter;
import org.openstreetmap.josm.tools.HiDPISupport;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
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
     * This stores a style and a primitive that should be painted with that style.
     */
    public static class StyleRecord implements Comparable<StyleRecord> {
        private final StyleElement style;
        private final OsmPrimitive osm;
        private final int flags;
        private final long order;

        StyleRecord(StyleElement style, OsmPrimitive osm, int flags) {
            this.style = style;
            this.osm = osm;
            this.flags = flags;

            long order = 0;
            if ((this.flags & FLAG_DISABLED) == 0) {
                order |= 1;
            }

            order <<= 24;
            order |= floatToFixed(this.style.majorZIndex, 24);

            // selected on top of member of selected on top of unselected
            // FLAG_DISABLED bit is the same at this point, but we simply ignore it
            order <<= 4;
            order |= this.flags & 0xf;

            order <<= 24;
            order |= floatToFixed(this.style.zIndex, 24);

            order <<= 1;
            // simple node on top of icons and shapes
            if (NodeElement.SIMPLE_NODE_ELEMSTYLE.equals(this.style)) {
                order |= 1;
            }

            this.order = order;
        }

        /**
         * Converts a float to a fixed point decimal so that the order stays the same.
         *
         * @param number The float to convert
         * @param totalBits
         *            Total number of bits. 1 sign bit. There should be at least 15 bits.
         * @return The float converted to an integer.
         */
        protected static long floatToFixed(float number, int totalBits) {
            long value = Float.floatToIntBits(number) & 0xffffffffL;

            boolean negative = (value & 0x80000000L) != 0;
            // Invert the sign bit, so that negative numbers are lower
            value ^= 0x80000000L;
            // Now do the shift. Do it before accounting for negative numbers (symetry)
            if (totalBits < 32) {
                value >>= (32 - totalBits);
            }
            // positive numbers are sorted now. Negative ones the wrong way.
            if (negative) {
                // Negative number: re-map it
                value = (1L << (totalBits - 1)) - value;
            }
            return value;
        }

        @Override
        public int compareTo(StyleRecord other) {
            int d = Long.compare(order, other.order);
            if (d != 0) {
                return d;
            }

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

    private static final Map<Font, Boolean> IS_GLYPH_VECTOR_DOUBLE_TRANSLATION_BUG = new HashMap<>();

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
        String overridePref = Config.getPref().get("glyph-bug", "auto");
        if ("auto".equals(overridePref)) {
            FontRenderContext frc = new FontRenderContext(null, false, false);
            GlyphVector gv = font.createGlyphVector(frc, "x");
            gv.setGlyphTransform(0, AffineTransform.getTranslateInstance(1000, 1000));
            Shape shape = gv.getGlyphOutline(0);
            if (Logging.isTraceEnabled()) {
                Logging.trace("#10446: shape: {0}", shape.getBounds());
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
    static final int FLAG_NORMAL = 0;
    /**
     * A primitive with {@link OsmPrimitive#isDisabled()}
     */
    static final int FLAG_DISABLED = 1;
    /**
     * A primitive with {@link OsmPrimitive#isMemberOfSelected()}
     */
    static final int FLAG_MEMBER_OF_SELECTED = 2;
    /**
     * A primitive with {@link OsmPrimitive#isSelected()}
     */
    static final int FLAG_SELECTED = 4;
    /**
     * A primitive with {@link OsmPrimitive#isOuterMemberOfSelected()}
     */
    static final int FLAG_OUTERMEMBER_OF_SELECTED = 8;

    private static final double PHI = Utils.toRadians(20);
    private static final double cosPHI = Math.cos(PHI);
    private static final double sinPHI = Math.sin(PHI);
    /**
     * If we should use left hand traffic.
     */
    private static final AbstractProperty<Boolean> PREFERENCE_LEFT_HAND_TRAFFIC
            = new BooleanProperty("mappaint.lefthandtraffic", false).cached();
    /**
     * Indicates that the renderer should enable anti-aliasing
     * @since 11758
     */
    public static final AbstractProperty<Boolean> PREFERENCE_ANTIALIASING_USE
            = new BooleanProperty("mappaint.use-antialiasing", true).cached();
    /**
     * The mode that is used for anti-aliasing
     * @since 11758
     */
    public static final AbstractProperty<String> PREFERENCE_TEXT_ANTIALIASING
            = new StringProperty("mappaint.text-antialiasing", "default").cached();

    /**
     * The line with to use for highlighting
     */
    private static final AbstractProperty<Integer> HIGHLIGHT_LINE_WIDTH = new IntegerProperty("mappaint.highlight.width", 4).cached();
    private static final AbstractProperty<Integer> HIGHLIGHT_POINT_RADIUS = new IntegerProperty("mappaint.highlight.radius", 7).cached();
    private static final AbstractProperty<Integer> WIDER_HIGHLIGHT = new IntegerProperty("mappaint.highlight.bigger-increment", 5).cached();
    private static final AbstractProperty<Integer> HIGHLIGHT_STEP = new IntegerProperty("mappaint.highlight.step", 4).cached();

    private Collection<WaySegment> highlightWaySegments;

    //flag that activate wider highlight mode
    private boolean useWiderHighlight;

    private boolean useStrokes;
    private boolean showNames;
    private boolean showIcons;
    private boolean isOutlineOnly;

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
        Component focusOwner = FocusManager.getCurrentManager().getFocusOwner();
        useWiderHighlight = !(focusOwner instanceof AbstractButton || focusOwner == nc);
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
     * Worker function for drawing areas.
     *
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
     */
    protected void drawArea(MapViewPath path, Color color,
            MapImage fillImage, Float extent, Path2D.Double pfClip, boolean disabled) {
        if (!isOutlineOnly && color.getAlpha() != 0) {
            Shape area = path;
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
                Image img = fillImage.getImage(disabled);
                // TexturePaint requires BufferedImage -> get base image from
                // possible multi-resolution image
                img = HiDPISupport.getBaseImage(img);
                TexturePaint texture = new TexturePaint((BufferedImage) img,
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
     * @param text Ignored. Use {@link #drawText} instead.
     * @deprecated use {@link #drawArea(Relation r, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled)}
     */
    @Deprecated
    public void drawArea(Relation r, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled, TextLabel text) {
        drawArea(r, color, fillImage, extent, extentThreshold, disabled);
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
     * @since 12285
     */
    public void drawArea(Relation r, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled) {
        Multipolygon multipolygon = MultipolygonCache.getInstance().get(r);
        if (!r.isDisabled() && !multipolygon.getOuterWays().isEmpty()) {
            for (PolyData pd : multipolygon.getCombinedPolygons()) {
                if (!isAreaVisible(pd.get())) {
                    continue;
                }
                MapViewPath p = new MapViewPath(mapState);
                p.appendFromEastNorth(pd.get());
                p.setWindingRule(Path2D.WIND_EVEN_ODD);
                Path2D.Double pfClip = null;
                if (extent != null) {
                    if (!usePartialFill(pd.getAreaAndPerimeter(null), extent, extentThreshold)) {
                        extent = null;
                    } else if (!pd.isClosed()) {
                        pfClip = getPFClip(pd, extent * scale);
                    }
                }
                drawArea(p,
                        pd.isSelected() ? paintSettings.getRelationSelectedColor(color.getAlpha()) : color,
                        fillImage, extent, pfClip, disabled);
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
     * @param text Ignored. Use {@link #drawText} instead.
     * @deprecated use {@link #drawArea(Way w, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled)}
     */
    @Deprecated
    public void drawArea(Way w, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled, TextLabel text) {
        drawArea(w, color, fillImage, extent, extentThreshold, disabled);
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
     * @since 12285
     */
    public void drawArea(Way w, Color color, MapImage fillImage, Float extent, Float extentThreshold, boolean disabled) {
        Path2D.Double pfClip = null;
        if (extent != null) {
            if (!usePartialFill(Geometry.getAreaAndPerimeter(w.getNodes()), extent, extentThreshold)) {
                extent = null;
            } else if (!w.isClosed()) {
                pfClip = getPFClip(w, extent * scale);
            }
        }
        drawArea(getPath(w), color, fillImage, extent, pfClip, disabled);
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

    /**
     * Draw a text onto a node
     * @param n The node to draw the text on
     * @param bs The text and it's alignment.
     */
    public void drawBoxText(Node n, BoxTextElement bs) {
        if (!isShowNames() || bs == null)
            return;

        MapViewPoint p = mapState.getPointFor(n);
        TextLabel text = bs.text;
        String s = text.labelCompositionStrategy.compose(n);
        if (s == null || s.isEmpty()) return;

        Font defaultFont = g.getFont();
        g.setFont(text.font);

        FontRenderContext frc = g.getFontRenderContext();
        Rectangle2D bounds = text.font.getStringBounds(s, frc);

        double x = Math.round(p.getInViewX()) + bs.xOffset + bounds.getCenterX();
        double y = Math.round(p.getInViewY()) + bs.yOffset + bounds.getCenterY();
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
            LineMetrics metrics = text.font.getLineMetrics(s, frc);
            if (bs.vAlign == VerticalTextAlignment.ABOVE) {
                y -= -box.y + (int) metrics.getDescent();
            } else if (bs.vAlign == VerticalTextAlignment.TOP) {
                y -= -box.y - (int) metrics.getAscent();
            } else if (bs.vAlign == VerticalTextAlignment.CENTER) {
                y += (int) ((metrics.getAscent() - metrics.getDescent()) / 2);
            } else if (bs.vAlign == VerticalTextAlignment.BELOW) {
                y += box.y + box.height + (int) metrics.getAscent() + 2;
            } else throw new AssertionError();
        }

        displayText(n, text, s, bounds, new MapViewPositionAndRotation(mapState.getForView(x, y), 0));
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

        OffsetIterator it = new OffsetIterator(mapState, way.getNodes(), offset);
        MapViewPath path = new MapViewPath(mapState);
        if (it.hasNext()) {
            path.moveTo(it.next());
        }
        while (it.hasNext()) {
            path.lineTo(it.next());
        }

        double startOffset = computeStartOffset(phase, repeat);

        Image image = pattern.getImage(disabled);

        path.visitClippedLine(repeat, (inLineOffset, start, end, startIsOldEnd) -> {
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
            // It is shifted by startOffset.
            double imageStart = -((inLineOffset - startOffset + repeat) % repeat);

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

    private static double computeStartOffset(double phase, final double repeat) {
        double startOffset = phase % repeat;
        if (startOffset < 0) {
            startOffset += repeat;
        }
        return startOffset;
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

        drawIcon(p, img, disabled, selected, member, theta, (g, r) -> {
            Color color = getSelectionHintColor(disabled, selected);
            g.setColor(color);
            g.draw(r);
        });
    }


    /**
     * Draw the icon for a given area. Normally, the icon is drawn around the center of the area.
     * @param osm The primitive to draw the icon for
     * @param img The icon to draw
     * @param disabled {@code} true to render disabled version, {@code false} for the standard version
     * @param selected {@code} true to render it as selected, {@code false} otherwise
     * @param member {@code} true to render it as a relation member, {@code false} otherwise
     * @param theta the angle of rotation in radians
     * @param iconPosition Where to place the icon.
     * @since 11670
     */
    public void drawAreaIcon(OsmPrimitive osm, MapImage img, boolean disabled, boolean selected, boolean member, double theta,
            PositionForAreaStrategy iconPosition) {
        Rectangle2D.Double iconRect = new Rectangle2D.Double(-img.getWidth() / 2.0, -img.getHeight() / 2.0, img.getWidth(), img.getHeight());

        forEachPolygon(osm, path -> {
            MapViewPositionAndRotation placement = iconPosition.findLabelPlacement(path, iconRect);
            if (placement == null) {
                return;
            }
            MapViewPoint p = placement.getPoint();
            drawIcon(p, img, disabled, selected, member, theta + placement.getRotation(), (g, r) -> {
                if (useStrokes) {
                    g.setStroke(new BasicStroke(2));
                }
                // only draw a minor highlighting, so that users do not confuse this for a point.
                Color color = getSelectionHintColor(disabled, selected);
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * .2));
                g.setColor(color);
                g.draw(r);
            });
        });
    }

    private void drawIcon(MapViewPoint p, MapImage img, boolean disabled, boolean selected, boolean member, double theta,
            BiConsumer<Graphics2D, Rectangle2D> selectionDrawer) {
        float alpha = img.getAlphaFloat();

        Graphics2D temporaryGraphics = (Graphics2D) g.create();
        if (!Utils.equalsEpsilon(alpha, 1f)) {
            temporaryGraphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }

        double x = Math.round(p.getInViewX());
        double y = Math.round(p.getInViewY());
        temporaryGraphics.translate(x, y);
        temporaryGraphics.rotate(theta);
        int drawX = -img.getWidth() / 2 + img.offsetX;
        int drawY = -img.getHeight() / 2 + img.offsetY;
        temporaryGraphics.drawImage(img.getImage(disabled), drawX, drawY, nc);
        if (selected || member) {
            selectionDrawer.accept(temporaryGraphics, new Rectangle2D.Double(drawX - 2, drawY - 2, img.getWidth() + 4, img.getHeight() + 4));
        }
    }

    private Color getSelectionHintColor(boolean disabled, boolean selected) {
        Color color;
        if (disabled) {
            color = inactiveColor;
        } else if (selected) {
            color = selectedColor;
        } else {
            color = relationSelectedColor;
        }
        return color;
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
     * style. Width of the highlight can be changed by user preferences
     * @param path path to draw
     * @param line line style
     */
    private void drawPathHighlight(MapViewPath path, BasicStroke line) {
        if (path == null)
            return;
        g.setColor(highlightColorTransparent);
        float w = line.getLineWidth() + HIGHLIGHT_LINE_WIDTH.get();
        if (useWiderHighlight) {
            w += WIDER_HIGHLIGHT.get();
        }
        int step = Math.max(HIGHLIGHT_STEP.get(), 1);
        while (w >= line.getLineWidth()) {
            g.setStroke(new BasicStroke(w, line.getEndCap(), line.getLineJoin(), line.getMiterLimit()));
            g.draw(path);
            w -= step;
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
        int s = size + HIGHLIGHT_POINT_RADIUS.get();
        if (useWiderHighlight) {
            s += WIDER_HIGHLIGHT.get();
        }
        int step = Math.max(HIGHLIGHT_STEP.get(), 1);
        while (s >= size) {
            int r = (int) Math.floor(s/2d);
            g.fill(new RoundRectangle2D.Double(p.getX()-r, p.getY()-r, s, s, r, r));
            s -= step;
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

    /**
     * Draw a turn restriction
     * @param r The turn restriction relation
     * @param icon The icon to draw at the turn point
     * @param disabled draw using disabled style
     */
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
                    if (via == null && "via".equals(m.getRole())) {
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
                    onewayvia = Optional.ofNullable(OsmUtils.getOsmBoolean(onewayviastr)).orElse(Boolean.FALSE);
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
        double fromAngleDeg = Utils.toDegrees(fromAngle);

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
                vx2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg - 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg + 90));
            }
            iconAngle = 270+fromAngleDeg;
        }
        if (pFrom.x < pVia.x && pFrom.y >= pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg));
            } else {
                vx2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg + 180));
            }
            iconAngle = 90-fromAngleDeg;
        }
        if (pFrom.x < pVia.x && pFrom.y < pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg + 90));
                vy2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg + 90));
            } else {
                vx2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg - 90));
                vy2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg - 90));
            }
            iconAngle = 90+fromAngleDeg;
        }
        if (pFrom.x >= pVia.x && pFrom.y < pVia.y) {
            if (!leftHandTraffic) {
                vx2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg + 180));
                vy2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg + 180));
            } else {
                vx2 = distanceFromWay * Math.sin(Utils.toRadians(fromAngleDeg));
                vy2 = distanceFromWay * Math.cos(Utils.toRadians(fromAngleDeg));
            }
            iconAngle = 270-fromAngleDeg;
        }

        drawRestriction(icon.getImage(disabled),
                pVia, vx, vx2, vy, vy2, iconAngle, r.isSelected());
    }

    /**
     * Draws a text for the given primitive
     * @param osm The primitive to draw the text for
     * @param text The text definition (font/position/.../text content) to draw
     * @param labelPositionStrategy The position of the text
     * @since 11722
     */
    public void drawText(OsmPrimitive osm, TextLabel text, PositionForAreaStrategy labelPositionStrategy) {
        if (!isShowNames()) {
            return;
        }
        String name = text.getString(osm);
        if (name == null || name.isEmpty()) {
            return;
        }

        FontMetrics fontMetrics = g.getFontMetrics(text.font); // if slow, use cache
        Rectangle2D nb = fontMetrics.getStringBounds(name, g); // if slow, approximate by strlen()*maxcharbounds(font)

        Font defaultFont = g.getFont();
        forEachPolygon(osm, path -> {
            //TODO: Ignore areas that are out of bounds.
            PositionForAreaStrategy position = labelPositionStrategy;
            MapViewPositionAndRotation center = position.findLabelPlacement(path, nb);
            if (center != null) {
                displayText(osm, text, name, nb, center);
            } else if (position.supportsGlyphVector()) {
                List<GlyphVector> gvs = Utils.getGlyphVectorsBidi(name, text.font, g.getFontRenderContext());

                List<GlyphVector> translatedGvs = position.generateGlyphVectors(path, nb, gvs, isGlyphVectorDoubleTranslationBug(text.font));
                displayText(() -> translatedGvs.forEach(gv -> g.drawGlyphVector(gv, 0, 0)),
                        () -> translatedGvs.stream().collect(
                                Path2D.Double::new,
                                (p, gv) -> p.append(gv.getOutline(0, 0), false),
                                (p1, p2) -> p1.append(p2, false)),
                        osm.isDisabled(), text);
            } else {
                Logging.trace("Couldn't find a correct label placement for {0} / {1}", osm, name);
            }
        });
        g.setFont(defaultFont);
    }

    private void displayText(OsmPrimitive osm, TextLabel text, String name, Rectangle2D nb,
            MapViewPositionAndRotation center) {
        AffineTransform at = new AffineTransform();
        if (Math.abs(center.getRotation()) < .01) {
            // Explicitly no rotation: move to full pixels.
            at.setToTranslation(Math.round(center.getPoint().getInViewX() - nb.getCenterX()),
                    Math.round(center.getPoint().getInViewY() - nb.getCenterY()));
        } else {
            at.setToTranslation(center.getPoint().getInViewX(), center.getPoint().getInViewY());
            at.rotate(center.getRotation());
            at.translate(-nb.getCenterX(), -nb.getCenterY());
        }
        displayText(() -> {
            AffineTransform defaultTransform = g.getTransform();
            g.transform(at);
            g.setFont(text.font);
            g.drawString(name, 0, 0);
            g.setTransform(defaultTransform);
        }, () -> {
            FontRenderContext frc = g.getFontRenderContext();
            TextLayout tl = new TextLayout(name, text.font, frc);
            return tl.getOutline(at);
        }, osm.isDisabled(), text);
    }

    /**
     * Displays text at specified position including its halo, if applicable.
     *
     * @param fill The function that fills the text
     * @param outline The function to draw the outline
     * @param disabled {@code true} if element is disabled (filtered out)
     * @param text text style to use
     */
    private void displayText(Runnable fill, Supplier<Shape> outline, boolean disabled, TextLabel text) {
        if (isInactiveMode || disabled) {
            g.setColor(inactiveColor);
            fill.run();
        } else if (text.haloRadius != null) {
            g.setStroke(new BasicStroke(2*text.haloRadius, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            g.setColor(text.haloColor);
            Shape textOutline = outline.get();
            g.draw(textOutline);
            g.setStroke(new BasicStroke());
            g.setColor(text.color);
            g.fill(textOutline);
        } else {
            g.setColor(text.color);
            fill.run();
        }
    }

    /**
     * Calls a consumer for each path of the area shape-
     * @param osm A way or a multipolygon
     * @param consumer The consumer to call.
     */
    private void forEachPolygon(OsmPrimitive osm, Consumer<MapViewPath> consumer) {
        if (osm instanceof Way) {
            consumer.accept(getPath((Way) osm));
        } else if (osm instanceof Relation) {
            Multipolygon multipolygon = MultipolygonCache.getInstance().get((Relation) osm);
            if (!multipolygon.getOuterWays().isEmpty()) {
                for (PolyData pd : multipolygon.getCombinedPolygons()) {
                    MapViewPath path = new MapViewPath(mapState);
                    path.appendFromEastNorth(pd.get());
                    path.setWindingRule(MapViewPath.WIND_EVEN_ODD);
                    consumer.accept(path);
                }
            }
        }
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
        Iterator<MapViewPoint> it = new OffsetIterator(mapState, wayNodes, offset);
        boolean initialMoveToNeeded = true;
        ArrowPaintHelper drawArrowHelper = null;
        if (showOrientation) {
            drawArrowHelper = new ArrowPaintHelper(PHI, 10 + line.getLineWidth());
        }
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
                if (drawArrowHelper != null) {
                    boolean drawArrow;
                    // always draw last arrow - no matter how short the segment is
                    drawArrow = !it.hasNext();
                    if (!showHeadArrowOnly) {
                        // draw arrows in between only if there is enough space
                        drawArrow = drawArrow || p1.distanceToInView(p2) > drawArrowHelper.getOnLineLength() * 1.3;
                    }
                    if (drawArrow) {
                        drawArrowHelper.paintArrowAt(orientationArrows, p2, p1);
                    }
                }
            }
            lastPoint = p;
        }
        if (showOneway) {
            onewayArrows = new MapViewPath(mapState);
            onewayArrowsCasing = new MapViewPath(mapState);
            double interval = 60;

            path.visitClippedLine(60, (inLineOffset, start, end, startIsOldEnd) -> {
                double segmentLength = start.distanceToInView(end);
                if (segmentLength > 0.001) {
                    final double nx = (end.getInViewX() - start.getInViewX()) / segmentLength;
                    final double ny = (end.getInViewY() - start.getInViewY()) / segmentLength;

                    // distance from p1
                    double dist = interval - (inLineOffset % interval);

                    while (dist < segmentLength) {
                        appendOnewayPath(onewayReversed, start, nx, ny, dist, 3d, onewayArrowsCasing);
                        appendOnewayPath(onewayReversed, start, nx, ny, dist, 2d, onewayArrows);
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

    private static void appendOnewayPath(boolean onewayReversed, MapViewPoint p1, double nx, double ny, double dist,
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

        leftHandTraffic = PREFERENCE_LEFT_HAND_TRAFFIC.get();

        useStrokes = paintSettings.getUseStrokesDistance() > circum;
        showNames = paintSettings.getShowNamesDistance() > circum;
        showIcons = paintSettings.getShowIconsDistance() > circum;
        isOutlineOnly = paintSettings.isOutlineOnly();

        antialiasing = PREFERENCE_ANTIALIASING_USE.get() ?
                        RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antialiasing);

        Object textAntialiasing;
        switch (PREFERENCE_TEXT_ANTIALIASING.get()) {
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
    }

    private MapViewPath getPath(Way w) {
        MapViewPath path = new MapViewPath(mapState);
        if (w.isClosed()) {
            path.appendClosed(w.getNodes(), false);
        } else {
            path.append(w.getNodes(), false);
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
        if (p.getInViewY() < 0 || p.getInViewX() > mapState.getViewWidth()) return false;
        p = mapState.getPointFor(new EastNorth(bounds.getX() + bounds.getWidth(), bounds.getY() + bounds.getHeight()));
        return p.getInViewX() >= 0 && p.getInViewY() <= mapState.getViewHeight();
    }

    /**
     * Determines if the paint visitor shall render OSM objects such that they look inactive.
     * @return {@code true} if the paint visitor shall render OSM objects such that they look inactive
     */
    public boolean isInactiveMode() {
        return isInactiveMode;
    }

    /**
     * Check if icons should be rendered
     * @return <code>true</code> to display icons
     */
    public boolean isShowIcons() {
        return showIcons;
    }

    /**
     * Test if names should be rendered
     * @return <code>true</code> to display names
     */
    public boolean isShowNames() {
        return showNames;
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

        try {
            if (data.getReadLock().tryLock(1, TimeUnit.SECONDS)) {
                try {
                    paintWithLock(data, renderVirtualNodes, benchmark, bbox);
                } finally {
                    data.getReadLock().unlock();
                }
            } else {
                Logging.warn("Cannot paint layer {0}: It is locked.");
            }
        } catch (InterruptedException e) {
            Logging.warn("Cannot paint layer {0}: Interrupted");
        }
    }

    private void paintWithLock(final DataSet data, boolean renderVirtualNodes, RenderBenchmarkCollector benchmark,
            BBox bbox) {
        try {
            highlightWaySegments = data.getHighlightedWaySegments();

            benchmark.renderStart(circum);

            List<Node> nodes = data.searchNodes(bbox);
            List<Way> ways = data.searchWays(bbox);
            List<Relation> relations = data.searchRelations(bbox);

            final List<StyleRecord> allStyleElems = new ArrayList<>(nodes.size()+ways.size()+relations.size());

            // Need to process all relations first.
            // Reason: Make sure, ElemStyles.getStyleCacheWithRange is not called for the same primitive in parallel threads.
            // (Could be synchronized, but try to avoid this for performance reasons.)
            THREAD_POOL.invoke(new ComputeStyleListWorker(circum, nc, relations, allStyleElems,
                    Math.max(20, relations.size() / THREAD_POOL.getParallelism() / 3)));
            THREAD_POOL.invoke(new ComputeStyleListWorker(circum, nc, new CompositeList<>(nodes, ways), allStyleElems,
                    Math.max(100, (nodes.size() + ways.size()) / THREAD_POOL.getParallelism() / 3)));

            if (!benchmark.renderSort()) {
                return;
            }

            // We use parallel sort here. This is only available for arrays.
            StyleRecord[] sorted = allStyleElems.toArray(new StyleRecord[allStyleElems.size()]);
            Arrays.parallelSort(sorted, null);

            if (!benchmark.renderDraw(allStyleElems)) {
                return;
            }

            for (StyleRecord record : sorted) {
                paintRecord(record);
            }

            drawVirtualNodes(data, bbox);

            benchmark.renderDone();
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException e) {
            throw BugReport.intercept(e)
                    .put("data", data)
                    .put("circum", circum)
                    .put("scale", scale)
                    .put("paintSettings", paintSettings)
                    .put("renderVirtualNodes", renderVirtualNodes);
        }
    }

    private void paintRecord(StyleRecord record) {
        try {
            record.paintPrimitive(paintSettings, this);
        } catch (JosmRuntimeException | IllegalArgumentException | IllegalStateException | NullPointerException e) {
            throw BugReport.intercept(e).put("record", record);
        }
    }
}
