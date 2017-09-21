// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Interface for a preference value.
 *
 * Implementations must provide a proper <code>equals</code> method.
 *
 * @param <T> the data type for the value
 * @since xxx (moved from package {@code org.openstreetmap.josm.data.preferences})
 */
public interface Setting<T> {
    /**
     * Returns the value of this setting.
     *
     * @return the value of this setting
     */
    T getValue();

    /**
     * Check if the value of this Setting object is equal to the given value.
     * @param otherVal the other value
     * @return true if the values are equal
     */
    default boolean equalVal(T otherVal) {
        return getValue() == null ? (otherVal == null) : getValue().equals(otherVal);
    }

    /**
     * Clone the current object.
     * @return an identical copy of the current object
     */
    Setting<T> copy();

    /**
     * Enable usage of the visitor pattern.
     *
     * @param visitor the visitor
     */
    void visit(SettingVisitor visitor);

    /**
     * Returns a setting whose value is null.
     *
     * Cannot be static, because there is no static inheritance.
     * @return a Setting object that isn't null itself, but returns null
     * for {@link #getValue()}
     */
    Setting<T> getNullInstance();

    /**
     * Set the time for this setting.
     *
     * For default preferences. They are saved in a cache file. Keeping the
     * time allows to discard very old default settings.
     * @param time the time in seconds since epoch
     */
    void setTime(Long time);

    /**
     * Get the time for this setting.
     * @return the time for this setting
     * @see #setTime(java.lang.Long)
     */
    Long getTime();

    /**
     * Mark setting as new.
     *
     * For default preferences. A setting is marked as new, if it has been seen
     * in the current session.
     * Methods like {@link IPreferences#get(java.lang.String, java.lang.String)},
     * can be called from different parts of the code with the same key. In this case,
     * the supplied default value must match. However, this is only an error if the mismatching
     * default value has been seen in the same session (and not loaded from cache).
     * @param isNew true, if it is new
     */
    void setNew(boolean isNew);

    /**
     * Return if the setting has been marked as new.
     * @return true, if the setting has been marked as new
     * @see #setNew(boolean)
     */
    boolean isNew();
}
