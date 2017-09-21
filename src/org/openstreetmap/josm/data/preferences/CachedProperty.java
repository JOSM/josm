// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.PreferenceChangedListener;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * This is an old-style cached preference value.
 *
 * You can replace this using the {@link StringProperty#cached()}, {@link BooleanProperty#cached()} accessors
 *
 * @param <T> The value type of this property
 */
public abstract class CachedProperty<T> extends AbstractProperty<T> implements PreferenceChangedListener {

    private final String defaultValueAsString;
    private T value;
    private int updateCount;

    protected CachedProperty(String key, String defaultValueAsString) {
        super(key, null);
        Config.getPref().addKeyPreferenceChangeListener(key, this);
        this.defaultValueAsString = defaultValueAsString;
        updateValue();
    }

    protected final void updateValue() {
        if (!Config.getPref().get(key).isEmpty()) {
            this.value = fromString(Config.getPref().get(key));
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
        Config.getPref().put(key, value);
        this.value = fromString(value);
        updateCount++;
    }

    @Override
    public final boolean put(T value) {
        // Not used
        throw new UnsupportedOperationException("You cannot use put(T). Use put(String) instead.");
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
        return getPreferences().get(getKey(), getDefaultValueAsString());
    }

    @Override
    public void preferenceChanged(PreferenceChangeEvent e) {
        if (e.getKey().equals(key)) {
            updateValue();
        }
    }

}
