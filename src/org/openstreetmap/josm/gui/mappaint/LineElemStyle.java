// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Arrays;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.tools.Utils;

public class LineElemStyle extends ElemStyle {

    public static LineElemStyle createSimpleLineStyle(Color color) {
        return new LineElemStyle(Cascade.EMPTY_CASCADE, -1f, 0f, color != null ? color : PaintColors.UNTAGGED.get(), null, null);
    }
    public static final LineElemStyle UNTAGGED_WAY = createSimpleLineStyle(null);

    private float width;
    public float realWidth; // the real width of this line in meter
    public Color color;
    private float[] dashed;
    public Color dashedColor;

    protected LineElemStyle(Cascade c, float width, float realWidth, Color color, float[] dashed, Color dashedColor) {
        super(c);
        setWidth(width);
        this.realWidth = realWidth;
        this.color = color;
        this.dashed = dashed;
        this.dashedColor = dashedColor;
    }

    public static LineElemStyle createLine(Cascade c) {
        return createImpl(c, "");
    }

    public static LineElemStyle createCasing(Cascade c) {
        LineElemStyle casing =  createImpl(c, "casing-");
        if (casing != null) {
            casing.object_z_index = -1;
        }
        return casing;
    }

    private static LineElemStyle createImpl(Cascade c, String prefix) {
        Float width = c.getFloat(prefix + "width", null);
        if (width == null)
            return null;

        float realWidth = c.getFloat(prefix + "real-width", 0f);
        Color color = c.getColor(prefix + "color", null);
        if (color == null) {
            color = c.getColor(prefix + "fill-color", null);
        }
        if (color == null) {
            color = PaintColors.UNTAGGED.get();
        }
        float[] dashes = c.get(prefix + "dashes", null, float[].class);
        Color dashesBackground = c.getColor(prefix + "dashes-background-color", null);

        return new LineElemStyle(c, width, realWidth, color, dashes, dashesBackground);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        Way w = (Way)primitive;
        /* show direction arrows, if draw.segment.relevant_directions_only is not set,
        the way is tagged with a direction key
        (even if the tag is negated as in oneway=false) or the way is selected */
        boolean showDirection = selected || ((!paintSettings.isUseRealWidth()) && (paintSettings.isShowDirectionArrow()
                && (!paintSettings.isShowRelevantDirectionsOnly() || w.hasDirectionKeys())));
        boolean reversedDirection = w.reversedDirection();
        /* head only takes over control if the option is true,
        the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showDirection && !selected && paintSettings.isShowHeadArrowOnly();
        Node lastN;

        Color myDashedColor = dashedColor;
        float myWidth = getWidth();

        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showDirection) {

            /* if we have a "width" tag, try use it */
            /* (this might be slow and could be improved by caching the value in the Way, on the other hand only used if "real width" is enabled) */
            String widthTag = w.get("width");
            if(widthTag == null) {
                widthTag = w.get("est_width");
            }
            if(widthTag != null) {
                try {
                    realWidth = new Float(Integer.parseInt(widthTag));
                }
                catch(NumberFormatException nfe) {
                }
            }

            myWidth = (int) (100 /  (float) (painter.getCircum() / realWidth));
            if (myWidth < getWidth()) {
                myWidth = getWidth();
            }
        }

        Color markColor = null;
        if(w.isHighlighted()) {
            markColor = paintSettings.getHighlightColor();
        } else if (selected) {
            markColor = paintSettings.getSelectedColor();
        } else if (member) {
            markColor = paintSettings.getRelationSelectedColor();
        } else if(w.isDisabled()) {
            markColor = paintSettings.getInactiveColor();
            myDashedColor = paintSettings.getInactiveColor();
        }

        painter.drawWay(w, markColor != null ? markColor : color, myWidth, dashed, myDashedColor, showDirection,
                    selected ? false : reversedDirection, showOnlyHeadArrowOnly);

        if(paintSettings.isShowOrderNumber()) {
            int orderNumber = 0;
            lastN = null;
            for(Node n : w.getNodes()) {
                if(lastN != null) {
                    orderNumber++;
                    painter.drawOrderNumber(lastN, n, orderNumber);
                }
                lastN = n;
            }
        }
    }

    public float getWidth() {
        if (width == -1f)
            return MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LineElemStyle other = (LineElemStyle) obj;
        return width == other.width &&
                realWidth == other.realWidth &&
                Utils.equal(color, other.color) &&
                Arrays.equals(dashed, other.dashed) &&
                Utils.equal(dashedColor, other.dashedColor);
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + Float.floatToIntBits(width);
        hash = 29 * hash + Float.floatToIntBits(realWidth);
        hash = 29 * hash + color.hashCode();
        hash = 29 * hash + Arrays.hashCode(dashed);
        hash = 29 * hash + (dashedColor != null ? dashedColor.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "LineElemStyle{" + super.toString() + "width=" + width + " realWidth=" + realWidth + " color=" + color + " dashed=" + Arrays.toString(dashed) + " dashedColor=" + dashedColor + '}';
    }
}
