// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import java.util.Objects;
import java.util.Optional;

import javax.swing.Icon;

import org.openstreetmap.josm.spi.preferences.Config;
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

        public StyleSettingGroup(StyleSource parentStyle, String label, String key, Icon icon) {
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
                Logging.warn("property 'label' required for boolean style setting");
                return null;
            }
            Icon icon = Optional.ofNullable(c.get("icon", null, String.class))
                    .map(s -> ImageProvider.get(s, ImageSizes.MENU)).orElse(null);
            return new StyleSettingGroup(parentStyle, label, key, icon);
        }
    }

    /**
     * A style setting for boolean value (yes / no).
     */
    class BooleanStyleSetting extends LabeledStyleSetting implements StyleSetting {
        public final String prefKey;
        public final boolean def;

        public BooleanStyleSetting(StyleSource parentStyle, String prefKey, String label, boolean def) {
            super(parentStyle, label);
            this.prefKey = Objects.requireNonNull(prefKey);
            this.def = def;
        }

        /**
         * Creates a new {@code BooleanStyleSetting}.
         * @param c cascade
         * @param parentStyle parent style source
         * @param key setting identifier
         * @return newly created {@code BooleanStyleSetting}
         */
        public static BooleanStyleSetting create(Cascade c, StyleSource parentStyle, String key) {
            String label = c.get("label", null, String.class);
            if (label == null) {
                Logging.warn("property 'label' required for boolean style setting");
                return null;
            }
            Boolean def = c.get("default", null, Boolean.class);
            if (def == null) {
                Logging.warn("property 'default' required for boolean style setting");
                return null;
            }
            String prefKey = parentStyle.url + ":boolean:" + key;
            return new BooleanStyleSetting(parentStyle, prefKey, label, def);
        }

        @Override
        public Object getValue() {
            String val = Config.getPref().get(prefKey, null);
            if (val == null) return def;
            return Boolean.valueOf(val);
        }

        public void setValue(Object o) {
            if (!(o instanceof Boolean)) {
                throw new IllegalArgumentException();
            }
            boolean b = (Boolean) o;
            if (b == def) {
                Config.getPref().put(prefKey, null);
            } else {
                Config.getPref().putBoolean(prefKey, b);
            }
        }
    }
}
