// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.Objects;

/**
 * Base abstract class of all settings, holding the setting value.
 *
 * @param <T> The setting type
 * @since 12881 (moved from package {@code org.openstreetmap.josm.data.preferences})
 */
public abstract class AbstractSetting<T> implements Setting<T> {
    protected final T value;
    protected Long time;
    protected boolean isNew;
    /**
     * Constructs a new {@code AbstractSetting} with the given value
     * @param value The setting value
     */
    public AbstractSetting(T value) {
        this.value = value;
        this.time = null;
        this.isNew = false;
    }

    @Override
    public T getValue() {
        return value;
    }

    @Override
    public void setTime(Long time) {
        this.time = time;
    }

    @Override
    public Long getTime() {
        return this.time;
    }

    @Override
    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    @Override
    public boolean isNew() {
        return isNew;
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
