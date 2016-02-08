// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

/**
 * Interface for a preference value.
 *
 * Implementations must provide a proper <code>equals</code> method.
 *
 * @param <T> the data type for the value
 * @since 9759
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
    boolean equalVal(T otherVal);

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
}
