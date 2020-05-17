// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.spi.preferences;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.openstreetmap.josm.tools.Logging;

/**
 * Abstract implementation of the {@link IPreferences} interface.
 * @since 12847
 */
public abstract class AbstractPreferences implements IPreferences {

    @Override
    public synchronized String get(final String key, final String def) {
        return getSetting(key, new StringSetting(def), StringSetting.class).getValue();
    }

    @Override
    public boolean put(final String key, String value) {
        return putSetting(key, value == null || value.isEmpty() ? null : new StringSetting(value));
    }

    @Override
    public boolean getBoolean(final String key, final boolean def) {
        return Boolean.parseBoolean(get(key, Boolean.toString(def)));
    }

    @Override
    public boolean putBoolean(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    @Override
    public synchronized int getInt(String key, int def) {
        String v = get(key, Integer.toString(def));
        if (v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            // fall out
            Logging.trace(e);
        }
        return def;
    }

    @Override
    public boolean putInt(String key, int value) {
        return put(key, Integer.toString(value));
    }

    @Override
    public long getLong(String key, long def) {
        String v = get(key, Long.toString(def));
        if (null == v)
            return def;

        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            // fall out
            Logging.trace(e);
        }
        return def;
    }

    @Override
    public boolean putLong(final String key, final long value) {
        return put(key, Long.toString(value));
    }

    @Override
    public synchronized double getDouble(String key, double def) {
        String v = get(key, Double.toString(def));
        if (null == v)
            return def;

        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            // fall out
            Logging.trace(e);
        }
        return def;
    }

    @Override
    public boolean putDouble(final String key, final double value) {
        return put(key, Double.toString(value));
    }

    @Override
    public List<String> getList(String key, List<String> def) {
        return getSetting(key, new ListSetting(def), ListSetting.class).getValue();
    }

    @Override
    public boolean putList(String key, List<String> value) {
        return putSetting(key, value == null ? null : new ListSetting(value));
    }

    @Override
    public List<List<String>> getListOfLists(String key, List<List<String>> def) {
        return getSetting(key, new ListListSetting(def), ListListSetting.class).getValue();
    }

    @Override
    public boolean putListOfLists(String key, List<List<String>> value) {
        return putSetting(key, value == null ? null : new ListListSetting(value));
    }

    @Override
    public List<Map<String, String>> getListOfMaps(String key, List<Map<String, String>> def) {
        return getSetting(key, new MapListSetting(def), MapListSetting.class).getValue();
    }

    @Override
    public boolean putListOfMaps(String key, List<Map<String, String>> value) {
        return putSetting(key, value == null ? null : new MapListSetting(value));
    }

    /**
     * Gets a map of all settings that are currently stored
     * @return The settings
     */
    public abstract Map<String, Setting<?>> getAllSettings();

    /**
     * Set a value for a certain setting. The changed setting is saved to the preference file immediately.
     * Due to caching mechanisms on modern operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param setting the value of the setting. In case it is null, the key-value entry will be removed.
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public abstract boolean putSetting(String key, Setting<?> setting);

    /**
     * Get settings value for a certain key and provide default a value.
     * @param <T> the setting type
     * @param key the identifier for the setting
     * @param def the default value. For each call of getSetting() with a given key, the default value must be the same.
     * <code>def</code> must not be null, but the value of <code>def</code> can be null.
     * @param klass the setting type (same as T)
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     */
    public abstract <T extends Setting<?>> T getSetting(String key, T def, Class<T> klass);

    /**
     * Gets all normal (string) settings that have a key starting with the prefix
     * @param prefix The start of the key
     * @return The key names of the settings
     */
    public Map<String, String> getAllPrefix(String prefix) {
        return getAllSettings().entrySet().stream()
                .filter(e -> e.getKey().startsWith(prefix) && (e.getValue() instanceof StringSetting))
                .collect(Collectors.toMap(Entry::getKey, e -> ((StringSetting) e.getValue()).getValue(), (a, b) -> b, TreeMap::new));
    }

    /**
     * Gets all list settings that have a key starting with the prefix
     * @param prefix The start of the key
     * @return The key names of the list settings
     */
    public List<String> getAllPrefixCollectionKeys(String prefix) {
        return getAllSettings().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(prefix) && entry.getValue() instanceof ListSetting)
                .map(Entry::getKey)
                .collect(Collectors.toCollection(LinkedList::new));
    }
}
