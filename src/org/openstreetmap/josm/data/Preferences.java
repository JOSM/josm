// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.swing.JOptionPane;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.data.preferences.ListListSetting;
import org.openstreetmap.josm.data.preferences.ListSetting;
import org.openstreetmap.josm.data.preferences.LongProperty;
import org.openstreetmap.josm.data.preferences.MapListSetting;
import org.openstreetmap.josm.data.preferences.PreferencesReader;
import org.openstreetmap.josm.data.preferences.PreferencesWriter;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.data.preferences.StringSetting;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * This class holds all preferences for JOSM.
 *
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 *
 * Each property is a key=setting pair, where key is a String and setting can be one of
 * 4 types:
 *     string, list, list of lists and list of maps.
 * In addition, each key has a unique default value that is set when the value is first
 * accessed using one of the get...() methods. You can use the same preference
 * key in different parts of the code, but the default value must be the same
 * everywhere. A default value of null means, the setting has been requested, but
 * no default value was set. This is used in advanced preferences to present a list
 * off all possible settings.
 *
 * At the moment, you cannot put the empty string for string properties.
 * put(key, "") means, the property is removed.
 *
 * @author imi
 * @since 74
 */
public class Preferences {

    private static final String[] OBSOLETE_PREF_KEYS = {
    };

    private static final long MAX_AGE_DEFAULT_PREFERENCES = 60L * 60L * 24L * 50L; // 50 days (in seconds)

    /**
     * Internal storage for the preference directory.
     * Do not access this variable directly!
     * @see #getPreferencesDirectory()
     */
    private File preferencesDir;

    /**
     * Internal storage for the cache directory.
     */
    private File cacheDir;

    /**
     * Internal storage for the user data directory.
     */
    private File userdataDir;

    /**
     * Determines if preferences file is saved each time a property is changed.
     */
    private boolean saveOnPut = true;

    /**
     * Maps the setting name to the current value of the setting.
     * The map must not contain null as key or value. The mapped setting objects
     * must not have a null value.
     */
    protected final SortedMap<String, Setting<?>> settingsMap = new TreeMap<>();

    /**
     * Maps the setting name to the default value of the setting.
     * The map must not contain null as key or value. The value of the mapped
     * setting objects can be null.
     */
    protected final SortedMap<String, Setting<?>> defaultsMap = new TreeMap<>();

    private final Predicate<Entry<String, Setting<?>>> NO_DEFAULT_SETTINGS_ENTRY =
            e -> !e.getValue().equals(defaultsMap.get(e.getKey()));

    /**
     * Maps color keys to human readable color name
     */
    protected final SortedMap<String, String> colornames = new TreeMap<>();

    /**
     * Indicates whether {@link #init(boolean)} completed successfully.
     * Used to decide whether to write backup preference file in {@link #save()}
     */
    protected boolean initSuccessful;

    /**
     * Event triggered when a preference entry value changes.
     */
    public interface PreferenceChangeEvent {
        /**
         * Returns the preference key.
         * @return the preference key
         */
        String getKey();

        /**
         * Returns the old preference value.
         * @return the old preference value
         */
        Setting<?> getOldValue();

        /**
         * Returns the new preference value.
         * @return the new preference value
         */
        Setting<?> getNewValue();
    }

    /**
     * Listener to preference change events.
     * @since 10600 (functional interface)
     */
    @FunctionalInterface
    public interface PreferenceChangedListener {
        /**
         * Trigerred when a preference entry value changes.
         * @param e the preference change event
         */
        void preferenceChanged(PreferenceChangeEvent e);
    }

    private static class DefaultPreferenceChangeEvent implements PreferenceChangeEvent {
        private final String key;
        private final Setting<?> oldValue;
        private final Setting<?> newValue;

        DefaultPreferenceChangeEvent(String key, Setting<?> oldValue, Setting<?> newValue) {
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

    private final ListenerList<PreferenceChangedListener> listeners = ListenerList.create();

    private final HashMap<String, ListenerList<PreferenceChangedListener>> keyListeners = new HashMap<>();

    /**
     * Adds a new preferences listener.
     * @param listener The listener to add
     */
    public void addPreferenceChangeListener(PreferenceChangedListener listener) {
        if (listener != null) {
            listeners.addListener(listener);
        }
    }

    /**
     * Removes a preferences listener.
     * @param listener The listener to remove
     */
    public void removePreferenceChangeListener(PreferenceChangedListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Adds a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 10824
     */
    public void addKeyPreferenceChangeListener(String key, PreferenceChangedListener listener) {
        listenersForKey(key).addListener(listener);
    }

    /**
     * Adds a weak listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 10824
     */
    public void addWeakKeyPreferenceChangeListener(String key, PreferenceChangedListener listener) {
        listenersForKey(key).addWeakListener(listener);
    }

    private ListenerList<PreferenceChangedListener> listenersForKey(String key) {
        ListenerList<PreferenceChangedListener> keyListener = keyListeners.get(key);
        if (keyListener == null) {
            keyListener = ListenerList.create();
            keyListeners.put(key, keyListener);
        }
        return keyListener;
    }

    /**
     * Removes a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     */
    public void removeKeyPreferenceChangeListener(String key, PreferenceChangedListener listener) {
        ListenerList<PreferenceChangedListener> keyListener = keyListeners.get(key);
        if (keyListener == null) {
            throw new IllegalArgumentException("There are no listeners registered for " + key);
        }
        keyListener.removeListener(listener);
    }

    protected void firePreferenceChanged(String key, Setting<?> oldValue, Setting<?> newValue) {
        final PreferenceChangeEvent evt = new DefaultPreferenceChangeEvent(key, oldValue, newValue);
        listeners.fireEvent(listener -> listener.preferenceChanged(evt));

        ListenerList<PreferenceChangedListener> forKey = keyListeners.get(key);
        if (forKey != null) {
            forKey.fireEvent(listener -> listener.preferenceChanged(evt));
        }
    }

    /**
     * Returns the user defined preferences directory, containing the preferences.xml file
     * @return The user defined preferences directory, containing the preferences.xml file
     * @since 7834
     */
    public File getPreferencesDirectory() {
        if (preferencesDir != null)
            return preferencesDir;
        String path;
        path = System.getProperty("josm.pref");
        if (path != null) {
            preferencesDir = new File(path).getAbsoluteFile();
        } else {
            path = System.getProperty("josm.home");
            if (path != null) {
                preferencesDir = new File(path).getAbsoluteFile();
            } else {
                preferencesDir = Main.platform.getDefaultPrefDirectory();
            }
        }
        return preferencesDir;
    }

    /**
     * Returns the user data directory, containing autosave, plugins, etc.
     * Depending on the OS it may be the same directory as preferences directory.
     * @return The user data directory, containing autosave, plugins, etc.
     * @since 7834
     */
    public File getUserDataDirectory() {
        if (userdataDir != null)
            return userdataDir;
        String path;
        path = System.getProperty("josm.userdata");
        if (path != null) {
            userdataDir = new File(path).getAbsoluteFile();
        } else {
            path = System.getProperty("josm.home");
            if (path != null) {
                userdataDir = new File(path).getAbsoluteFile();
            } else {
                userdataDir = Main.platform.getDefaultUserDataDirectory();
            }
        }
        return userdataDir;
    }

    /**
     * Returns the user preferences file (preferences.xml).
     * @return The user preferences file (preferences.xml)
     */
    public File getPreferenceFile() {
        return new File(getPreferencesDirectory(), "preferences.xml");
    }

    /**
     * Returns the cache file for default preferences.
     * @return the cache file for default preferences
     */
    public File getDefaultsCacheFile() {
        return new File(getCacheDirectory(), "default_preferences.xml");
    }

    /**
     * Returns the user plugin directory.
     * @return The user plugin directory
     */
    public File getPluginsDirectory() {
        return new File(getUserDataDirectory(), "plugins");
    }

    /**
     * Get the directory where cached content of any kind should be stored.
     *
     * If the directory doesn't exist on the file system, it will be created by this method.
     *
     * @return the cache directory
     */
    public File getCacheDirectory() {
        if (cacheDir != null)
            return cacheDir;
        String path = System.getProperty("josm.cache");
        if (path != null) {
            cacheDir = new File(path).getAbsoluteFile();
        } else {
            path = System.getProperty("josm.home");
            if (path != null) {
                cacheDir = new File(path, "cache");
            } else {
                path = get("cache.folder", null);
                if (path != null) {
                    cacheDir = new File(path).getAbsoluteFile();
                } else {
                    cacheDir = Main.platform.getDefaultCacheDirectory();
                }
            }
        }
        if (!cacheDir.exists() && !cacheDir.mkdirs()) {
            Main.warn(tr("Failed to create missing cache directory: {0}", cacheDir.getAbsoluteFile()));
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to create missing cache directory: {0}</html>", cacheDir.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return cacheDir;
    }

    private static void addPossibleResourceDir(Set<String> locations, String s) {
        if (s != null) {
            if (!s.endsWith(File.separator)) {
                s += File.separator;
            }
            locations.add(s);
        }
    }

    /**
     * Returns a set of all existing directories where resources could be stored.
     * @return A set of all existing directories where resources could be stored.
     */
    public Collection<String> getAllPossiblePreferenceDirs() {
        Set<String> locations = new HashSet<>();
        addPossibleResourceDir(locations, getPreferencesDirectory().getPath());
        addPossibleResourceDir(locations, getUserDataDirectory().getPath());
        addPossibleResourceDir(locations, System.getenv("JOSM_RESOURCES"));
        addPossibleResourceDir(locations, System.getProperty("josm.resources"));
        if (Main.isPlatformWindows()) {
            String appdata = System.getenv("APPDATA");
            if (System.getenv("ALLUSERSPROFILE") != null && appdata != null
                    && appdata.lastIndexOf(File.separator) != -1) {
                appdata = appdata.substring(appdata.lastIndexOf(File.separator));
                locations.add(new File(new File(System.getenv("ALLUSERSPROFILE"),
                        appdata), "JOSM").getPath());
            }
        } else {
            locations.add("/usr/local/share/josm/");
            locations.add("/usr/local/lib/josm/");
            locations.add("/usr/share/josm/");
            locations.add("/usr/lib/josm/");
        }
        return locations;
    }

    /**
     * Get settings value for a certain key.
     * @param key the identifier for the setting
     * @return "" if there is nothing set for the preference key, the corresponding value otherwise. The result is not null.
     */
    public synchronized String get(final String key) {
        String value = get(key, null);
        return value == null ? "" : value;
    }

    /**
     * Get settings value for a certain key and provide default a value.
     * @param key the identifier for the setting
     * @param def the default value. For each call of get() with a given key, the default value must be the same.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     */
    public synchronized String get(final String key, final String def) {
        return getSetting(key, new StringSetting(def), StringSetting.class).getValue();
    }

    public synchronized Map<String, String> getAllPrefix(final String prefix) {
        final Map<String, String> all = new TreeMap<>();
        for (final Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            if (e.getKey().startsWith(prefix) && (e.getValue() instanceof StringSetting)) {
                all.put(e.getKey(), ((StringSetting) e.getValue()).getValue());
            }
        }
        return all;
    }

    public synchronized List<String> getAllPrefixCollectionKeys(final String prefix) {
        final List<String> all = new LinkedList<>();
        for (Map.Entry<String, Setting<?>> entry : settingsMap.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue() instanceof ListSetting) {
                all.add(entry.getKey());
            }
        }
        return all;
    }

    public synchronized Map<String, String> getAllColors() {
        final Map<String, String> all = new TreeMap<>();
        for (final Entry<String, Setting<?>> e : defaultsMap.entrySet()) {
            if (e.getKey().startsWith("color.") && e.getValue() instanceof StringSetting) {
                StringSetting d = (StringSetting) e.getValue();
                if (d.getValue() != null) {
                    all.put(e.getKey().substring(6), d.getValue());
                }
            }
        }
        for (final Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            if (e.getKey().startsWith("color.") && (e.getValue() instanceof StringSetting)) {
                all.put(e.getKey().substring(6), ((StringSetting) e.getValue()).getValue());
            }
        }
        return all;
    }

    public synchronized boolean getBoolean(final String key) {
        String s = get(key, null);
        return s != null && Boolean.parseBoolean(s);
    }

    public synchronized boolean getBoolean(final String key, final boolean def) {
        return Boolean.parseBoolean(get(key, Boolean.toString(def)));
    }

    public synchronized boolean getBoolean(final String key, final String specName, final boolean def) {
        boolean generic = getBoolean(key, def);
        String skey = key+'.'+specName;
        Setting<?> prop = settingsMap.get(skey);
        if (prop instanceof StringSetting)
            return Boolean.parseBoolean(((StringSetting) prop).getValue());
        else
            return generic;
    }

    /**
     * Set a value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value the value of the setting. Can be null or "" which both removes the key-value entry.
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public boolean put(final String key, String value) {
        return putSetting(key, value == null || value.isEmpty() ? null : new StringSetting(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see BooleanProperty
     */
    public boolean put(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see IntegerProperty
     */
    public boolean putInteger(final String key, final Integer value) {
        return put(key, Integer.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see DoubleProperty
     */
    public boolean putDouble(final String key, final Double value) {
        return put(key, Double.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see LongProperty
     */
    public boolean putLong(final String key, final Long value) {
        return put(key, Long.toString(value));
    }

    /**
     * Called after every put. In case of a problem, do nothing but output the error in log.
     * @throws IOException if any I/O error occurs
     */
    public synchronized void save() throws IOException {
        save(getPreferenceFile(), settingsMap.entrySet().stream().filter(NO_DEFAULT_SETTINGS_ENTRY), false);
    }

    public synchronized void saveDefaults() throws IOException {
        save(getDefaultsCacheFile(), defaultsMap.entrySet().stream(), true);
    }

    protected void save(File prefFile, Stream<Entry<String, Setting<?>>> settings, boolean defaults) throws IOException {
        if (!defaults) {
            /* currently unused, but may help to fix configuration issues in future */
            putInteger("josm.version", Version.getInstance().getVersion());

            updateSystemProperties();
        }

        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if (prefFile.exists() && prefFile.length() > 0 && initSuccessful) {
            Utils.copyFile(prefFile, backupFile);
        }

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(prefFile + "_tmp"), StandardCharsets.UTF_8), false)) {
            PreferencesWriter writer = new PreferencesWriter(out, false, defaults);
            writer.write(settings);
        }

        File tmpFile = new File(prefFile + "_tmp");
        Utils.copyFile(tmpFile, prefFile);
        Utils.deleteFile(tmpFile, marktr("Unable to delete temporary file {0}"));

        setCorrectPermissions(prefFile);
        setCorrectPermissions(backupFile);
    }

    private static void setCorrectPermissions(File file) {
        if (!file.setReadable(false, false) && Main.isDebugEnabled()) {
            Main.debug(tr("Unable to set file non-readable {0}", file.getAbsolutePath()));
        }
        if (!file.setWritable(false, false) && Main.isDebugEnabled()) {
            Main.debug(tr("Unable to set file non-writable {0}", file.getAbsolutePath()));
        }
        if (!file.setExecutable(false, false) && Main.isDebugEnabled()) {
            Main.debug(tr("Unable to set file non-executable {0}", file.getAbsolutePath()));
        }
        if (!file.setReadable(true, true) && Main.isDebugEnabled()) {
            Main.debug(tr("Unable to set file readable {0}", file.getAbsolutePath()));
        }
        if (!file.setWritable(true, true) && Main.isDebugEnabled()) {
            Main.debug(tr("Unable to set file writable {0}", file.getAbsolutePath()));
        }
    }

    /**
     * Loads preferences from settings file.
     * @throws IOException if any I/O error occurs while reading the file
     * @throws SAXException if the settings file does not contain valid XML
     * @throws XMLStreamException if an XML error occurs while parsing the file (after validation)
     */
    protected void load() throws IOException, SAXException, XMLStreamException {
        File pref = getPreferenceFile();
        PreferencesReader.validateXML(pref);
        PreferencesReader reader = new PreferencesReader(pref, false);
        reader.parse();
        settingsMap.clear();
        settingsMap.putAll(reader.getSettings());
        updateSystemProperties();
        removeObsolete(reader.getVersion());
    }

    /**
     * Loads default preferences from default settings cache file.
     *
     * Discards entries older than {@link #MAX_AGE_DEFAULT_PREFERENCES}.
     *
     * @throws IOException if any I/O error occurs while reading the file
     * @throws SAXException if the settings file does not contain valid XML
     * @throws XMLStreamException if an XML error occurs while parsing the file (after validation)
     */
    protected void loadDefaults() throws IOException, XMLStreamException, SAXException {
        File def = getDefaultsCacheFile();
        PreferencesReader.validateXML(def);
        PreferencesReader reader = new PreferencesReader(def, true);
        reader.parse();
        defaultsMap.clear();
        long minTime = System.currentTimeMillis() / 1000 - MAX_AGE_DEFAULT_PREFERENCES;
        for (Entry<String, Setting<?>> e : reader.getSettings().entrySet()) {
            if (e.getValue().getTime() >= minTime) {
                defaultsMap.put(e.getKey(), e.getValue());
            }
        }
    }

    /**
     * Loads preferences from XML reader.
     * @param in XML reader
     * @throws XMLStreamException if any XML stream error occurs
     * @throws IOException if any I/O error occurs
     */
    public void fromXML(Reader in) throws XMLStreamException, IOException {
        PreferencesReader reader = new PreferencesReader(in, false);
        reader.parse();
        settingsMap.clear();
        settingsMap.putAll(reader.getSettings());
    }

    /**
     * Initializes preferences.
     * @param reset if {@code true}, current settings file is replaced by the default one
     */
    public void init(boolean reset) {
        initSuccessful = false;
        // get the preferences.
        File prefDir = getPreferencesDirectory();
        if (prefDir.exists()) {
            if (!prefDir.isDirectory()) {
                Main.warn(tr("Failed to initialize preferences. Preference directory ''{0}'' is not a directory.",
                        prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Preference directory ''{0}'' is not a directory.</html>",
                                prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        } else {
            if (!prefDir.mkdirs()) {
                Main.warn(tr("Failed to initialize preferences. Failed to create missing preference directory: {0}",
                        prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Failed to create missing preference directory: {0}</html>",
                                prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        File preferenceFile = getPreferenceFile();
        try {
            if (!preferenceFile.exists()) {
                Main.info(tr("Missing preference file ''{0}''. Creating a default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            } else if (reset) {
                File backupFile = new File(prefDir, "preferences.xml.bak");
                Main.platform.rename(preferenceFile, backupFile);
                Main.warn(tr("Replacing existing preference file ''{0}'' with default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            }
        } catch (IOException e) {
            Main.error(e);
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to initialize preferences.<br>Failed to reset preference file to default: {0}</html>",
                            getPreferenceFile().getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try {
            load();
            initSuccessful = true;
        } catch (IOException | SAXException | XMLStreamException e) {
            Main.error(e);
            File backupFile = new File(prefDir, "preferences.xml.bak");
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> " +
                            "and creating a new default preference file.</html>",
                            backupFile.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            Main.platform.rename(preferenceFile, backupFile);
            try {
                resetToDefault();
                save();
            } catch (IOException e1) {
                Main.error(e1);
                Main.warn(tr("Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
            }
        }
        File def = getDefaultsCacheFile();
        if (def.exists()) {
            try {
                loadDefaults();
            } catch (IOException | XMLStreamException | SAXException e) {
                Main.error(e);
                Main.warn(tr("Failed to load defaults cache file: {0}", def));
                defaultsMap.clear();
                if (!def.delete()) {
                    Main.warn(tr("Failed to delete faulty defaults cache file: {0}", def));
                }
            }
        }
    }

    /**
     * Resets the preferences to their initial state. This resets all values and file associations.
     * The default values and listeners are not removed.
     * <p>
     * It is meant to be called before {@link #init(boolean)}
     * @since 10876
     */
    public void resetToInitialState() {
        resetToDefault();
        preferencesDir = null;
        cacheDir = null;
        userdataDir = null;
        saveOnPut = true;
        initSuccessful = false;
    }

    /**
     * Reset all values stored in this map to the default values. This clears the preferences.
     */
    public final void resetToDefault() {
        settingsMap.clear();
    }

    /**
     * Convenience method for accessing colour preferences.
     * <p>
     * To be removed: end of 2016
     *
     * @param colName name of the colour
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     * @deprecated Use a {@link ColorProperty} instead.
     */
    @Deprecated
    public synchronized Color getColor(String colName, Color def) {
        return getColor(colName, null, def);
    }

    /* only for preferences */
    public synchronized String getColorName(String o) {
        Matcher m = Pattern.compile("mappaint\\.(.+?)\\.(.+)").matcher(o);
        if (m.matches()) {
            return tr("Paint style {0}: {1}", tr(I18n.escape(m.group(1))), tr(I18n.escape(m.group(2))));
        }
        m = Pattern.compile("layer (.+)").matcher(o);
        if (m.matches()) {
            return tr("Layer: {0}", tr(I18n.escape(m.group(1))));
        }
        return tr(I18n.escape(colornames.containsKey(o) ? colornames.get(o) : o));
    }

    /**
     * Convenience method for accessing colour preferences.
     * <p>
     * To be removed: end of 2016
     * @param colName name of the colour
     * @param specName name of the special colour settings
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     * @deprecated Use a {@link ColorProperty} instead.
     * You can replace this by: <code>new ColorProperty(colName, def).getChildColor(specName)</code>
     */
    @Deprecated
    public synchronized Color getColor(String colName, String specName, Color def) {
        String colKey = ColorProperty.getColorKey(colName);
        registerColor(colKey, colName);
        String colStr = specName != null ? get("color."+specName) : "";
        if (colStr.isEmpty()) {
            colStr = get(colKey, ColorHelper.color2html(def, true));
        }
        if (colStr != null && !colStr.isEmpty()) {
            return ColorHelper.html2color(colStr);
        } else {
            return def;
        }
    }

    /**
     * Registers a color name conversion for the global color registry.
     * @param colKey The key
     * @param colName The name of the color.
     * @since 10824
     */
    public void registerColor(String colKey, String colName) {
        if (!colKey.equals(colName)) {
            colornames.put(colKey, colName);
        }
    }

    public synchronized Color getDefaultColor(String colKey) {
        StringSetting col = Utils.cast(defaultsMap.get("color."+colKey), StringSetting.class);
        String colStr = col == null ? null : col.getValue();
        return colStr == null || colStr.isEmpty() ? null : ColorHelper.html2color(colStr);
    }

    public synchronized boolean putColor(String colKey, Color val) {
        return put("color."+colKey, val != null ? ColorHelper.color2html(val, true) : null);
    }

    public synchronized int getInteger(String key, int def) {
        String v = get(key, Integer.toString(def));
        if (v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            // fall out
            Main.trace(e);
        }
        return def;
    }

    public synchronized int getInteger(String key, String specName, int def) {
        String v = get(key+'.'+specName);
        if (v.isEmpty())
            v = get(key, Integer.toString(def));
        if (v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            // fall out
            Main.trace(e);
        }
        return def;
    }

    public synchronized long getLong(String key, long def) {
        String v = get(key, Long.toString(def));
        if (null == v)
            return def;

        try {
            return Long.parseLong(v);
        } catch (NumberFormatException e) {
            // fall out
            Main.trace(e);
        }
        return def;
    }

    public synchronized double getDouble(String key, double def) {
        String v = get(key, Double.toString(def));
        if (null == v)
            return def;

        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            // fall out
            Main.trace(e);
        }
        return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     */
    public Collection<String> getCollection(String key, Collection<String> def) {
        return getSetting(key, ListSetting.create(def), ListSetting.class).getValue();
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before, an empty collection otherwise.
     */
    public Collection<String> getCollection(String key) {
        Collection<String> val = getCollection(key, null);
        return val == null ? Collections.<String>emptyList() : val;
    }

    public synchronized void removeFromCollection(String key, String value) {
        List<String> a = new ArrayList<>(getCollection(key, Collections.<String>emptyList()));
        a.remove(value);
        putCollection(key, a);
    }

    /**
     * Set a value for a certain setting. The changed setting is saved to the preference file immediately.
     * Due to caching mechanisms on modern operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param setting the value of the setting. In case it is null, the key-value entry will be removed.
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public boolean putSetting(final String key, Setting<?> setting) {
        CheckParameterUtil.ensureParameterNotNull(key);
        if (setting != null && setting.getValue() == null)
            throw new IllegalArgumentException("setting argument must not have null value");
        Setting<?> settingOld;
        Setting<?> settingCopy = null;
        synchronized (this) {
            if (setting == null) {
                settingOld = settingsMap.remove(key);
                if (settingOld == null)
                    return false;
            } else {
                settingOld = settingsMap.get(key);
                if (setting.equals(settingOld))
                    return false;
                if (settingOld == null && setting.equals(defaultsMap.get(key)))
                    return false;
                settingCopy = setting.copy();
                settingsMap.put(key, settingCopy);
            }
            if (saveOnPut) {
                try {
                    save();
                } catch (IOException e) {
                    Main.warn(e, tr("Failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, settingOld, settingCopy);
        return true;
    }

    public synchronized Setting<?> getSetting(String key, Setting<?> def) {
        return getSetting(key, def, Setting.class);
    }

    /**
     * Get settings value for a certain key and provide default a value.
     * @param <T> the setting type
     * @param key the identifier for the setting
     * @param def the default value. For each call of getSetting() with a given key, the default value must be the same.
     * <code>def</code> must not be null, but the value of <code>def</code> can be null.
     * @param klass the setting type (same as T)
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Setting<?>> T getSetting(String key, T def, Class<T> klass) {
        CheckParameterUtil.ensureParameterNotNull(key);
        CheckParameterUtil.ensureParameterNotNull(def);
        Setting<?> oldDef = defaultsMap.get(key);
        if (oldDef != null && oldDef.isNew() && oldDef.getValue() != null && def.getValue() != null && !def.equals(oldDef)) {
            Main.info("Defaults for " + key + " differ: " + def + " != " + defaultsMap.get(key));
        }
        if (def.getValue() != null || oldDef == null) {
            Setting<?> defCopy = def.copy();
            defCopy.setTime(System.currentTimeMillis() / 1000);
            defCopy.setNew(true);
            defaultsMap.put(key, defCopy);
        }
        Setting<?> prop = settingsMap.get(key);
        if (klass.isInstance(prop)) {
            return (T) prop;
        } else {
            return def;
        }
    }

    /**
     * Put a collection.
     * @param key key
     * @param value value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public boolean putCollection(String key, Collection<String> value) {
        return putSetting(key, value == null ? null : ListSetting.create(value));
    }

    /**
     * Saves at most {@code maxsize} items of collection {@code val}.
     * @param key key
     * @param maxsize max number of items to save
     * @param val value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public boolean putCollectionBounded(String key, int maxsize, Collection<String> val) {
        Collection<String> newCollection = new ArrayList<>(Math.min(maxsize, val.size()));
        for (String i : val) {
            if (newCollection.size() >= maxsize) {
                break;
            }
            newCollection.add(i);
        }
        return putCollection(key, newCollection);
    }

    /**
     * Used to read a 2-dimensional array of strings from the preference file.
     * If not a single entry could be found, <code>def</code> is returned.
     * @param key preference key
     * @param def default array value
     * @return array value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized Collection<Collection<String>> getArray(String key, Collection<Collection<String>> def) {
        ListListSetting val = getSetting(key, ListListSetting.create(def), ListListSetting.class);
        return (Collection) val.getValue();
    }

    public Collection<Collection<String>> getArray(String key) {
        Collection<Collection<String>> res = getArray(key, null);
        return res == null ? Collections.<Collection<String>>emptyList() : res;
    }

    /**
     * Put an array.
     * @param key key
     * @param value value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    public boolean putArray(String key, Collection<Collection<String>> value) {
        return putSetting(key, value == null ? null : ListListSetting.create(value));
    }

    public Collection<Map<String, String>> getListOfStructs(String key, Collection<Map<String, String>> def) {
        return getSetting(key, new MapListSetting(def == null ? null : new ArrayList<>(def)), MapListSetting.class).getValue();
    }

    public boolean putListOfStructs(String key, Collection<Map<String, String>> value) {
        return putSetting(key, value == null ? null : new MapListSetting(new ArrayList<>(value)));
    }

    /**
     * Annotation used for converting objects to String Maps and vice versa.
     * Indicates that a certain field should be considered in the conversion process. Otherwise it is ignored.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     * @see #deserializeStruct(java.util.Map, java.lang.Class)
     */
    @Retention(RetentionPolicy.RUNTIME) // keep annotation at runtime
    public @interface pref { }

    /**
     * Annotation used for converting objects to String Maps.
     * Indicates that a certain field should be written to the map, even if the value is the same as the default value.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     */
    @Retention(RetentionPolicy.RUNTIME) // keep annotation at runtime
    public @interface writeExplicitly { }

    /**
     * Get a list of hashes which are represented by a struct-like class.
     * Possible properties are given by fields of the class klass that have the @pref annotation.
     * Default constructor is used to initialize the struct objects, properties then override some of these default values.
     * @param <T> klass type
     * @param key main preference key
     * @param klass The struct class
     * @return a list of objects of type T or an empty list if nothing was found
     */
    public <T> List<T> getListOfStructs(String key, Class<T> klass) {
        List<T> r = getListOfStructs(key, null, klass);
        if (r == null)
            return Collections.emptyList();
        else
            return r;
    }

    /**
     * same as above, but returns def if nothing was found
     * @param <T> klass type
     * @param key main preference key
     * @param def default value
     * @param klass The struct class
     * @return a list of objects of type T or {@code def} if nothing was found
     */
    public <T> List<T> getListOfStructs(String key, Collection<T> def, Class<T> klass) {
        Collection<Map<String, String>> prop =
            getListOfStructs(key, def == null ? null : serializeListOfStructs(def, klass));
        if (prop == null)
            return def == null ? null : new ArrayList<>(def);
        List<T> lst = new ArrayList<>();
        for (Map<String, String> entries : prop) {
            T struct = deserializeStruct(entries, klass);
            lst.add(struct);
        }
        return lst;
    }

    /**
     * Convenience method that saves a MapListSetting which is provided as a collection of objects.
     *
     * Each object is converted to a <code>Map&lt;String, String&gt;</code> using the fields with {@link pref} annotation.
     * The field name is the key and the value will be converted to a string.
     *
     * Considers only fields that have the @pref annotation.
     * In addition it does not write fields with null values. (Thus they are cleared)
     * Default values are given by the field values after default constructor has been called.
     * Fields equal to the default value are not written unless the field has the @writeExplicitly annotation.
     * @param <T> the class,
     * @param key main preference key
     * @param val the list that is supposed to be saved
     * @param klass The struct class
     * @return true if something has changed
     */
    public <T> boolean putListOfStructs(String key, Collection<T> val, Class<T> klass) {
        return putListOfStructs(key, serializeListOfStructs(val, klass));
    }

    private static <T> Collection<Map<String, String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
        if (l == null)
            return null;
        Collection<Map<String, String>> vals = new ArrayList<>();
        for (T struct : l) {
            if (struct == null) {
                continue;
            }
            vals.add(serializeStruct(struct, klass));
        }
        return vals;
    }

    @SuppressWarnings("rawtypes")
    private static String mapToJson(Map map) {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter writer = Json.createWriter(stringWriter)) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            for (Object o: map.entrySet()) {
                Entry e = (Entry) o;
                Object evalue = e.getValue();
                object.add(e.getKey().toString(), evalue.toString());
            }
            writer.writeObject(object.build());
        }
        return stringWriter.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static Map mapFromJson(String s) {
        Map ret = null;
        try (JsonReader reader = Json.createReader(new StringReader(s))) {
            JsonObject object = reader.readObject();
            ret = new HashMap(object.size());
            for (Entry<String, JsonValue> e: object.entrySet()) {
                JsonValue value = e.getValue();
                if (value instanceof JsonString) {
                    // in some cases, when JsonValue.toString() is called, then additional quotation marks are left in value
                    ret.put(e.getKey(), ((JsonString) value).getString());
                } else {
                    ret.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return ret;
    }

    @SuppressWarnings("rawtypes")
    private static String multiMapToJson(MultiMap map) {
        StringWriter stringWriter = new StringWriter();
        try (JsonWriter writer = Json.createWriter(stringWriter)) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            for (Object o: map.entrySet()) {
                Entry e = (Entry) o;
                Set evalue = (Set) e.getValue();
                JsonArrayBuilder a = Json.createArrayBuilder();
                for (Object evo: evalue) {
                    a.add(evo.toString());
                }
                object.add(e.getKey().toString(), a.build());
            }
            writer.writeObject(object.build());
        }
        return stringWriter.toString();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static MultiMap multiMapFromJson(String s) {
        MultiMap ret = null;
        try (JsonReader reader = Json.createReader(new StringReader(s))) {
            JsonObject object = reader.readObject();
            ret = new MultiMap(object.size());
            for (Entry<String, JsonValue> e: object.entrySet()) {
                JsonValue value = e.getValue();
                if (value instanceof JsonArray) {
                    for (JsonString js: ((JsonArray) value).getValuesAs(JsonString.class)) {
                        ret.put(e.getKey(), js.getString());
                    }
                } else if (value instanceof JsonString) {
                    // in some cases, when JsonValue.toString() is called, then additional quotation marks are left in value
                    ret.put(e.getKey(), ((JsonString) value).getString());
                } else {
                    ret.put(e.getKey(), e.getValue().toString());
                }
            }
        }
        return ret;
    }

    /**
     * Convert an object to a String Map, by using field names and values as map key and value.
     *
     * The field value is converted to a String.
     *
     * Only fields with annotation {@link pref} are taken into account.
     *
     * Fields will not be written to the map if the value is null or unchanged
     * (compared to an object created with the no-arg-constructor).
     * The {@link writeExplicitly} annotation overrides this behavior, i.e. the default value will also be written.
     *
     * @param <T> the class of the object <code>struct</code>
     * @param struct the object to be converted
     * @param klass the class T
     * @return the resulting map (same data content as <code>struct</code>)
     */
    public static <T> Map<String, String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }

        Map<String, String> hash = new LinkedHashMap<>();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            Utils.setObjectsAccessible(f);
            try {
                Object fieldValue = f.get(struct);
                Object defaultFieldValue = f.get(structPrototype);
                if (fieldValue != null && (f.getAnnotation(writeExplicitly.class) != null || !Objects.equals(fieldValue, defaultFieldValue))) {
                    String key = f.getName().replace('_', '-');
                    if (fieldValue instanceof Map) {
                        hash.put(key, mapToJson((Map<?, ?>) fieldValue));
                    } else if (fieldValue instanceof MultiMap) {
                        hash.put(key, multiMapToJson((MultiMap<?, ?>) fieldValue));
                    } else {
                        hash.put(key, fieldValue.toString());
                    }
                }
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        return hash;
    }

    /**
     * Converts a String-Map to an object of a certain class, by comparing map keys to field names of the class and assigning
     * map values to the corresponding fields.
     *
     * The map value (a String) is converted to the field type. Supported types are: boolean, Boolean, int, Integer, double,
     * Double, String, Map&lt;String, String&gt; and Map&lt;String, List&lt;String&gt;&gt;.
     *
     * Only fields with annotation {@link pref} are taken into account.
     * @param <T> the class
     * @param hash the string map with initial values
     * @param klass the class T
     * @return an object of class T, initialized as described above
     */
    public static <T> T deserializeStruct(Map<String, String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.getConstructor().newInstance();
        } catch (ReflectiveOperationException ex) {
            throw new IllegalArgumentException(ex);
        }
        for (Entry<String, String> key_value : hash.entrySet()) {
            Object value;
            Field f;
            try {
                f = klass.getDeclaredField(key_value.getKey().replace('-', '_'));
            } catch (NoSuchFieldException ex) {
                Main.trace(ex);
                continue;
            }
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            Utils.setObjectsAccessible(f);
            if (f.getType() == Boolean.class || f.getType() == boolean.class) {
                value = Boolean.valueOf(key_value.getValue());
            } else if (f.getType() == Integer.class || f.getType() == int.class) {
                try {
                    value = Integer.valueOf(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                try {
                    value = Double.valueOf(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == String.class) {
                value = key_value.getValue();
            } else if (f.getType().isAssignableFrom(Map.class)) {
                value = mapFromJson(key_value.getValue());
            } else if (f.getType().isAssignableFrom(MultiMap.class)) {
                value = multiMapFromJson(key_value.getValue());
            } else
                throw new RuntimeException("unsupported preference primitive type");

            try {
                f.set(struct, value);
            } catch (IllegalArgumentException ex) {
                throw new AssertionError(ex);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        return struct;
    }

    public Map<String, Setting<?>> getAllSettings() {
        return new TreeMap<>(settingsMap);
    }

    public Map<String, Setting<?>> getAllDefaults() {
        return new TreeMap<>(defaultsMap);
    }

    /**
     * Updates system properties with the current values in the preferences.
     *
     */
    public void updateSystemProperties() {
        if ("true".equals(get("prefer.ipv6", "auto")) && !"true".equals(Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true"))) {
            // never set this to false, only true!
            Main.info(tr("Try enabling IPv6 network, prefering IPv6 over IPv4 (only works on early startup)."));
        }
        Utils.updateSystemProperty("http.agent", Version.getInstance().getAgentString());
        Utils.updateSystemProperty("user.language", get("language"));
        // Workaround to fix a Java bug. This ugly hack comes from Sun bug database: https://bugs.openjdk.java.net/browse/JDK-6292739
        // Force AWT toolkit to update its internal preferences (fix #6345).
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                Field field = Toolkit.class.getDeclaredField("resources");
                Utils.setObjectsAccessible(field);
                field.set(null, ResourceBundle.getBundle("sun.awt.resources.awt"));
            } catch (ReflectiveOperationException | MissingResourceException e) {
                Main.warn(e);
            }
        }
        // Possibility to disable SNI (not by default) in case of misconfigured https servers
        // See #9875 + http://stackoverflow.com/a/14884941/2257172
        // then https://josm.openstreetmap.de/ticket/12152#comment:5 for details
        if (getBoolean("jdk.tls.disableSNIExtension", false)) {
            Utils.updateSystemProperty("jsse.enableSNIExtension", "false");
        }
    }

    /**
     * Replies the collection of plugin site URLs from where plugin lists can be downloaded.
     * @return the collection of plugin site URLs
     * @see #getOnlinePluginSites
     */
    public Collection<String> getPluginSites() {
        return getCollection("pluginmanager.sites", Collections.singleton(Main.getJOSMWebsite()+"/pluginicons%<?plugins=>"));
    }

    /**
     * Returns the list of plugin sites available according to offline mode settings.
     * @return the list of available plugin sites
     * @since 8471
     */
    public Collection<String> getOnlinePluginSites() {
        Collection<String> pluginSites = new ArrayList<>(getPluginSites());
        for (Iterator<String> it = pluginSites.iterator(); it.hasNext();) {
            try {
                OnlineResource.JOSM_WEBSITE.checkOfflineAccess(it.next(), Main.getJOSMWebsite());
            } catch (OfflineAccessException ex) {
                Main.warn(ex, false);
                it.remove();
            }
        }
        return pluginSites;
    }

    /**
     * Sets the collection of plugin site URLs.
     *
     * @param sites the site URLs
     */
    public void setPluginSites(Collection<String> sites) {
        putCollection("pluginmanager.sites", sites);
    }

    /**
     * Returns XML describing these preferences.
     * @param nopass if password must be excluded
     * @return XML
     */
    public String toXML(boolean nopass) {
        return toXML(settingsMap.entrySet(), nopass, false);
    }

    /**
     * Returns XML describing the given preferences.
     * @param settings preferences settings
     * @param nopass if password must be excluded
     * @param defaults true, if default values are converted to XML, false for
     * regular preferences
     * @return XML
     */
    public String toXML(Collection<Entry<String, Setting<?>>> settings, boolean nopass, boolean defaults) {
        try (
            StringWriter sw = new StringWriter();
            PreferencesWriter prefWriter = new PreferencesWriter(new PrintWriter(sw), nopass, defaults);
        ) {
            prefWriter.write(settings);
            sw.flush();
            return sw.toString();
        } catch (IOException e) {
            Main.error(e);
            return null;
        }
    }

    /**
     * Removes obsolete preference settings. If you throw out a once-used preference
     * setting, add it to the list here with an expiry date (written as comment). If you
     * see something with an expiry date in the past, remove it from the list.
     * @param loadedVersion JOSM version when the preferences file was written
     */
    private void removeObsolete(int loadedVersion) {
        /* drop in October 2016 */
        if (loadedVersion < 9715) {
            Setting<?> setting = settingsMap.get("imagery.entries");
            if (setting instanceof MapListSetting) {
                List<Map<String, String>> l = new LinkedList<>();
                boolean modified = false;
                for (Map<String, String> map: ((MapListSetting) setting).getValue()) {
                    Map<String, String> newMap = new HashMap<>();
                    for (Entry<String, String> entry: map.entrySet()) {
                        String value = entry.getValue();
                        if ("noTileHeaders".equals(entry.getKey())) {
                            value = value.replaceFirst("\":(\".*\")\\}", "\":[$1]}");
                            if (!value.equals(entry.getValue())) {
                                modified = true;
                            }
                        }
                        newMap.put(entry.getKey(), value);
                    }
                    l.add(newMap);
                }
                if (modified) {
                    putListOfStructs("imagery.entries", l);
                }
            }
        }
        // drop in November 2016
        removeUrlFromEntries(loadedVersion, 9965,
                "mappaint.style.entries",
                "josm.openstreetmap.de/josmfile?page=Styles/LegacyStandard");
        // drop in December 2016
        removeUrlFromEntries(loadedVersion, 10063,
                "validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries",
                "resource://data/validator/power.mapcss");

        for (String key : OBSOLETE_PREF_KEYS) {
            if (settingsMap.containsKey(key)) {
                settingsMap.remove(key);
                Main.info(tr("Preference setting {0} has been removed since it is no longer used.", key));
            }
        }
    }

    private void removeUrlFromEntries(int loadedVersion, int versionMax, String key, String urlPart) {
        if (loadedVersion < versionMax) {
            Setting<?> setting = settingsMap.get(key);
            if (setting instanceof MapListSetting) {
                List<Map<String, String>> l = new LinkedList<>();
                boolean modified = false;
                for (Map<String, String> map: ((MapListSetting) setting).getValue()) {
                    String url = map.get("url");
                    if (url != null && url.contains(urlPart)) {
                        modified = true;
                    } else {
                        l.add(map);
                    }
                }
                if (modified) {
                    putListOfStructs(key, l);
                }
            }
        }
    }

    /**
     * Enables or not the preferences file auto-save mechanism (save each time a setting is changed).
     * This behaviour is enabled by default.
     * @param enable if {@code true}, makes JOSM save preferences file each time a setting is changed
     * @since 7085
     */
    public final void enableSaveOnPut(boolean enable) {
        synchronized (this) {
            saveOnPut = enable;
        }
    }
}
