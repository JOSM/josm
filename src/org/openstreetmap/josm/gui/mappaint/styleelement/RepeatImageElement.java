// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.tools.CheckParameterUtil;

public class RepeatImageElement extends StyleElement {

    /**
     * The side on which the image should be aligned to the line.
     */
    public enum LineImageAlignment {
        TOP(.5),
        CENTER(0),
        BOTTOM(-.5);

        private final double alignmentOffset;

        LineImageAlignment(double alignmentOffset) {
            this.alignmentOffset = alignmentOffset;
        }

        /**
         * Gets the alignment offset.
         * @return The offset relative to the image height compared to placing the image in the middle of the line.
         */
        public double getAlignmentOffset() {
            return alignmentOffset;
        }
    }

    public MapImage pattern;
    public float offset;
    public float spacing;
    public float phase;
    public LineImageAlignment align;

    private static final String[] REPEAT_IMAGE_KEYS = {REPEAT_IMAGE, REPEAT_IMAGE_WIDTH, REPEAT_IMAGE_HEIGHT, REPEAT_IMAGE_OPACITY,
            null, null};

    public RepeatImageElement(Cascade c, MapImage pattern, float offset, float spacing, float phase, LineImageAlignment align) {
        super(c, 2.9f);
        CheckParameterUtil.ensureParameterNotNull(pattern);
        CheckParameterUtil.ensureParameterNotNull(align);
        this.pattern = pattern;
        this.offset = offset;
        this.spacing = spacing;
        this.phase = phase;
        this.align = align;
    }

    public static RepeatImageElement create(Environment env) {
        MapImage pattern = NodeElement.createIcon(env, REPEAT_IMAGE_KEYS);
        if (pattern == null)
            return null;
        Cascade c = env.mc.getCascade(env.layer);
        float offset = c.get(REPEAT_IMAGE_OFFSET, 0f, Float.class);
        float spacing = c.get(REPEAT_IMAGE_SPACING, 0f, Float.class);
        float phase = -c.get(REPEAT_IMAGE_PHASE, 0f, Float.class);

        LineImageAlignment align = LineImageAlignment.CENTER;
        Keyword alignKW = c.get(REPEAT_IMAGE_ALIGN, Keyword.CENTER, Keyword.class);
        if ("top".equals(alignKW.val)) {
            align = LineImageAlignment.TOP;
        } else if ("bottom".equals(alignKW.val)) {
            align = LineImageAlignment.BOTTOM;
        }

        return new RepeatImageElement(c, pattern, offset, spacing, phase, align);
    }

    @Override
    public void paintPrimitive(OsmPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        Way w = (Way) primitive;
        painter.drawRepeatImage(w, pattern, painter.isInactiveMode() || w.isDisabled(), offset, spacing, phase, align);
    }

    @Override
    public boolean isProperLineStyle() {
        return true;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        RepeatImageElement that = (RepeatImageElement) obj;
        return Float.compare(that.offset, offset) == 0 &&
                Float.compare(that.spacing, spacing) == 0 &&
                Float.compare(that.phase, phase) == 0 &&
                Objects.equals(pattern, that.pattern) &&
                align == that.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pattern, offset, spacing, phase, align);
    }

    @Override
    public String toString() {
        return "RepeatImageStyle{" + super.toString() + "pattern=[" + pattern +
                "], offset=" + offset + ", spacing=" + spacing +
                ", phase=" + (-phase) + ", align=" + align + '}';
    }
}
