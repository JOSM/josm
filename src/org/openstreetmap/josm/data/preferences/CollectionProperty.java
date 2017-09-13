// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collection;

import org.openstreetmap.josm.Main;

/**
 * A property containing a {@code Collection} of {@code String} as value.
 * @deprecated use {@link ListProperty}
 */
@Deprecated
public class CollectionProperty extends AbstractProperty<Collection<String>> {

    /**
     * Constructs a new {@code CollectionProperty}.
     * @param key The property key
     * @param defaultValue The default value
     */
    public CollectionProperty(String key, Collection<String> defaultValue) {
        super(key, defaultValue);
        if (Main.pref != null) {
            get();
        }
    }

    @Override
    public Collection<String> get() {
        return getPreferences().getCollection(getKey(), getDefaultValue());
    }

    @Override
    public boolean put(Collection<String> value) {
        return getPreferences().putCollection(getKey(), value);
    }
}
