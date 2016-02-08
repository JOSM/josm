// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Objects;

/**
 * Base abstract class of all settings, holding the setting value.
 *
 * @param <T> The setting type
 * @since 9759
 */
public abstract class AbstractSetting<T> implements Setting<T> {
    protected final T value;
    /**
     * Constructs a new {@code AbstractSetting} with the given value
     * @param value The setting value
     */
    public AbstractSetting(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value != null ? value.toString() : "null";
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        return Objects.equals(value, ((AbstractSetting<?>) obj).value);
    }
}
