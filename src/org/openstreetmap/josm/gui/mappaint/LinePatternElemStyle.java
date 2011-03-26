// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

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

    public ImageIcon pattern;

    public LinePatternElemStyle(Cascade c, ImageIcon pattern) {
        super(c, -1f);
        this.pattern = pattern;
    }

    public static LinePatternElemStyle create(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);

        IconReference iconRef = c.get("pattern-image", null, IconReference.class);
        if (iconRef == null)
            return null;
        ImageIcon icon = MapPaintStyles.getIcon(iconRef, false);
        if (icon == null)
            return null;
        return new LinePatternElemStyle(c, icon);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member) {
        Way w = (Way)primitive;
        painter.drawLinePattern(w, pattern);
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
        return pattern.getImage() == other.pattern.getImage();
    }

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public String toString() {
        return "LinePatternElemStyle{" + super.toString() + "pattern=" + pattern + '}';
    }
}
