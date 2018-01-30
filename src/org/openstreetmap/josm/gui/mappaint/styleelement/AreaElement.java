// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.HiDPISupport;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.bugreport.BugReport;

/**
 * This is the style that defines how an area is filled.
 */
public class AreaElement extends StyleElement {

    /**
     * The default opacity for the fill. For historical reasons in range 0.255.
     */
    private static final IntegerProperty DEFAULT_FILL_ALPHA = new IntegerProperty("mappaint.fillalpha", 50);

    /**
     * If fillImage == null, color is the fill-color, otherwise
     * an arbitrary color value sampled from the fillImage.
     *
     * The color may be fully transparent to indicate that the area should not be filled.
     */
    public Color color;

    /**
     * An image to cover this area. May be null to disable this feature.
     */
    public MapImage fillImage;

    /**
     * Fill the area only partially from the borders
     * <p>
     * Public access is discouraged.
     * @see StyledMapRenderer#drawArea
     */
    public Float extent;

    /**
     * Areas smaller than this are filled no matter what value {@link #extent} has.
     * <p>
     * Public access is discouraged.
     * @see StyledMapRenderer#drawArea
     */
    public Float extentThreshold;

    protected AreaElement(Cascade c, Color color, MapImage fillImage, Float extent, Float extentThreshold) {
        super(c, 1f);
        CheckParameterUtil.ensureParameterNotNull(color);
        this.color = color;
        this.fillImage = fillImage;
        this.extent = extent;
        this.extentThreshold = extentThreshold;
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
            Image img = fillImage.getImage(false);
            // get base image from possible multi-resolution image, so we can
            // cast to BufferedImage and get pixel value at the center of the image
            img = HiDPISupport.getBaseImage(img);
            try {
                color = new Color(((BufferedImage) img).getRGB(
                        img.getWidth(null) / 2, img.getHeight(null) / 2)
                );
            } catch (ArrayIndexOutOfBoundsException e) {
                throw BugReport.intercept(e).put("env.osm", env.osm).put("iconRef", iconRef).put("fillImage", fillImage).put("img", img);
            }

            fillImage.alpha = Utils.clamp(Config.getPref().getInt("mappaint.fill-image-alpha", 255), 0, 255);
            Integer pAlpha = Utils.colorFloat2int(c.get(FILL_OPACITY, null, float.class));
            if (pAlpha != null) {
                fillImage.alpha = pAlpha;
            }
        } else {
            color = c.get(FILL_COLOR, null, Color.class);
            if (color != null) {
                float defaultOpacity = Utils.colorInt2float(DEFAULT_FILL_ALPHA.get());
                float opacity = c.get(FILL_OPACITY, defaultOpacity, Float.class);
                color = Utils.alphaMultiply(color, opacity);
            }
        }

        if (color != null) {
            Float extent = c.get(FILL_EXTENT, null, float.class);
            Float extentThreshold = c.get(FILL_EXTENT_THRESHOLD, null, float.class);

            return new AreaElement(c, color, fillImage, extent, extentThreshold);
        } else {
            return null;
        }
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
            painter.drawArea((Way) osm, myColor, fillImage, extent, extentThreshold, painter.isInactiveMode() || osm.isDisabled());
        } else if (osm instanceof Relation) {
            if (color != null && (selected || outermember)) {
                myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
            }
            painter.drawArea((Relation) osm, myColor, fillImage, extent, extentThreshold, painter.isInactiveMode() || osm.isDisabled());
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
                Objects.equals(extent, that.extent) &&
                Objects.equals(extentThreshold, that.extentThreshold);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color, fillImage, extent, extentThreshold);
    }

    @Override
    public String toString() {
        return "AreaElemStyle{" + super.toString() + "color=" + Utils.toString(color) +
                " fillImage=[" + fillImage + "] extent=[" + extent + "] extentThreshold=[" + extentThreshold + "]}";
    }
}
