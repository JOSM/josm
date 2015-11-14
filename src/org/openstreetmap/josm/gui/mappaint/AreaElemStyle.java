// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Objects;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.PaintColors;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

public class AreaElemStyle extends ElemStyle {

    /**
     * If fillImage == null, color is the fill-color, otherwise
     * an arbitrary color value sampled from the fillImage
     */
    public Color color;
    public MapImage fillImage;
    public TextElement text;
    public Float extent;

    protected AreaElemStyle(Cascade c, Color color, MapImage fillImage, Float extent, TextElement text) {
        super(c, 1f);
        CheckParameterUtil.ensureParameterNotNull(color);
        this.color = color;
        this.extent = extent;
        this.fillImage = fillImage;
        this.text = text;
    }

    public static AreaElemStyle create(final Environment env) {
        final Cascade c = env.mc.getCascade(env.layer);
        MapImage fillImage = null;
        Color color = null;
        Float extent = null;

        IconReference iconRef = c.get(FILL_IMAGE, null, IconReference.class);
        if (iconRef != null) {
            fillImage = new MapImage(iconRef.iconName, iconRef.source, false);

            color = new Color(fillImage.getImage(false).getRGB(
                    fillImage.getWidth() / 2, fillImage.getHeight() / 2)
            );

            fillImage.alpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fill-image-alpha", 255))));
            Integer pAlpha = Utils.color_float2int(c.get(FILL_OPACITY, null, float.class));
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
                    alpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
                }
                Integer pAlpha = Utils.color_float2int(c.get(FILL_OPACITY, null, float.class));
                if (pAlpha != null) {
                    alpha = pAlpha;
                }
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
                extent = c.get(FILL_EXTENT, null, float.class);
            }
        }

        TextElement text = null;
        Keyword textPos = c.get(TEXT_POSITION, null, Keyword.class);
        if (textPos == null || "center".equals(textPos.val)) {
            text = TextElement.create(env, PaintColors.AREA_TEXT.get(), true);
        }

        if (color != null)
            return new AreaElemStyle(c, color, fillImage, extent, text);
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
            painter.drawArea((Way) osm, myColor, fillImage, extent, painter.isInactiveMode() || osm.isDisabled(), text);
        } else if (osm instanceof Relation) {
            if (color != null && (selected || outermember)) {
                myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
            }
            painter.drawArea((Relation) osm, myColor, fillImage, extent, painter.isInactiveMode() || osm.isDisabled(), text);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        AreaElemStyle other = (AreaElemStyle) obj;
        // we should get the same image object due to caching
        if (!Objects.equals(fillImage, other.fillImage))
            return false;
        if (!Objects.equals(color, other.color))
            return false;
        if (extent != other.extent)
            return false;
        if (!Objects.equals(text, other.text))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + color.hashCode();
        hash = 61 * hash + (extent != null ? Float.floatToIntBits(extent) : 0);
        hash = 61 * hash + (fillImage != null ? fillImage.hashCode() : 0);
        hash = 61 * hash + (text != null ? text.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "AreaElemStyle{" + super.toString() + "color=" + Utils.toString(color) +
                " fillImage=[" + fillImage + "]}";
    }
}
