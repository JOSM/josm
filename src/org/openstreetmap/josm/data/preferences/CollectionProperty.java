// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collection;

import org.openstreetmap.josm.Main;

/**
 * A property containing a {@code Collection} of {@code String} as value.
 */
public class CollectionProperty extends AbstractProperty<Collection<String>> {

    /**
     * Constructs a new {@code CollectionProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public CollectionProperty(String key, Collection<String> defaultValue) {
        super(key, defaultValue);
    }

    @Override
    public Collection<String> get() {
        return Main.pref.getCollection(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(Collection<String> value) {
        return Main.pref.putCollection(getKey(), value);
    }
}
