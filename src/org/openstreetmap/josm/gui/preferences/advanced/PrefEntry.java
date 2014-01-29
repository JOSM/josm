// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Class to store single preference line for the table.
 * @since 6021
 */
public class PrefEntry implements Comparable<PrefEntry> {
    private String key;
    private Preferences.Setting value;
    private Preferences.Setting defaultValue;
    private boolean isDefault;
    private boolean changed;

    /**
     * Constructs a new {@code PrefEntry}.
     * @param key The preference key
     * @param value The preference value
     * @param defaultValue The preference default value
     * @param isDefault determines if the current value is the default value
     */
    public PrefEntry(String key, Preferences.Setting value, Preferences.Setting defaultValue, boolean isDefault) {
        CheckParameterUtil.ensureParameterNotNull(key);
        CheckParameterUtil.ensureParameterNotNull(value);
        CheckParameterUtil.ensureParameterNotNull(defaultValue);
        this.key = key;
        this.value = value;
        this.defaultValue = defaultValue;
        this.isDefault = isDefault;
    }

    /**
     * Returns the preference key.
     * @return the preference key
     */
    public String getKey() {
        return key;
    }

    /**
     * Returns the preference value.
     * @return the preference value
     */
    public Preferences.Setting getValue() {
        return value;
    }

    /**
     * Returns the preference default value.
     * @return the preference default value
     */
    public Preferences.Setting getDefaultValue() {
        return defaultValue;
    }

    /**
     * Sets the preference value.
     * @param value the preference value
     */
    public void setValue(Preferences.Setting value) {
        this.value = value;
        changed = true;
        isDefault = false;
    }

    /**
     * Determines if the current value is the default value.
     * @return {@code true} if the current value is the default value, {@code false} otherwise
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * Determines if this preference entry has been modified.
     * @return {@code true} if this preference entry has been modified, {@code false} otherwise
     */
    public boolean isChanged() {
        return changed;
    }

    /**
     * Marks this preference entry as modified.
     */
    public void markAsChanged() {
        changed = true;
    }

    /**
     * Resets this preference entry to default state.
     */
    public void reset() {
        value = defaultValue;
        changed = true;
        isDefault = true;
    }

    @Override
    public int compareTo(PrefEntry other) {
        return key.compareTo(other.key);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}