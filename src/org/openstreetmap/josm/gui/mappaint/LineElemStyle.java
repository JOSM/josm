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

    public static final LineElemStyle UNTAGGED_WAY;

    static {
        UNTAGGED_WAY = new LineElemStyle(0, Long.MAX_VALUE, -1, 0, PaintColors.UNTAGGED.get(), new float[0], null);
    }

    public static LineElemStyle createSimpleLineStyle(Color color) {
        return new LineElemStyle(0, Long.MAX_VALUE, -1, 0, color, new float[0], null);
    }

    private int width;
    public int realWidth; //the real width of this line in meter
    public Color color;
    private float[] dashed;
    public Color dashedColor;

    public LineElemStyle(long minScale, long maxScale, int width, int realWidth, Color color, float[] dashed, Color dashedColor) {
        super(minScale, maxScale);
        setWidth(width);
        this.realWidth = realWidth;
        this.color = color;
        this.dashed = dashed;
        this.dashedColor = dashedColor;
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
        int myWidth = getWidth();

        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showDirection) {

            /* if we have a "width" tag, try use it */
            /* (this might be slow and could be improved by caching the value in the Way, on the other hand only used if "real width" is enabled) */
            String widthTag = w.get("width");
            if(widthTag == null) {
                widthTag = w.get("est_width");
            }
            if(widthTag != null) {
                try {
                    realWidth = Integer.parseInt(widthTag);
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

    public int getWidth() {
        if (width == -1)
            return MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
        return width;
    }

    public void setWidth(int width) {
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
        int hash = 3;
        hash = 29 * hash + this.width;
        hash = 29 * hash + this.realWidth;
        hash = 29 * hash + this.color.hashCode();
        hash = 29 * hash + Arrays.hashCode(this.dashed);
        hash = 29 * hash + (this.dashedColor != null ? this.dashedColor.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "LineElemStyle{" + "width=" + width + " realWidth=" + realWidth + " color=" + color + " dashed=" + Arrays.toString(dashed) + " dashedColor=" + dashedColor + '}';
    }
}
