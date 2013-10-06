// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Arrays;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;
import org.openstreetmap.josm.tools.Utils;

public class LineElemStyle extends ElemStyle {

    public static LineElemStyle createSimpleLineStyle(Color color, boolean isAreaEdge) {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put(WIDTH, Keyword.DEFAULT);
        c.put(COLOR, color != null ? color : PaintColors.UNTAGGED.get());
        if (isAreaEdge) {
            c.put(Z_INDEX, -3f);
        }
        return createLine(new Environment(null, mc, "default", null));
    }
    public static final LineElemStyle UNTAGGED_WAY = createSimpleLineStyle(null, false);

    private BasicStroke line;
    public Color color;
    public Color dashesBackground;
    public float offset;
    public float realWidth; // the real width of this line in meter

    private BasicStroke dashesLine;

    protected enum LineType {
        NORMAL("", 3f),
        CASING("casing-", 2f),
        LEFT_CASING("left-casing-", 2.1f),
        RIGHT_CASING("right-casing-", 2.1f);

        public final String prefix;
        public final float default_major_z_index;

        LineType(String prefix, float default_major_z_index) {
            this.prefix = prefix;
            this.default_major_z_index = default_major_z_index;
        }
    }

    protected LineElemStyle(Cascade c, float default_major_z_index, BasicStroke line, Color color, BasicStroke dashesLine, Color dashesBackground, float offset, float realWidth) {
        super(c, default_major_z_index);
        this.line = line;
        this.color = color;
        this.dashesLine = dashesLine;
        this.dashesBackground = dashesBackground;
        this.offset = offset;
        this.realWidth = realWidth;
    }

    public static LineElemStyle createLine(Environment env) {
        return createImpl(env, LineType.NORMAL);
    }

    public static LineElemStyle createLeftCasing(Environment env) {
        LineElemStyle leftCasing = createImpl(env, LineType.LEFT_CASING);
        if (leftCasing != null) {
            leftCasing.isModifier = true;
        }
        return leftCasing;
    }

    public static LineElemStyle createRightCasing(Environment env) {
        LineElemStyle rightCasing = createImpl(env, LineType.RIGHT_CASING);
        if (rightCasing != null) {
            rightCasing.isModifier = true;
        }
        return rightCasing;
    }

    public static LineElemStyle createCasing(Environment env) {
        LineElemStyle casing = createImpl(env, LineType.CASING);
        if (casing != null) {
            casing.isModifier = true;
        }
        return casing;
    }

    private static LineElemStyle createImpl(Environment env, LineType type) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade c_def = env.mc.getCascade("default");
        Float width;
        switch (type) {
            case NORMAL:
            {
                Float widthOnDefault = getWidth(c_def, WIDTH, null);
                width = getWidth(c, WIDTH, widthOnDefault);
                break;
            }
            case CASING:
            {
                Float casingWidth = c.get(type.prefix + WIDTH, null, Float.class, true);
                if (casingWidth == null) {
                    RelativeFloat rel_casingWidth = c.get(type.prefix + WIDTH, null, RelativeFloat.class, true);
                    if (rel_casingWidth != null) {
                        casingWidth = rel_casingWidth.val / 2;
                    }
                }
                if (casingWidth == null)
                    return null;
                Float widthOnDefault = getWidth(c_def, WIDTH, null);
                width = getWidth(c, WIDTH, widthOnDefault);
                if (width == null) {
                    width = 0f;
                }
                width += 2 * casingWidth;
                break;
            }
            case LEFT_CASING:
            case RIGHT_CASING:
                width = getWidth(c, type.prefix + WIDTH, null);
                break;
            default:
                throw new AssertionError();
        }
        if (width == null)
            return null;

        float realWidth = c.get(type.prefix + REAL_WIDTH, 0f, Float.class);
        if (realWidth > 0 && MapPaintSettings.INSTANCE.isUseRealWidth()) {

            /* if we have a "width" tag, try use it */
            String widthTag = env.osm.get("width");
            if (widthTag == null) {
                widthTag = env.osm.get("est_width");
            }
            if (widthTag != null) {
                try {
                    realWidth = Float.valueOf(widthTag);
                } catch(NumberFormatException nfe) {
                    Main.warn(nfe);
                }
            }
        }

        Float offset = c.get(OFFSET, 0f, Float.class);
        switch (type) {
            case NORMAL:
                break;
            case CASING:
                offset += c.get(type.prefix + OFFSET, 0f, Float.class);
                break;
            case LEFT_CASING:
            case RIGHT_CASING:
            {
                Float baseWidthOnDefault = getWidth(c_def, WIDTH, null);
                Float baseWidth = getWidth(c, WIDTH, baseWidthOnDefault);
                if (baseWidth == null || baseWidth < 2f) {
                    baseWidth = 2f;
                }
                float casingOffset = c.get(type.prefix + OFFSET, 0f, Float.class);
                casingOffset += baseWidth / 2 + width / 2;
                /* flip sign for the right-casing-offset */
                if (type == LineType.RIGHT_CASING) {
                    casingOffset *= -1f;
                }
                offset += casingOffset;
                break;
            }
        }

        Color color = c.get(type.prefix + COLOR, null, Color.class);
        if (type == LineType.NORMAL && color == null) {
            color = c.get(FILL_COLOR, null, Color.class);
        }
        if (color == null) {
            color = PaintColors.UNTAGGED.get();
        }

        int alpha = 255;
        Integer pAlpha = Utils.color_float2int(c.get(type.prefix + OPACITY, null, Float.class));
        if (pAlpha != null) {
            alpha = pAlpha;
        }
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        float[] dashes = c.get(type.prefix + DASHES, null, float[].class);
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
        float dashesOffset = c.get(type.prefix + DASHES_OFFSET, 0f, Float.class);
        Color dashesBackground = c.get(type.prefix + DASHES_BACKGROUND_COLOR, null, Color.class);
        if (dashesBackground != null) {
            pAlpha = Utils.color_float2int(c.get(type.prefix + DASHES_BACKGROUND_OPACITY, null, Float.class));
            if (pAlpha != null) {
                alpha = pAlpha;
            }
            dashesBackground = new Color(dashesBackground.getRed(), dashesBackground.getGreen(),
                    dashesBackground.getBlue(), alpha);
        }

        Integer cap = null;
        Keyword capKW = c.get(type.prefix + "linecap", null, Keyword.class);
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
        Keyword joinKW = c.get(type.prefix + "linejoin", null, Keyword.class);
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

        float miterlimit = c.get(type.prefix + "miterlimit", 10f, Float.class);
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

        return new LineElemStyle(c, type.default_major_z_index, line, color, dashesLine, dashesBackground, offset, realWidth);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter, boolean selected, boolean member) {
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
        if (selected) {
            myColor = paintSettings.getSelectedColor(color.getAlpha());
        } else if (member) {
            myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
        } else if(w.isDisabled()) {
            myColor = paintSettings.getInactiveColor();
            myDashedColor = paintSettings.getInactiveColor();
        }

        painter.drawWay(w, myColor, myLine, myDashLine, myDashedColor, offset, showOrientation,
                showOnlyHeadArrowOnly, showOneway, onewayReversed);

        if(paintSettings.isShowOrderNumber() && !painter.isInactiveMode()) {
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
            offset == other.offset &&
            realWidth == other.realWidth;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 29 * hash + line.hashCode();
        hash = 29 * hash + color.hashCode();
        hash = 29 * hash + (dashesLine != null ? dashesLine.hashCode() : 0);
        hash = 29 * hash + (dashesBackground != null ? dashesBackground.hashCode() : 0);
        hash = 29 * hash + Float.floatToIntBits(offset);
        hash = 29 * hash + Float.floatToIntBits(realWidth);
        return hash;
    }

    @Override
    public String toString() {
        return "LineElemStyle{" + super.toString() + "width=" + line.getLineWidth() +
            " realWidth=" + realWidth + " color=" + Utils.toString(color) +
            " dashed=" + Arrays.toString(line.getDashArray()) +
            (line.getDashPhase() == 0f ? "" : " dashesOffses=" + line.getDashPhase()) +
            " dashedColor=" + Utils.toString(dashesBackground) +
            " linejoin=" + linejoinToString(line.getLineJoin()) +
            " linecap=" + linecapToString(line.getEndCap()) +
            (offset == 0 ? "" : " offset=" + offset) +
            '}';
    }

    public String linejoinToString(int linejoin) {
        switch (linejoin) {
            case BasicStroke.JOIN_BEVEL: return "bevel";
            case BasicStroke.JOIN_ROUND: return "round";
            case BasicStroke.JOIN_MITER: return "miter";
            default: return null;
        }
    }
    public String linecapToString(int linecap) {
        switch (linecap) {
            case BasicStroke.CAP_BUTT: return "none";
            case BasicStroke.CAP_ROUND: return "round";
            case BasicStroke.CAP_SQUARE: return "square";
            default: return null;
        }
    }
}
