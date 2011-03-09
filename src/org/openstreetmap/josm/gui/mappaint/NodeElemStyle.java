// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
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

    public enum SymbolShape { SQUARE, CIRCLE }
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

        public NodeTextElement(String textKey, HorizontalTextAlignment hAlign, VerticalTextAlignment vAlign, Font font, int xOffset, int yOffset, Color color) {
            super(textKey, font, xOffset, yOffset, color);
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

    }

    public static final NodeElemStyle SIMPLE_NODE_ELEMSTYLE;
    static {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("text", "auto");
        SIMPLE_NODE_ELEMSTYLE = create(new Environment(null, mc, "default", null), true);
    }

    protected NodeElemStyle(Cascade c, ImageIcon icon, int iconAlpha, Symbol symbol, NodeTextElement text) {
        super(c);
        this.icon = icon;
        this.iconAlpha = iconAlpha;
        this.symbol = symbol;
        this.text = text;
    }

    public static NodeElemStyle create(Environment env) {
        return create(env, false);
    }

    private static NodeElemStyle create(Environment env, boolean allowOnlyText) {
        Cascade c = env.mc.getCascade(env.layer);

        IconReference iconRef = c.get("icon-image", null, IconReference.class);
        ImageIcon icon = null;
        int iconAlpha = 0;
        Symbol symbol = null;

        if (iconRef != null) {
            icon = MapPaintStyles.getIcon(iconRef, false);
            if (icon == null) {
                icon = MapPaintStyles.getNoIcon_Icon(iconRef.source, false);
            }
            iconAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.icon-image-alpha", 255))));
            Integer pAlpha = Utils.color_float2int(c.get("icon-opacity", null, float.class));
            if (pAlpha != null) {
                iconAlpha = pAlpha;
            }
        } else {
            symbol = createSymbol(env);
        }

        if (icon == null && symbol == null && !allowOnlyText)
            return null;

        NodeTextElement text = null;
        TextElement te = TextElement.create(c, PaintColors.TEXT.get());
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
            text = new NodeTextElement(te.textKey, hAlign, vAlign, te.font, te.xOffset, te.yOffset, te.color);
        }
        
        return new NodeElemStyle(c, icon, iconAlpha, symbol, text);
    }

    private static Symbol createSymbol(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade c_def = env.mc.getCascade("default");

        SymbolShape shape;
        Keyword shapeKW = c.get("symbol-shape", null, Keyword.class);
        if (shapeKW == null)
            return null;
        if (equal(shapeKW, "square")) {
            shape = SymbolShape.SQUARE;
        } else if (equal(shapeKW, "circle")) {
            shape = SymbolShape.CIRCLE;
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
        if (stroke == null && fillColor == null)
            fillColor = Color.BLUE;

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
                painter.drawNodeIcon(n, (painter.isInactive() || n.isDisabled()) ? getDisabledIcon() : icon,
                        Utils.color_int2float(iconAlpha), selected, member, text);
            } else if (symbol != null) {
                Color fillColor = symbol.fillColor;
                if (fillColor != null) {
                    if (n.isHighlighted()) {
                        fillColor = settings.getHighlightColor();
                    } else {
                        if (painter.isInactive() || n.isDisabled()) {
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
                        if (painter.isInactive() || n.isDisabled()) {
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

                    if (painter.isInactive() || n.isDisabled()) {
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
        return "NodeElemStyle{" + super.toString() +
                (icon != null ? ("icon=" + icon + " iconAlpha=" + iconAlpha) : "") +
                (symbol != null ? (" symbol=[" + symbol + "]") : "") + '}';
    }

}
