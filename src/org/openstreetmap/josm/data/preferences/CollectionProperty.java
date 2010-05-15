// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collection;

import org.openstreetmap.josm.Main;

public class CollectionProperty {
    private final String key;
    private final Collection<String> defaultValue;

    public CollectionProperty(String key, Collection<String> defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public Collection<String> get() {
        return Main.pref.getCollection(key, defaultValue);
    }

    public boolean put(Collection<String> value) {
        return Main.pref.putCollection(key, value);
    }

}
