// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.Utils.getSystemEnv;
import static org.openstreetmap.josm.tools.Utils.getSystemProperty;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;
import javax.xml.stream.XMLStreamException;

import org.openstreetmap.josm.data.preferences.ColorInfo;
import org.openstreetmap.josm.data.preferences.JosmBaseDirectories;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.data.preferences.PreferencesReader;
import org.openstreetmap.josm.data.preferences.PreferencesWriter;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.spi.preferences.AbstractPreferences;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.spi.preferences.DefaultPreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.IBaseDirectories;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.spi.preferences.PreferenceChangeEvent;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.ReflectionUtils;
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
public class Preferences extends AbstractPreferences {

    /** remove if key equals */
    private static final String[] OBSOLETE_PREF_KEYS = {
        "remotecontrol.https.enabled", /* remove entry after Dec. 2019 */
        "remotecontrol.https.port", /* remove entry after Dec. 2019 */
    };

    /** remove if key starts with */
    private static final String[] OBSOLETE_PREF_KEYS_START = {
            //only remove layer specific prefs
            "draw.rawgps.layer.wpt.",
            "draw.rawgps.layer.audiowpt.",
            "draw.rawgps.lines.force.",
            "draw.rawgps.lines.alpha-blend.",
            "draw.rawgps.lines.",
            "markers.show ", //uses space as separator
            "marker.makeautomarker.",
            "clr.layer.",

            //remove both layer specific and global prefs
            "draw.rawgps.colors",
            "draw.rawgps.direction",
            "draw.rawgps.alternatedirection",
            "draw.rawgps.linewidth",
            "draw.rawgps.max-line-length.local",
            "draw.rawgps.max-line-length",
            "draw.rawgps.large",
            "draw.rawgps.large.size",
            "draw.rawgps.hdopcircle",
            "draw.rawgps.min-arrow-distance",
            "draw.rawgps.colorTracksTune",
            "draw.rawgps.colors.dynamic",
            "draw.rawgps.lines.local",
            "draw.rawgps.heatmap"
    };

    /** keep subkey even if it starts with any of {@link #OBSOLETE_PREF_KEYS_START} */
    private static final List<String> KEEP_PREF_KEYS = Arrays.asList(
            "draw.rawgps.lines.alpha-blend",
            "draw.rawgps.lines.arrows",
            "draw.rawgps.lines.arrows.fast",
            "draw.rawgps.lines.arrows.min-distance",
            "draw.rawgps.lines.force",
            "draw.rawgps.lines.max-length",
            "draw.rawgps.lines.max-length.local",
            "draw.rawgps.lines.width"
    );

    /** rename keys that equal */
    private static final Map<String, String> UPDATE_PREF_KEYS = getUpdatePrefKeys();

    private static Map<String, String> getUpdatePrefKeys() {
        HashMap<String, String> m = new HashMap<>();
        m.put("draw.rawgps.direction", "draw.rawgps.lines.arrows");
        m.put("draw.rawgps.alternatedirection", "draw.rawgps.lines.arrows.fast");
        m.put("draw.rawgps.min-arrow-distance", "draw.rawgps.lines.arrows.min-distance");
        m.put("draw.rawgps.linewidth", "draw.rawgps.lines.width");
        m.put("draw.rawgps.max-line-length.local", "draw.rawgps.lines.max-length.local");
        m.put("draw.rawgps.max-line-length", "draw.rawgps.lines.max-length");
        m.put("draw.rawgps.large", "draw.rawgps.points.large");
        m.put("draw.rawgps.large.alpha", "draw.rawgps.points.large.alpha");
        m.put("draw.rawgps.large.size", "draw.rawgps.points.large.size");
        m.put("draw.rawgps.hdopcircle", "draw.rawgps.points.hdopcircle");
        m.put("draw.rawgps.layer.wpt.pattern", "draw.rawgps.markers.pattern");
        m.put("draw.rawgps.layer.audiowpt.pattern", "draw.rawgps.markers.audio.pattern");
        m.put("draw.rawgps.colors", "draw.rawgps.colormode");
        m.put("draw.rawgps.colorTracksTune", "draw.rawgps.colormode.velocity.tune");
        m.put("draw.rawgps.colors.dynamic", "draw.rawgps.colormode.dynamic-range");
        m.put("draw.rawgps.heatmap.line-extra", "draw.rawgps.colormode.heatmap.line-extra");
        m.put("draw.rawgps.heatmap.colormap", "draw.rawgps.colormode.heatmap.colormap");
        m.put("draw.rawgps.heatmap.use-points", "draw.rawgps.colormode.heatmap.use-points");
        m.put("draw.rawgps.heatmap.gain", "draw.rawgps.colormode.heatmap.gain");
        m.put("draw.rawgps.heatmap.lower-limit", "draw.rawgps.colormode.heatmap.lower-limit");
        m.put("draw.rawgps.date-coloring-min-dt", "draw.rawgps.colormode.time.min-distance");
        return Collections.unmodifiableMap(m);
    }

    private static final long MAX_AGE_DEFAULT_PREFERENCES = TimeUnit.DAYS.toSeconds(50);

    private final IBaseDirectories dirs;
    boolean modifiedDefault;

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
     * Indicates whether {@link #init(boolean)} completed successfully.
     * Used to decide whether to write backup preference file in {@link #save()}
     */
    protected boolean initSuccessful;

    private final ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener> listeners = ListenerList.create();

    private final HashMap<String, ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener>> keyListeners = new HashMap<>();

    private static final Preferences defaultInstance = new Preferences(JosmBaseDirectories.getInstance());

    /**
     * Preferences classes calling directly the method {@link #putSetting(String, Setting)}.
     * This collection allows us to exclude them when searching the business class who set a preference.
     * The found class is used as event source when notifying event listeners.
     */
    private static final Collection<Class<?>> preferencesClasses = Arrays.asList(
            Preferences.class, PreferencesUtils.class, AbstractPreferences.class);

    /**
     * Constructs a new {@code Preferences}.
     */
    public Preferences() {
        this.dirs = Config.getDirs();
    }

    /**
     * Constructs a new {@code Preferences}.
     *
     * @param dirs the directories to use for saving the preferences
     */
    public Preferences(IBaseDirectories dirs) {
        this.dirs = dirs;
    }

    /**
     * Constructs a new {@code Preferences} from an existing instance.
     * @param pref existing preferences to copy
     * @since 12634
     */
    public Preferences(Preferences pref) {
        this(pref.dirs);
        settingsMap.putAll(pref.settingsMap);
        defaultsMap.putAll(pref.defaultsMap);
    }

    /**
     * Returns the main (default) preferences instance.
     * @return the main (default) preferences instance
     * @since 14149
     */
    public static Preferences main() {
        return defaultInstance;
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
     * Removes a preferences listener.
     * @param listener The listener to remove
     * @since 12881
     */
    @Override
    public void removePreferenceChangeListener(org.openstreetmap.josm.spi.preferences.PreferenceChangedListener listener) {
        listeners.removeListener(listener);
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

    protected void firePreferenceChanged(String key, Setting<?> oldValue, Setting<?> newValue) {
        final Class<?> source = ReflectionUtils.findCallerClass(preferencesClasses);
        final PreferenceChangeEvent evt =
                new DefaultPreferenceChangeEvent(source != null ? source : getClass(), key, oldValue, newValue);
        listeners.fireEvent(listener -> listener.preferenceChanged(evt));

        ListenerList<org.openstreetmap.josm.spi.preferences.PreferenceChangedListener> forKey = keyListeners.get(key);
        if (forKey != null) {
            forKey.fireEvent(listener -> listener.preferenceChanged(evt));
        }
    }

    /**
     * Get the base name of the JOSM directories for preferences, cache and user data.
     * Default value is "JOSM", unless overridden by system property "josm.dir.name".
     * @return the base name of the JOSM directories for preferences, cache and user data
     */
    public static String getJOSMDirectoryBaseName() {
        String name = getSystemProperty("josm.dir.name");
        if (name != null)
            return name;
        else
            return "JOSM";
    }

    /**
     * Get the base directories associated with this preference instance.
     * @return the base directories
     */
    public IBaseDirectories getDirs() {
        return dirs;
    }

    /**
     * Returns the user preferences file (preferences.xml).
     * @return The user preferences file (preferences.xml)
     */
    public File getPreferenceFile() {
        return new File(dirs.getPreferencesDirectory(false), "preferences.xml");
    }

    /**
     * Returns the cache file for default preferences.
     * @return the cache file for default preferences
     */
    public File getDefaultsCacheFile() {
        return new File(dirs.getCacheDirectory(true), "default_preferences.xml");
    }

    /**
     * Returns the user plugin directory.
     * @return The user plugin directory
     */
    public File getPluginsDirectory() {
        return new File(dirs.getUserDataDirectory(false), "plugins");
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
    public static Collection<String> getAllPossiblePreferenceDirs() {
        Set<String> locations = new HashSet<>();
        addPossibleResourceDir(locations, defaultInstance.dirs.getPreferencesDirectory(false).getPath());
        addPossibleResourceDir(locations, defaultInstance.dirs.getUserDataDirectory(false).getPath());
        addPossibleResourceDir(locations, getSystemEnv("JOSM_RESOURCES"));
        addPossibleResourceDir(locations, getSystemProperty("josm.resources"));
        locations.addAll(PlatformManager.getPlatform().getPossiblePreferenceDirs());
        return locations;
    }

    /**
     * Get all named colors, including customized and the default ones.
     * @return a map of all named colors (maps preference key to {@link ColorInfo})
     */
    public synchronized Map<String, ColorInfo> getAllNamedColors() {
        final Map<String, ColorInfo> all = new TreeMap<>();
        for (final Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            if (!e.getKey().startsWith(NamedColorProperty.NAMED_COLOR_PREFIX))
                continue;
            Utils.instanceOfAndCast(e.getValue(), ListSetting.class)
                    .map(ListSetting::getValue)
                    .map(lst -> ColorInfo.fromPref(lst, false))
                    .ifPresent(info -> all.put(e.getKey(), info));
        }
        for (final Entry<String, Setting<?>> e : defaultsMap.entrySet()) {
            if (!e.getKey().startsWith(NamedColorProperty.NAMED_COLOR_PREFIX))
                continue;
            Utils.instanceOfAndCast(e.getValue(), ListSetting.class)
                    .map(ListSetting::getValue)
                    .map(lst -> ColorInfo.fromPref(lst, true))
                    .ifPresent(infoDef -> {
                        ColorInfo info = all.get(e.getKey());
                        if (info == null) {
                            all.put(e.getKey(), infoDef);
                        } else {
                            info.setDefaultValue(infoDef.getDefaultValue());
                        }
                    });
        }
        return all;
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
        }

        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if (initSuccessful && prefFile.exists() && prefFile.length() > 0) {
            Utils.copyFile(prefFile, backupFile);
        }

        try (PreferencesWriter writer = new PreferencesWriter(
                new PrintWriter(new File(prefFile + "_tmp"), StandardCharsets.UTF_8.name()), false, defaults)) {
            writer.write(settings);
        } catch (SecurityException e) {
            throw new IOException(e);
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
        removeAndUpdateObsolete(reader.getVersion());
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
        File prefDir = dirs.getPreferencesDirectory(false);
        if (prefDir.exists()) {
            if (!prefDir.isDirectory()) {
                Logging.warn(tr("Failed to initialize preferences. Preference directory ''{0}'' is not a directory.",
                        prefDir.getAbsoluteFile()));
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("<html>Failed to initialize preferences.<br>Preference directory ''{0}'' is not a directory.</html>",
                                    prefDir.getAbsoluteFile()),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
                return;
            }
        } else {
            if (!prefDir.mkdirs()) {
                Logging.warn(tr("Failed to initialize preferences. Failed to create missing preference directory: {0}",
                        prefDir.getAbsoluteFile()));
                if (!GraphicsEnvironment.isHeadless()) {
                    JOptionPane.showMessageDialog(
                            MainApplication.getMainFrame(),
                            tr("<html>Failed to initialize preferences.<br>Failed to create missing preference directory: {0}</html>",
                                    prefDir.getAbsoluteFile()),
                            tr("Error"),
                            JOptionPane.ERROR_MESSAGE
                    );
                }
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
                PlatformManager.getPlatform().rename(preferenceFile, backupFile);
                Logging.warn(tr("Replacing existing preference file ''{0}'' with default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            }
        } catch (IOException | InvalidPathException e) {
            Logging.error(e);
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("<html>Failed to initialize preferences.<br>Failed to reset preference file to default: {0}</html>",
                                getPreferenceFile().getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
            return;
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
        try {
            load();
            initSuccessful = true;
        } catch (IOException | SAXException | XMLStreamException e) {
            Logging.error(e);
            File backupFile = new File(prefDir, "preferences.xml.bak");
            if (!GraphicsEnvironment.isHeadless()) {
                JOptionPane.showMessageDialog(
                        MainApplication.getMainFrame(),
                        tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> " +
                                "and creating a new default preference file.</html>",
                                backupFile.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
            }
            PlatformManager.getPlatform().rename(preferenceFile, backupFile);
            try {
                resetToDefault();
                save();
            } catch (IOException e1) {
                Logging.error(e1);
                Logging.warn(tr("Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
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
                } catch (IOException | InvalidPathException e) {
                    File file = getPreferenceFile();
                    try {
                        file = file.getAbsoluteFile();
                    } catch (SecurityException ex) {
                        Logging.trace(ex);
                    }
                    Logging.log(Logging.LEVEL_WARN, tr("Failed to persist preferences to ''{0}''", file), e);
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

    @Override
    public Set<String> getKeySet() {
        return Collections.unmodifiableSet(settingsMap.keySet());
    }

    @Override
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
     * Replies the collection of plugin site URLs from where plugin lists can be downloaded.
     * @return the collection of plugin site URLs
     * @see #getOnlinePluginSites
     */
    public Collection<String> getPluginSites() {
        return getList("pluginmanager.sites", Collections.singletonList(Config.getUrls().getJOSMWebsite()+"/pluginicons%<?plugins=>"));
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
                OnlineResource.JOSM_WEBSITE.checkOfflineAccess(it.next(), Config.getUrls().getJOSMWebsite());
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
     * Removes and updates obsolete preference settings. If you throw out a once-used preference
     * setting, add it to the list here with an expiry date (written as comment). If you
     * see something with an expiry date in the past, remove it from the list.
     * @param loadedVersion JOSM version when the preferences file was written
     */
    private void removeAndUpdateObsolete(int loadedVersion) {
        Logging.trace("Update obsolete preference keys for version {0}", Integer.toString(loadedVersion));
        for (Entry<String, String> e : UPDATE_PREF_KEYS.entrySet()) {
            String oldkey = e.getKey();
            String newkey = e.getValue();
            if (settingsMap.containsKey(oldkey)) {
                Setting<?> value = settingsMap.remove(oldkey);
                settingsMap.putIfAbsent(newkey, value);
                Logging.info(tr("Updated preference setting {0} to {1}", oldkey, newkey));
            }
        }

        Logging.trace("Remove obsolete preferences for version {0}", Integer.toString(loadedVersion));
        for (String key : OBSOLETE_PREF_KEYS) {
            if (settingsMap.containsKey(key)) {
                settingsMap.remove(key);
                Logging.info(tr("Removed preference setting {0} since it is no longer used", key));
            }
            if (defaultsMap.containsKey(key)) {
                defaultsMap.remove(key);
                Logging.info(tr("Removed preference default {0} since it is no longer used", key));
                modifiedDefault = true;
            }
        }
        for (String key : OBSOLETE_PREF_KEYS_START) {
            settingsMap.entrySet().stream()
            .filter(e -> e.getKey().startsWith(key))
            .collect(Collectors.toSet())
            .forEach(e -> {
                String k = e.getKey();
                if (!KEEP_PREF_KEYS.contains(k)) {
                    settingsMap.remove(k);
                    Logging.info(tr("Removed preference setting {0} since it is no longer used", k));
                }
            });
            defaultsMap.entrySet().stream()
            .filter(e -> e.getKey().startsWith(key))
            .collect(Collectors.toSet())
            .forEach(e -> {
                String k = e.getKey();
                if (!KEEP_PREF_KEYS.contains(k)) {
                    defaultsMap.remove(k);
                    Logging.info(tr("Removed preference default {0} since it is no longer used", k));
                    modifiedDefault = true;
                }
            });
        }
        if (!getBoolean("preferences.reset.draw.rawgps.lines")) {
            // see #18444
            // add "preferences.reset.draw.rawgps.lines" to OBSOLETE_PREF_KEYS when removing
            putBoolean("preferences.reset.draw.rawgps.lines", true);
            putInt("draw.rawgps.lines", -1);
        }
        if (modifiedDefault) {
            try {
                saveDefaults();
                Logging.info(tr("Saved updated default preferences."));
            } catch (IOException ex) {
                Logging.log(Logging.LEVEL_WARN, tr("Failed to save default preferences."), ex);
            }
            modifiedDefault = false;
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
