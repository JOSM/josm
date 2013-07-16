// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import org.openstreetmap.josm.Main;

/**
 * Captures the common functionality of preference properties
 * @param <T> The type of object accessed by this property
 */
public abstract class AbstractProperty<T> {
    protected final String key;
    protected final T defaultValue;

    /**
     * Constructs a new {@code AbstractProperty}.
     * @param key The property key
     * @param defaultValue The default value
     * @since 5464
     */
    public AbstractProperty(String key, T defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /**
     * Replies the property key.
     * @return The property key
     */
    public String getKey() {
        return key;
    }

    /**
     * Determines if this property is currently set in JOSM preferences.
     * @return true if {@code Main.pref} contains this property.
     */
    public boolean isSet() {
        return !Main.pref.get(key).isEmpty();
    }

    /**
     * Replies the default value of this property.
     * @return The default value of this property
     */
    public T getDefaultValue() {
        return defaultValue;
    }

    /**
     * Removes this property from JOSM preferences (i.e replace it by its default value).
     */
    public void remove() {
        Main.pref.put(getKey(), String.valueOf(getDefaultValue()));
    }

    /**
     * Replies the value of this property.
     * @return the value of this property
     * @since 5464
     */
    public abstract T get();

    /**
     * Sets this property to the specified value.
     * @param value The new value of this property
     * @return true if something has changed (i.e. value is different than before)
     * @since 5464
     */
    public abstract boolean put(T value);
}
