// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collection;

import org.openstreetmap.josm.Main;

public class CollectionProperty extends AbstractProperty {
    protected final Collection<String> defaultValue;

    public CollectionProperty(String key, Collection<String> defaultValue) {
        super(key);
        this.defaultValue = defaultValue;
    }

    public Collection<String> get() {
        return Main.pref.getCollection(getKey(), getDefaultValue());
    }

    public boolean put(Collection<String> value) {
        return Main.pref.putCollection(getKey(), value);
    }

    public Collection<String> getDefaultValue() {
        return defaultValue;
    }

}
