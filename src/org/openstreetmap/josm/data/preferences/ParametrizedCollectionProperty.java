// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collection;

import org.openstreetmap.josm.Main;

public abstract class ParametrizedCollectionProperty {

    private final Collection<String> defaultValue;

    public ParametrizedCollectionProperty(Collection<String> defaultValue) {
        this.defaultValue = defaultValue;
    }

    protected abstract String getKey(String... params);

    public Collection<String> get(String... params) {
        return Main.pref.getCollection(getKey(params), defaultValue);
    }

    public boolean put(Collection<String> value, String... params) {
        return Main.pref.putCollection(getKey(params), value);
    }

}
