// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

public class BooleanProperty extends AbstractProperty<Boolean> {

    protected final boolean defaultValue;

    public BooleanProperty(String key, boolean defaultValue) {
        super(key);
        this.defaultValue = defaultValue;
    }

    public boolean get() {
        return Main.pref.getBoolean(getKey(), defaultValue);
    }

    public boolean put(boolean value) {
        return Main.pref.put(getKey(), value);
    }

    @Override
    public Boolean getDefaultValue() {
        return defaultValue;
    }
}
