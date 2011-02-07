// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

abstract public class ElemStyle {

    public float z_index;
    public float object_z_index;
    public boolean isModifier;  // false, if style can serve as main style for the
                                // primitive; true, if it is a highlight or modifier

    public ElemStyle(float z_index, float object_z_index, boolean isModifier) {
        this.z_index = z_index;
        this.object_z_index = object_z_index;
        this.isModifier = isModifier;
    }

    protected ElemStyle(Cascade c) {
        z_index = c.get("z-index", 0f, Float.class);
        object_z_index = c.get("object-z-index", 0f, Float.class);
        isModifier = c.get("modifier", false, Boolean.class);
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ElemStyle))
            return false;
        ElemStyle s = (ElemStyle) o;
        return z_index == s.z_index && object_z_index == s.object_z_index && isModifier == s.isModifier;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Float.floatToIntBits(this.z_index);
        hash = 41 * hash + Float.floatToIntBits(this.object_z_index);
        hash = 41 * hash + (isModifier ? 1 : 0);
        return hash;
    }

    @Override
    public String toString() {
        if (z_index != 0f || object_z_index != 0f)
            return String.format("z_idx=%s/%s ", z_index, object_z_index) + (isModifier ? "modifier " : "");
        return "";
    }
}
