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

public enum PaintColors {

    INACTIVE(marktr("inactive"), Color.darkGray),
    SELECTED(marktr("selected"), Color.red),
    RELATIONSELECTED(marktr("Relation: selected"), Color.magenta),
    NODE(marktr("Node: standard"), Color.yellow),
    CONNECTION(marktr("Node: connection"), Color.yellow),
    TAGGED(marktr("Node: tagged"), new Color(204, 255, 255)), // light cyan
    DEFAULT_WAY(marktr("way"), new Color(0, 0, 128)), // dark blue
    RELATION(marktr("relation"), new Color(0, 128, 128)), // teal
    UNTAGGED_WAY(marktr("untagged way"), new Color(0, 128, 0)), // dark green
    BACKGROUND(marktr("background"), Color.BLACK),
    HIGHLIGHT(marktr("highlight"), SELECTED.get()),
    HIGHLIGHT_WIREFRAME(marktr("highlight wireframe"), Color.orange),

    UNTAGGED(marktr("untagged"), Color.GRAY),
    TEXT(marktr("text"), Color.WHITE),
    AREA_TEXT(marktr("areatext"), Color.LIGHT_GRAY);

    private final String name;
    private final Color defaultColor;
    private final ColorProperty baseProperty;
    private final CachingProperty<Color> property;

    private static volatile Color backgroundColorCache;

    private static final MapPaintSylesUpdateListener styleOverrideListener = new MapPaintSylesUpdateListener() {
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
        MapPaintStyles.addMapPaintSylesUpdateListener(styleOverrideListener);
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
