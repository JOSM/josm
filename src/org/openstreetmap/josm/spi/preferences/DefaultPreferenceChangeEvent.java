// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.EventObject;

/**
 * Default implementation of the {@link PreferenceChangeEvent} interface.
 * @since 12881
 */
public class DefaultPreferenceChangeEvent extends EventObject implements PreferenceChangeEvent {

    private final String key;
    private final Setting<?> oldValue;
    private final Setting<?> newValue;

    /**
     * Constructs a new {@code DefaultPreferenceChangeEvent}.
     * @param source the class source of this event
     * @param key preference key
     * @param oldValue preference old value
     * @param newValue preference new value
     * @since 14977
     */
    public DefaultPreferenceChangeEvent(Class<?> source, String key, Setting<?> oldValue, Setting<?> newValue) {
        super(source);
        this.key = key;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    @Override
    public Class<?> getSource() {
        return (Class<?>) super.getSource();
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
