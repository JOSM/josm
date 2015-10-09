// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.ColorProperty;
import org.openstreetmap.josm.io.CachedFile;
import org.openstreetmap.josm.io.OfflineAccessException;
import org.openstreetmap.josm.io.OnlineResource;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.I18n;
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

    /**
     * Maps color keys to human readable color name
     */
    protected final SortedMap<String, String> colornames = new TreeMap<>();

    /**
     * Interface for a preference value.
     *
     * Implementations must provide a proper <code>equals</code> method.
     *
     * @param <T> the data type for the value
     */
    public interface Setting<T> {
        /**
         * Returns the value of this setting.
         *
         * @return the value of this setting
         */
        T getValue();

        /**
         * Check if the value of this Setting object is equal to the given value.
         * @param otherVal the other value
         * @return true if the values are equal
         */
        boolean equalVal(T otherVal);

        /**
         * Clone the current object.
         * @return an identical copy of the current object
         */
        Setting<T> copy();

        /**
         * Enable usage of the visitor pattern.
         *
         * @param visitor the visitor
         */
        void visit(SettingVisitor visitor);

        /**
         * Returns a setting whose value is null.
         *
         * Cannot be static, because there is no static inheritance.
         * @return a Setting object that isn't null itself, but returns null
         * for {@link #getValue()}
         */
        Setting<T> getNullInstance();
    }

    /**
     * Base abstract class of all settings, holding the setting value.
     *
     * @param <T> The setting type
     */
    public abstract static class AbstractSetting<T> implements Setting<T> {
        protected final T value;
        /**
         * Constructs a new {@code AbstractSetting} with the given value
         * @param value The setting value
         */
        public AbstractSetting(T value) {
            this.value = value;
        }

        @Override
        public T getValue() {
            return value;
        }

        @Override
        public String toString() {
            return value != null ? value.toString() : "null";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (!(obj instanceof AbstractSetting))
                return false;
            AbstractSetting<?> other = (AbstractSetting<?>) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }
    }

    /**
     * Setting containing a {@link String} value.
     */
    public static class StringSetting extends AbstractSetting<String> {
        /**
         * Constructs a new {@code StringSetting} with the given value
         * @param value The setting value
         */
        public StringSetting(String value) {
            super(value);
        }

        @Override
        public boolean equalVal(String otherVal) {
            if (value == null) return otherVal == null;
            return value.equals(otherVal);
        }

        @Override
        public StringSetting copy() {
            return new StringSetting(value);
        }

        @Override
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public StringSetting getNullInstance() {
            return new StringSetting(null);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof StringSetting)) return false;
            return equalVal(((StringSetting) other).getValue());
        }
    }

    /**
     * Setting containing a {@link List} of {@link String} values.
     */
    public static class ListSetting extends AbstractSetting<List<String>> {
        /**
         * Constructs a new {@code ListSetting} with the given value
         * @param value The setting value
         */
        public ListSetting(List<String> value) {
            super(value);
            consistencyTest();
        }

        /**
         * Convenience factory method.
         * @param value the value
         * @return a corresponding ListSetting object
         */
        public static ListSetting create(Collection<String> value) {
            return new ListSetting(value == null ? null : Collections.unmodifiableList(new ArrayList<>(value)));
        }

        @Override
        public boolean equalVal(List<String> otherVal) {
            return equalCollection(value, otherVal);
        }

        public static boolean equalCollection(Collection<String> a, Collection<String> b) {
            if (a == null) return b == null;
            if (b == null) return false;
            if (a.size() != b.size()) return false;
            Iterator<String> itA = a.iterator();
            Iterator<String> itB = b.iterator();
            while (itA.hasNext()) {
                String aStr = itA.next();
                String bStr = itB.next();
                if (!Objects.equals(aStr, bStr)) return false;
            }
            return true;
        }

        @Override
        public ListSetting copy() {
            return ListSetting.create(value);
        }

        private void consistencyTest() {
            if (value != null && value.contains(null))
                throw new RuntimeException("Error: Null as list element in preference setting");
        }

        @Override
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public ListSetting getNullInstance() {
            return new ListSetting(null);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ListSetting)) return false;
            return equalVal(((ListSetting) other).getValue());
        }
    }

    /**
     * Setting containing a {@link List} of {@code List}s of {@link String} values.
     */
    public static class ListListSetting extends AbstractSetting<List<List<String>>> {

        /**
         * Constructs a new {@code ListListSetting} with the given value
         * @param value The setting value
         */
        public ListListSetting(List<List<String>> value) {
            super(value);
            consistencyTest();
        }

        /**
         * Convenience factory method.
         * @param value the value
         * @return a corresponding ListListSetting object
         */
        public static ListListSetting create(Collection<Collection<String>> value) {
            if (value != null) {
                List<List<String>> valueList = new ArrayList<>(value.size());
                for (Collection<String> lst : value) {
                    valueList.add(new ArrayList<>(lst));
                }
                return new ListListSetting(valueList);
            }
            return new ListListSetting(null);
        }

        @Override
        public boolean equalVal(List<List<String>> otherVal) {
            if (value == null) return otherVal == null;
            if (otherVal == null) return false;
            if (value.size() != otherVal.size()) return false;
            Iterator<List<String>> itA = value.iterator();
            Iterator<List<String>> itB = otherVal.iterator();
            while (itA.hasNext()) {
                if (!ListSetting.equalCollection(itA.next(), itB.next())) return false;
            }
            return true;
        }

        @Override
        public ListListSetting copy() {
            if (value == null) return new ListListSetting(null);

            List<List<String>> copy = new ArrayList<>(value.size());
            for (Collection<String> lst : value) {
                List<String> lstCopy = new ArrayList<>(lst);
                copy.add(Collections.unmodifiableList(lstCopy));
            }
            return new ListListSetting(Collections.unmodifiableList(copy));
        }

        private void consistencyTest() {
            if (value == null) return;
            if (value.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting");
            for (Collection<String> lst : value) {
                if (lst.contains(null)) throw new RuntimeException("Error: Null as inner list element in preference setting");
            }
        }

        @Override
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public ListListSetting getNullInstance() {
            return new ListListSetting(null);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof ListListSetting)) return false;
            return equalVal(((ListListSetting) other).getValue());
        }
    }

    /**
     * Setting containing a {@link List} of {@link Map}s of {@link String} values.
     */
    public static class MapListSetting extends AbstractSetting<List<Map<String, String>>> {

        /**
         * Constructs a new {@code MapListSetting} with the given value
         * @param value The setting value
         */
        public MapListSetting(List<Map<String, String>> value) {
            super(value);
            consistencyTest();
        }

        @Override
        public boolean equalVal(List<Map<String, String>> otherVal) {
            if (value == null) return otherVal == null;
            if (otherVal == null) return false;
            if (value.size() != otherVal.size()) return false;
            Iterator<Map<String, String>> itA = value.iterator();
            Iterator<Map<String, String>> itB = otherVal.iterator();
            while (itA.hasNext()) {
                if (!equalMap(itA.next(), itB.next())) return false;
            }
            return true;
        }

        private static boolean equalMap(Map<String, String> a, Map<String, String> b) {
            if (a == null) return b == null;
            if (b == null) return false;
            if (a.size() != b.size()) return false;
            for (Entry<String, String> e : a.entrySet()) {
                if (!Objects.equals(e.getValue(), b.get(e.getKey()))) return false;
            }
            return true;
        }

        @Override
        public MapListSetting copy() {
            if (value == null) return new MapListSetting(null);
            List<Map<String, String>> copy = new ArrayList<>(value.size());
            for (Map<String, String> map : value) {
                Map<String, String> mapCopy = new LinkedHashMap<>(map);
                copy.add(Collections.unmodifiableMap(mapCopy));
            }
            return new MapListSetting(Collections.unmodifiableList(copy));
        }

        private void consistencyTest() {
            if (value == null) return;
            if (value.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting");
            for (Map<String, String> map : value) {
                if (map.keySet().contains(null)) throw new RuntimeException("Error: Null as map key in preference setting");
                if (map.values().contains(null)) throw new RuntimeException("Error: Null as map value in preference setting");
            }
        }

        @Override
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }

        @Override
        public MapListSetting getNullInstance() {
            return new MapListSetting(null);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof MapListSetting)) return false;
            return equalVal(((MapListSetting) other).getValue());
        }
    }

    public interface SettingVisitor {
        void visit(StringSetting setting);

        void visit(ListSetting value);

        void visit(ListListSetting value);

        void visit(MapListSetting value);
    }

    public interface PreferenceChangeEvent {
        String getKey();

        Setting<?> getOldValue();

        Setting<?> getNewValue();
    }

    public interface PreferenceChangedListener {
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

    public interface ColorKey {
        String getColorName();

        String getSpecialName();

        Color getDefaultValue();
    }

    private final CopyOnWriteArrayList<PreferenceChangedListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * Adds a new preferences listener.
     * @param listener The listener to add
     */
    public void addPreferenceChangeListener(PreferenceChangedListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    /**
     * Removes a preferences listener.
     * @param listener The listener to remove
     */
    public void removePreferenceChangeListener(PreferenceChangedListener listener) {
        listeners.remove(listener);
    }

    protected void firePreferenceChanged(String key, Setting<?> oldValue, Setting<?> newValue) {
        PreferenceChangeEvent evt = new DefaultPreferenceChangeEvent(key, oldValue, newValue);
        for (PreferenceChangedListener l : listeners) {
            l.preferenceChanged(evt);
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
     * Returns the user preferences file (preferences.xml)
     * @return The user preferences file (preferences.xml)
     */
    public File getPreferenceFile() {
        return new File(getPreferencesDirectory(), "preferences.xml");
    }

    /**
     * Returns the user plugin directory
     * @return The user plugin directory
     */
    public File getPluginsDirectory() {
        return new File(getUserDataDirectory(), "plugins");
    }

    /**
     * Get the directory where cached content of any kind should be stored.
     *
     * If the directory doesn't exist on the file system, it will be created
     * by this method.
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

    private void addPossibleResourceDir(Set<String> locations, String s) {
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
     * @return "" if there is nothing set for the preference key,
     *  the corresponding value otherwise. The result is not null.
     */
    public synchronized String get(final String key) {
        String value = get(key, null);
        return value == null ? "" : value;
    }

    /**
     * Get settings value for a certain key and provide default a value.
     * @param key the identifier for the setting
     * @param def the default value. For each call of get() with a given key, the
     *  default value must be the same.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
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
        return s == null ? false : Boolean.parseBoolean(s);
    }

    public synchronized boolean getBoolean(final String key, final boolean def) {
        return Boolean.parseBoolean(get(key, Boolean.toString(def)));
    }

    public synchronized boolean getBoolean(final String key, final String specName, final boolean def) {
        boolean generic = getBoolean(key, def);
        String skey = key+"."+specName;
        Setting<?> prop = settingsMap.get(skey);
        if (prop instanceof StringSetting)
            return Boolean.parseBoolean(((StringSetting) prop).getValue());
        else
            return generic;
    }

    /**
     * Set a value for a certain setting.
     * @param key the unique identifier for the setting
     * @param value the value of the setting. Can be null or "" which both removes
     *  the key-value entry.
     * @return true, if something has changed (i.e. value is different than before)
     */
    public boolean put(final String key, String value) {
        if (value != null && value.isEmpty()) {
            value = null;
        }
        return putSetting(key, value == null ? null : new StringSetting(value));
    }

    public boolean put(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    public boolean putInteger(final String key, final Integer value) {
        return put(key, Integer.toString(value));
    }

    public boolean putDouble(final String key, final Double value) {
        return put(key, Double.toString(value));
    }

    public boolean putLong(final String key, final Long value) {
        return put(key, Long.toString(value));
    }

    /**
     * Called after every put. In case of a problem, do nothing but output the error in log.
     */
    public void save() throws IOException {
        /* currently unused, but may help to fix configuration issues in future */
        putInteger("josm.version", Version.getInstance().getVersion());

        updateSystemProperties();

        File prefFile = getPreferenceFile();
        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if (prefFile.exists()) {
            Utils.copyFile(prefFile, backupFile);
        }

        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(prefFile + "_tmp"), StandardCharsets.UTF_8), false)) {
            out.print(toXML(false));
        }

        File tmpFile = new File(prefFile + "_tmp");
        Utils.copyFile(tmpFile, prefFile);
        if (!tmpFile.delete()) {
            Main.warn(tr("Unable to delete temporary file {0}", tmpFile.getAbsolutePath()));
        }

        setCorrectPermissions(prefFile);
        setCorrectPermissions(backupFile);
    }

    private void setCorrectPermissions(File file) {
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
    public void load() throws IOException, SAXException, XMLStreamException {
        settingsMap.clear();
        File pref = getPreferenceFile();
        try (BufferedReader in = Files.newBufferedReader(pref.toPath(), StandardCharsets.UTF_8)) {
            validateXML(in);
        }
        try (BufferedReader in = Files.newBufferedReader(pref.toPath(), StandardCharsets.UTF_8)) {
            fromXML(in);
        }
        updateSystemProperties();
        removeObsolete();
    }

    /**
     * Initializes preferences.
     * @param reset if {@code true}, current settings file is replaced by the default one
     */
    public void init(boolean reset) {
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
        } catch (Exception e) {
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
    }

    public final void resetToDefault() {
        settingsMap.clear();
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    public synchronized Color getColor(String colName, Color def) {
        return getColor(colName, null, def);
    }

    public synchronized Color getUIColor(String colName) {
        return UIManager.getColor(colName);
    }

    /* only for preferences */
    public synchronized String getColorName(String o) {
        try {
            Matcher m = Pattern.compile("mappaint\\.(.+?)\\.(.+)").matcher(o);
            if (m.matches()) {
                return tr("Paint style {0}: {1}", tr(I18n.escape(m.group(1))), tr(I18n.escape(m.group(2))));
            }
        } catch (Exception e) {
            Main.warn(e);
        }
        try {
            Matcher m = Pattern.compile("layer (.+)").matcher(o);
            if (m.matches()) {
                return tr("Layer: {0}", tr(I18n.escape(m.group(1))));
            }
        } catch (Exception e) {
            Main.warn(e);
        }
        return tr(I18n.escape(colornames.containsKey(o) ? colornames.get(o) : o));
    }

    /**
     * Returns the color for the given key.
     * @param key The color key
     * @return the color
     */
    public Color getColor(ColorKey key) {
        return getColor(key.getColorName(), key.getSpecialName(), key.getDefaultValue());
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param specName name of the special colour settings
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    public synchronized Color getColor(String colName, String specName, Color def) {
        String colKey = ColorProperty.getColorKey(colName);
        if (!colKey.equals(colName)) {
            colornames.put(colKey, colName);
        }
        String colStr = specName != null ? get("color."+specName) : "";
        if (colStr.isEmpty()) {
            colStr = get("color." + colKey, ColorHelper.color2html(def, true));
        }
        if (colStr != null && !colStr.isEmpty()) {
            return ColorHelper.html2color(colStr);
        } else {
            return def;
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
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
        }
        return def;
    }

    public synchronized int getInteger(String key, String specName, int def) {
        String v = get(key+"."+specName);
        if (v.isEmpty())
            v = get(key, Integer.toString(def));
        if (v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch (NumberFormatException e) {
            // fall out
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
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
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
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
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
        }
        return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    public Collection<String> getCollection(String key, Collection<String> def) {
        return getSetting(key, ListSetting.create(def), ListSetting.class).getValue();
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before,
     *  an empty Collection otherwise.
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
     * Set a value for a certain setting. The changed setting is saved
     * to the preference file immediately. Due to caching mechanisms on modern
     * operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param setting the value of the setting. In case it is null, the key-value
     * entry will be removed.
     * @return true, if something has changed (i.e. value is different than before)
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
                    Main.warn(tr("Failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
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
     * @param def the default value. For each call of getSetting() with a given
     * key, the default value must be the same. <code>def</code> must not be
     * null, but the value of <code>def</code> can be null.
     * @param klass the setting type (same as T)
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    @SuppressWarnings("unchecked")
    public synchronized <T extends Setting<?>> T getSetting(String key, T def, Class<T> klass) {
        CheckParameterUtil.ensureParameterNotNull(key);
        CheckParameterUtil.ensureParameterNotNull(def);
        Setting<?> oldDef = defaultsMap.get(key);
        if (oldDef != null && oldDef.getValue() != null && def.getValue() != null && !def.equals(oldDef)) {
            Main.info("Defaults for " + key + " differ: " + def + " != " + defaultsMap.get(key));
        }
        if (def.getValue() != null || oldDef == null) {
            defaultsMap.put(key, def.copy());
        }
        Setting<?> prop = settingsMap.get(key);
        if (klass.isInstance(prop)) {
            return (T) prop;
        } else {
            return def;
        }
    }

    public boolean putCollection(String key, Collection<String> value) {
        return putSetting(key, value == null ? null : ListSetting.create(value));
    }

    /**
     * Saves at most {@code maxsize} items of collection {@code val}.
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

    public boolean putArray(String key, Collection<Collection<String>> value) {
        return putSetting(key, value == null ? null : ListListSetting.create(value));
    }

    public Collection<Map<String, String>> getListOfStructs(String key, Collection<Map<String, String>> def) {
        return getSetting(key, new MapListSetting(def == null ? null : new ArrayList<>(def)), MapListSetting.class).getValue();
    }

    public boolean putListOfStructs(String key, Collection<Map<String, String>> value) {
        return putSetting(key, value == null ? null : new MapListSetting(new ArrayList<>(value)));
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface pref { }
    @Retention(RetentionPolicy.RUNTIME) public @interface writeExplicitly { }

    /**
     * Get a list of hashes which are represented by a struct-like class.
     * Possible properties are given by fields of the class klass that have
     * the @pref annotation.
     * Default constructor is used to initialize the struct objects, properties
     * then override some of these default values.
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
     * Save a list of hashes represented by a struct-like class.
     * Considers only fields that have the @pref annotation.
     * In addition it does not write fields with null values. (Thus they are cleared)
     * Default values are given by the field values after default constructor has
     * been called.
     * Fields equal to the default value are not written unless the field has
     * the @writeExplicitly annotation.
     * @param key main preference key
     * @param val the list that is supposed to be saved
     * @param klass The struct class
     * @return true if something has changed
     */
    public <T> boolean putListOfStructs(String key, Collection<T> val, Class<T> klass) {
        return putListOfStructs(key, serializeListOfStructs(val, klass));
    }

    private <T> Collection<Map<String, String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
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
                object.add(e.getKey().toString(), e.getValue().toString());
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

    public static <T> Map<String, String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        Map<String, String> hash = new LinkedHashMap<>();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object fieldValue = f.get(struct);
                Object defaultFieldValue = f.get(structPrototype);
                if (fieldValue != null) {
                    if (f.getAnnotation(writeExplicitly.class) != null || !Objects.equals(fieldValue, defaultFieldValue)) {
                        String key = f.getName().replace("_", "-");
                        if (fieldValue instanceof Map) {
                            hash.put(key, mapToJson((Map) fieldValue));
                        } else {
                            hash.put(key, fieldValue.toString());
                        }
                    }
                }
            } catch (IllegalArgumentException | IllegalAccessException ex) {
                throw new RuntimeException(ex);
            }
        }
        return hash;
    }

    public static <T> T deserializeStruct(Map<String, String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.newInstance();
        } catch (InstantiationException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
        for (Entry<String, String> key_value : hash.entrySet()) {
            Object value = null;
            Field f;
            try {
                f = klass.getDeclaredField(key_value.getKey().replace("-", "_"));
            } catch (NoSuchFieldException ex) {
                continue;
            } catch (SecurityException ex) {
                throw new RuntimeException(ex);
            }
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
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
            } else  if (f.getType() == String.class) {
                value = key_value.getValue();
            } else if (f.getType().isAssignableFrom(Map.class)) {
                value = mapFromJson(key_value.getValue());
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
        if ("true".equals(get("prefer.ipv6", "auto"))) {
            // never set this to false, only true!
            if (!"true".equals(Utils.updateSystemProperty("java.net.preferIPv6Addresses", "true"))) {
                Main.info(tr("Try enabling IPv6 network, prefering IPv6 over IPv4 (only works on early startup)."));
            }
        }
        Utils.updateSystemProperty("http.agent", Version.getInstance().getAgentString());
        Utils.updateSystemProperty("user.language", get("language"));
        // Workaround to fix a Java bug.
        // Force AWT toolkit to update its internal preferences (fix #6345).
        // This ugly hack comes from Sun bug database: https://bugs.openjdk.java.net/browse/JDK-6292739
        if (!GraphicsEnvironment.isHeadless()) {
            try {
                Field field = Toolkit.class.getDeclaredField("resources");
                field.setAccessible(true);
                field.set(null, ResourceBundle.getBundle("sun.awt.resources.awt"));
            } catch (Exception | InternalError e) {
                // Ignore all exceptions, including internal error raised by Java 9 Jigsaw EA:
                // java.lang.InternalError: legacy getBundle can't be used to find sun.awt.resources.awt in module java.desktop
                // InternalError catch to remove when https://bugs.openjdk.java.net/browse/JI-9025152 is resolved
                if (Main.isTraceEnabled()) {
                    Main.trace(e.getMessage());
                }
            }
        }
        // Workaround to fix a Java "feature"
        // See http://stackoverflow.com/q/7615645/2257172 and #9875
        if (getBoolean("jdk.tls.disableSNIExtension", true)) {
            Utils.updateSystemProperty("jsse.enableSNIExtension", "false");
        }
        // Workaround to fix another Java bug
        // Force Java 7 to use old sorting algorithm of Arrays.sort (fix #8712).
        // See Oracle bug database: https://bugs.openjdk.java.net/browse/JDK-7075600
        // and https://bugs.openjdk.java.net/browse/JDK-6923200
        if (getBoolean("jdk.Arrays.useLegacyMergeSort", !Version.getInstance().isLocalBuild())) {
            Utils.updateSystemProperty("java.util.Arrays.useLegacyMergeSort", "true");
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

    protected XMLStreamReader parser;

    public void validateXML(Reader in) throws IOException, SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream xsdStream = new CachedFile("resource://data/preferences.xsd").getInputStream()) {
            Schema schema = factory.newSchema(new StreamSource(xsdStream));
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(in));
        }
    }

    public void fromXML(Reader in) throws XMLStreamException {
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
        this.parser = parser;
        parse();
    }

    public void parse() throws XMLStreamException {
        int event = parser.getEventType();
        while (true) {
            if (event == XMLStreamConstants.START_ELEMENT) {
                parseRoot();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
            if (parser.hasNext()) {
                event = parser.next();
            } else {
                break;
            }
        }
        parser.close();
    }

    public void parseRoot() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = parser.getLocalName();
                switch(localName) {
                case "tag":
                    settingsMap.put(parser.getAttributeValue(null, "key"), new StringSetting(parser.getAttributeValue(null, "value")));
                    jumpToEnd();
                    break;
                case "list":
                case "collection":
                case "lists":
                case "maps":
                    parseToplevelList();
                    break;
                default:
                    throwException("Unexpected element: "+localName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    private void jumpToEnd() throws XMLStreamException {
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                jumpToEnd();
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                return;
            }
        }
    }

    protected void parseToplevelList() throws XMLStreamException {
        String key = parser.getAttributeValue(null, "key");
        String name = parser.getLocalName();

        List<String> entries = null;
        List<List<String>> lists = null;
        List<Map<String, String>> maps = null;
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                String localName = parser.getLocalName();
                switch(localName) {
                case "entry":
                    if (entries == null) {
                        entries = new ArrayList<>();
                    }
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                    break;
                case "list":
                    if (lists == null) {
                        lists = new ArrayList<>();
                    }
                    lists.add(parseInnerList());
                    break;
                case "map":
                    if (maps == null) {
                        maps = new ArrayList<>();
                    }
                    maps.add(parseMap());
                    break;
                default:
                    throwException("Unexpected element: "+localName);
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (entries != null) {
            settingsMap.put(key, new ListSetting(Collections.unmodifiableList(entries)));
        } else if (lists != null) {
            settingsMap.put(key, new ListListSetting(Collections.unmodifiableList(lists)));
        } else if (maps != null) {
            settingsMap.put(key, new MapListSetting(Collections.unmodifiableList(maps)));
        } else {
            if ("lists".equals(name)) {
                settingsMap.put(key, new ListListSetting(Collections.<List<String>>emptyList()));
            } else if ("maps".equals(name)) {
                settingsMap.put(key, new MapListSetting(Collections.<Map<String, String>>emptyList()));
            } else {
                settingsMap.put(key, new ListSetting(Collections.<String>emptyList()));
            }
        }
    }

    protected List<String> parseInnerList() throws XMLStreamException {
        List<String> entries = new ArrayList<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("entry".equals(parser.getLocalName())) {
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableList(entries);
    }

    protected Map<String, String> parseMap() throws XMLStreamException {
        Map<String, String> map = new LinkedHashMap<>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if ("tag".equals(parser.getLocalName())) {
                    map.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return Collections.unmodifiableMap(map);
    }

    protected void throwException(String msg) {
        throw new RuntimeException(msg + tr(" (at line {0}, column {1})",
                parser.getLocation().getLineNumber(), parser.getLocation().getColumnNumber()));
    }

    private class SettingToXml implements SettingVisitor {
        private StringBuilder b;
        private boolean noPassword;
        private String key;

        SettingToXml(StringBuilder b, boolean noPassword) {
            this.b = b;
            this.noPassword = noPassword;
        }

        public void setKey(String key) {
            this.key = key;
        }

        @Override
        public void visit(StringSetting setting) {
            if (noPassword && "osm-server.password".equals(key))
                return; // do not store plain password.
            /* don't save default values */
            if (setting.equals(defaultsMap.get(key)))
                return;
            b.append("  <tag key='");
            b.append(XmlWriter.encode(key));
            b.append("' value='");
            b.append(XmlWriter.encode(setting.getValue()));
            b.append("'/>\n");
        }

        @Override
        public void visit(ListSetting setting) {
            /* don't save default values */
            if (setting.equals(defaultsMap.get(key)))
                return;
            b.append("  <list key='").append(XmlWriter.encode(key)).append("'>\n");
            for (String s : setting.getValue()) {
                b.append("    <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
            }
            b.append("  </list>\n");
        }

        @Override
        public void visit(ListListSetting setting) {
            /* don't save default values */
            if (setting.equals(defaultsMap.get(key)))
                return;
            b.append("  <lists key='").append(XmlWriter.encode(key)).append("'>\n");
            for (List<String> list : setting.getValue()) {
                b.append("    <list>\n");
                for (String s : list) {
                    b.append("      <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
                }
                b.append("    </list>\n");
            }
            b.append("  </lists>\n");
        }

        @Override
        public void visit(MapListSetting setting) {
            b.append("  <maps key='").append(XmlWriter.encode(key)).append("'>\n");
            for (Map<String, String> struct : setting.getValue()) {
                b.append("    <map>\n");
                for (Entry<String, String> e : struct.entrySet()) {
                    b.append("      <tag key='").append(XmlWriter.encode(e.getKey()))
                     .append("' value='").append(XmlWriter.encode(e.getValue())).append("'/>\n");
                }
                b.append("    </map>\n");
            }
            b.append("  </maps>\n");
        }
    }

    public String toXML(boolean nopass) {
        StringBuilder b = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<preferences xmlns=\"")
                .append(Main.getXMLBase()).append("/preferences-1.0\" version=\"")
                .append(Version.getInstance().getVersion()).append("\">\n");
        SettingToXml toXml = new SettingToXml(b, nopass);
        for (Entry<String, Setting<?>> e : settingsMap.entrySet()) {
            toXml.setKey(e.getKey());
            e.getValue().visit(toXml);
        }
        b.append("</preferences>\n");
        return b.toString();
    }

    /**
     * Removes obsolete preference settings. If you throw out a once-used preference
     * setting, add it to the list here with an expiry date (written as comment). If you
     * see something with an expiry date in the past, remove it from the list.
     */
    public void removeObsolete() {
        // drop this block end of 2015
        // update old style JOSM server links to use zip now, see #10581
        // actually also cache and mirror entries should be cleared
        if (getInteger("josm.version", Version.getInstance().getVersion()) < 8099) {
            for (String key: new String[]{"mappaint.style.entries", "taggingpreset.entries"}) {
                Collection<Map<String, String>> data = getListOfStructs(key, (Collection<Map<String, String>>) null);
                if (data != null) {
                    List<Map<String, String>> newlist = new ArrayList<Map<String, String>>();
                    boolean modified = false;
                    for (Map<String, String> map : data) {
                         Map<String, String> newmap = new LinkedHashMap<String, String>();
                         for (Entry<String, String> entry : map.entrySet()) {
                             String val = entry.getValue();
                             String mkey = entry.getKey();
                             if ("url".equals(mkey) && val.contains("josm.openstreetmap.de/josmfile") && !val.contains("zip=1")) {
                                 val += "&zip=1";
                                 modified = true;

                             }
                             newmap.put(mkey, val);
                         }
                         newlist.add(newmap);
                    }
                    if (modified) {
                        putListOfStructs(key, newlist);
                    }
                }
            }
        }

        String[] obsolete = {
                "remote.control.host", // replaced by individual values for IPv4 and IPv6. To remove end of 2015
                "osm.notes.enableDownload", // was used prior to r8071 when notes was an hidden feature. To remove end of 2015
                "mappaint.style.migration.switchedToMapCSS", // was used prior to 8315 for MapCSS switch. To remove end of 2015
                "mappaint.style.migration.changedXmlName" // was used prior to 8315 for MapCSS switch. To remove end of 2015
        };
        for (String key : obsolete) {
            if (settingsMap.containsKey(key)) {
                settingsMap.remove(key);
                Main.info(tr("Preference setting {0} has been removed since it is no longer used.", key));
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
