// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Arrays;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;
import org.openstreetmap.josm.tools.Utils;

public class LineElemStyle extends ElemStyle {

    public static LineElemStyle createSimpleLineStyle(Color color) {
        Cascade c = new Cascade();
        c.put("width", -1f);
        c.put("color", color != null ? color : PaintColors.UNTAGGED.get());
        MultiCascade mc = new MultiCascade();
        mc.put("default", c);
        return createLine(new Environment(null, mc, "default", null));
    }
    public static final LineElemStyle UNTAGGED_WAY = createSimpleLineStyle(null);

    public float realWidth; // the real width of this line in meter
    public Color color;
    public Color dashesBackground;

    private BasicStroke line;
    private BasicStroke dashesLine;

    protected LineElemStyle(Cascade c, BasicStroke line, Color color, BasicStroke dashesLine, Color dashesBackground, float realWidth) {
        super(c);
        this.line = line;
        this.color = color;
        this.dashesLine = dashesLine;
        this.dashesBackground = dashesBackground;
        this.realWidth = realWidth;
    }

    public static LineElemStyle createLine(Environment env) {
        return createImpl(env, "");
    }

    public static LineElemStyle createCasing(Environment env) {
        LineElemStyle casing =  createImpl(env, "casing-");
        if (casing != null) {
            casing.object_z_index = -1;
        }
        return casing;
    }

    private static LineElemStyle createImpl(Environment env, String prefix) {
        Cascade c = env.getCascade();
        Float width = c.get(prefix + "width", null, Float.class, true);
        if (width != null) {
            if (width == -1f) {
                width = (float) MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
            }
            if (width <= 0)
                return null;
        } else {
            String width_key = c.get(prefix + "width", null, String.class, true);
            if (equal(width_key, "thinnest")) {
                width = 0f;
            } else if (! equal(env.layer, "default")) {
                RelativeFloat width_rel = c.get(prefix + "width", null, RelativeFloat.class, true);
                if (width_rel != null) {
                    width = env.mc.getCascade("default").get("width", 0f, Float.class) + width_rel.val;
                } else
                    return null;
            } else
                return null;
        }

        float realWidth = c.get(prefix + "real-width", 0f, Float.class);
        if (realWidth > 0 && MapPaintSettings.INSTANCE.isUseRealWidth()) {

            /* if we have a "width" tag, try use it */
            String widthTag = env.osm.get("width");
            if(widthTag == null) {
                widthTag = env.osm.get("est_width");
            }
            if(widthTag != null) {
                try {
                    realWidth = new Float(Integer.parseInt(widthTag));
                }
                catch(NumberFormatException nfe) {
                }
            }
        }

        Color color = c.get(prefix + "color", null, Color.class);
        if (color == null) {
            color = c.get(prefix + "fill-color", null, Color.class);
        }
        if (color == null) {
            color = PaintColors.UNTAGGED.get();
        }

        int alpha = 255;
        Integer pAlpha = color_float2int(c.get("opacity", null, float.class));
        if (pAlpha != null) {
            alpha = pAlpha;
        }
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        float[] dashes = c.get(prefix + "dashes", null, float[].class);
        if (dashes != null) {
            boolean hasPositive = false;
            for (float f : dashes) {
                if (f > 0) {
                    hasPositive = true;
                }
                if (f < 0) {
                    dashes = null;
                    break;
                }
            }
            if (!hasPositive || dashes.length == 0) {
                dashes = null;
            }
        }
        float dashesOffset = c.get(prefix + "dashes-offset", 0f, Float.class);
        Color dashesBackground = c.get(prefix + "dashes-background-color", null, Color.class);
        if (dashesBackground != null) {
            pAlpha = color_float2int(c.get(prefix + "dashes-background-opacity", null, Float.class));
            if (pAlpha != null) {
                alpha = pAlpha;
            }
            dashesBackground = new Color(dashesBackground.getRed(), dashesBackground.getGreen(),
                    dashesBackground.getBlue(), alpha);
        }

        int cap;
        String capStr = c.get(prefix + "linecap", null, String.class);
        if (equal(capStr, "none")) {
            cap = BasicStroke.CAP_BUTT;
        } else if (equal(capStr, "round")) {
            cap = BasicStroke.CAP_ROUND;
        } else if (equal(capStr, "square")) {
            cap = BasicStroke.CAP_SQUARE;
        } else {
            cap = dashes != null ? BasicStroke.CAP_BUTT : BasicStroke.CAP_ROUND;
        }

        int join;
        String joinStr = c.get(prefix + "linejoin", null, String.class);
        if (equal(joinStr, "round")) {
            join = BasicStroke.JOIN_ROUND;
        } else if (equal(joinStr, "miter")) {
            join = BasicStroke.JOIN_MITER;
        } else if (equal(joinStr, "bevel")) {
            join = BasicStroke.JOIN_BEVEL;
        } else {
            join = BasicStroke.JOIN_ROUND;
        }

        float miterlimit = c.get(prefix + "miterlimit", 10f, Float.class);

        BasicStroke line = new BasicStroke(width, cap, join, miterlimit, dashes, dashesOffset);
        BasicStroke dashesLine = null;

        if (dashes != null && dashesBackground != null) {
            float[] dashes2 = new float[dashes.length];
            System.arraycopy(dashes, 0, dashes2, 1, dashes.length - 1);
            dashes2[0] = dashes[dashes.length-1];
            dashesLine = new BasicStroke(width, cap, join, miterlimit, dashes2, dashes2[0] + dashesOffset);
        }

        return new LineElemStyle(c, line, color, dashesLine, dashesBackground, realWidth);
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

        Color myDashedColor = dashesBackground;
        BasicStroke myLine = line, myDashLine = dashesLine;
        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showDirection) {
            float myWidth = (int) (100 /  (float) (painter.getCircum() / realWidth));
            if (myWidth < line.getLineWidth()) {
                myWidth = line.getLineWidth();
            }
            myLine = new BasicStroke(myWidth, line.getEndCap(), line.getLineJoin(), 
                    line.getMiterLimit(), line.getDashArray(), line.getDashPhase());
            if (dashesLine != null) {
                myDashLine = new BasicStroke(myWidth, dashesLine.getEndCap(), dashesLine.getLineJoin(),
                        dashesLine.getMiterLimit(), dashesLine.getDashArray(), dashesLine.getDashPhase());
            }
        }

        Color markColor = null;
        if(w.isHighlighted()) {
            markColor = paintSettings.getHighlightColor();
        } else if (selected) {
            markColor = paintSettings.getSelectedColor(color.getAlpha());
        } else if (member) {
            markColor = paintSettings.getRelationSelectedColor(color.getAlpha());
        } else if(w.isDisabled()) {
            markColor = paintSettings.getInactiveColor();
            myDashedColor = paintSettings.getInactiveColor();
        }

        painter.drawWay(w, markColor != null ? markColor : color, myLine, myDashLine, myDashedColor, showDirection,
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LineElemStyle other = (LineElemStyle) obj;
        return  equal(line, other.line) &&
                equal(color, other.color) &&
                equal(dashesLine, other.dashesLine) &&
                equal(dashesBackground, other.dashesBackground) &&
                realWidth == other.realWidth;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + line.hashCode();
        hash = 29 * hash + color.hashCode();
        hash = 29 * hash + (dashesLine != null ? dashesLine.hashCode() : 0);
        hash = 29 * hash + (dashesBackground != null ? dashesBackground.hashCode() : 0);
        hash = 29 * hash + Float.floatToIntBits(realWidth);
        return hash;
    }

    @Override
    public String toString() {
        return "LineElemStyle{" + super.toString() + "width=" + line.getLineWidth() +
                " realWidth=" + realWidth + " color=" + Utils.toString(color) +
                " dashed=" + Arrays.toString(line.getDashArray()) +
                (line.getDashPhase() == 0f ? "" : " dashesOffses=" + line.getDashPhase()) +
                " dashedColor=" + Utils.toString(dashesBackground) + '}';
    }
}
