// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * A property containing an {@code String} value.
 */
public class StringProperty extends AbstractProperty<String> {

    /**
     * Constructs a new {@code StringProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public StringProperty(String key, String defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public String get() {
        return Main.pref.get(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(String value) {
        return Main.pref.put(getKey(), value);
    }
}
