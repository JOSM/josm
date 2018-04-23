// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.util.Objects;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Style element that displays a repeated image pattern along a way.
 */
public class RepeatImageElement extends StyleElement {

    /**
     * The side on which the image should be aligned to the line.
     */
    public enum LineImageAlignment {
        /**
         * Align it to the top side of the line
         */
        TOP(.5),
        /**
         * Align it to the center of the line
         */
        CENTER(0),
        /**
         * Align it to the bottom of the line
         */
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

    /**
     * The image to draw on the line repeatedly
     */
    public MapImage pattern;
    /**
     * The offset to the side of the way
     */
    public float offset;
    /**
     * The space between the images
     */
    public float spacing;
    /**
     * The offset of the first image along the way
     */
    public float phase;
    /**
     * The alignment of the image
     */
    public LineImageAlignment align;

    private static final String[] REPEAT_IMAGE_KEYS = {REPEAT_IMAGE, REPEAT_IMAGE_WIDTH, REPEAT_IMAGE_HEIGHT, REPEAT_IMAGE_OPACITY,
            null, null};

    /**
     * Create a new image element
     * @param c The cascade
     * @param pattern The image to draw on the line repeatedly
     * @param offset The offset to the side of the way
     * @param spacing The space between the images
     * @param phase The offset of the first image along the way
     * @param align The alignment of the image
     */
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

    /**
     * Create a RepeatImageElement from the given environment
     * @param env The environment
     * @return The image style element or <code>null</code> if none should be painted
     */
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
    public void paintPrimitive(IPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member) {
        if (primitive instanceof Way) {
            Way w = (Way) primitive;
            painter.drawRepeatImage(w, pattern, painter.isInactiveMode() || w.isDisabled(), offset, spacing, phase, align);
        }
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
        return align == that.align &&
               Float.compare(that.offset, offset) == 0 &&
               Float.compare(that.spacing, spacing) == 0 &&
               Float.compare(that.phase, phase) == 0 &&
               Objects.equals(pattern, that.pattern);
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
