// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

public abstract class ParametrizedEnumProperty<T extends Enum<T>> {


    private final T defaultValue;
    private final Class<T> enumClass;

    public ParametrizedEnumProperty(Class<T> enumClass, T defaultValue) {
        this.defaultValue = defaultValue;
        this.enumClass = enumClass;
    }

    protected abstract String getKey(String... params);

    public T get(String... params) {
        return parse(Main.pref.get(getKey(params), defaultValue.name()));
    }

    public boolean put(T value, String... params) {
        return Main.pref.put(getKey(params), value.name());
    }

    protected T parse(String s) {
        return Enum.valueOf(enumClass, s);
    }


}
