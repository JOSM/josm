// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MultiCascade;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is the style definition for a simple line.
 */
public class LineElement extends StyleElement {
    /**
     * The default style for any untagged way.
     */
    public static final LineElement UNTAGGED_WAY = createSimpleLineStyle(null, false);

    private final BasicStroke line;
    public Color color;
    public Color dashesBackground;
    public float offset;
    public float realWidth; // the real width of this line in meter
    public boolean wayDirectionArrows;

    private final BasicStroke dashesLine;

    public enum LineType {
        NORMAL("", 3f),
        CASING("casing-", 2f),
        LEFT_CASING("left-casing-", 2.1f),
        RIGHT_CASING("right-casing-", 2.1f);

        public final String prefix;
        public final float defaultMajorZIndex;

        LineType(String prefix, float defaultMajorZindex) {
            this.prefix = prefix;
            this.defaultMajorZIndex = defaultMajorZindex;
        }
    }

    protected LineElement(Cascade c, float defaultMajorZindex, BasicStroke line, Color color, BasicStroke dashesLine,
            Color dashesBackground, float offset, float realWidth, boolean wayDirectionArrows) {
        super(c, defaultMajorZindex);
        this.line = line;
        this.color = color;
        this.dashesLine = dashesLine;
        this.dashesBackground = dashesBackground;
        this.offset = offset;
        this.realWidth = realWidth;
        this.wayDirectionArrows = wayDirectionArrows;
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        /* show direction arrows, if draw.segment.relevant_directions_only is not set,
        the way is tagged with a direction key
        (even if the tag is negated as in oneway=false) or the way is selected */
        boolean showOrientation;
        if (defaultSelectedHandling) {
            showOrientation = !isModifier && (selected || paintSettings.isShowDirectionArrow()) && !paintSettings.isUseRealWidth();
        } else {
            showOrientation = wayDirectionArrows;
        }
        boolean showOneway = !isModifier && !selected &&
                !paintSettings.isUseRealWidth() &&
                paintSettings.isShowOnewayArrow() && primitive.hasDirectionKeys();
        boolean onewayReversed = primitive.reversedDirection();
        /* head only takes over control if the option is true,
        the direction should be shown at all and not only because it's selected */
        boolean showOnlyHeadArrowOnly = showOrientation && !selected && paintSettings.isShowHeadArrowOnly();
        Node lastN;

        Color myDashedColor = dashesBackground;
        BasicStroke myLine = line, myDashLine = dashesLine;
        if (realWidth > 0 && paintSettings.isUseRealWidth() && !showOrientation) {
            float myWidth = (int) (100 / (float) (painter.getCircum() / realWidth));
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
        if (defaultSelectedHandling && selected) {
            myColor = paintSettings.getSelectedColor(color.getAlpha());
        } else if (member || outermember) {
            myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
        } else if (primitive.isDisabled()) {
            myColor = paintSettings.getInactiveColor();
            myDashedColor = paintSettings.getInactiveColor();
        }

        if (primitive instanceof Way) {
            Way w = (Way) primitive;
            painter.drawWay(w, myColor, myLine, myDashLine, myDashedColor, offset, showOrientation,
                    showOnlyHeadArrowOnly, showOneway, onewayReversed);

            if ((paintSettings.isShowOrderNumber() || (paintSettings.isShowOrderNumberOnSelectedWay() && selected))
                    && !painter.isInactiveMode()) {
                int orderNumber = 0;
                lastN = null;
                for (Node n : w.getNodes()) {
                    if (lastN != null) {
                        orderNumber++;
                        painter.drawOrderNumber(lastN, n, orderNumber, myColor);
                    }
                    lastN = n;
                }
            }
        }
    }

    @Override
    public boolean isProperLineStyle() {
        return !isModifier;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LineElement other = (LineElement) obj;
        return offset == other.offset &&
               realWidth == other.realWidth &&
               wayDirectionArrows == other.wayDirectionArrows &&
               Objects.equals(line, other.line) &&
               Objects.equals(color, other.color) &&
               Objects.equals(dashesLine, other.dashesLine) &&
               Objects.equals(dashesBackground, other.dashesBackground);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), line, color, dashesBackground, offset, realWidth, wayDirectionArrows, dashesLine);
    }

    @Override
    public String toString() {
        return "LineElemStyle{" + super.toString() + "width=" + line.getLineWidth() +
            " realWidth=" + realWidth + " color=" + Utils.toString(color) +
            " dashed=" + Arrays.toString(line.getDashArray()) +
            (line.getDashPhase() == 0 ? "" : " dashesOffses=" + line.getDashPhase()) +
            " dashedColor=" + Utils.toString(dashesBackground) +
            " linejoin=" + linejoinToString(line.getLineJoin()) +
            " linecap=" + linecapToString(line.getEndCap()) +
            (offset == 0 ? "" : " offset=" + offset) +
            '}';
    }

    /**
     * Creates a simple line with default widt.
     * @param color The color to use
     * @param isAreaEdge If this is an edge for an area. Edges are drawn at lower Z-Index.
     * @return The line style.
     */
    public static LineElement createSimpleLineStyle(Color color, boolean isAreaEdge) {
        MultiCascade mc = new MultiCascade();
        Cascade c = mc.getOrCreateCascade("default");
        c.put(WIDTH, Keyword.DEFAULT);
        c.put(COLOR, color != null ? color : PaintColors.UNTAGGED.get());
        c.put(OPACITY, 1f);
        if (isAreaEdge) {
            c.put(Z_INDEX, -3f);
        }
        Way w = new Way();
        return createLine(new Environment(w, mc, "default", null));
    }

    public static LineElement createLine(Environment env) {
        return createImpl(env, LineType.NORMAL);
    }

    public static LineElement createLeftCasing(Environment env) {
        LineElement leftCasing = createImpl(env, LineType.LEFT_CASING);
        if (leftCasing != null) {
            leftCasing.isModifier = true;
        }
        return leftCasing;
    }

    public static LineElement createRightCasing(Environment env) {
        LineElement rightCasing = createImpl(env, LineType.RIGHT_CASING);
        if (rightCasing != null) {
            rightCasing.isModifier = true;
        }
        return rightCasing;
    }

    public static LineElement createCasing(Environment env) {
        LineElement casing = createImpl(env, LineType.CASING);
        if (casing != null) {
            casing.isModifier = true;
        }
        return casing;
    }

    private static LineElement createImpl(Environment env, LineType type) {
        Cascade c = env.mc.getCascade(env.layer);
        Cascade cDef = env.mc.getCascade("default");
        Float width = computeWidth(type, c, cDef);
        if (width == null)
            return null;

        float realWidth = computeRealWidth(env, type, c);

        Float offset = computeOffset(type, c, cDef, width);

        int alpha = 255;
        Color color = c.get(type.prefix + COLOR, null, Color.class);
        if (color != null) {
            alpha = color.getAlpha();
        }
        if (type == LineType.NORMAL && color == null) {
            color = c.get(FILL_COLOR, null, Color.class);
        }
        if (color == null) {
            color = PaintColors.UNTAGGED.get();
        }

        Integer pAlpha = Utils.colorFloat2int(c.get(type.prefix + OPACITY, null, Float.class));
        if (pAlpha != null) {
            alpha = pAlpha;
        }
        color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);

        float[] dashes = c.get(type.prefix + DASHES, null, float[].class, true);
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
            pAlpha = Utils.colorFloat2int(c.get(type.prefix + DASHES_BACKGROUND_OPACITY, null, Float.class));
            if (pAlpha != null) {
                alpha = pAlpha;
            }
            dashesBackground = new Color(dashesBackground.getRed(), dashesBackground.getGreen(),
                    dashesBackground.getBlue(), alpha);
        }

        Integer cap = null;
        Keyword capKW = c.get(type.prefix + LINECAP, null, Keyword.class);
        if (capKW != null) {
            if ("none".equals(capKW.val)) {
                cap = BasicStroke.CAP_BUTT;
            } else if ("round".equals(capKW.val)) {
                cap = BasicStroke.CAP_ROUND;
            } else if ("square".equals(capKW.val)) {
                cap = BasicStroke.CAP_SQUARE;
            }
        }
        if (cap == null) {
            cap = dashes != null ? BasicStroke.CAP_BUTT : BasicStroke.CAP_ROUND;
        }

        Integer join = null;
        Keyword joinKW = c.get(type.prefix + LINEJOIN, null, Keyword.class);
        if (joinKW != null) {
            if ("round".equals(joinKW.val)) {
                join = BasicStroke.JOIN_ROUND;
            } else if ("miter".equals(joinKW.val)) {
                join = BasicStroke.JOIN_MITER;
            } else if ("bevel".equals(joinKW.val)) {
                join = BasicStroke.JOIN_BEVEL;
            }
        }
        if (join == null) {
            join = BasicStroke.JOIN_ROUND;
        }

        float miterlimit = c.get(type.prefix + MITERLIMIT, 10f, Float.class);
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

        boolean wayDirectionArrows = c.get(type.prefix + WAY_DIRECTION_ARROWS, env.osm.isSelected(), Boolean.class);

        return new LineElement(c, type.defaultMajorZIndex, line, color, dashesLine, dashesBackground,
                offset, realWidth, wayDirectionArrows);
    }

    private static Float computeWidth(LineType type, Cascade c, Cascade cDef) {
        Float width;
        switch (type) {
            case NORMAL:
                width = getWidth(c, WIDTH, getWidth(cDef, WIDTH, null));
                break;
            case CASING:
                Float casingWidth = c.get(type.prefix + WIDTH, null, Float.class, true);
                if (casingWidth == null) {
                    RelativeFloat relCasingWidth = c.get(type.prefix + WIDTH, null, RelativeFloat.class, true);
                    if (relCasingWidth != null) {
                        casingWidth = relCasingWidth.val / 2;
                    }
                }
                if (casingWidth == null)
                    return null;
                width = Optional.ofNullable(getWidth(c, WIDTH, getWidth(cDef, WIDTH, null))).orElse(0f) + 2 * casingWidth;
                break;
            case LEFT_CASING:
            case RIGHT_CASING:
                width = getWidth(c, type.prefix + WIDTH, null);
                break;
            default:
                throw new AssertionError();
        }
        return width;
    }

    private static float computeRealWidth(Environment env, LineType type, Cascade c) {
        float realWidth = c.get(type.prefix + REAL_WIDTH, 0f, Float.class);
        if (realWidth > 0 && MapPaintSettings.INSTANCE.isUseRealWidth()) {

            /* if we have a "width" tag, try use it */
            String widthTag = Optional.ofNullable(env.osm.get("width")).orElseGet(() -> env.osm.get("est_width"));
            if (widthTag != null) {
                try {
                    realWidth = Float.parseFloat(widthTag);
                } catch (NumberFormatException nfe) {
                    Main.warn(nfe);
                }
            }
        }
        return realWidth;
    }

    private static Float computeOffset(LineType type, Cascade c, Cascade cDef, Float width) {
        Float offset = c.get(OFFSET, 0f, Float.class);
        switch (type) {
            case NORMAL:
                break;
            case CASING:
                offset += c.get(type.prefix + OFFSET, 0f, Float.class);
                break;
            case LEFT_CASING:
            case RIGHT_CASING:
                Float baseWidthOnDefault = getWidth(cDef, WIDTH, null);
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
        return offset;
    }
}
