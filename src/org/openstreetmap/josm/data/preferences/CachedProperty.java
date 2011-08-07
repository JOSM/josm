// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

public abstract class CachedProperty<T> extends AbstractProperty<T> implements PreferenceChangedListener {

    protected final String defaultValue;
    private T value;
    private int updateCount;

    protected CachedProperty(String key, String defaultValue) {
        super(key);
        Main.pref.addPreferenceChangeListener(this);
        this.defaultValue = defaultValue;
        updateValue();
    }

    protected void updateValue() {
        if (Main.pref.hasKey(key)) {
            this.value = fromString(Main.pref.get(key));
        } else {
            this.value = getDefaultValue();
        }
        updateCount++;
    }

    protected abstract T fromString(String s);

    public T get() {
        return value;
    }

    public void put(String value) {
        Main.pref.put(key, value);
        this.value = fromString(value);
        updateCount++;
    }

    public int getUpdateCount() {
        return updateCount;
    }

    @Override
    public T getDefaultValue() {
        return fromString(getDefaultValueAsString());
    }

    public String getDefaultValueAsString() {
        return defaultValue;
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
