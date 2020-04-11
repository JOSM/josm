// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.awt.Font;
import java.awt.geom.Point2D;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.TagKeyReference;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.DeriveLabelFromNameTagsCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.StaticLabelCompositionStrategy;
import org.openstreetmap.josm.gui.mappaint.styleelement.LabelCompositionStrategy.TagLookupCompositionStrategy;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.RotationAngle;

/**
 * Represents the rendering style for a textual label placed somewhere on the map.
 * @since 3880
 */
public class TextLabel implements StyleKeys {
    /**
     * The default strategy to use when determining the label of a element.
     */
    public static final LabelCompositionStrategy AUTO_LABEL_COMPOSITION_STRATEGY = new DeriveLabelFromNameTagsCompositionStrategy();

    /**
     * The strategy for building the actual label value for a given a {@link OsmPrimitive}.
     * Check for null before accessing.
     */
    public LabelCompositionStrategy labelCompositionStrategy;
    /**
     * the font to be used when rendering
     */
    public Font font;
    /**
     * The rotation angle to be used when rendering
     */
    public RotationAngle rotationAngle;
    /**
     * The color to draw the text in, includes alpha.
     */
    public Color color;
    /**
     * The radius of the halo effect.
     */
    public Float haloRadius;
    /**
     * The color of the halo effect.
     */
    public Color haloColor;

    /**
     * Creates a new text element
     *
     * @param strategy the strategy indicating how the text is composed for a specific {@link OsmPrimitive} to be rendered.
     * If null, no label is rendered.
     * @param font the font to be used. Must not be null.
     * @param rotationAngle the rotation angle to be used. Must not be null.
     * @param color the color to be used. Must not be null
     * @param haloRadius halo radius
     * @param haloColor halo color
     */
    protected TextLabel(LabelCompositionStrategy strategy, Font font, RotationAngle rotationAngle,
                        Color color, Float haloRadius, Color haloColor) {
        this.labelCompositionStrategy = strategy;
        this.font = Objects.requireNonNull(font, "font");
        this.rotationAngle = Objects.requireNonNull(rotationAngle, "rotationAngle");
        this.color = Objects.requireNonNull(color, "color");
        this.haloRadius = haloRadius;
        this.haloColor = haloColor;
    }

    /**
     * Copy constructor
     *
     * @param other the other element.
     */
    public TextLabel(TextLabel other) {
        this.labelCompositionStrategy = other.labelCompositionStrategy;
        this.font = other.font;
        this.rotationAngle = other.rotationAngle;
        this.color = other.color;
        this.haloColor = other.haloColor;
        this.haloRadius = other.haloRadius;
    }

    /**
     * Derives a suitable label composition strategy from the style properties in {@code c}.
     *
     * @param c the style properties
     * @param defaultAnnotate whether to return {@link #AUTO_LABEL_COMPOSITION_STRATEGY} if not strategy is found
     * @return the label composition strategy, or {@code null}
     */
    protected static LabelCompositionStrategy buildLabelCompositionStrategy(Cascade c, boolean defaultAnnotate) {
        /*
         * If the cascade includes a TagKeyReference we will lookup the rendered label
         * from a tag value.
         */
        TagKeyReference tkr = c.get(TEXT, null, TagKeyReference.class, true);
        if (tkr != null)
            return new TagLookupCompositionStrategy(tkr.key);

        /*
         * Check whether the label composition strategy is given by a keyword
         */
        Keyword keyword = c.get(TEXT, null, Keyword.class, true);
        if (Keyword.AUTO.equals(keyword))
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
     * @param env the environment
     * @param defaultTextColor the default text color. Must not be null.
     * @param defaultAnnotate true, if a text label shall be rendered by default, even if the style sheet
     *   doesn't include respective style declarations
     * @return the text element or null, if the style properties don't include
     * properties for text rendering
     * @throws IllegalArgumentException if {@code defaultTextColor} is null
     */
    public static TextLabel create(Environment env, Color defaultTextColor, boolean defaultAnnotate) {
        CheckParameterUtil.ensureParameterNotNull(defaultTextColor);
        Cascade c = env.mc.getCascade(env.layer);

        LabelCompositionStrategy strategy = buildLabelCompositionStrategy(c, defaultAnnotate);
        if (strategy == null) return null;
        String s = strategy.compose(env.osm);
        if (s == null) return null;
        Font font = StyleElement.getFont(c, s);
        RotationAngle rotationAngle = NodeElement.createTextRotationAngle(env);

        Color color = c.get(TEXT_COLOR, defaultTextColor, Color.class);
        float alpha = c.get(TEXT_OPACITY, 1f, Float.class);
        color = ColorHelper.alphaMultiply(color, alpha);

        Float haloRadius = c.get(TEXT_HALO_RADIUS, null, Float.class);
        if (haloRadius != null && haloRadius <= 0) {
            haloRadius = null;
        }
        Color haloColor = null;
        if (haloRadius != null) {
            haloColor = c.get(TEXT_HALO_COLOR, ColorHelper.complement(color), Color.class);
            float haloAlphaFactor = c.get(TEXT_HALO_OPACITY, 1f, Float.class);
            haloColor = ColorHelper.alphaMultiply(haloColor, haloAlphaFactor);
        }

        return new TextLabel(strategy, font, rotationAngle, color, haloRadius, haloColor);
    }

    /**
     * Gets the text-offset property from a cascade
     * @param c The cascade
     * @return The text offset property
     */
    public static Point2D getTextOffset(Cascade c) {
        float xOffset = 0;
        float yOffset = 0;
        float[] offset = c.get(TEXT_OFFSET, null, float[].class);
        if (offset != null) {
            if (offset.length == 1) {
                yOffset = offset[0];
            } else if (offset.length >= 2) {
                xOffset = offset[0];
                yOffset = offset[1];
            }
        }
        xOffset = c.get(TEXT_OFFSET_X, xOffset, Float.class);
        yOffset = c.get(TEXT_OFFSET_Y, yOffset, Float.class);
        return new Point2D.Double(xOffset, yOffset);
    }

    /**
     * Replies the label to be rendered for the primitive {@code osm}.
     *
     * @param osm the OSM object
     * @return the label, or null, if {@code osm} is null or if no label can be
     * derived for {@code osm}
     */
    public String getString(IPrimitive osm) {
        if (labelCompositionStrategy == null) return null;
        return labelCompositionStrategy.compose(osm);
    }

    @Override
    public String toString() {
        return "TextLabel{" + toStringImpl() + '}';
    }

    protected String toStringImpl() {
        StringBuilder sb = new StringBuilder(96);
        sb.append("labelCompositionStrategy=").append(labelCompositionStrategy)
          .append(" font=").append(font)
          .append(" rotationAngle=").append(rotationAngle)
          .append(" color=").append(ColorHelper.color2html(color));
        if (haloRadius != null) {
            sb.append(" haloRadius=").append(haloRadius)
              .append(" haloColor=").append(haloColor);
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(labelCompositionStrategy, font, rotationAngle, color, haloRadius, haloColor);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TextLabel textLabel = (TextLabel) obj;
        return Objects.equals(labelCompositionStrategy, textLabel.labelCompositionStrategy) &&
                Objects.equals(font, textLabel.font) &&
                Objects.equals(rotationAngle, textLabel.rotationAngle) &&
                Objects.equals(color, textLabel.color) &&
                Objects.equals(haloRadius, textLabel.haloRadius) &&
                Objects.equals(haloColor, textLabel.haloColor);
    }
}
