// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Preferences implementation that keeps all settings in memory.
 *
 * Holds saved preferences for the current session, but does not retain any data when the
 * program terminates.
 *
 * @since 12906
 */
public class MemoryPreferences extends AbstractPreferences {

    private final Map<String, Setting<?>> settings = new HashMap<>();

    @Override
    public boolean putSetting(String key, Setting<?> setting) {
        Setting current = settings.get(key);
        if (setting == null) {
            settings.remove(key);
        } else {
            settings.put(key, setting);
        }
        return Objects.equals(setting, current);
    }

    @Override
    public <T extends Setting<?>> T getSetting(String key, T def, Class<T> klass) {
        Setting current = settings.get(key);
        if (current != null && klass.isInstance(current)) {
            @SuppressWarnings("unchecked")
            T result = (T) current;
            return result;
        }
        return def;
    }

    @Override
    public Set<String> getKeySet() {
        return Collections.unmodifiableSet(settings.keySet());
    }

    @Override
    public void addPreferenceChangeListener(PreferenceChangedListener listener) {
        // do nothing
    }

    @Override
    public void removePreferenceChangeListener(PreferenceChangedListener listener) {
        // do nothing
    }

    @Override
    public void addKeyPreferenceChangeListener(String key, PreferenceChangedListener listener) {
        // do nothing
    }

    @Override
    public void removeKeyPreferenceChangeListener(String key, PreferenceChangedListener listener) {
        // do nothing
    }

}
