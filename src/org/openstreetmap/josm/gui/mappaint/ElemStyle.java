// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

abstract public class ElemStyle {
    // zoom range to display the feature
    public long minScale;
    public long maxScale;

    public ElemStyle(long minScale, long maxScale) {
        this.minScale = minScale;
        this.maxScale = maxScale;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ElemStyle))
            return false;
        ElemStyle s = (ElemStyle) o;
        return minScale == s.minScale && maxScale == s.maxScale;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member);
}
