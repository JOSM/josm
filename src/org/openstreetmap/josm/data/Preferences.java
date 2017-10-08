// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.data.preferences.DoubleProperty;
import org.openstreetmap.josm.data.preferences.IntegerProperty;
import org.openstreetmap.josm.spi.preferences.ListListSetting;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.data.preferences.LongProperty;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.data.preferences.PreferencesReader;
import org.openstreetmap.josm.data.preferences.PreferencesWriter;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.AbstractPreferences;
import org.openstreetmap.josm.spi.preferences.IBaseDirectories;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.I18n;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
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
public class Preferences extends AbstractPreferences implements IBaseDirectories {

    private static final String COLOR_PREFIX = "color.";
    private static final Pattern COLOR_LAYER_PATTERN = Pattern.compile("layer\\.(.+)");
    private static final Pattern COLOR_MAPPAINT_PATTERN = Pattern.compile("mappaint\\.(.+?)\\.(.+)");

    private static final String[] OBSOLETE_PREF_KEYS = {
      "imagery.layers.addedIds", /* remove entry after June 2017 */
      "projection", /* remove entry after Nov. 2017 */
      "projection.sub", /* remove entry after Nov. 2017 */
    };

    private static final long MAX_AGE_DEFAULT_PREFERENCES = TimeUnit.DAYS.toSeconds(50);

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
     * @deprecated use {@link org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent}
     */
    @Deprecated
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
     * @deprecated use {@link org.openstreetmap.josm.spi.preferences.PreferenceChangedListener}
     */
    @FunctionalInterface
    @Deprecated
    public interface PreferenceChangedListener {
        /**
         * Trigerred when a preference entry value changes.
         * @param e the preference change event
         */
        void preferenceChanged(PreferenceChangeEvent e);
    }

    /**
     * @deprecated private class is deprecated
     */
    @Deprecated
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

    private final ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener> listeners = ListenerList.create();

    private final HashMap<String, ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener>> keyListeners = new HashMap<>();

    /**
     * @deprecated deprecated private field
     */
    @Deprecated
    private final ListenerList<Preferences.PreferenceChangedListener> listenersDeprecated = ListenerList.create();

    /**
     * @deprecated deprecated private field
     */
    @Deprecated
    private final HashMap<String, ListenerList<Preferences.PreferenceChangedListener>> keyListenersDeprecated = new HashMap<>();

    /**
     * Constructs a new {@code Preferences}.
     */
    public Preferences() {
        // Default constructor
    }

    /**
     * Constructs a new {@code Preferences} from an existing instance.
     * @param pref existing preferences to copy
     * @since 12634
     */
    public Preferences(Preferences pref) {
        settingsMap.putAll(pref.settingsMap);
        defaultsMap.putAll(pref.defaultsMap);
        colornames.putAll(pref.colornames);
    }

    /**
     * Adds a new preferences listener.
     * @param listener The listener to add
     * @since 12881
     */
    @Override
    public void addPreferenceChangeListener(org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        if (listener != null) {
            listeners.addListener(listener);
        }
    }

    /**
     * Adds a new preferences listener.
     * @param listener The listener to add
     * @deprecated use {@link #addPreferenceChangeListener(org.openstreetmap.josm.spi.preferences.PreferenceChangedListener)}
     */
    @Deprecated
    public void addPreferenceChangeListener(Preferences.PreferenceChangedListener listener) {
        if (listener != null) {
            listenersDeprecated.addListener(listener);
        }
    }

    /**
     * Removes a preferences listener.
     * @param listener The listener to remove
     * @since 12881
     */
    @Override
    public void removePreferenceChangeListener(org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        listeners.removeListener(listener);
    }

    /**
     * Removes a preferences listener.
     * @param listener The listener to remove
     * @deprecated use {@link #removePreferenceChangeListener(org.openstreetmap.josm.spi.preferences.PreferenceChangedListener)}
     */
    @Deprecated
    public void removePreferenceChangeListener(Preferences.PreferenceChangedListener listener) {
        listenersDeprecated.removeListener(listener);
    }

    /**
     * Adds a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 12881
     */
    @Override
    public void addKeyPreferenceChangeListener(String key, org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        listenersForKey(key).addListener(listener);
    }

    /**
     * Adds a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 10824
     * @deprecated use
     * {@link #addKeyPreferenceChangeListener(java.lang.String, org.openstreetmap.josm.spi.preferences.PreferenceChangedListener)}
     */
    @Deprecated
    public void addKeyPreferenceChangeListener(String key, Preferences.PreferenceChangedListener listener) {
        listenersForKeyDeprecated(key).addListener(listener);
    }

    /**
     * Adds a weak listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 10824
     */
    public void addWeakKeyPreferenceChangeListener(String key, org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        listenersForKey(key).addWeakListener(listener);
    }

    private ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener> listenersForKey(String key) {
        return keyListeners.computeIfAbsent(key, k -> ListenerList.create());
    }

    /**
     * @deprecated deprecated private method
     */
    @Deprecated
    private ListenerList<Preferences.PreferenceChangedListener> listenersForKeyDeprecated(String key) {
        return keyListenersDeprecated.computeIfAbsent(key, k -> ListenerList.create());
    }

    /**
     * Removes a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @since 12881
     */
    @Override
    public void removeKeyPreferenceChangeListener(String key, org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        Optional.ofNullable(keyListeners.get(key)).orElseThrow(
                () -> new IllegalArgumentException("There are no listeners registered for " + key))
        .removeListener(listener);
    }

    /**
     * Removes a listener that only listens to changes in one preference
     * @param key The preference key to listen to
     * @param listener The listener to add.
     * @deprecated use
     * {@link #removeKeyPreferenceChangeListener(java.lang.String, org.openstreetmap.josm.spi.preferences.PreferenceChangedListener)}
     */
    @Deprecated
    public void removeKeyPreferenceChangeListener(String key, Preferences.PreferenceChangedListener listener) {
        Optional.ofNullable(keyListenersDeprecated.get(key)).orElseThrow(
                () -> new IllegalArgumentException("There are no listeners registered for " + key))
        .removeListener(listener);
    }

    protected void firePreferenceChanged(String key, Setting<?> oldValue, Setting<?> newValue) {
        final org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent evt =
                new org.openstreetmap.josm.spi.preferences.DefaultPreferenceChangeEvent(key, oldValue, newValue);
        listeners.fireEvent(listener -> listener.preferenceChanged(evt));

        ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener> forKey = keyListeners.get(key);
        if (forKey != null) {
            forKey.fireEvent(listener -> listener.preferenceChanged(evt));
        }
        firePreferenceChangedDeprecated(key, oldValue, newValue);
    }

    /**
     * @deprecated deprecated private method
     */
    @Deprecated
    private void firePreferenceChangedDeprecated(String key, Setting<?> oldValue, Setting<?> newValue) {
        final Preferences.PreferenceChangeEvent evtDeprecated = new Preferences.DefaultPreferenceChangeEvent(key, oldValue, newValue);
        listenersDeprecated.fireEvent(listener -> listener.preferenceChanged(evtDeprecated));

        ListenerList<Preferences.PreferenceChangedListener> forKeyDeprecated = keyListenersDeprecated.get(key);
        if (forKeyDeprecated != null) {
            forKeyDeprecated.fireEvent(listener -> listener.preferenceChanged(evtDeprecated));
        }
    }

    /**
     * Get the base name of the JOSM directories for preferences, cache and
     * user data.
     * Default value is "JOSM", unless overridden by system property "josm.dir.name".
     * @return the base name of the JOSM directories for preferences, cache and
     * user data
     */
    public String getJOSMDirectoryBaseName() {
        String name = System.getProperty("josm.dir.name");
        if (name != null)
            return name;
        else
            return "JOSM";
    }

    /**
     * Returns the user defined preferences directory, containing the preferences.xml file
     * @return The user defined preferences directory, containing the preferences.xml file
     * @since 7834
     * @deprecated use {@link #getPreferencesDirectory(boolean)}
     */
    @Deprecated
    public File getPreferencesDirectory() {
        return getPreferencesDirectory(false);
    }

    @Override
    public File getPreferencesDirectory(boolean createIfMissing) {
        if (preferencesDir == null) {
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
        }
        if (createIfMissing && !preferencesDir.exists() && !preferencesDir.mkdirs()) {
            Logging.warn(tr("Failed to create missing preferences directory: {0}", preferencesDir.getAbsoluteFile()));
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to create missing preferences directory: {0}</html>", preferencesDir.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return preferencesDir;
    }

    /**
     * Returns the user data directory, containing autosave, plugins, etc.
     * Depending on the OS it may be the same directory as preferences directory.
     * @return The user data directory, containing autosave, plugins, etc.
     * @since 7834
     * @deprecated use {@link #getUserDataDirectory(boolean)}
     */
    @Deprecated
    public File getUserDataDirectory() {
        return getUserDataDirectory(false);
    }

    @Override
    public File getUserDataDirectory(boolean createIfMissing) {
        if (userdataDir == null) {
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
        }
        if (createIfMissing && !userdataDir.exists() && !userdataDir.mkdirs()) {
            Logging.warn(tr("Failed to create missing user data directory: {0}", userdataDir.getAbsoluteFile()));
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to create missing user data directory: {0}</html>", userdataDir.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
        }
        return userdataDir;
    }

    /**
     * Returns the user preferences file (preferences.xml).
     * @return The user preferences file (preferences.xml)
     */
    public File getPreferenceFile() {
        return new File(getPreferencesDirectory(false), "preferences.xml");
    }

    /**
     * Returns the cache file for default preferences.
     * @return the cache file for default preferences
     */
    public File getDefaultsCacheFile() {
        return new File(getCacheDirectory(true), "default_preferences.xml");
    }

    /**
     * Returns the user plugin directory.
     * @return The user plugin directory
     */
    public File getPluginsDirectory() {
        return new File(getUserDataDirectory(false), "plugins");
    }

    /**
     * Get the directory where cached content of any kind should be stored.
     *
     * If the directory doesn't exist on the file system, it will be created by this method.
     *
     * @return the cache directory
     * @deprecated use {@link #getCacheDirectory(boolean)}
     */
    @Deprecated
    public File getCacheDirectory() {
        return getCacheDirectory(true);
    }

    @Override
    public File getCacheDirectory(boolean createIfMissing) {
        if (cacheDir == null) {
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
        }
        if (createIfMissing && !cacheDir.exists() && !cacheDir.mkdirs()) {
            Logging.warn(tr("Failed to create missing cache directory: {0}", cacheDir.getAbsoluteFile()));
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
        addPossibleResourceDir(locations, getPreferencesDirectory(false).getPath());
        addPossibleResourceDir(locations, getUserDataDirectory(false).getPath());
        addPossibleResourceDir(locations, System.getenv("JOSM_RESOURCES"));
        addPossibleResourceDir(locations, System.getProperty("josm.resources"));
        if (Main.isPlatformWindows()) {
            String appdata = System.getenv("APPDATA");
            if (appdata != null && System.getenv("ALLUSERSPROFILE") != null
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
     * Gets all normal (string) settings that have a key starting with the prefix
     * @param prefix The start of the key
     * @return The key names of the settings
     */
    public synchronized Map<String, String> getAllPrefix(final String prefix) {
        final Map<String, String> all = new TreeMap<>();
        for (final Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            if (e.getKey().startsWith(prefix) && (e.getValue() instanceof StringSetting)) {
                all.put(e.getKey(), ((StringSetting) e.getValue()).getValue());
            }
        }
        return all;
    }

    /**
     * Gets all list settings that have a key starting with the prefix
     * @param prefix The start of the key
     * @return The key names of the list settings
     */
    public synchronized List<String> getAllPrefixCollectionKeys(final String prefix) {
        final List<String> all = new LinkedList<>();
        for (Map.Entry<String, Setting<?>> entry : settingsMap.entrySet()) {
            if (entry.getKey().startsWith(prefix) && entry.getValue() instanceof ListSetting) {
                all.add(entry.getKey());
            }
        }
        return all;
    }

    /**
     * Gets all known colors (preferences starting with the color prefix)
     * @return All colors
     */
    public synchronized Map<String, String> getAllColors() {
        final Map<String, String> all = new TreeMap<>();
        for (final Entry<String, Setting<?>> e : defaultsMap.entrySet()) {
            if (e.getKey().startsWith(COLOR_PREFIX) && e.getValue() instanceof StringSetting) {
                if (e.getKey().startsWith(COLOR_PREFIX+"layer."))
                    continue; // do not add unchanged layer colors
                StringSetting d = (StringSetting) e.getValue();
                if (d.getValue() != null) {
                    all.put(e.getKey().substring(6), d.getValue());
                }
            }
        }
        for (final Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            if (e.getKey().startsWith(COLOR_PREFIX) && (e.getValue() instanceof StringSetting)) {
                all.put(e.getKey().substring(6), ((StringSetting) e.getValue()).getValue());
            }
        }
        return all;
    }

    /**
     * Gets an boolean that may be specialized
     * @param key The basic key
     * @param specName The sub-key to append to the key
     * @param def The default value
     * @return The boolean value or the default value if it could not be parsed
     * @deprecated use {@link PreferencesUtils#getBoolean(IPreferences, String, String, boolean)}
     */
    @Deprecated
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
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see BooleanProperty
     * @deprecated use {@link IPreferences#putBoolean(String, boolean)}
     */
    @Deprecated
    public boolean put(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see IntegerProperty#put(Integer)
     * @deprecated use {@link IPreferences#putInt(String, int)}
     */
    @Deprecated
    public boolean putInteger(final String key, final Integer value) {
        return put(key, Integer.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see DoubleProperty#put(Double)
     * @deprecated use {@link IPreferences#putDouble(java.lang.String, double)}
     */
    @Deprecated
    public boolean putDouble(final String key, final Double value) {
        return put(key, Double.toString(value));
    }

    /**
     * Set a boolean value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value The new value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @see LongProperty#put(Long)
     * @deprecated use {@link IPreferences#putLong(java.lang.String, long)}
     */
    @Deprecated
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

    /**
     * Stores the defaults to the defaults file
     * @throws IOException If the file could not be saved
     */
    public synchronized void saveDefaults() throws IOException {
        save(getDefaultsCacheFile(), defaultsMap.entrySet().stream(), true);
    }

    protected void save(File prefFile, Stream<Entry<String, Setting<?>>> settings, boolean defaults) throws IOException {
        if (!defaults) {
            /* currently unused, but may help to fix configuration issues in future */
            putInt("josm.version", Version.getInstance().getVersion());

            updateSystemProperties();
        }

        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if (initSuccessful && prefFile.exists() && prefFile.length() > 0) {
            Utils.copyFile(prefFile, backupFile);
        }

        try (PreferencesWriter writer = new PreferencesWriter(
                new PrintWriter(new File(prefFile + "_tmp"), StandardCharsets.UTF_8.name()), false, defaults)) {
            writer.write(settings);
        }

        File tmpFile = new File(prefFile + "_tmp");
        Utils.copyFile(tmpFile, prefFile);
        Utils.deleteFile(tmpFile, marktr("Unable to delete temporary file {0}"));

        setCorrectPermissions(prefFile);
        setCorrectPermissions(backupFile);
    }

    private static void setCorrectPermissions(File file) {
        if (!file.setReadable(false, false) && Logging.isTraceEnabled()) {
            Logging.trace(tr("Unable to set file non-readable {0}", file.getAbsolutePath()));
        }
        if (!file.setWritable(false, false) && Logging.isTraceEnabled()) {
            Logging.trace(tr("Unable to set file non-writable {0}", file.getAbsolutePath()));
        }
        if (!file.setExecutable(false, false) && Logging.isTraceEnabled()) {
            Logging.trace(tr("Unable to set file non-executable {0}", file.getAbsolutePath()));
        }
        if (!file.setReadable(true, true) && Logging.isTraceEnabled()) {
            Logging.trace(tr("Unable to set file readable {0}", file.getAbsolutePath()));
        }
        if (!file.setWritable(true, true) && Logging.isTraceEnabled()) {
            Logging.trace(tr("Unable to set file writable {0}", file.getAbsolutePath()));
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
        File prefDir = getPreferencesDirectory(false);
        if (prefDir.exists()) {
            if (!prefDir.isDirectory()) {
                Logging.warn(tr("Failed to initialize preferences. Preference directory ''{0}'' is not a directory.",
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
                Logging.warn(tr("Failed to initialize preferences. Failed to create missing preference directory: {0}",
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
                Logging.info(tr("Missing preference file ''{0}''. Creating a default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            } else if (reset) {
                File backupFile = new File(prefDir, "preferences.xml.bak");
                Main.platform.rename(preferenceFile, backupFile);
                Logging.warn(tr("Replacing existing preference file ''{0}'' with default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            }
        } catch (IOException e) {
            Logging.error(e);
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
            Logging.error(e);
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
                Logging.error(e1);
                Logging.warn(tr("Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
            }
        }
        File def = getDefaultsCacheFile();
        if (def.exists()) {
            try {
                loadDefaults();
            } catch (IOException | XMLStreamException | SAXException e) {
                Logging.error(e);
                Logging.warn(tr("Failed to load defaults cache file: {0}", def));
                defaultsMap.clear();
                if (!def.delete()) {
                    Logging.warn(tr("Failed to delete faulty defaults cache file: {0}", def));
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
        Matcher m = COLOR_LAYER_PATTERN.matcher(o);
        if (m.matches()) {
            return tr("Layer: {0}", tr(I18n.escape(m.group(1))));
        }
        String fullKey = COLOR_PREFIX + o;
        if (colornames.containsKey(fullKey)) {
            String name = colornames.get(fullKey);
            Matcher m2 = COLOR_MAPPAINT_PATTERN.matcher(name);
            if (m2.matches()) {
                return tr("Paint style {0}: {1}", tr(I18n.escape(m2.group(1))), tr(I18n.escape(m2.group(2))));
            } else {
                return tr(I18n.escape(colornames.get(fullKey)));
            }
        } else {
            return fullKey;
        }
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
        String colStr = specName != null ? get(COLOR_PREFIX+specName) : "";
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

    /**
     * Gets the default color that was registered with the preference
     * @param colKey The color name
     * @return The color
     */
    public synchronized Color getDefaultColor(String colKey) {
        StringSetting col = Utils.cast(defaultsMap.get(COLOR_PREFIX+colKey), StringSetting.class);
        String colStr = col == null ? null : col.getValue();
        return colStr == null || colStr.isEmpty() ? null : ColorHelper.html2color(colStr);
    }

    /**
     * Stores a color
     * @param colKey The color name
     * @param val The color
     * @return true if the setting was modified
     * @see ColorProperty#put(Color)
     */
    public synchronized boolean putColor(String colKey, Color val) {
        return put(COLOR_PREFIX+colKey, val != null ? ColorHelper.color2html(val, true) : null);
    }

    /**
     * Gets an integer preference
     * @param key The preference key
     * @param def The default value to use
     * @return The integer
     * @see IntegerProperty#get()
     * @deprecated use {@link IPreferences#getInt(String, int)}
     */
    @Deprecated
    public synchronized int getInteger(String key, int def) {
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

    /**
     * Gets an integer that may be specialized
     * @param key The basic key
     * @param specName The sub-key to append to the key
     * @param def The default value
     * @return The integer value or the default value if it could not be parsed
     * @deprecated use {@link PreferencesUtils#getInteger(IPreferences, String, String, int)}
     */
    @Deprecated
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
            Logging.trace(e);
        }
        return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before, {@code def} otherwise
     * @deprecated use {@link IPreferences#getList(java.lang.String, java.util.List)}
     */
    @Deprecated
    public Collection<String> getCollection(String key, Collection<String> def) {
        return getSetting(key, ListSetting.create(def), ListSetting.class).getValue();
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before, an empty collection otherwise.
     * @deprecated use {@link IPreferences#getList(java.lang.String)}
     */
    @Deprecated
    public Collection<String> getCollection(String key) {
        Collection<String> val = getList(key, null);
        return val == null ? Collections.<String>emptyList() : val;
    }

    /**
     * Removes a value from a given String collection
     * @param key The preference key the collection is stored with
     * @param value The value that should be removed in the collection
     * @see #getList(String)
     * @deprecated use {@link PreferencesUtils#removeFromList(IPreferences, String, String)}
     */
    @Deprecated
    public synchronized void removeFromCollection(String key, String value) {
        List<String> a = new ArrayList<>(getList(key, Collections.<String>emptyList()));
        a.remove(value);
        putList(key, a);
    }

    /**
     * Set a value for a certain setting. The changed setting is saved to the preference file immediately.
     * Due to caching mechanisms on modern operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param setting the value of the setting. In case it is null, the key-value entry will be removed.
     * @return {@code true}, if something has changed (i.e. value is different than before)
     */
    @Override
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
                    Logging.log(Logging.LEVEL_WARN, tr("Failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()), e);
                }
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, settingOld, settingCopy);
        return true;
    }

    /**
     * Get a setting of any type
     * @param key The key for the setting
     * @param def The default value to use if it was not found
     * @return The setting
     */
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
    @Override
    public synchronized <T extends Setting<?>> T getSetting(String key, T def, Class<T> klass) {
        CheckParameterUtil.ensureParameterNotNull(key);
        CheckParameterUtil.ensureParameterNotNull(def);
        Setting<?> oldDef = defaultsMap.get(key);
        if (oldDef != null && oldDef.isNew() && oldDef.getValue() != null && def.getValue() != null && !def.equals(oldDef)) {
            Logging.info("Defaults for " + key + " differ: " + def + " != " + defaultsMap.get(key));
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
     * @deprecated use {@link IPreferences#putList(java.lang.String, java.util.List)}
     */
    @Deprecated
    public boolean putCollection(String key, Collection<String> value) {
        return putSetting(key, value == null ? null : ListSetting.create(value));
    }

    /**
     * Saves at most {@code maxsize} items of collection {@code val}.
     * @param key key
     * @param maxsize max number of items to save
     * @param val value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @deprecated use {@link PreferencesUtils#putListBounded(IPreferences, String, int, List)}
     */
    @Deprecated
    public boolean putCollectionBounded(String key, int maxsize, Collection<String> val) {
        List<String> newCollection = new ArrayList<>(Math.min(maxsize, val.size()));
        for (String i : val) {
            if (newCollection.size() >= maxsize) {
                break;
            }
            newCollection.add(i);
        }
        return putList(key, newCollection);
    }

    /**
     * Used to read a 2-dimensional array of strings from the preference file.
     * If not a single entry could be found, <code>def</code> is returned.
     * @param key preference key
     * @param def default array value
     * @return array value
     * @deprecated use {@link #getListOfLists(java.lang.String, java.util.List)}
     */
    @Deprecated
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public synchronized Collection<Collection<String>> getArray(String key, Collection<Collection<String>> def) {
        ListListSetting val = getSetting(key, ListListSetting.create(def), ListListSetting.class);
        return (Collection) val.getValue();
    }

    /**
     * Gets a collection of string collections for the given key
     * @param key The key
     * @return The collection of string collections or an empty collection as default
     * @deprecated use {@link IPreferences#getListOfLists(java.lang.String)}
     */
    @Deprecated
    public Collection<Collection<String>> getArray(String key) {
        Collection<Collection<String>> res = getArray(key, null);
        return res == null ? Collections.<Collection<String>>emptyList() : res;
    }

    /**
     * Put an array.
     * @param key key
     * @param value value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @deprecated use {@link IPreferences#putListOfLists(java.lang.String, java.util.List)}
     */
    @Deprecated
    public boolean putArray(String key, Collection<Collection<String>> value) {
        return putSetting(key, value == null ? null : ListListSetting.create(value));
    }

    /**
     * Gets a collection of key/value maps.
     * @param key The key to search at
     * @param def The default value to use
     * @return The stored value or the default one if it could not be parsed
     * @deprecated use {@link IPreferences#getListOfMaps(java.lang.String, java.util.List)}
     */
    @Deprecated
    public Collection<Map<String, String>> getListOfStructs(String key, Collection<Map<String, String>> def) {
        return getSetting(key, new MapListSetting(def == null ? null : new ArrayList<>(def)), MapListSetting.class).getValue();
    }

    /**
     * Stores a list of structs
     * @param key The key to store the list in
     * @param value A list of key/value maps
     * @return <code>true</code> if the value was changed
     * @see #getListOfMaps(java.lang.String, java.util.List)
     * @deprecated use {@link IPreferences#putListOfMaps(java.lang.String, java.util.List)}
     */
    @Deprecated
    public boolean putListOfStructs(String key, Collection<Map<String, String>> value) {
        return putSetting(key, value == null ? null : new MapListSetting(new ArrayList<>(value)));
    }

    /**
     * Annotation used for converting objects to String Maps and vice versa.
     * Indicates that a certain field should be considered in the conversion process. Otherwise it is ignored.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     * @see #deserializeStruct(java.util.Map, java.lang.Class)
     * @deprecated use {@link StructUtils.StructEntry}
     */
    @Deprecated
    @Retention(RetentionPolicy.RUNTIME) // keep annotation at runtime
    public @interface pref { }

    /**
     * Annotation used for converting objects to String Maps.
     * Indicates that a certain field should be written to the map, even if the value is the same as the default value.
     *
     * @see #serializeStruct(java.lang.Object, java.lang.Class)
     * @deprecated use {@link StructUtils.WriteExplicitly}
     */
    @Deprecated
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
     * @deprecated use {@link StructUtils#getListOfStructs(IPreferences, String, Class)}
     */
    @Deprecated
    public <T> List<T> getListOfStructs(String key, Class<T> klass) {
        return StructUtils.getListOfStructs(this, key, klass);
    }

    /**
     * same as above, but returns def if nothing was found
     * @param <T> klass type
     * @param key main preference key
     * @param def default value
     * @param klass The struct class
     * @return a list of objects of type T or {@code def} if nothing was found
     * @deprecated use {@link StructUtils#getListOfStructs(IPreferences, String, Collection, Class)}
     */
    @Deprecated
    public <T> List<T> getListOfStructs(String key, Collection<T> def, Class<T> klass) {
        return StructUtils.getListOfStructs(this, key, def, klass);
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
     * @deprecated use {@link StructUtils#putListOfStructs(IPreferences, String, Collection, Class)}
     */
    @Deprecated
    public <T> boolean putListOfStructs(String key, Collection<T> val, Class<T> klass) {
        return StructUtils.putListOfStructs(this, key, val, klass);
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
     * @deprecated use {@link StructUtils#serializeStruct(java.lang.Object, java.lang.Class)}
     */
    @Deprecated
    public static <T> Map<String, String> serializeStruct(T struct, Class<T> klass) {
        return StructUtils.serializeStruct(struct, klass);
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
     * @deprecated use {@link StructUtils#deserializeStruct(java.util.Map, java.lang.Class)}
     */
    @Deprecated
    public static <T> T deserializeStruct(Map<String, String> hash, Class<T> klass) {
        return StructUtils.deserializeStruct(hash, klass);
    }

    /**
     * Gets a map of all settings that are currently stored
     * @return The settings
     */
    public Map<String, Setting<?>> getAllSettings() {
        return new TreeMap<>(settingsMap);
    }

    /**
     * Gets a map of all currently known defaults
     * @return The map (key/setting)
     */
    public Map<String, Setting<?>> getAllDefaults() {
        return new TreeMap<>(defaultsMap);
    }

    /**
     * Updates system properties with the current values in the preferences.
     */
    public void updateSystemProperties() {
        if ("true".equals(get("prefer.ipv6", "auto")) && !"true".equals(Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true"))) {
            // never set this to false, only true!
            Logging.info(tr("Try enabling IPv6 network, prefering IPv6 over IPv4 (only works on early startup)."));
        }
        Utils.updateSystemProperty("http.agent", Version.getInstance().getAgentString());
        Utils.updateSystemProperty("user.language", get("language"));
        // Workaround to fix a Java bug. This ugly hack comes from Sun bug database: https://bugs.openjdk.java.net/browse/JDK-6292739
        // Force AWT toolkit to update its internal preferences (fix #6345).
        // Does not work anymore with Java 9, to remove with Java 9 migration
        if (Utils.getJavaVersion() < 9 && !GraphicsEnvironment.isHeadless()) {
            try {
                Field field = Toolkit.class.getDeclaredField("resources");
                Utils.setObjectsAccessible(field);
                field.set(null, ResourceBundle.getBundle("sun.awt.resources.awt"));
            } catch (ReflectiveOperationException | RuntimeException e) { // NOPMD
                // Catch RuntimeException in order to catch InaccessibleObjectException, new in Java 9
                Logging.warn(e);
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
        return getList("pluginmanager.sites", Collections.singletonList(Main.getJOSMWebsite()+"/pluginicons%<?plugins=>"));
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
                Logging.log(Logging.LEVEL_WARN, ex);
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
        putList("pluginmanager.sites", new ArrayList<>(sites));
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
            PreferencesWriter prefWriter = new PreferencesWriter(new PrintWriter(sw), nopass, defaults)
        ) {
            prefWriter.write(settings);
            sw.flush();
            return sw.toString();
        } catch (IOException e) {
            Logging.error(e);
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
        // drop in March 2017
        removeUrlFromEntries(loadedVersion, 10063,
                "validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries",
                "resource://data/validator/power.mapcss");
        // drop in March 2017
        if (loadedVersion < 11058) {
            migrateOldColorKeys();
        }
        // drop in September 2017
        if (loadedVersion < 11424) {
            addNewerDefaultEntry(
                    "validator.org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker.entries",
                    "resource://data/validator/territories.mapcss");
        }

        for (String key : OBSOLETE_PREF_KEYS) {
            if (settingsMap.containsKey(key)) {
                settingsMap.remove(key);
                Logging.info(tr("Preference setting {0} has been removed since it is no longer used.", key));
            }
        }
    }

    private void migrateOldColorKeys() {
        settingsMap.keySet().stream()
                .filter(key -> key.startsWith(COLOR_PREFIX))
                .flatMap(this::searchOldColorKey)
                .collect(Collectors.toList()) // to avoid ConcurrentModificationException
                .forEach(entry -> {
                    final String oldKey = entry.getKey();
                    final String newKey = entry.getValue();
                    Logging.info("Migrating old color key {0} => {1}", oldKey, newKey);
                    put(newKey, get(oldKey));
                    put(oldKey, null);
                });
    }

    private Stream<AbstractMap.SimpleImmutableEntry<String, String>> searchOldColorKey(String key) {
        final String newKey = ColorProperty.getColorKey(key.substring(COLOR_PREFIX.length()));
        return key.equals(newKey) || settingsMap.containsKey(newKey)
                ? Stream.empty()
                : Stream.of(new AbstractMap.SimpleImmutableEntry<>(key, newKey));
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
                    putListOfMaps(key, l);
                }
            }
        }
    }

    private void addNewerDefaultEntry(String key, final String url) {
        Setting<?> setting = settingsMap.get(key);
        if (setting instanceof MapListSetting) {
            List<Map<String, String>> l = new ArrayList<>(((MapListSetting) setting).getValue());
            if (l.stream().noneMatch(x -> x.containsValue(url))) {
                ValidatorPrefHelper helper = ValidatorPrefHelper.INSTANCE;
                Optional<ExtendedSourceEntry> val = helper.getDefault().stream().filter(x -> url.equals(x.url)).findFirst();
                if (val.isPresent()) {
                    l.add(helper.serialize(val.get()));
                }
                putListOfMaps(key, l);
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
