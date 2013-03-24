// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.IconReference;

/**
 * Similar to mapnik's LinePatternSymbolizer.
 *
 * @deprecated superseded by #{@link RepeatImageElemStyle}
 */
@Deprecated
public class LinePatternElemStyle extends ElemStyle {

    public MapImage pattern;

    public LinePatternElemStyle(Cascade c, MapImage pattern) {
        super(c, 2.9f);
        this.pattern = pattern;
    }

    public static LinePatternElemStyle create(Environment env) {
        Cascade c = env.mc.getCascade(env.layer);

        IconReference iconRef = c.get("pattern-image", null, IconReference.class);
        if (iconRef == null)
            return null;
        MapImage pattern = new MapImage(iconRef.iconName, iconRef.source);
        return new LinePatternElemStyle(c, pattern);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter, boolean selected, boolean member) {
        Way w = (Way)primitive;
        painter.drawLinePattern(w, pattern.getImage());
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
