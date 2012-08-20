// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * A property containing an {@code Integer} value.
 */
public class IntegerProperty extends AbstractProperty<Integer> {

    /**
     * Constructs a new {@code IntegerProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public IntegerProperty(String key, int defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Integer get() {
        return Main.pref.getInteger(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(Integer value) {
        return Main.pref.putInteger(getKey(), value);
    }

    /**
     * parses and saves an integer value
     * @param value the value to be parsed
     * @return true - preference value has changed
     *         false - parsing failed or preference value has not changed
     */
    public boolean parseAndPut(String value) {
        Integer intVal;
        try {
            intVal = Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return false;
        }
        return put(intVal);
    }
}
