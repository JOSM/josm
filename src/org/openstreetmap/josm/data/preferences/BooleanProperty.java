// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

public class BooleanProperty {

    private final String key;
    private final boolean defaultValue;

    public BooleanProperty(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public boolean get() {
        return Main.pref.getBoolean(key, defaultValue);
    }

    public boolean put(boolean value) {
        return Main.pref.put(key, value);
    }

}
