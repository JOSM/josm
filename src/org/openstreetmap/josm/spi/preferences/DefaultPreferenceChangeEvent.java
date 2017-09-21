// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

/**
 * Default implementation of the {@link PreferenceChangeEvent} interface.
 * @since xxx
 */
public class DefaultPreferenceChangeEvent implements PreferenceChangeEvent {
    
    private final String key;
    private final Setting<?> oldValue;
    private final Setting<?> newValue;

    public DefaultPreferenceChangeEvent(String key, Setting<?> oldValue, Setting<?> newValue) {
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public Setting<?> getOldValue() {
        return oldValue;
    }

    @Override
    public Setting<?> getNewValue() {
        return newValue;
    }
    
}
