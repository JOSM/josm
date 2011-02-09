// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Font;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPainter;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;

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

    protected static Float getWidth(Cascade c, String key, Float relativeTo) {
        Float width = c.get(key, null, Float.class, true);
        if (width != null) {
            if (width == -1f)
                return (float) MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
            if (width > 0)
                return width;
        } else {
            String width_key = c.get(key, null, String.class, true);
            if (equal(width_key, "thinnest"))
                return 0f;
            else if (relativeTo != null) {
                RelativeFloat width_rel = c.get(key, null, RelativeFloat.class, true);
                if (width_rel != null)
                    return relativeTo + width_rel.val;
            }
        }
        return null;
    }

    protected static Font getFont(Cascade c) {
        String name = c.get("font-family", Main.pref.get("mappaint.font", "Helvetica"), String.class);
        float size = c.get("font-size", (float) Main.pref.getInteger("mappaint.fontsize", 8), Float.class);
        int weight = Font.PLAIN;
        String weightStr = c.get("font-wheight", null, String.class);
        if (equal(weightStr, "bold")) {
            weight = Font.BOLD;
        }
        int style = Font.PLAIN;
        String styleStr = c.get("font-style", null, String.class);
        if (equal(styleStr, "italic")) {
            style = Font.ITALIC;
        }
        return new Font(name, weight | style, Math.round(size));
    }

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
