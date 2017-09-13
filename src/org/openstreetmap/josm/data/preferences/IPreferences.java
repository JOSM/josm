// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.preferences;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.data.Preferences.PreferenceChangedListener;

/**
 * Interface for preference handling.
 *
 * Allows to save and retrieve user defined settings. The backend storage depends
 * on the implementation.
 * @since 12840
 */
public interface IPreferences {

    /**
     * Adds a new preferences listener.
     * @param listener The listener to add
     */
    void addPreferenceChangeListener(PreferenceChangedListener listener);

    /**
     * Removes a preferences listener.
     * @param listener The listener to remove
     */
    void removePreferenceChangeListener(PreferenceChangedListener listener);

    /**
     * Adds a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     */
    void addKeyPreferenceChangeListener(String key, PreferenceChangedListener listener);

    /**
     * Removes a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     */
    void removeKeyPreferenceChangeListener(String key, PreferenceChangedListener listener);

    /**
     * Get settings value for a certain key and provide a default value.
     * @param key the identifier for the setting
     * @param def the default value. For each call of get() with a given key, the
     * default value must be the same. {@code def} may be null.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     */
    String get(String key, String def);

    /**
     * Get settings value for a certain key.
     * @param key the identifier for the setting
     * @return "" if there is nothing set for the preference key, the corresponding value otherwise. The result is not null.
     */
    default String get(final String key) {
        return get(key, "");
    }

    /**
     * Set a value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value the value of the setting. Can be null or "" which both removes the key-value entry.
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    boolean put(String key, String value);

    /**
     * Gets a boolean preference
     * @param key The preference key
     * @param def The default value to use
     * @return The boolean, <code>false</code> if it could not be parsed, the default value if it is unset
     */
    boolean getBoolean(String key, boolean def);

    /**
     * Gets a boolean preference
     * @param key The preference key
     * @return The boolean or <code>false</code> if it could not be parsed
     */
    default boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12840
     */
    boolean putBoolean(String key, boolean value);

    /**
     * Gets an integer preference
     * @param key The preference key
     * @param def The default value to use
     * @return The integer
     * @since 12840
     */
    int getInt(String key, int def);

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12840
     */
    boolean putInt(String key, int value);

    /**
     * Gets a double preference
     * @param key The preference key
     * @param def The default value to use
     * @return The double value or the default value if it could not be parsed
     */
    double getDouble(String key, double def);

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12840
     */
    boolean putDouble(String key, double value);

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     * @since 12840
     */
    List<String> getList(String key, List<String> def);

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before, an
     * empty list otherwise.
     * @since 12840
     */
    default List<String> getList(String key) {
        List<String> val = getList(key, null);
        return val == null ? Collections.emptyList() : val;
    }

    /**
     * Set a list of values for a certain key.
     * @param key the identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12840
     */
    boolean putList(String key, List<String> value);

    /**
     * Get an array of values (list of lists) for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     * @since 12840
     */
    List<List<String>> getListOfLists(String key, List<List<String>> def);

    /**
     * Get an array of values (list of lists) for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before, an
     * empty list otherwise
     * @since 12840
     */
    default List<List<String>> getListOfLists(String key) {
        List<List<String>> val = getListOfLists(key, null);
        return val == null ? Collections.emptyList() : val;
    }

    /**
     * Set an array of values (list of lists) for a certain key.
     * @param key the identifier for the setting
     * @param value the new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12840
     */
    boolean putListOfLists(String key, List<List<String>> value);

    /**
     * Gets a list of key/value maps.
     * @param key the key to search at
     * @param def the default value to use
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     * @since 12840
     */
    List<Map<String, String>> getListOfMaps(String key, List<Map<String, String>> def);

    /**
     * Gets a list of key/value maps.
     * @param key the key to search at
     * @return the corresponding value if the property has been set before, an
     * empty list otherwise
     * @since 12840
     */
    default List<Map<String, String>> getListOfMaps(String key) {
        List<Map<String, String>> val = getListOfMaps(key, null);
        return val == null ? Collections.emptyList() : val;
    }

    /**
     * Set an a list of key/value maps.
     * @param key the key to store the list in
     * @param value a list of key/value maps
     * @return <code>true</code> if the value was changed
     * @since 12840
     */
    boolean putListOfMaps(String key, List<Map<String, String>> value);

}
