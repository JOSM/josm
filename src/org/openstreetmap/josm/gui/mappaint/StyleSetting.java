// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint;

import org.openstreetmap.josm.spi.preferences.Config;
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
     * A style setting for boolean value (yes / no).
     */
    class BooleanStyleSetting implements StyleSetting {
        public final StyleSource parentStyle;
        public final String prefKey;
        public final String label;
        public final boolean def;

        public BooleanStyleSetting(StyleSource parentStyle, String prefKey, String label, boolean def) {
            this.parentStyle = parentStyle;
            this.prefKey = prefKey;
            this.label = label;
            this.def = def;
        }

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
