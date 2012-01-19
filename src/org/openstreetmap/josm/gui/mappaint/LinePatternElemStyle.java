// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Image;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;

/**
 * similar to mapnik's LinePatternSymbolizer
 */
public class LinePatternElemStyle extends ElemStyle {

    public MapImage<Image> pattern;

    public LinePatternElemStyle(Cascade c, MapImage<Image> pattern) {
        super(c, -1f);
        this.pattern = pattern;
    }

    public static LinePatternElemStyle create(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);

        IconReference iconRef = c.get("pattern-image", null, IconReference.class);
        if (iconRef == null)
            return null;
        ImageIcon icon = MapPaintStyles.getIcon(iconRef, -1, -1);
        if (icon == null)
            return null;
        MapImage<Image> pattern = new MapImage<Image>(iconRef.iconName, iconRef.source);
        pattern.img = icon.getImage();
        return new LinePatternElemStyle(c, pattern);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        Way w = (Way)primitive;
        painter.drawLinePattern(w, pattern.img);
    }

    @Override
    public boolean isProperLineStyle() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        final LinePatternElemStyle other = (LinePatternElemStyle) obj;
        return pattern.equals(other.pattern);
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return "LinePatternElemStyle{" + super.toString() + "pattern=[" + pattern + "]}";
    }
}
