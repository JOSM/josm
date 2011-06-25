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
import org.openstreetmap.josm.tools.Utils;

public class LineElemStyle extends ElemStyle {

    public static LineElemStyle createSimpleLineStyle(Color color, boolean isAreaEdge) {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put("width", Keyword.DEFAULT);
        c.put("color", color != null ? color : PaintColors.UNTAGGED.get());
        if (isAreaEdge) {
            c.put("z-index", -3f);
        }
        return createLine(new Environment(null, mc, "default", null));
    }
    public static final LineElemStyle UNTAGGED_WAY = createSimpleLineStyle(null, false);

    private BasicStroke line;
    public Color color;
    public Color dashesBackground;
    public float realWidth; // the real width of this line in meter

    private BasicStroke dashesLine;

    protected LineElemStyle(Cascade c, BasicStroke line, Color color, BasicStroke dashesLine, Color dashesBackground, float realWidth) {
        super(c, 0f);
        this.line = line;
        this.color = color;
        this.dashesLine = dashesLine;
        this.dashesBackground = dashesBackground;
        this.realWidth = realWidth;
    }

    public static LineElemStyle createLine(Environment env) {
        return createImpl(env, false);
    }

    public static LineElemStyle createCasing(Environment env) {
        LineElemStyle casing =  createImpl(env, true);
        if (casing != null) {
            casing.z_index = -100;
            casing.isModifier = true;
        }
        return casing;
    }

    private static LineElemStyle createImpl(Environment env, boolean casing) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade c_def = env.mc.getCascade("default");

        String prefix = casing ? "casing-" : "";

        Float width;
        if (casing) {
            Float widthOnDefault = getWidth(c_def, "width", null);
            Float widthLine = getWidth(c, "width", widthOnDefault);
            width = getWidth(c, "casing-width", widthLine);
        } else {
            Float widthOnDefault = getWidth(c_def, "width", null);
            width = getWidth(c, "width", widthOnDefault);
        }

        if (width == null)
            return null;

        float realWidth = c.get(prefix + "real-width", 0f, Float.class);
        if (realWidth > 0 && MapPaintSettings.INSTANCE.isUseRealWidth()) {

            /* if we have a "width" tag, try use it */
            String widthTag = env.osm.get("width");
            if(widthTag == null) {
                widthTag = env.osm.get("est_width");
            }
            if(widthTag != null) {
                try {
                    realWidth = Float.valueOf(widthTag);
                }
                catch(NumberFormatException nfe) {
                }
            }
        }

        Color color = c.get(prefix + "color", null, Color.class);
        if (!casing && color == null) {
            color = c.get("fill-color", null, Color.class);
        }
        if (color == null) {
            color = PaintColors.UNTAGGED.get();
        }

        int alpha = 255;
        Integer pAlpha = Utils.color_float2int(c.get("opacity", null, Float.class));
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
            if (!hasPositive || (dashes != null && dashes.length == 0)) {
                dashes = null;
            }
        }
        float dashesOffset = c.get(prefix + "dashes-offset", 0f, Float.class);
        Color dashesBackground = c.get(prefix + "dashes-background-color", null, Color.class);
        if (dashesBackground != null) {
            pAlpha = Utils.color_float2int(c.get(prefix + "dashes-background-opacity", null, Float.class));
            if (pAlpha != null) {
                alpha = pAlpha;
            }
            dashesBackground = new Color(dashesBackground.getRed(), dashesBackground.getGreen(),
                    dashesBackground.getBlue(), alpha);
        }

        Integer cap = null;
        Keyword capKW = c.get(prefix + "linecap", null, Keyword.class);
        if (capKW != null) {
            if (equal(capKW.val, "none")) {
                cap = BasicStroke.CAP_BUTT;
            } else if (equal(capKW.val, "round")) {
                cap = BasicStroke.CAP_ROUND;
            } else if (equal(capKW.val, "square")) {
                cap = BasicStroke.CAP_SQUARE;
            }
        }
        if (cap == null) {
            cap = dashes != null ? BasicStroke.CAP_BUTT : BasicStroke.CAP_ROUND;
        }

        Integer join = null;
        Keyword joinKW = c.get(prefix + "linejoin", null, Keyword.class);
        if (joinKW != null) {
            if (equal(joinKW.val, "round")) {
                join = BasicStroke.JOIN_ROUND;
            } else if (equal(joinKW.val, "miter")) {
                join = BasicStroke.JOIN_MITER;
            } else if (equal(joinKW.val, "bevel")) {
                join = BasicStroke.JOIN_BEVEL;
            }
        }
        if (join == null) {
            join = BasicStroke.JOIN_ROUND;
        }

        float miterlimit = c.get(prefix + "miterlimit", 10f, Float.class);
        if (miterlimit < 1f) {
            miterlimit = 10f;
        }

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
        boolean showOrientation = !isModifier && (selected || paintSettings.isShowDirectionArrow()) && !paintSettings.isUseRealWidth();
        boolean showOneway = !isModifier && !selected &&
        !paintSettings.isUseRealWidth() &&
        paintSettings.isShowOnewayArrow() && w.hasDirectionKeys();
        boolean onewayReversed = w.reversedDirection();
        /* head only takes over control if the option is true,
        the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showOrientation && !selected && paintSettings.isShowHeadArrowOnly();
        Node lastN;

        Color myDashedColor = dashesBackground;
        BasicStroke myLine = line, myDashLine = dashesLine;
        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showOrientation) {
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

        Color myColor = color;
        if(w.isHighlighted()) {
            myColor = paintSettings.getHighlightColor();
        } else if (selected) {
            myColor = paintSettings.getSelectedColor(color.getAlpha());
        } else if (member) {
            myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
        } else if(w.isDisabled()) {
            myColor = paintSettings.getInactiveColor();
            myDashedColor = paintSettings.getInactiveColor();
        }

        painter.drawWay(w, myColor, myLine, myDashLine, myDashedColor, showOrientation,
                showOnlyHeadArrowOnly, showOneway, onewayReversed);

        if(paintSettings.isShowOrderNumber()) {
            int orderNumber = 0;
            lastN = null;
            for(Node n : w.getNodes()) {
                if(lastN != null) {
                    orderNumber++;
                    painter.drawOrderNumber(lastN, n, orderNumber, myColor);
                }
                lastN = n;
            }
        }
    }

    @Override
    public boolean isProperLineStyle() {
        return !isModifier;
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
