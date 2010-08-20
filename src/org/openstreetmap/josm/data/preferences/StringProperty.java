// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

public class StringProperty {

    private final String key;
    private final String defaultValue;

    public StringProperty(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String get() {
        return Main.pref.get(getKey(), getDefaultValue());
    }

    public boolean put(String value) {
        return Main.pref.put(getKey(), value);
    }

    public String getKey() {
        return key;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

}
