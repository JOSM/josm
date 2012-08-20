// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * A property containing a {@code Boolean} value.
 */
public class BooleanProperty extends AbstractProperty<Boolean> {

    /**
     * Constructs a new {@code BooleanProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public BooleanProperty(String key, boolean defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Boolean get() {
        return Main.pref.getBoolean(getKey(), defaultValue);
    }

    @Override
    public boolean put(Boolean value) {
        return Main.pref.put(getKey(), value);
    }
}
