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

    public String getKey() {
        return key;
    }

    public int getDefaultValue() {
        return defaultValue;
    }

}
