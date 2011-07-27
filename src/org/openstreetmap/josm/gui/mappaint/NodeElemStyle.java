// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Stroke;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

/**
 * applies for Nodes and turn restriction relations
 */
public class NodeElemStyle extends ElemStyle {
    public ImageIcon icon;
    public int iconAlpha;
    public Symbol symbol;
    public NodeTextElement text;

    private ImageIcon disabledIcon;

    public enum SymbolShape { SQUARE, CIRCLE, TRIANGLE, PENTAGON, HEXAGON, HEPTAGON, OCTAGON, NONAGON, DECAGON }
    public enum HorizontalTextAlignment { LEFT, CENTER, RIGHT }
    public enum VerticalTextAlignment { ABOVE, TOP, CENTER, BOTTOM, BELOW }

    public static class Symbol {
        public SymbolShape symbol;
        public int size;
        public Stroke stroke;
        public Color strokeColor;
        public Color fillColor;

        public Symbol(SymbolShape symbol, int size, Stroke stroke, Color strokeColor, Color fillColor) {
            if (stroke != null && strokeColor == null)
                throw new IllegalArgumentException();
            if (stroke == null && fillColor == null)
                throw new IllegalArgumentException();
            this.symbol = symbol;
            this.size = size;
            this.stroke = stroke;
            this.strokeColor = strokeColor;
            this.fillColor = fillColor;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null || getClass() != obj.getClass())
                return false;
            final Symbol other = (Symbol) obj;
            return  symbol == other.symbol &&
                    size == other.size &&
                    equal(stroke, other.stroke) &&
                    equal(strokeColor, other.strokeColor) &&
                    equal(fillColor, other.fillColor);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + symbol.hashCode();
            hash = 67 * hash + size;
            hash = 67 * hash + (stroke != null ? stroke.hashCode() : 0);
            hash = 67 * hash + (strokeColor != null ? strokeColor.hashCode() : 0);
            hash = 67 * hash + (fillColor != null ? fillColor.hashCode() : 0);
            return hash;
        }

        @Override
        public String toString() {
            return "symbol=" + symbol + " size=" + size +
                    (stroke != null ? (" stroke=" + stroke + " strokeColor=" + strokeColor) : "") +
                    (fillColor != null ? (" fillColor=" + fillColor) : "");
        }
    }

    public static class NodeTextElement extends TextElement {
        public HorizontalTextAlignment hAlign;
        public VerticalTextAlignment vAlign;

        public NodeTextElement(TextElement text, HorizontalTextAlignment hAlign, VerticalTextAlignment vAlign) {
            super(text);
            CheckParameterUtil.ensureParameterNotNull(hAlign);
            CheckParameterUtil.ensureParameterNotNull(vAlign);
            this.hAlign = hAlign;
            this.vAlign = vAlign;
        }

        @Override
        public boolean equals(Object obj) {
            if (!super.equals(obj))
                return false;
            if (obj == null || getClass() != obj.getClass())
                return false;
            final NodeTextElement other = (NodeTextElement) obj;
            return hAlign == other.hAlign &&
                    vAlign == other.vAlign;
        }

        @Override
        public int hashCode() {
            int hash = super.hashCode();
            hash = 97 * hash + hAlign.hashCode();
            hash = 97 * hash + vAlign.hashCode();
            return hash;
        }

        @Override
        public String toString() {
            return "NodeTextElement{" + toStringImpl() + '}';
        }

        @Override
        protected String toStringImpl() {
            return super.toStringImpl() + " hAlign=" + hAlign + " vAlign=" + vAlign;
        }
    }

    public static final NodeElemStyle SIMPLE_NODE_ELEMSTYLE;
    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", Keyword.AUTO);
        SIMPLE_NODE_ELEMSTYLE = create(new Environment(null, mc, "default", null), true);
    }

    protected NodeElemStyle(Cascade c, ImageIcon icon, Integer iconAlpha, Symbol symbol, NodeTextElement text) {
        super(c, 1000f);
        this.icon = icon;
        this.iconAlpha = iconAlpha == null ? 0 : iconAlpha;
        this.symbol = symbol;
        this.text = text;
    }

    public static NodeElemStyle create(Environment env) {
        return create(env, false);
    }

    /*
     * Caches the default text color from the preferences.
     *
     * FIXME: the cache isn't updated if the user changes the preference during a JOSM
     * session. There should be preference listener updating this cache.
     */
    static private Color DEFAULT_TEXT_COLOR = null;
    static private void initDefaultParameters() {
        if (DEFAULT_TEXT_COLOR != null) return;
        DEFAULT_TEXT_COLOR = PaintColors.TEXT.get();
    }

    private static NodeElemStyle create(Environment env, boolean allowOnlyText) {
        initDefaultParameters();
        Cascade c = env.mc.getCascade(env.layer);

        Pair<ImageIcon, Integer> icon = createIcon(env);
        Symbol symbol = null;
        if (icon == null) {
            symbol = createSymbol(env);
        }

        NodeTextElement text = null;
        TextElement te = TextElement.create(c, DEFAULT_TEXT_COLOR, symbol == null && icon == null);
        // optimization: if we neither have a symbol, nor an icon, nor a text element
        // we don't have to check for the remaining style properties and we don't
        // have to allocate a node element style.
        if (symbol == null && icon == null && te == null) return null;

        if (te != null) {
            HorizontalTextAlignment hAlign = HorizontalTextAlignment.RIGHT;
            Keyword hAlignKW = c.get("text-anchor-horizontal", Keyword.RIGHT, Keyword.class);
            if (equal(hAlignKW.val, "left")) {
                hAlign = HorizontalTextAlignment.LEFT;
            } else if (equal(hAlignKW.val, "center")) {
                hAlign = HorizontalTextAlignment.CENTER;
            } else if (equal(hAlignKW.val, "right")) {
                hAlign = HorizontalTextAlignment.RIGHT;
            }
            VerticalTextAlignment vAlign = VerticalTextAlignment.BOTTOM;
            String vAlignStr = c.get("text-anchor-vertical", Keyword.BOTTOM, Keyword.class).val;
            if (equal(vAlignStr, "above")) {
                vAlign = VerticalTextAlignment.ABOVE;
            } else if (equal(vAlignStr, "top")) {
                vAlign = VerticalTextAlignment.TOP;
            } else if (equal(vAlignStr, "center")) {
                vAlign = VerticalTextAlignment.CENTER;
            } else if (equal(vAlignStr, "bottom")) {
                vAlign = VerticalTextAlignment.BOTTOM;
            } else if (equal(vAlignStr, "below")) {
                vAlign = VerticalTextAlignment.BELOW;
            }
            text = new NodeTextElement(te, hAlign, vAlign);
        }

        return new NodeElemStyle(c,
                icon == null ? null : icon.a,
                icon == null ? null : icon.b,
                symbol,
                text);
    }

    private static Pair<ImageIcon, Integer> createIcon(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade c_def = env.mc.getCascade("default");

        IconReference iconRef = c.get("icon-image", null, IconReference.class);
        if (iconRef == null)
            return null;

        Float widthOnDefault = c_def.get("icon-width", null, Float.class);
        if (widthOnDefault != null && widthOnDefault <= 0) {
            widthOnDefault = null;
        }
        Float widthF = getWidth(c, "icon-width", widthOnDefault);

        Float heightOnDefault = c_def.get("icon-height", null, Float.class);
        if (heightOnDefault != null && heightOnDefault <= 0) {
            heightOnDefault = null;
        }
        Float heightF = getWidth(c, "icon-height", heightOnDefault);

        int width = widthF == null ? -1 : Math.round(widthF);
        int height = heightF == null ? -1 : Math.round(heightF);

        ImageIcon icon = MapPaintStyles.getIcon(iconRef, width, height, false);
        if (icon == null)
            return new Pair<ImageIcon, Integer>(MapPaintStyles.getNoIcon_Icon(iconRef.source, false), 255);
        int iconAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.icon-image-alpha", 255))));
        Integer pAlpha = Utils.color_float2int(c.get("icon-opacity", null, float.class));
        if (pAlpha != null) {
            iconAlpha = pAlpha;
        }

        return new Pair<ImageIcon, Integer>(icon, iconAlpha);
    }

    private static Symbol createSymbol(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade c_def = env.mc.getCascade("default");

        SymbolShape shape;
        Keyword shapeKW = c.get("symbol-shape", null, Keyword.class);
        if (shapeKW == null)
            return null;
        if (equal(shapeKW.val, "square")) {
            shape = SymbolShape.SQUARE;
        } else if (equal(shapeKW.val, "circle")) {
            shape = SymbolShape.CIRCLE;
        } else if (equal(shapeKW.val, "triangle")) {
            shape = SymbolShape.TRIANGLE;
        } else if (equal(shapeKW.val, "pentagon")) {
            shape = SymbolShape.PENTAGON;
        } else if (equal(shapeKW.val, "hexagon")) {
            shape = SymbolShape.HEXAGON;
        } else if (equal(shapeKW.val, "heptagon")) {
            shape = SymbolShape.HEPTAGON;
        } else if (equal(shapeKW.val, "octagon")) {
            shape = SymbolShape.OCTAGON;
        } else if (equal(shapeKW.val, "nonagon")) {
            shape = SymbolShape.NONAGON;
        } else if (equal(shapeKW.val, "decagon")) {
            shape = SymbolShape.DECAGON;
        } else
            return null;

        Float sizeOnDefault = c_def.get("symbol-size", null, Float.class);
        if (sizeOnDefault != null && sizeOnDefault <= 0) {
            sizeOnDefault = null;
        }
        Float size = getWidth(c, "symbol-size", sizeOnDefault);

        if (size == null) {
            size = 10f;
        }

        if (size <= 0)
            return null;

        Float strokeWidthOnDefault = getWidth(c_def, "symbol-stroke-width", null);
        Float strokeWidth = getWidth(c, "symbol-stroke-width", strokeWidthOnDefault);

        Color strokeColor = c.get("symbol-stroke-color", null, Color.class);

        if (strokeWidth == null && strokeColor != null) {
            strokeWidth = 1f;
        } else if (strokeWidth != null && strokeColor == null) {
            strokeColor = Color.ORANGE;
        }

        Stroke stroke = null;
        if (strokeColor != null) {
            float strokeAlpha = c.get("symbol-stroke-opacity", 1f, Float.class);
            strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(),
                    strokeColor.getBlue(), Utils.color_float2int(strokeAlpha));
            stroke = new BasicStroke(strokeWidth);
        }

        Color fillColor = c.get("symbol-fill-color", null, Color.class);
        if (stroke == null && fillColor == null) {
            fillColor = Color.BLUE;
        }

        if (fillColor != null) {
            float fillAlpha = c.get("symbol-fill-opacity", 1f, Float.class);
            fillColor = new Color(fillColor.getRed(), fillColor.getGreen(),
                    fillColor.getBlue(), Utils.color_float2int(fillAlpha));
        }

        return new Symbol(shape, Math.round(size), stroke, strokeColor, fillColor);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter, boolean selected, boolean member) {
        if (primitive instanceof Node) {
            Node n = (Node) primitive;
            if (icon != null && painter.isShowIcons()) {
                painter.drawNodeIcon(n, (painter.isInactiveMode() || n.isDisabled()) ? getDisabledIcon() : icon,
                        Utils.color_int2float(iconAlpha), selected, member, text);
            } else if (symbol != null) {
                Color fillColor = symbol.fillColor;
                if (fillColor != null) {
                    if (n.isHighlighted()) {
                        fillColor = settings.getHighlightColor();
                    } else {
                        if (painter.isInactiveMode() || n.isDisabled()) {
                            fillColor = settings.getInactiveColor();
                        } else if (selected) {
                            fillColor = settings.getSelectedColor(fillColor.getAlpha());
                        } else if (member) {
                            fillColor = settings.getRelationSelectedColor(fillColor.getAlpha());
                        }
                    }
                }
                Color strokeColor = symbol.strokeColor;
                if (strokeColor != null) {
                    if (n.isHighlighted()) {
                        strokeColor = settings.getHighlightColor();
                    } else {
                        if (painter.isInactiveMode() || n.isDisabled()) {
                            strokeColor = settings.getInactiveColor();
                        } else if (selected) {
                            strokeColor = settings.getSelectedColor(strokeColor.getAlpha());
                        } else if (member) {
                            strokeColor = settings.getRelationSelectedColor(strokeColor.getAlpha());
                        }
                    }
                }
                painter.drawNodeSymbol(n, symbol, fillColor, strokeColor, text);
            } else {
                if (n.isHighlighted()) {
                    painter.drawNode(n, settings.getHighlightColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), text);
                } else {
                    Color color;
                    boolean isConnection = n.isConnectionNode();

                    if (painter.isInactiveMode() || n.isDisabled()) {
                        color = settings.getInactiveColor();
                    } else if (selected) {
                        color = settings.getSelectedColor();
                    } else if (member) {
                        color = settings.getRelationSelectedColor();
                    } else if (isConnection) {
                        if (n.isTagged()) {
                            color = settings.getTaggedConnectionColor();
                        } else {
                            color = settings.getConnectionColor();
                        }
                    } else {
                        if (n.isTagged()) {
                            color = settings.getTaggedColor();
                        } else {
                            color = settings.getNodeColor();
                        }
                    }

                    final int size = Utils.max((selected ? settings.getSelectedNodeSize() : 0),
                            (n.isTagged() ? settings.getTaggedNodeSize() : 0),
                            (isConnection ? settings.getConnectionNodeSize() : 0),
                            settings.getUnselectedNodeSize());

                    final boolean fill = (selected && settings.isFillSelectedNode()) ||
                            (n.isTagged() && settings.isFillTaggedNode()) ||
                            (isConnection && settings.isFillConnectionNode()) ||
                            settings.isFillUnselectedNode();

                    painter.drawNode(n, color, size, fill, text);
                }
            }
        } else if (primitive instanceof Relation && icon != null) {
            painter.drawRestriction((Relation) primitive, this);
        }
    }

    public ImageIcon getDisabledIcon() {
        if (disabledIcon != null)
            return disabledIcon;
        if (icon == null)
            return null;
        return disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(icon.getImage()));
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 17 * hash + (icon != null ? icon.getImage().hashCode() : 0);
        hash = 17 * hash + iconAlpha;
        hash = 17 * hash + (symbol != null ? symbol.hashCode() : 0);
        hash = 17 * hash + (text != null ? text.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;

        final NodeElemStyle other = (NodeElemStyle) obj;
        // we should get the same image object due to caching
        if (icon != other.icon && (icon == null || other.icon == null || icon.getImage() != other.icon.getImage()))
            return false;
        if (this.iconAlpha != other.iconAlpha)
            return false;
        if (!equal(symbol, other.symbol))
            return false;
        if (!equal(text, other.text))
            return false;
        return true;
    }


    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("NodeElemStyle{");
        s.append(super.toString());
        if (icon != null) {
            s.append(" icon=" + icon + " iconAlpha=" + iconAlpha);
        }
        if (symbol != null) {
            s.append(" symbol=[" + symbol + "]");
        }
        if (text != null) {
            s.append(" text=[" + text.toStringImpl() + "]");
        }
        s.append('}');
        return s.toString();
    }
}
