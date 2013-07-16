// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.awt.Font;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.StaticLabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.LabelCompositionStrategy.TagLookupCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * Represents the rendering style for a textual label placed somewhere on the map.
 *
 */
public class TextElement implements StyleKeys {
    static public final LabelCompositionStrategy AUTO_LABEL_COMPOSITION_STRATEGY = new DeriveLabelFromNameTagsCompositionStrategy();

    /** the strategy for building the actual label value for a given a {@link OsmPrimitive}.
     * Check for null before accessing.
     */
    public LabelCompositionStrategy labelCompositionStrategy;
    /** the font to be used when rendering*/
    public Font font;
    public int xOffset;
    public int yOffset;
    public Color color;
    public Float haloRadius;
    public Color haloColor;

    /**
     * Creates a new text element
     *
     * @param strategy the strategy indicating how the text is composed for a specific {@link OsmPrimitive} to be rendered.
     * If null, no label is rendered.
     * @param font the font to be used. Must not be null.
     * @param xOffset
     * @param yOffset
     * @param color the color to be used. Must not be null
     * @param haloRadius
     * @param haloColor
     */
    public TextElement(LabelCompositionStrategy strategy, Font font, int xOffset, int yOffset, Color color, Float haloRadius, Color haloColor) {
        CheckParameterUtil.ensureParameterNotNull(font);
        CheckParameterUtil.ensureParameterNotNull(color);
        labelCompositionStrategy = strategy;
        this.font = font;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.color = color;
        this.haloRadius = haloRadius;
        this.haloColor = haloColor;
    }

    /**
     * Copy constructor
     *
     * @param other the other element.
     */
    public TextElement(TextElement other) {
        this.labelCompositionStrategy = other.labelCompositionStrategy;
        this.font = other.font;
        this.xOffset = other.xOffset;
        this.yOffset = other.yOffset;
        this.color = other.color;
        this.haloColor = other.haloColor;
        this.haloRadius = other.haloRadius;
    }

    /**
     * Derives a suitable label composition strategy from the style properties in
     * {@code c}.
     *
     * @param c the style properties
     * @return the label composition strategy
     */
    protected static LabelCompositionStrategy buildLabelCompositionStrategy(Cascade c, boolean defaultAnnotate){
        /*
         * If the cascade includes a TagKeyReference we will lookup the rendered label
         * from a tag value.
         */
        TagKeyReference tkr = c.get(TEXT, null, TagKeyReference.class, true);
        if (tkr != null)
            return new TagLookupCompositionStrategy(tkr.key);

        /*
         * Check whether the label composition strategy is given by
         * a keyword
         */
        Keyword keyword = c.get(TEXT, null, Keyword.class, true);
        if (equal(keyword, Keyword.AUTO))
            return AUTO_LABEL_COMPOSITION_STRATEGY;

        /*
         * Do we have a static text label?
         */
        String text = c.get(TEXT, null, String.class, true);
        if (text != null)
            return new StaticLabelCompositionStrategy(text);
        return defaultAnnotate ? AUTO_LABEL_COMPOSITION_STRATEGY : null;
    }

    /**
     * Builds a text element from style properties in {@code c} and the
     * default text color {@code defaultTextColor}
     *
     * @param c the style properties
     * @param defaultTextColor the default text color. Must not be null.
     * @param defaultAnnotate true, if a text label shall be rendered by default, even if the style sheet
     *   doesn't include respective style declarations
     * @return the text element or null, if the style properties don't include
     * properties for text rendering
     * @throws IllegalArgumentException thrown if {@code defaultTextColor} is null
     */
    public static TextElement create(Cascade c, Color defaultTextColor, boolean defaultAnnotate)  throws IllegalArgumentException{
        CheckParameterUtil.ensureParameterNotNull(defaultTextColor);

        LabelCompositionStrategy strategy = buildLabelCompositionStrategy(c, defaultAnnotate);
        if (strategy == null) return null;
        Font font = ElemStyle.getFont(c);

        float xOffset = 0;
        float yOffset = 0;
        float[] offset = c.get("text-offset", null, float[].class);
        if (offset != null) {
            if (offset.length == 1) {
                yOffset = offset[0];
            } else if (offset.length >= 2) {
                xOffset = offset[0];
                yOffset = offset[1];
            }
        }
        xOffset = c.get("text-offset-x", xOffset, Float.class);
        yOffset = c.get("text-offset-y", yOffset, Float.class);

        Color color = c.get("text-color", defaultTextColor, Color.class);
        float alpha = c.get("text-opacity", 1f, Float.class);
        color = new Color(color.getRed(), color.getGreen(),
                color.getBlue(), Utils.color_float2int(alpha));

        Float haloRadius = c.get("text-halo-radius", null, Float.class);
        if (haloRadius != null && haloRadius <= 0) {
            haloRadius = null;
        }
        Color haloColor = null;
        if (haloRadius != null) {
            haloColor = c.get("text-halo-color", Utils.complement(color), Color.class);
            float haloAlpha = c.get("text-halo-opacity", 1f, Float.class);
            haloColor = new Color(haloColor.getRed(), haloColor.getGreen(),
                    haloColor.getBlue(), Utils.color_float2int(haloAlpha));
        }

        return new TextElement(strategy, font, (int) xOffset, - (int) yOffset, color, haloRadius, haloColor);
    }

    /**
     * Replies the label to be rendered for the primitive {@code osm}.
     *
     * @param osm the OSM object
     * @return the label, or null, if {@code osm} is null or if no label can be
     * derived for {@code osm}
     */
    public String getString(OsmPrimitive osm) {
        if (labelCompositionStrategy == null) return null;
        return labelCompositionStrategy.compose(osm);
    }

    @Override
    public String toString() {
        return "TextElement{" + toStringImpl() + '}';
    }

    protected String toStringImpl() {
        StringBuilder sb = new StringBuilder();
        sb.append("labelCompositionStrategy=" + labelCompositionStrategy);
        sb.append(" font=" + font);
        if (xOffset != 0) {
            sb.append(" xOffset=" + xOffset);
        }
        if (yOffset != 0) {
            sb.append(" yOffset=" + yOffset);
        }
        sb.append(" color=" + Utils.toString(color));
        if (haloRadius != null) {
            sb.append(" haloRadius=" + haloRadius);
            sb.append(" haloColor=" + haloColor);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (labelCompositionStrategy != null ? labelCompositionStrategy.hashCode() : 0);
        hash = 79 * hash + font.hashCode();
        hash = 79 * hash + xOffset;
        hash = 79 * hash + yOffset;
        hash = 79 * hash + color.hashCode();
        hash = 79 * hash + (haloRadius != null ? Float.floatToIntBits(haloRadius) : 0);
        hash = 79 * hash + (haloColor != null ? haloColor.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        final TextElement other = (TextElement) obj;
        return  equal(labelCompositionStrategy, other.labelCompositionStrategy) &&
        equal(font, other.font) &&
        xOffset == other.xOffset &&
        yOffset == other.yOffset &&
        equal(color, other.color) &&
        equal(haloRadius, other.haloRadius) &&
        equal(haloColor, other.haloColor);
    }
}
