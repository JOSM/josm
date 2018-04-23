// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.styleelement;

import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.paint.MapPaintSettings;
import org.openstreetmap.josm.data.osm.visitor.paint.StyledMapRenderer;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Keyword;
import org.openstreetmap.josm.gui.mappaint.StyleKeys;
import org.openstreetmap.josm.gui.mappaint.mapcss.Instruction.RelativeFloat;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Class that defines how objects ({@link OsmPrimitive}) should be drawn on the map.
 *
 * Several subclasses of this abstract class implement different drawing features,
 * like icons for a node or area fill. This class and all its subclasses are immutable
 * and tend to get shared when multiple objects have the same style (in order to
 * save memory, see {@link org.openstreetmap.josm.gui.mappaint.StyleCache#intern()}).
 */
public abstract class StyleElement implements StyleKeys {

    protected static final int ICON_IMAGE_IDX = 0;
    protected static final int ICON_WIDTH_IDX = 1;
    protected static final int ICON_HEIGHT_IDX = 2;
    protected static final int ICON_OPACITY_IDX = 3;
    protected static final int ICON_OFFSET_X_IDX = 4;
    protected static final int ICON_OFFSET_Y_IDX = 5;

    /**
     * The major z index of this style element
     */
    public float majorZIndex;
    /**
     * The z index as set by the user
     */
    public float zIndex;
    /**
     * The object z index
     */
    public float objectZIndex;
    /**
     * false, if style can serve as main style for the primitive;
     * true, if it is a highlight or modifier
     */
    public boolean isModifier;
    /**
     * A flag indicating that the selection color handling should be done automatically
     */
    public boolean defaultSelectedHandling;

    /**
     * Construct a new StyleElement
     * @param majorZindex like z-index, but higher priority
     * @param zIndex order the objects are drawn
     * @param objectZindex like z-index, but lower priority
     * @param isModifier if false, a default line or node symbol is generated
     * @param defaultSelectedHandling true if default behavior for selected objects
     * is enabled, false if a style for selected state is given explicitly
     */
    public StyleElement(float majorZindex, float zIndex, float objectZindex, boolean isModifier, boolean defaultSelectedHandling) {
        this.majorZIndex = majorZindex;
        this.zIndex = zIndex;
        this.objectZIndex = objectZindex;
        this.isModifier = isModifier;
        this.defaultSelectedHandling = defaultSelectedHandling;
    }

    protected StyleElement(Cascade c, float defaultMajorZindex) {
        majorZIndex = c.get(MAJOR_Z_INDEX, defaultMajorZindex, Float.class);
        zIndex = c.get(Z_INDEX, 0f, Float.class);
        objectZIndex = c.get(OBJECT_Z_INDEX, 0f, Float.class);
        isModifier = c.get(MODIFIER, Boolean.FALSE, Boolean.class);
        defaultSelectedHandling = c.isDefaultSelectedHandling();
    }

    /**
     * draws a primitive
     * @param primitive primitive to draw
     * @param paintSettings paint settings
     * @param painter painter
     * @param selected true, if primitive is selected
     * @param outermember true, if primitive is not selected and outer member of a selected multipolygon relation
     * @param member true, if primitive is not selected and member of a selected relation
     * @since 13662 (signature)
     */
    public abstract void paintPrimitive(IPrimitive primitive, MapPaintSettings paintSettings, StyledMapRenderer painter,
            boolean selected, boolean outermember, boolean member);

    /**
     * Check if this is a style that makes the line visible to the user
     * @return <code>true</code> for line styles
     */
    public boolean isProperLineStyle() {
        return false;
    }

    /**
     * Get a property value of type Width
     * @param c the cascade
     * @param key property key for the width value
     * @param relativeTo reference width. Only needed, when relative width syntax is used, e.g. "+4".
     * @return width
     */
    protected static Float getWidth(Cascade c, String key, Float relativeTo) {
        Float width = c.get(key, null, Float.class, true);
        if (width != null) {
            if (width > 0)
                return width;
        } else {
            Keyword widthKW = c.get(key, null, Keyword.class, true);
            if (Keyword.THINNEST.equals(widthKW))
                return 0f;
            if (Keyword.DEFAULT.equals(widthKW))
                return (float) MapPaintSettings.INSTANCE.getDefaultSegmentWidth();
            if (relativeTo != null) {
                RelativeFloat widthRel = c.get(key, null, RelativeFloat.class, true);
                if (widthRel != null)
                    return relativeTo + widthRel.val;
            }
        }
        return null;
    }

    /* ------------------------------------------------------------------------------- */
    /* cached values                                                                   */
    /* ------------------------------------------------------------------------------- */
    /*
     * Two preference values and the set of created fonts are cached in order to avoid
     * expensive lookups and to avoid too many font objects
     *
     * FIXME: cached preference values are not updated if the user changes them during
     * a JOSM session. Should have a listener listening to preference changes.
     */
    private static volatile String defaultFontName;
    private static volatile Float defaultFontSize;
    private static final Object lock = new Object();

    // thread save access (double-checked locking)
    private static Float getDefaultFontSize() {
        Float s = defaultFontSize;
        if (s == null) {
            synchronized (lock) {
                s = defaultFontSize;
                if (s == null) {
                    defaultFontSize = s = (float) Config.getPref().getInt("mappaint.fontsize", 8);
                }
            }
        }
        return s;
    }

    private static String getDefaultFontName() {
        String n = defaultFontName;
        if (n == null) {
            synchronized (lock) {
                n = defaultFontName;
                if (n == null) {
                    defaultFontName = n = Config.getPref().get("mappaint.font", "Droid Sans");
                }
            }
        }
        return n;
    }

    private static class FontDescriptor {
        public String name;
        public int style;
        public int size;

        FontDescriptor(String name, int style, int size) {
            this.name = name;
            this.style = style;
            this.size = size;
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, style, size);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            FontDescriptor that = (FontDescriptor) obj;
            return style == that.style &&
                    size == that.size &&
                    Objects.equals(name, that.name);
        }
    }

    private static final Map<FontDescriptor, Font> FONT_MAP = new HashMap<>();

    private static Font getCachedFont(FontDescriptor fd) {
        Font f = FONT_MAP.get(fd);
        if (f != null) return f;
        f = new Font(fd.name, fd.style, fd.size);
        FONT_MAP.put(fd, f);
        return f;
    }

    private static Font getCachedFont(String name, int style, int size) {
        return getCachedFont(new FontDescriptor(name, style, size));
    }

    protected static Font getFont(Cascade c, String s) {
        String name = c.get(FONT_FAMILY, getDefaultFontName(), String.class);
        float size = c.get(FONT_SIZE, getDefaultFontSize(), Float.class);
        int weight = Font.PLAIN;
        if ("bold".equalsIgnoreCase(c.get(FONT_WEIGHT, null, String.class))) {
            weight = Font.BOLD;
        }
        int style = Font.PLAIN;
        if ("italic".equalsIgnoreCase(c.get(FONT_STYLE, null, String.class))) {
            style = Font.ITALIC;
        }
        Font f = getCachedFont(name, style | weight, Math.round(size));
        if (f.canDisplayUpTo(s) == -1)
            return f;
        else {
            // fallback if the string contains characters that cannot be
            // rendered by the selected font
            return getCachedFont("SansSerif", style | weight, Math.round(size));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StyleElement that = (StyleElement) o;
        return isModifier == that.isModifier &&
               Float.compare(that.majorZIndex, majorZIndex) == 0 &&
               Float.compare(that.zIndex, zIndex) == 0 &&
               Float.compare(that.objectZIndex, objectZIndex) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(majorZIndex, zIndex, objectZIndex, isModifier);
    }

    @Override
    public String toString() {
        return String.format("z_idx=[%s/%s/%s] ", majorZIndex, zIndex, objectZIndex) + (isModifier ? "modifier " : "");
    }
}
