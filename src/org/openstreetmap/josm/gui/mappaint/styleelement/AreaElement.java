// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

/**
 * This is the style that defines how an area is filled.
 */
public class AreaElement extends StyleElement {

    /**
     * If fillImage == null, color is the fill-color, otherwise
     * an arbitrary color value sampled from the fillImage
     */
    public Color color;

    /**
     * An image to cover this area. May be null to disable this feature.
     */
    public MapImage fillImage;

    /**
     * The text that should be written on this area.
     */
    public TextLabel text;

    /**
     * Fill the area only partially from the borders
     * <p>
     * Public access is discouraged.
     * @see StyledMapRenderer#drawArea(Way, Color, MapImage, Float, Float, boolean, TextLabel)
     */
    public Float extent;

    /**
     * Areas smaller than this are filled no matter what value {@link #extent} has.
     * <p>
     * Public access is discouraged.
     * @see StyledMapRenderer#drawArea(Way, Color, MapImage, Float, Float, boolean, TextLabel)
     */
    public Float extentThreshold;

    protected AreaElement(Cascade c, Color color, MapImage fillImage, Float extent, Float extentThreshold, TextLabel text) {
        super(c, 1f);
        CheckParameterUtil.ensureParameterNotNull(color);
        this.color = color;
        this.fillImage = fillImage;
        this.extent = extent;
        this.extentThreshold = extentThreshold;
        this.text = text;
    }

    /**
     * Create a new {@link AreaElement}
     * @param env The current style definitions
     * @return The area element or <code>null</code> if the area should not be filled.
     */
    public static AreaElement create(final Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);
        MapImage fillImage = null;
        Color color;

        IconReference iconRef = c.get(FILL_IMAGE, null, IconReference.class);
        if (iconRef != null) {
            fillImage = new MapImage(iconRef.iconName, iconRef.source, false);

            color = new Color(fillImage.getImage(false).getRGB(
                    fillImage.getWidth() / 2, fillImage.getHeight() / 2)
            );

            fillImage.alpha = Math.min(255, Math.max(0, Main.pref.getInteger("mappaint.fill-image-alpha", 255)));
            Integer pAlpha = Utils.colorFloat2int(c.get(FILL_OPACITY, null, float.class));
            if (pAlpha != null) {
                fillImage.alpha = pAlpha;
            }
        } else {
            color = c.get(FILL_COLOR, null, Color.class);
            if (color != null) {
                int alpha = color.getAlpha();
                if (alpha == 255) {
                    // Assume alpha value has not been specified by the user if
                    // is set to fully opaque. Use default value in this case.
                    // It is not an ideal solution, but a little tricky to get this
                    // right, especially as named map colors can be changed in
                    // the preference GUI and written to the preferences file.
                    alpha = Math.min(255, Math.max(0, Main.pref.getInteger("mappaint.fillalpha", 50)));
                }
                Integer pAlpha = Utils.colorFloat2int(c.get(FILL_OPACITY, null, float.class));
                if (pAlpha != null) {
                    alpha = pAlpha;
                }
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            }
        }

        TextLabel text = null;
        Keyword textPos = c.get(TEXT_POSITION, null, Keyword.class);
        if (textPos == null || "center".equals(textPos.val)) {
            text = TextLabel.create(env, PaintColors.AREA_TEXT.get(), true);
        }

        Float extent = c.get(FILL_EXTENT, null, float.class);
        Float extentThreshold = c.get(FILL_EXTENT_THRESHOLD, null, float.class);

        if (color != null)
            return new AreaElement(c, color, fillImage, extent, extentThreshold, text);
        else
            return null;
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        Color myColor = color;
        if (osm instanceof Way) {
            if (color != null) {
                if (selected) {
                    myColor = paintSettings.getSelectedColor(color.getAlpha());
                } else if (outermember) {
                    myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
                }
            }
            painter.drawArea((Way) osm, myColor, fillImage, extent, extentThreshold, painter.isInactiveMode() || osm.isDisabled(), text);
        } else if (osm instanceof Relation) {
            if (color != null && (selected || outermember)) {
                myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
            }
            painter.drawArea((Relation) osm, myColor, fillImage, extent, extentThreshold, painter.isInactiveMode() || osm.isDisabled(), text);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        AreaElement that = (AreaElement) obj;
        return Objects.equals(color, that.color) &&
                Objects.equals(fillImage, that.fillImage) &&
                Objects.equals(text, that.text) &&
                Objects.equals(extent, that.extent) &&
                Objects.equals(extentThreshold, that.extentThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color, fillImage, text, extent, extentThreshold);
    }

    @Override
    public String toString() {
        return "AreaElemStyle{" + super.toString() + "color=" + Utils.toString(color) +
                " fillImage=[" + fillImage + "]}";
    }
}
