// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * A property containing an {@code Double} value.
 * @since 3246
 */
public class DoubleProperty extends AbstractProperty<Double> {

    /**
     * Constructs a new {@code DoubleProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public DoubleProperty(String key, double defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Double get() {
        return Main.pref.getDouble(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(Double value) {
        return Main.pref.putDouble(getKey(), value);
    }

    /**
     * parses and saves a double precision value
     * @param value the value to be parsed
     * @return true - preference value has changed
     *         false - parsing failed or preference value has not changed
     */
    public boolean parseAndPut(String value) {
        try {
            return put(Double.valueOf(value));
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
