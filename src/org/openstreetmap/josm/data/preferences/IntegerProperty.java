// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

public class IntegerProperty {

    private final String key;
    private final int defaultValue;

    public IntegerProperty(String key, int defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public int get() {
        return Main.pref.getInteger(getKey(), getDefaultValue());
    }

    public boolean put(int value) {
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

    public String getKey() {
        return key;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

}
