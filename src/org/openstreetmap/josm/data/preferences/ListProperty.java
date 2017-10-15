// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.List;

/**
 * A property containing a {@code List} of {@code String} as value.
 */
public class ListProperty extends AbstractProperty<List<String>> {

    /**
     * Constructs a new {@code CollectionProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public ListProperty(String key, List<String> defaultValue) {
        super(key, defaultValue);
        if (getPreferences() != null) {
            get();
        }
    }

    @Override
    public List<String> get() {
        return getPreferences().getList(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(List<String> value) {
        return getPreferences().putList(getKey(), value);
    }
}
