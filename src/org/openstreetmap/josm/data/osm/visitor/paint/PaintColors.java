// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor.paint;

import static org.openstreetmap.josm.tools.I18n.marktr;

import java.awt.Color;
import java.util.List;
import java.util.Optional;

import org.openstreetmap.josm.data.preferences.CachingProperty;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles.MapPaintSylesUpdateListener;
import org.openstreetmap.josm.gui.mappaint.StyleSource;

/**
 * The colors used to paint the map, especially with the wireframe renderer
 * <p>
 * This enum stores the colors to be set in the preferences
 */
public enum PaintColors {

    /**
     * Inactive objects
     */
    INACTIVE(marktr("inactive"), Color.darkGray),
    /**
     * Currently selected objects
     */
    SELECTED(marktr("selected"), Color.red),
    /**
     * Objects that are part of a selected relation
     */
    RELATIONSELECTED(marktr("Relation: selected"), Color.magenta),
    /**
     * Normal nodes
     */
    NODE(marktr("Node: standard"), Color.yellow),
    /**
     * Connected nodes
     */
    CONNECTION(marktr("Node: connection"), Color.yellow),
    /**
     * A tagged node
     */
    TAGGED(marktr("Node: tagged"), new Color(204, 255, 255)), // light cyan
    /**
     * Default way color
     */
    DEFAULT_WAY(marktr("way"), new Color(0, 0, 128)), // dark blue
    /**
     * Relation color
     */
    RELATION(marktr("relation"), new Color(0, 128, 128)), // teal
    /**
     * Color for untagged way
     */
    UNTAGGED_WAY(marktr("untagged way"), new Color(0, 128, 0)), // dark green
    /**
     * Background of the map
     */
    BACKGROUND(marktr("background"), Color.BLACK),
    /**
     * Highlight around a selected node/way, MapCSS renderer
     */
    HIGHLIGHT(marktr("highlight"), SELECTED.get()),
    /**
     * Highlight around a selected node/way, Wireframe renderer
     */
    HIGHLIGHT_WIREFRAME(marktr("highlight wireframe"), Color.orange),

    /**
     * Untagged way
     */
    UNTAGGED(marktr("untagged"), Color.GRAY),
    /**
     * Default text color
     */
    TEXT(marktr("text"), Color.WHITE),
    /**
     * Default text color for areas
     */
    AREA_TEXT(marktr("areatext"), Color.LIGHT_GRAY);

    /**
     * The name of the color
     */
    private final String name;
    private final Color defaultColor;
    private final ColorProperty baseProperty;
    private final CachingProperty<Color> property;

    private static volatile Color backgroundColorCache;

    private static final MapPaintSylesUpdateListener STYLE_OVERRIDE_LISTENER = new MapPaintSylesUpdateListener() {
        //TODO: Listen to wireframe map mode changes.
        @Override
        public void mapPaintStylesUpdated() {
            backgroundColorCache = null;
        }

        @Override
        public void mapPaintStyleEntryUpdated(int idx) {
            mapPaintStylesUpdated();
        }
    };

    static {
        MapPaintStyles.addMapPaintSylesUpdateListener(STYLE_OVERRIDE_LISTENER);
    }

    PaintColors(String name, Color defaultColor) {
        baseProperty = new ColorProperty(name, defaultColor);
        property = baseProperty.cached();
        this.name = name;
        this.defaultColor = defaultColor;
    }

    /**
     * Gets the default value for this color.
     * @return The default value
     */
    public Color getDefaultValue() {
        return property.getDefaultValue();
    }

    /**
     * Get the given color
     * @return The color
     */
    public Color get() {
        return property.get();
    }

    /**
     * Returns the background color.
     * @return the background color
     */
    public static Color getBackgroundColor() {
        if (backgroundColorCache != null)
            return backgroundColorCache;
        List<StyleSource> sources = MapPaintStyles.getStyles().getStyleSources();
        for (StyleSource s : sources) {
            if (!s.active) {
                continue;
            }
            Color backgroundColorOverride = s.getBackgroundColorOverride();
            if (backgroundColorOverride != null) {
                backgroundColorCache = backgroundColorOverride;
            }
        }
        return Optional.ofNullable(backgroundColorCache).orElseGet(BACKGROUND::get);
    }

    /**
     * Get the color property
     * @return The property that is used to access the color.
     * @since 10874
     */
    public ColorProperty getProperty() {
        return baseProperty;
    }
}
