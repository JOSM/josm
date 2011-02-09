// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

public class AreaElemStyle extends ElemStyle
{
    /**
     * If fillImage == null, color is the fill-color, otherwise
     * an arbitrary color value sampled from the fillImage
     */
    public Color color;
    public BufferedImage fillImage;
    public float fillImageAlpha;

    protected AreaElemStyle(Cascade c, Color color, BufferedImage fillImage, float fillImageAlpha) {
        super(c);
        CheckParameterUtil.ensureParameterNotNull(color);
        this.color = color;
        this.fillImage = fillImage;
        this.fillImageAlpha = fillImageAlpha;
    }

    public static AreaElemStyle create(Cascade c) {
        BufferedImage fillImage = null;
        Color color = null;
        float fillImageAlpha = 1f;

        IconReference iconRef = c.get("fill-image", null, IconReference.class);
        if (iconRef != null) {
            ImageIcon icon = MapPaintStyles.getIcon(iconRef, false);
            if (icon != null) {
                if (!(icon.getImage() instanceof BufferedImage)) {
                    icon = MapPaintStyles.getIcon(iconRef, true);
                }
                if (!(icon.getImage() instanceof BufferedImage))
                    throw new RuntimeException();
                fillImage = (BufferedImage) icon.getImage();

                color = new Color(fillImage.getRGB(fillImage.getWidth() / 2, fillImage.getHeight() / 2));

                fillImageAlpha = Utils.color_int2float(Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fill-image-alpha", 255)))));
                Float pAlpha = c.get("fill-opacity", null, Float.class);
                if (pAlpha != null) {
                    if (pAlpha < 0f || pAlpha > 1f) {
                        pAlpha= 1f;
                    }
                    fillImageAlpha = pAlpha;
                }
            }
        } else {
            color = c.get("fill-color", null, Color.class);
            if (color != null) {
                int alpha = Math.min(255, Math.max(0, Integer.valueOf(Main.pref.getInteger("mappaint.fillalpha", 50))));
                Integer pAlpha = Utils.color_float2int(c.get("fill-opacity", null, float.class));
                if (pAlpha != null) {
                    alpha = pAlpha;
                }
                color = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
            }
        }
        
        if (color != null)
            return new AreaElemStyle(c, color, fillImage, fillImageAlpha);
        else
            return null;
    }

    @Override
    public void paintPrimitive(OsmPrimitive osm, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        if (osm instanceof Way)
        {
            Color myColor = color;
            if (color != null) {
                if (osm.isSelected()) {
                    myColor = paintSettings.getSelectedColor(color.getAlpha());
                }
            }
            painter.drawArea((Way) osm, myColor, fillImage, fillImageAlpha,
                    painter.isShowNames() ? painter.getAreaName(osm) : null);
        } else if (osm instanceof Relation)
        {
            Color myColor = color;
            if (color != null) {
                if (selected) {
                    myColor = paintSettings.getRelationSelectedColor(color.getAlpha());
                }
            }
            painter.drawArea((Relation) osm, myColor, fillImage, fillImageAlpha,
                    painter.getAreaName(osm));
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
        if (fillImage != other.fillImage)
            return false;
        if (!Utils.equal(color, other.color))
            return false;
        if (fillImageAlpha != other.fillImageAlpha)
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 61 * hash + color.hashCode();
        hash = 61 * hash + (fillImage != null ? fillImage.hashCode() : 0);
        hash = 61 * hash + Float.floatToIntBits(fillImageAlpha);
        return hash;
    }

    @Override
    public String toString() {
        return "AreaElemStyle{" + super.toString() + "color=" + Utils.toString(color) +
                " fillImageAlpha=" + fillImageAlpha + " fillImage=[" + fillImage + "]}";
    }
}
