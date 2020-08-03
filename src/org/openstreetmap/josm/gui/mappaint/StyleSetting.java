// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.awt.Color;
import java.util.Objects;
import java.util.Optional;

import javax.swing.Icon;

import org.openstreetmap.josm.data.preferences.AbstractToStringProperty;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.ImageProvider.ImageSizes;
import org.openstreetmap.josm.tools.Logging;

/**
 * Setting to customize a MapPaint style.
 *
 * Can be changed by the user in the right click menu of the mappaint style
 * dialog.
 *
 * Defined in the MapCSS style, e.g.
 * <pre>
 * setting::highway_casing {
 *   type: boolean;
 *   label: tr("Draw highway casing");
 *   default: true;
 * }
 *
 * way[highway][setting("highway_casing")] {
 *   casing-width: 2;
 *   casing-color: white;
 * }
 * </pre>
 */
public interface StyleSetting {

    /**
     * gets the value for this setting
     * @return The value the user selected
     */
    Object getValue();

    /**
     * Create a matching {@link StyleSettingGui} instances for a given {@link StyleSetting} object.
     * @return matching {@code StyleSettingGui}
     * @throws UnsupportedOperationException when class of {@link StyleSetting} is not supported
     */
    default StyleSettingGui getStyleSettingGui() {
        throw new UnsupportedOperationException(getClass() + " not supported");
    }

    /**
     * Superclass of style settings and groups.
     * @since 15289
     */
    abstract class LabeledStyleSetting implements Comparable<LabeledStyleSetting> {
        public final StyleSource parentStyle;
        public final String label;

        LabeledStyleSetting(StyleSource parentStyle, String label) {
            this.parentStyle = Objects.requireNonNull(parentStyle);
            this.label = Objects.requireNonNull(label);
        }

        @Override
        public int hashCode() {
            return Objects.hash(label, parentStyle);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null || getClass() != obj.getClass())
                return false;
            LabeledStyleSetting other = (LabeledStyleSetting) obj;
            return Objects.equals(label, other.label) && Objects.equals(parentStyle, other.parentStyle);
        }

        @Override
        public int compareTo(LabeledStyleSetting o) {
            return label.compareTo(o.label);
        }
    }

    /**
     * A style setting group.
     * @since 15289
     */
    class StyleSettingGroup extends LabeledStyleSetting {
        /** group identifier */
        public final String key;
        /** group icon (optional) */
        public final Icon icon;

        StyleSettingGroup(StyleSource parentStyle, String label, String key, Icon icon) {
            super(parentStyle, label);
            this.key = Objects.requireNonNull(key);
            this.icon = icon;
        }

        /**
         * Creates a new {@code StyleSettingGroup}.
         * @param c cascade
         * @param parentStyle parent style source
         * @param key group identifier
         * @return newly created {@code StyleSettingGroup}
         */
        public static StyleSettingGroup create(Cascade c, StyleSource parentStyle, String key) {
            String label = c.get("label", null, String.class);
            if (label == null) {
                Logging.warn("property 'label' required for StyleSettingGroup");
                return null;
            }
            Icon icon = Optional.ofNullable(c.get("icon", null, String.class))
                    .map(s -> ImageProvider.get(s, ImageSizes.MENU)).orElse(null);
            return new StyleSettingGroup(parentStyle, label, key, icon);
        }
    }

    class PropertyStyleSetting<T> extends LabeledStyleSetting implements StyleSetting {
        private final Class<T> type;
        private final AbstractToStringProperty<T> property;

        PropertyStyleSetting(StyleSource parentStyle, String label, Class<T> type, AbstractToStringProperty<T> property) {
            super(parentStyle, label);
            this.type = type;
            this.property = property;
        }

        /**
         * Replies the property key.
         * @return The property key
         */
        public String getKey() {
            return property.getKey();
        }

        @Override
        public T getValue() {
            return property.get();
        }

        /**
         * Sets this property to the specified value.
         * @param value The new value of this property
         */
        public void setValue(T value) {
            property.put(value);
        }

        /**
         * Sets this property to the specified string value.
         * @param value The new string value of this property
         */
        public void setStringValue(String value) {
            setValue(Cascade.convertTo(value, type));
        }

        @Override
        public StyleSettingGui getStyleSettingGui() {
            return new PropertyStyleSettingGui<>(this);
        }
    }

    /**
     * A style setting for boolean value (yes / no).
     */
    class BooleanStyleSetting extends PropertyStyleSetting<Boolean> {
        BooleanStyleSetting(StyleSource parentStyle, String label, AbstractToStringProperty<Boolean> property) {
            super(parentStyle, label, Boolean.class, property);
        }

        @Override
        public StyleSettingGui getStyleSettingGui() {
            return new BooleanStyleSettingGui(this);
        }
    }

    /**
     * A style setting for color values.
     * @since 16842
     */
    class ColorStyleSetting extends PropertyStyleSetting<Color> {
        ColorStyleSetting(StyleSource parentStyle, String label, AbstractToStringProperty<Color> property) {
            super(parentStyle, label, Color.class, property);
        }

        @Override
        public StyleSettingGui getStyleSettingGui() {
            return new ColorStyleSettingGui(this);
        }
    }
}
