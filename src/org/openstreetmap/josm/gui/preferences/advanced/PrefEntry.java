// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.gui.preferences.advanced;

import org.openstreetmap.josm.data.Preferences;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * Class to store single preference line for the table
 * @since 6021 : extracted from AdvancedPreference class 
 */
public class PrefEntry implements Comparable<PrefEntry> {
    private String key;
    private Preferences.Setting value;
    private Preferences.Setting defaultValue;
    private boolean isDefault;
    private boolean changed;

    public PrefEntry(String key, Preferences.Setting value, Preferences.Setting defaultValue, boolean isDefault) {
        CheckParameterUtil.ensureParameterNotNull(key);
        CheckParameterUtil.ensureParameterNotNull(value);
        CheckParameterUtil.ensureParameterNotNull(defaultValue);
        this.key = key;
        this.value = value;
        this.defaultValue = defaultValue;
        this.isDefault = isDefault;
    }

    public String getKey() {
        return key;
    }

    public Preferences.Setting getValue() {
        return value;
    }

    public Preferences.Setting getDefaultValue() {
        return defaultValue;
    }

    public void setValue(Preferences.Setting value) {
        this.value = value;
        changed = true;
        isDefault = false;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public boolean isChanged() {
        return changed;
    }

    public void markAsChanged() {
        changed = true;
    }

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