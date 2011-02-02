// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;

abstract public class ElemStyle {
    
    public float z_index;
    public float object_z_index;

    public ElemStyle(float z_index, float object_z_index) {
        this.z_index = z_index;
        this.object_z_index = object_z_index;
    }

    protected ElemStyle(Cascade c) {
        z_index = c.getFloat("z-index", 0f);
        object_z_index = c.getFloat("object-z-index", 0f);
    }

    public abstract void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, MapPainter painter, boolean selected, boolean member);

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ElemStyle))
            return false;
        ElemStyle s = (ElemStyle) o;
        return z_index == s.z_index && object_z_index == s.object_z_index;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 41 * hash + Float.floatToIntBits(this.z_index);
        hash = 41 * hash + Float.floatToIntBits(this.object_z_index);
        return hash;
    }

    @Override
    public String toString() {
        if (z_index != 0f || object_z_index != 0f)
            return String.format("z_idx=%s/%s ", z_index, object_z_index);
        return "";
    }
}
