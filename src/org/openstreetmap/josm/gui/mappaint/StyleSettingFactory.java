// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.function.BiFunction;

import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.preferences.StringProperty;
import org.openstreetmap.josm.tools.Logging;

/**
 * Factory to create matching {@link StyleSetting} instances.
 * @since 15731
 */
public final class StyleSettingFactory {

    private StyleSettingFactory() {
        // private constructor for factory classes
    }

    /**
     * Creates a new {@code StyleSetting} based on the specified type by {@code c}.
     * The type must be supported by {@link Cascade#convertTo} as well as {@link org.openstreetmap.josm.data.preferences.AbstractProperty}.
     * @param c cascade
     * @param parentStyle parent style source
     * @param key setting identifier
     * @return newly created {@code StyleSetting}
     */
    public static StyleSetting create(Cascade c, StyleSource parentStyle, String key) {
        final String type = c.get("type", null, String.class);
        final String qualifiedKey = String.join(":", parentStyle.url, type, key);
        switch (type) {
            case "boolean":
                return forLabelAndDefault(c, Boolean.class, (label, defaultValue) -> {
                    final BooleanProperty property = new BooleanProperty(qualifiedKey, defaultValue);
                    return new StyleSetting.BooleanStyleSetting(parentStyle, label, property);
                });
            case "double":
                return forLabelAndDefault(c, Double.class, (label, defaultValue) -> {
                    final DoubleProperty property = new DoubleProperty(qualifiedKey, defaultValue);
                    return new StyleSetting.PropertyStyleSetting<>(parentStyle, label, Double.class, property);
                });
            case "string":
                return forLabelAndDefault(c, String.class, (label, defaultValue) -> {
                    final StringProperty property = new StringProperty(qualifiedKey, defaultValue);
                    return new StyleSetting.PropertyStyleSetting<>(parentStyle, label, String.class, property);
                });
            case "color":
                return forLabelAndDefault(c, Color.class, (label, defaultValue) -> {
                    final NamedColorProperty property = new NamedColorProperty(NamedColorProperty.COLOR_CATEGORY_MAPPAINT,
                            parentStyle.title == null ? "MapCSS" : parentStyle.title, label, defaultValue);
                    return new StyleSetting.ColorStyleSetting(parentStyle, label, property);
                });
            default:
                Logging.warn("Unknown setting type {0} for style {1}", type, parentStyle.url);
                return null;
        }
    }

    private static <T> StyleSetting forLabelAndDefault(Cascade c, final Class<T> type, BiFunction<String, T, StyleSetting> function) {
        String label = c.get("label", null, String.class);
        if (label == null) {
            Logging.warn("property 'label' required for style setting of type " + type);
            return null;
        }
        T defaultValue = c.get("default", null, type);
        if (defaultValue == null) {
            Logging.warn("property 'default' required for style setting of type " + type);
            return null;
        }
        return function.apply(label, defaultValue);
    }
}
