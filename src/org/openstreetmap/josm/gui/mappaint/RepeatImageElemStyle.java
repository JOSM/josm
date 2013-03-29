// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import static org.openstreetmap.josm.tools.Utils.equal;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class RepeatImageElemStyle extends ElemStyle implements StyleKeys {

    public enum LineImageAlignment { TOP, CENTER, BOTTOM }

    public MapImage pattern;
    public float offset;
    public float spacing;
    public float phase;
    public LineImageAlignment align;

    public RepeatImageElemStyle(Cascade c, MapImage pattern, float offset, float spacing, float phase, LineImageAlignment align) {
        super(c, 2.9f);
        CheckParameterUtil.ensureParameterNotNull(pattern);
        CheckParameterUtil.ensureParameterNotNull(align);
        this.pattern = pattern;
        this.offset = offset;
        this.spacing = spacing;
        this.phase = phase;
        this.align = align;
    }

    public static RepeatImageElemStyle create(Environment env) {
        MapImage pattern = NodeElemStyle.createIcon(env, REPEAT_IMAGE_KEYS);
        if (pattern == null)
            return null;
        Cascade c = env.mc.getCascade(env.layer);
        float offset = c.get(REPEAT_IMAGE_OFFSET, 0f, Float.class);
        float spacing = c.get(REPEAT_IMAGE_SPACING, 0f, Float.class);
        float phase = - c.get(REPEAT_IMAGE_PHASE, 0f, Float.class);

        LineImageAlignment align = LineImageAlignment.CENTER;
        Keyword alignKW = c.get(REPEAT_IMAGE_ALIGN, Keyword.CENTER, Keyword.class);
        if (equal(alignKW.val, "top")) {
            align = LineImageAlignment.TOP;
        } else if (equal(alignKW.val, "bottom")) {
            align = LineImageAlignment.BOTTOM;
        }

        return new RepeatImageElemStyle(c, pattern, offset, spacing, phase, align);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter, boolean selected, boolean member) {
        Way w = (Way) primitive;
        painter.drawRepeatImage(w, pattern.getImage(), offset, spacing, phase, align);
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
        final RepeatImageElemStyle other = (RepeatImageElemStyle) obj;
        if (!this.pattern.equals(other.pattern)) return false;
        if (this.offset != other.offset) return false;
        if (this.spacing != other.spacing) return false;
        if (this.phase != other.phase) return false;
        if (this.align != other.align) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + this.pattern.hashCode();
        hash = 83 * hash + Float.floatToIntBits(this.offset);
        hash = 83 * hash + Float.floatToIntBits(this.spacing);
        hash = 83 * hash + Float.floatToIntBits(this.phase);
        hash = 83 * hash + this.align.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "RepeatImageStyle{" + super.toString() + "pattern=[" + pattern +
                "], offset=" + offset + ", spacing=" + spacing +
                ", phase=" + (-phase) + ", align=" + align + "}";
    }
}
