// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

public abstract class CachedProperty<T> extends AbstractProperty<T> implements PreferenceChangedListener {

    private final String defaultValueAsString;
    private T value;
    private int updateCount;

    protected CachedProperty(String key, String defaultValueAsString) {
        super(key, null);
        Main.pref.addPreferenceChangeListener(this);
        this.defaultValueAsString = defaultValueAsString;
        updateValue();
    }

    protected void updateValue() {
        if (!Main.pref.get(key).isEmpty()) {
            this.value = fromString(Main.pref.get(key));
        } else {
            this.value = getDefaultValue();
        }
        updateCount++;
    }

    protected abstract T fromString(String s);

    @Override
    public T get() {
        return value;
    }

    public void put(String value) {
        Main.pref.put(key, value);
        this.value = fromString(value);
        updateCount++;
    }

    @Override
    public final boolean put(T value) {
        // Not used
        throw new IllegalAccessError("You cannot use put(T). Use put(String) instead.");
    }

    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public T getDefaultValue() {
        return fromString(getDefaultValueAsString());
    }

    public String getDefaultValueAsString() {
        return defaultValueAsString;
    }

    public String getAsString() {
        return Main.pref.get(getKey(), getDefaultValueAsString());
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().equals(key)) {
            updateValue();
        }
    }

}
