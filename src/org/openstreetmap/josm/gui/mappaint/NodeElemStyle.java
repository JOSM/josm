// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import javax.swing.GrayFilter;
import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.Utils;

/**
 * applies for Nodes and turn restriction relations
 */
public class NodeElemStyle extends ElemStyle {
    public boolean annotate;
    public String annotation_key;
    public ImageIcon icon;
    public int iconAlpha;
    public Symbol symbol;

    private ImageIcon disabledIcon;

    public static class Symbol {
        public SymbolShape symbol;
        public float size;
        public Stroke stroke;
        public Color strokeColor;
        public Color fillColor;

        public Symbol(SymbolShape symbol, float size, Stroke stroke, Color strokeColor, Color fillColor) {
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
            hash = 67 * hash + Float.floatToIntBits(size);
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
    
    public enum SymbolShape { SQUARE, CIRCLE }

    public static final NodeElemStyle SIMPLE_NODE_ELEMSTYLE = new NodeElemStyle(Cascade.EMPTY_CASCADE, true, null, null, 0, null);

    protected NodeElemStyle(Cascade c, boolean annotate, String annotation_key, ImageIcon icon, int iconAlpha, Symbol symbol) {
        super(c);
        this.annotate = annotate;
        this.annotation_key = annotation_key;
        this.icon = icon;
        this.iconAlpha = iconAlpha;
        this.symbol = symbol;
    }

    public static NodeElemStyle create(Cascade c) {
        IconReference iconRef = c.get("icon-image", null, IconReference.class);
        ImageIcon icon = null;
        int iconAlpha = 0;
        Symbol symbol = null;

        if (iconRef != null) {
            icon = MapPaintStyles.getIcon(iconRef, false);
            iconAlpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.icon-image-alpha", 255))));
            Integer pAlpha = Utils.color_float2int(c.get("icon-opacity", null, float.class));
            if (pAlpha != null) {
                iconAlpha = pAlpha;
            }
        } else {
            SymbolShape shape;
            String shapeStr = c.get("symbol-shape", null, String.class);
            if (equal(shapeStr, "square")) {
                shape = SymbolShape.SQUARE;
            } else if (equal(shapeStr, "circle")) {
                shape = SymbolShape.CIRCLE;
            } else
                return null;

            Float size = c.get("symbol-size", null, Float.class);
            if (size == null || size <= 0)
                return null;

            Float strokeWidth = c.get("symbol-stroke-width", null, Float.class);
            Color strokeColor = c.get("symbol-stroke-color", null, Color.class);
            if (strokeColor != null) {
                float strokeAlpha = c.get("symbol-stroke-opacity", 1f, Float.class);
                strokeColor = new Color(strokeColor.getRed(), strokeColor.getGreen(),
                        strokeColor.getBlue(), Utils.color_float2int(strokeAlpha));
            }
            Stroke stroke = null;
            if (strokeWidth != null && strokeWidth > 0 && strokeColor != null) {
                stroke = new BasicStroke(strokeWidth);
            }

            Color fillColor = c.get("symbol-fill-color", null, Color.class);
            if (fillColor != null) {
                float fillAlpha = c.get("symbol-fill-opacity", 1f, Float.class);
                fillColor = new Color(fillColor.getRed(), fillColor.getGreen(),
                        fillColor.getBlue(), Utils.color_float2int(fillAlpha));
            }

            if ((stroke == null || strokeColor == null) && fillColor == null)
                return null;

            symbol = new Symbol(shape, size, stroke, strokeColor, fillColor);
        }

        String text = c.get("text", null, String.class);

        boolean annotate = text != null;
        String annotation_key = null;

        if (annotate && !"auto".equalsIgnoreCase(text)) {
            annotation_key = text;
        }
        return new NodeElemStyle(c, annotate, annotation_key, icon, iconAlpha, symbol);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings settings, MapPainter painter, boolean selected, boolean member) {
        if (primitive instanceof Node) {
            Node n = (Node) primitive;
            if (icon != null && painter.isShowIcons()) {
                painter.drawNodeIcon(n, (painter.isInactive() || n.isDisabled()) ? getDisabledIcon() : icon,
                        Utils.color_int2float(iconAlpha), selected, member, getName(n, painter));
            } else if (symbol != null) {
                painter.drawNodeSymbol(n, symbol, selected, member, getName(n, painter));
            } else {
                if (n.isHighlighted()) {
                    painter.drawNode(n, settings.getHighlightColor(), settings.getSelectedNodeSize(), settings.isFillSelectedNode(), getName(n, painter));
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

                    painter.drawNode(n, color, size, fill, getName(n, painter));
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

    protected String getName(Node n, MapPainter painter) {
        if (painter.isShowNames() && annotate) {
            if (annotation_key != null) {
                return n.get(annotation_key);
            } else {
                return painter.getNodeName(n);
            }
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 17 * hash + (annotate ? 1 : 0);
        hash = 17 * hash + (annotation_key != null ? annotation_key.hashCode() : 0);
        hash = 17 * hash + (icon != null ? icon.getImage().hashCode() : 0);
        hash = 17 * hash + this.iconAlpha;
        hash = 17 * hash + (this.symbol != null ? this.symbol.hashCode() : 0);
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
        if (annotate != other.annotate)
            return false;
        if (!equal(annotation_key, annotation_key))
            return false;
        if (this.iconAlpha != other.iconAlpha)
            return false;
        if (!equal(symbol, other.symbol))
            return false;
        return true;
    }


    @Override
    public String toString() {
        return "NodeElemStyle{" + super.toString() + "annotate=" + annotate + " annotation_key=" + annotation_key +
                (icon != null ? (" icon=" + icon + " iconAlpha=" + iconAlpha) : "") +
                (symbol != null ? (" symbol=[" + symbol + "]") : "") + '}';
    }

}
