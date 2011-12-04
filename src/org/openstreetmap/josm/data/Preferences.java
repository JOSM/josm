// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;

/**
 * This class holds all preferences for JOSM.
 *
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 *
 * Each property is a simple key=value pair of Strings.
 * In addition, each key has a unique default value that is set when the value is first
 * accessed using one of the get...() methods. You can use the same preference
 * key in different parts of the code, but the default value must be the same
 * everywhere. A default value of null means, the setting has been requested, but
 * no default value was set. This is used in advanced preferences to present a list
 * off all possible settings.
 *
 * At the moment, there is no such thing as an empty value.
 * If you put "" or null as value, the property is removed.
 *
 * @author imi
 */
public class Preferences {
    /**
     * Internal storage for the preference directory.
     * Do not access this variable directly!
     * @see #getPreferencesDirFile()
     */
    private File preferencesDirFile = null;

    /**
     * Map the property name to the property object. Does not contain null or "" values.
     */
    protected final SortedMap<String, String> properties = new TreeMap<String, String>();
    protected final SortedMap<String, String> defaults = new TreeMap<String, String>();
    protected final SortedMap<String, String> colornames = new TreeMap<String, String>();

    protected final SortedMap<String, List<String>> collectionProperties = new TreeMap<String, List<String>>();
    protected final SortedMap<String, List<String>> collectionDefaults = new TreeMap<String, List<String>>();

    protected final SortedMap<String, List<List<String>>> arrayProperties = new TreeMap<String, List<List<String>>>();
    protected final SortedMap<String, List<List<String>>> arrayDefaults = new TreeMap<String, List<List<String>>>();

    protected final SortedMap<String, List<Map<String,String>>> listOfStructsProperties = new TreeMap<String, List<Map<String,String>>>();
    protected final SortedMap<String, List<Map<String,String>>> listOfStructsDefaults = new TreeMap<String, List<Map<String,String>>>();

    public interface Setting<T> {
        T getValue();
        void visit(SettingVisitor visitor);
        Setting<T> getNullInstance();
    }

    abstract public static class AbstractSetting<T> implements Setting<T> {
        private T value;
        public AbstractSetting(T value) {
            this.value = value;
        }
        public T getValue() {
            return value;
        }
    }

    public static class StringSetting extends AbstractSetting<String> {
        public StringSetting(String value) {
            super(value);
        }
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        public StringSetting getNullInstance() {
            return new StringSetting(null);
        }
    }

    public static class ListSetting extends AbstractSetting<List<String>> {
        public ListSetting(List<String> value) {
            super(value);
        }
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        public ListSetting getNullInstance() {
            return new ListSetting(null);
        }
    }

    public static class ListListSetting extends AbstractSetting<List<List<String>>> {
        public ListListSetting(List<List<String>> value) {
            super(value);
        }
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        public ListListSetting getNullInstance() {
            return new ListListSetting(null);
        }
    }

    public static class MapListSetting extends AbstractSetting<List<Map<String, String>>> {
        public MapListSetting(List<Map<String, String>> value) {
            super(value);
        }
        public void visit(SettingVisitor visitor) {
            visitor.visit(this);
        }
        public MapListSetting getNullInstance() {
            return new MapListSetting(null);
        }
    }

    public interface SettingVisitor {
        void visit(StringSetting setting);
        void visit(ListSetting value);
        void visit(ListListSetting value);
        void visit(MapListSetting value);
    }

    public interface PreferenceChangeEvent<T> {
        String getKey();
        Setting<T> getOldValue();
        Setting<T> getNewValue();
    }

    public interface PreferenceChangedListener {
        void preferenceChanged(PreferenceChangeEvent e);
    }

    private static class DefaultPreferenceChangeEvent<T> implements PreferenceChangeEvent<T> {
        private final String key;
        private final Setting<T> oldValue;
        private final Setting<T> newValue;

        public DefaultPreferenceChangeEvent(String key, Setting<T> oldValue, Setting<T> newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getKey() {
            return key;
        }
        public Setting<T> getOldValue() {
            return oldValue;
        }
        public Setting<T> getNewValue() {
            return newValue;
        }
    }

    public interface ColorKey {
        String getColorName();
        String getSpecialName();
        Color getDefault();
    }

    private final CopyOnWriteArrayList<PreferenceChangedListener> listeners = new CopyOnWriteArrayList<PreferenceChangedListener>();

    public void addPreferenceChangeListener(PreferenceChangedListener listener) {
        if (listener != null) {
            listeners.addIfAbsent(listener);
        }
    }

    public void removePreferenceChangeListener(PreferenceChangedListener listener) {
        listeners.remove(listener);
    }

    protected <T> void firePreferenceChanged(String key, Setting<T> oldValue, Setting<T> newValue) {
        PreferenceChangeEvent<T> evt = new DefaultPreferenceChangeEvent<T>(key, oldValue, newValue);
        for (PreferenceChangedListener l : listeners) {
            l.preferenceChanged(evt);
        }
    }

    /**
     * Return the location of the user defined preferences file
     */
    public String getPreferencesDir() {
        final String path = getPreferencesDirFile().getPath();
        if (path.endsWith(File.separator))
            return path;
        return path + File.separator;
    }

    public File getPreferencesDirFile() {
        if (preferencesDirFile != null)
            return preferencesDirFile;
        String path;
        path = System.getProperty("josm.home");
        if (path != null) {
            preferencesDirFile = new File(path);
        } else {
            path = System.getenv("APPDATA");
            if (path != null) {
                preferencesDirFile = new File(path, "JOSM");
            } else {
                preferencesDirFile = new File(System.getProperty("user.home"), ".josm");
            }
        }
        return preferencesDirFile;
    }

    public File getPreferenceFile() {
        return new File(getPreferencesDirFile(), "preferences.xml");
    }

    public File getOldPreferenceFile() {
        return new File(getPreferencesDirFile(), "preferences");
    }

    public File getPluginsDirectory() {
        return new File(getPreferencesDirFile(), "plugins");
    }

    /**
     * @return A list of all existing directories where resources could be stored.
     */
    public Collection<String> getAllPossiblePreferenceDirs() {
        LinkedList<String> locations = new LinkedList<String>();
        locations.add(Main.pref.getPreferencesDir());
        String s;
        if ((s = System.getenv("JOSM_RESOURCES")) != null) {
            if (!s.endsWith(File.separator)) {
                s = s + File.separator;
            }
            locations.add(s);
        }
        if ((s = System.getProperty("josm.resources")) != null) {
            if (!s.endsWith(File.separator)) {
                s = s + File.separator;
            }
            locations.add(s);
        }
        String appdata = System.getenv("APPDATA");
        if (System.getenv("ALLUSERSPROFILE") != null && appdata != null
                && appdata.lastIndexOf(File.separator) != -1) {
            appdata = appdata.substring(appdata.lastIndexOf(File.separator));
            locations.add(new File(new File(System.getenv("ALLUSERSPROFILE"),
                    appdata), "JOSM").getPath());
        }
        locations.add("/usr/local/share/josm/");
        locations.add("/usr/local/lib/josm/");
        locations.add("/usr/share/josm/");
        locations.add("/usr/lib/josm/");
        return locations;
    }

    synchronized public boolean hasKey(final String key) {
        return properties.containsKey(key);
    }

    /**
     * Get settings value for a certain key.
     * @param key the identifier for the setting
     * @return "" if there is nothing set for the preference key,
     *  the corresponding value otherwise. The result is not null.
     */
    synchronized public String get(final String key) {
        putDefault(key, null);
        if (!properties.containsKey(key))
            return "";
        return properties.get(key);
    }

    /**
     * Get settings value for a certain key and provide default a value.
     * @param key the identifier for the setting
     * @param def the default value. For each call of get() with a given key, the
     *  default value must be the same.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    synchronized public String get(final String key, final String def) {
        putDefault(key, def);
        final String prop = properties.get(key);
        if (prop == null || prop.equals(""))
            return def;
        return prop;
    }

    synchronized public Map<String, String> getAllPrefix(final String prefix) {
        final Map<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : properties.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                all.put(e.getKey(), e.getValue());
            }
        }
        return all;
    }

    synchronized private Map<String, String> getAllPrefixDefault(final String prefix) {
        final Map<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : defaults.entrySet()) {
            if (e.getKey().startsWith(prefix)) {
                all.put(e.getKey(), e.getValue());
            }
        }
        return all;
    }

    synchronized public TreeMap<String, String> getAllColors() {
        final TreeMap<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : defaults.entrySet()) {
            if (e.getKey().startsWith("color.") && e.getValue() != null) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        }
        for (final Entry<String,String> e : properties.entrySet()) {
            if (e.getKey().startsWith("color.")) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        }
        return all;
    }

    synchronized public Map<String, String> getDefaults() {
        return defaults;
    }

    synchronized public void putDefault(final String key, final String def) {
        if(!defaults.containsKey(key) || defaults.get(key) == null) {
            defaults.put(key, def);
        } else if(def != null && !defaults.get(key).equals(def)) {
            System.out.println("Defaults for " + key + " differ: " + def + " != " + defaults.get(key));
        }
    }

    synchronized public boolean getBoolean(final String key) {
        putDefault(key, null);
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : false;
    }

    synchronized public boolean getBoolean(final String key, final boolean def) {
        putDefault(key, Boolean.toString(def));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
    }

    synchronized public boolean getBoolean(final String key, final String specName, final boolean def) {
        putDefault(key, Boolean.toString(def));
        String skey = key+"."+specName;
        if(properties.containsKey(skey))
            return Boolean.parseBoolean(properties.get(skey));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
    }

    /**
     * Set a value for a certain setting. The changed setting is saved
     * to the preference file immediately. Due to caching mechanisms on modern
     * operating systems and hardware, this shouldn't be a performance problem.
     * @param key the unique identifier for the setting
     * @param value the value of the setting. Can be null or "" wich both removes
     *  the key-value entry.
     * @return if true, something has changed (i.e. value is different than before)
     */
    public boolean put(final String key, String value) {
        boolean changed = false;
        String oldValue = null;

        synchronized (this) {
            oldValue = properties.get(key);
            if(value != null && value.length() == 0) {
                value = null;
            }
            // value is the same as before - no need to save anything
            boolean equalValue = oldValue != null && oldValue.equals(value);
            // The setting was previously unset and we are supposed to put a
            // value that equals the default value. This is not necessary because
            // the default value is the same throughout josm. In addition we like
            // to have the possibility to change the default value from version
            // to version, which would not work if we wrote it to the preference file.
            boolean unsetIsDefault = oldValue == null && (value == null || value.equals(defaults.get(key)));

            if (!(equalValue || unsetIsDefault)) {
                if (value == null) {
                    properties.remove(key);
                } else {
                    properties.put(key, value);
                }
                try {
                    save();
                } catch(IOException e){
                    System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
                changed = true;
            }
        }
        if (changed) {
            // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
            firePreferenceChanged(key, new StringSetting(oldValue), new StringSetting(value));
        }
        return changed;
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
     * Called after every put. In case of a problem, do nothing but output the error
     * in log.
     */
    public void save() throws IOException {
        /* currently unused, but may help to fix configuration issues in future */
        putInteger("josm.version", Version.getInstance().getVersion());

        updateSystemProperties();
        if(Main.applet)
            return;

        File prefFile = getPreferenceFile();
        File backupFile = new File(prefFile + "_backup");

        // Backup old preferences if there are old preferences
        if(prefFile.exists()) {
            copyFile(prefFile, backupFile);
        }

        final PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(prefFile + "_tmp"), "utf-8"), false);
        out.print(toXML(false));
        out.close();

        File tmpFile = new File(prefFile + "_tmp");
        copyFile(tmpFile, prefFile);
        tmpFile.delete();

        setCorrectPermissions(prefFile);
        setCorrectPermissions(backupFile);
    }


    private void setCorrectPermissions(File file) {
        file.setReadable(false, false);
        file.setWritable(false, false);
        file.setExecutable(false, false);
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    /**
     * Simple file copy function that will overwrite the target file
     * Taken from http://www.rgagnon.com/javadetails/java-0064.html (CC-NC-BY-SA)
     * @param in
     * @param out
     * @throws IOException
     */
    public static void copyFile(File in, File out) throws IOException  {
        FileChannel inChannel = new FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(),
                    outChannel);
        }
        catch (IOException e) {
            throw e;
        }
        finally {
            if (inChannel != null) {
                inChannel.close();
            }
            if (outChannel != null) {
                outChannel.close();
            }
        }
    }

    public void loadOld() throws Exception {
        load(true);
    }

    public void load() throws Exception {
        load(false);
    }

    private void load(boolean old) throws Exception {
        properties.clear();
        if (!Main.applet) {
            File pref = old ? getOldPreferenceFile() : getPreferenceFile();
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(pref), "utf-8"));
            /* FIXME: TODO: remove old style config file end of 2012 */
            try {
                if (old) {
                    in.mark(1);
                    int v = in.read();
                    in.reset();
                    if(v == '<') {
                        validateXML(in);
                        Utils.close(in);
                        in = new BufferedReader(new InputStreamReader(new FileInputStream(pref), "utf-8"));
                        fromXML(in);
                    } else {
                        int lineNumber = 0;
                        ArrayList<Integer> errLines = new ArrayList<Integer>();
                        for (String line = in.readLine(); line != null; line = in.readLine(), lineNumber++) {
                            final int i = line.indexOf('=');
                            if (i == -1 || i == 0) {
                                errLines.add(lineNumber);
                                continue;
                            }
                            String key = line.substring(0,i);
                            String value = line.substring(i+1);
                            if (!value.isEmpty()) {
                                properties.put(key, value);
                            }
                        }
                        if (!errLines.isEmpty())
                            throw new IOException(tr("Malformed config file at lines {0}", errLines));
                    }
                } else {
                    validateXML(in);
                    Utils.close(in);
                    in = new BufferedReader(new InputStreamReader(new FileInputStream(pref), "utf-8"));
                    fromXML(in);
                }
            } finally {
                in.close();
            }
        }
        updateSystemProperties();
        /* FIXME: TODO: remove special version check end of 2012 */
        if(!properties.containsKey("expert")) {
            try {
                String v = get("josm.version");
                if(v.isEmpty() || Integer.parseInt(v) <= 4511)
                    properties.put("expert", "true");
            } catch(Exception e) {
                properties.put("expert", "true");
            }
        }
    }

    public void init(boolean reset){
        if(Main.applet)
            return;
        // get the preferences.
        File prefDir = getPreferencesDirFile();
        if (prefDir.exists()) {
            if(!prefDir.isDirectory()) {
                System.err.println(tr("Warning: Failed to initialize preferences. Preference directory ''{0}'' is not a directory.", prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Preference directory ''{0}'' is not a directory.</html>", prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        } else {
            if (! prefDir.mkdirs()) {
                System.err.println(tr("Warning: Failed to initialize preferences. Failed to create missing preference directory: {0}", prefDir.getAbsoluteFile()));
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("<html>Failed to initialize preferences.<br>Failed to create missing preference directory: {0}</html>",prefDir.getAbsoluteFile()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        }

        File preferenceFile = getPreferenceFile();
        try {
            if (!preferenceFile.exists()) {
                File oldPreferenceFile = getOldPreferenceFile();
                if (!oldPreferenceFile.exists()) {
                    System.out.println(tr("Warning: Missing preference file ''{0}''. Creating a default preference file.", preferenceFile.getAbsoluteFile()));
                    resetToDefault();
                    save();
                } else {
                    try {
                        loadOld();
                    } catch (Exception e) {
                        e.printStackTrace();
                        File backupFile = new File(prefDir,"preferences.bak");
                        JOptionPane.showMessageDialog(
                                Main.parent,
                                tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> and creating a new default preference file.</html>", backupFile.getAbsoluteFile()),
                                tr("Error"),
                                JOptionPane.ERROR_MESSAGE
                        );
                        Main.platform.rename(oldPreferenceFile, backupFile);
                        try {
                            resetToDefault();
                            save();
                        } catch(IOException e1) {
                            e1.printStackTrace();
                            System.err.println(tr("Warning: Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
                        }
                    }
                    return;
                }
            } else if (reset) {
                System.out.println(tr("Warning: Replacing existing preference file ''{0}'' with default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
            }
        } catch(IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Failed to initialize preferences.<br>Failed to reset preference file to default: {0}</html>",getPreferenceFile().getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            return;
        }
        try {
            load();
        } catch (Exception e) {
            e.printStackTrace();
            File backupFile = new File(prefDir,"preferences.xml.bak");
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> and creating a new default preference file.</html>", backupFile.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            Main.platform.rename(preferenceFile, backupFile);
            try {
                resetToDefault();
                save();
            } catch(IOException e1) {
                e1.printStackTrace();
                System.err.println(tr("Warning: Failed to initialize preferences. Failed to reset preference file to default: {0}", getPreferenceFile()));
            }
        }
    }

    public final void resetToDefault(){
        properties.clear();
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    synchronized public Color getColor(String colName, Color def) {
        return getColor(colName, null, def);
    }

    synchronized public Color getUIColor(String colName) {
        return UIManager.getColor(colName);
    }

    /* only for preferences */
    synchronized public String getColorName(String o) {
        try
        {
            Matcher m = Pattern.compile("mappaint\\.(.+?)\\.(.+)").matcher(o);
            m.matches();
            return tr("Paint style {0}: {1}", tr(m.group(1)), tr(m.group(2)));
        }
        catch (Exception e) {}
        try
        {
            Matcher m = Pattern.compile("layer (.+)").matcher(o);
            m.matches();
            return tr("Layer: {0}", tr(m.group(1)));
        }
        catch (Exception e) {}
        return tr(colornames.containsKey(o) ? colornames.get(o) : o);
    }

    public Color getColor(ColorKey key) {
        return getColor(key.getColorName(), key.getSpecialName(), key.getDefault());
    }

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param specName name of the special colour settings
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    synchronized public Color getColor(String colName, String specName, Color def) {
        String colKey = colName.toLowerCase().replaceAll("[^a-z0-9]+",".");
        if(!colKey.equals(colName)) {
            colornames.put(colKey, colName);
        }
        putDefault("color."+colKey, ColorHelper.color2html(def));
        String colStr = specName != null ? get("color."+specName) : "";
        if(colStr.equals("")) {
            colStr = get("color."+colKey);
        }
        return colStr.equals("") ? def : ColorHelper.html2color(colStr);
    }

    synchronized public Color getDefaultColor(String colName) {
        String colStr = defaults.get("color."+colName);
        return colStr == null || "".equals(colStr) ? null : ColorHelper.html2color(colStr);
    }

    synchronized public boolean putColor(String colName, Color val) {
        return put("color."+colName, val != null ? ColorHelper.color2html(val) : null);
    }

    synchronized public int getInteger(String key, int def) {
        putDefault(key, Integer.toString(def));
        String v = get(key);
        if(v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public int getInteger(String key, String specName, int def) {
        putDefault(key, Integer.toString(def));
        String v = get(key+"."+specName);
        if(v.isEmpty())
            v = get(key);
        if(v.isEmpty())
            return def;

        try {
            return Integer.parseInt(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public long getLong(String key, long def) {
        putDefault(key, Long.toString(def));
        String v = get(key);
        if(null == v)
            return def;

        try {
            return Long.parseLong(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public double getDouble(String key, double def) {
        putDefault(key, Double.toString(def));
        String v = get(key);
        if(null == v)
            return def;

        try {
            return Double.parseDouble(v);
        } catch(NumberFormatException e) {
            // fall out
        }
        return def;
    }

    synchronized public double getDouble(String key, String def) {
        putDefault(key, def);
        String v = get(key);
        if(v != null && v.length() != 0) {
            try { return Double.parseDouble(v); } catch(NumberFormatException e) {}
        }
        try { return Double.parseDouble(def); } catch(NumberFormatException e) {}
        return 0.0;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    public Collection<String> getCollection(String key, Collection<String> def) {
        putCollectionDefault(key, def == null ? null : new ArrayList<String>(def));
        Collection<String> prop = getCollectionInternal(key);
        if (prop != null)
            return prop;
        else
            return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before,
     *  an empty Collection otherwise.
     */
    public Collection<String> getCollection(String key) {
        putCollectionDefault(key, null);
        Collection<String> prop = getCollectionInternal(key);
        if (prop != null)
            return prop;
        else
            return Collections.emptyList();
    }

    synchronized private List<String> getCollectionInternal(String key) {
        List<String> prop = collectionProperties.get(key);
        if (prop != null)
            return prop;
        else {
            String s = properties.get(key);
            if(s != null) {
                prop = Arrays.asList(s.split("\u001e", -1));
                collectionProperties.put(key, Collections.unmodifiableList(prop));
                properties.remove(key);
                defaults.remove(key);
                return prop;
            }
        }
        return null;
    }

    synchronized public void removeFromCollection(String key, String value) {
        List<String> a = new ArrayList<String>(getCollection(key, Collections.<String>emptyList()));
        a.remove(value);
        putCollection(key, a);
    }

    public boolean putCollection(String key, Collection<String> value) {
        List<String> oldValue = null;
        List<String> valueCopy = null;

        synchronized (this) {
            if (value == null) {
                oldValue = collectionProperties.remove(key);
                boolean changed = oldValue != null;
                changed |= properties.remove(key) != null;
                if (!changed) return false;
            } else {
                oldValue = getCollectionInternal(key);
                if (equalCollection(value, oldValue)) return false;
                Collection<String> defValue = collectionDefaults.get(key);
                if (oldValue == null && equalCollection(value, defValue)) return false;

                valueCopy = new ArrayList<String>(value);
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                collectionProperties.put(key, Collections.unmodifiableList(valueCopy));
                try {
                    save();
                } catch(IOException e){
                    System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new ListSetting(oldValue), new ListSetting(valueCopy));
        return true;
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
            if (!Utils.equal(aStr,bStr)) return false;
        }
        return true;
    }

    /**
     * Saves at most {@code maxsize} items of collection {@code val}.
     */
    public boolean putCollectionBounded(String key, int maxsize, Collection<String> val) {
        Collection<String> newCollection = new ArrayList<String>(Math.min(maxsize, val.size()));
        for (String i : val) {
            if (newCollection.size() >= maxsize) {
                break;
            }
            newCollection.add(i);
        }
        return putCollection(key, newCollection);
    }

    synchronized private void putCollectionDefault(String key, List<String> val) {
        collectionDefaults.put(key, val);
    }

    /**
     * Used to read a 2-dimensional array of strings from the preference file.
     * If not a single entry could be found, def is returned.
     */
    synchronized public Collection<Collection<String>> getArray(String key, Collection<Collection<String>> def) {
        if (def != null) {
            List<List<String>> defCopy = new ArrayList<List<String>>(def.size());
            for (Collection<String> lst : def) {
                defCopy.add(Collections.unmodifiableList(new ArrayList<String>(lst)));
            }
            putArrayDefault(key, Collections.unmodifiableList(defCopy));
        } else {
            putArrayDefault(key, null);
        }
        List<List<String>> prop = getArrayInternal(key);
        if (prop != null) {
            @SuppressWarnings("unchecked")
            Collection<Collection<String>> prop_cast = (Collection) prop;
            return prop_cast;
        } else
            return def;
    }

    public Collection<Collection<String>> getArray(String key) {
        putArrayDefault(key, null);
        List<List<String>> prop = getArrayInternal(key);
        if (prop != null) {
            @SuppressWarnings("unchecked")
            Collection<Collection<String>> prop_cast = (Collection) prop;
            return prop_cast;
        } else
            return Collections.emptyList();
    }

    synchronized private List<List<String>> getArrayInternal(String key) {
        List<List<String>> prop = arrayProperties.get(key);
        if (prop != null)
            return prop;
        else {
            String keyDot = key + ".";
            int num = 0;
            List<List<String>> col = new ArrayList<List<String>>();
            while (true) {
                List<String> c = getCollectionInternal(keyDot+num);
                if (c == null) {
                    break;
                }
                col.add(c);
                collectionProperties.remove(keyDot+num);
                collectionDefaults.remove(keyDot+num);
                num++;
            }
            if (num > 0) {
                arrayProperties.put(key, Collections.unmodifiableList(col));
                return col;
            }
        }
        return null;
    }

    public boolean putArray(String key, Collection<Collection<String>> value) {
        boolean changed = false;

        List<List<String>> oldValue = null;
        List<List<String>> valueCopy = null;

        synchronized (this) {
            if (value == null) {
                oldValue = getArrayInternal(key);
                if (arrayProperties.remove(key) != null) return false;
            } else {
                oldValue = getArrayInternal(key);
                if (equalArray(value, oldValue)) return false;

                List<List<String>> defValue = arrayDefaults.get(key);
                if (oldValue == null && equalArray(value, defValue)) return false;

                valueCopy = new ArrayList<List<String>>(value.size());
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                for (Collection<String> lst : value) {
                    List<String> lstCopy = new ArrayList<String>(lst);
                    if (lstCopy.contains(null)) throw new RuntimeException("Error: Null as inner list element in preference setting (key '"+key+"')");
                    valueCopy.add(Collections.unmodifiableList(lstCopy));
                }
                arrayProperties.put(key, Collections.unmodifiableList(valueCopy));
                try {
                    save();
                } catch(IOException e){
                    System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new ListListSetting(oldValue), new ListListSetting(valueCopy));
        return true;
    }

    public static boolean equalArray(Collection<Collection<String>> a, Collection<List<String>> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<Collection<String>> itA = a.iterator();
        Iterator<List<String>> itB = b.iterator();
        while (itA.hasNext()) {
            if (!equalCollection(itA.next(), itB.next())) return false;
        }
        return true;
    }

    synchronized private void putArrayDefault(String key, List<List<String>> val) {
        arrayDefaults.put(key, val);
    }

    public Collection<Map<String, String>> getListOfStructs(String key, Collection<Map<String, String>> def) {
        if (def != null) {
            List<Map<String, String>> defCopy = new ArrayList<Map<String, String>>(def.size());
            for (Map<String, String> map : def) {
                defCopy.add(Collections.unmodifiableMap(new LinkedHashMap<String,String>(map)));
            }
            putListOfStructsDefault(key, Collections.unmodifiableList(defCopy));
        } else {
            putListOfStructsDefault(key, null);
        }
        Collection<Map<String, String>> prop = getListOfStructsInternal(key);
        if (prop != null)
            return prop;
        else
            return def;
    }

    private synchronized List<Map<String, String>> getListOfStructsInternal(String key) {
        List<Map<String, String>> prop = listOfStructsProperties.get(key);
        if (prop != null)
            return prop;
        else {
            List<List<String>> array = getArrayInternal(key);
            if (array == null) return null;
            prop = new ArrayList<Map<String, String>>(array.size());
            for (Collection<String> mapStr : array) {
                Map<String, String> map = new LinkedHashMap<String, String>();
                for (String key_value : mapStr) {
                    final int i = key_value.indexOf(':');
                    if (i == -1 || i == 0) {
                        continue;
                    }
                    String k = key_value.substring(0,i);
                    String v = key_value.substring(i+1);
                    map.put(k, v);
                }
                prop.add(Collections.unmodifiableMap(map));
            }
            arrayProperties.remove(key);
            arrayDefaults.remove(key);
            listOfStructsProperties.put(key, Collections.unmodifiableList(prop));
            return prop;
        }
    }

    public boolean putListOfStructs(String key, Collection<Map<String, String>> value) {
        boolean changed = false;

        List<Map<String, String>> oldValue;
        List<Map<String, String>> valueCopy = null;

        synchronized (this) {
            if (value == null) {
                oldValue = getListOfStructsInternal(key);
                if (listOfStructsProperties.remove(key) != null) return false;
            } else {
                oldValue = getListOfStructsInternal(key);
                if (equalListOfStructs(oldValue, value)) return false;

                List<Map<String, String>> defValue = listOfStructsDefaults.get(key);
                if (oldValue == null && equalListOfStructs(value, defValue)) return false;

                valueCopy = new ArrayList<Map<String, String>>(value.size());
                if (valueCopy.contains(null)) throw new RuntimeException("Error: Null as list element in preference setting (key '"+key+"')");
                for (Map<String, String> map : value) {
                    Map<String, String> mapCopy = new LinkedHashMap<String,String>(map);
                    if (mapCopy.keySet().contains(null)) throw new RuntimeException("Error: Null as map key in preference setting (key '"+key+"')");
                    if (mapCopy.values().contains(null)) throw new RuntimeException("Error: Null as map value in preference setting (key '"+key+"')");
                    valueCopy.add(Collections.unmodifiableMap(mapCopy));
                }
                listOfStructsProperties.put(key, Collections.unmodifiableList(valueCopy));
                try {
                    save();
                } catch(IOException e){
                    System.out.println(tr("Warning: failed to persist preferences to ''{0}''", getPreferenceFile().getAbsoluteFile()));
                }
            }
        }
        // Call outside of synchronized section in case some listener wait for other thread that wait for preference lock
        firePreferenceChanged(key, new MapListSetting(oldValue), new MapListSetting(valueCopy));
        return true;
    }

    public static boolean equalListOfStructs(Collection<Map<String, String>> a, Collection<Map<String, String>> b) {
        if (a == null) return b == null;
        if (b == null) return false;
        if (a.size() != b.size()) return false;
        Iterator<Map<String, String>> itA = a.iterator();
        Iterator<Map<String, String>> itB = b.iterator();
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
            if (!Utils.equal(e.getValue(), b.get(e.getKey()))) return false;
        }
        return true;
    }

    synchronized private void putListOfStructsDefault(String key, List<Map<String, String>> val) {
        listOfStructsDefaults.put(key, val);
    }

    @Retention(RetentionPolicy.RUNTIME) public @interface pref { }
    @Retention(RetentionPolicy.RUNTIME) public @interface writeExplicitly { }

    /**
     * Get a list of hashes which are represented by a struct-like class.
     * It reads lines of the form
     *  > key.0=prop:val \u001e prop:val \u001e ... \u001e prop:val
     *  > ...
     *  > key.N=prop:val \u001e prop:val \u001e ... \u001e prop:val
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
        Collection<Map<String,String>> prop =
            getListOfStructs(key, def == null ? null : serializeListOfStructs(def, klass));
        if (prop == null)
            return def == null ? null : new ArrayList<T>(def);
        List<T> lst = new ArrayList<T>();
        for (Map<String,String> entries : prop) {
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

    private <T> Collection<Map<String,String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
        if (l == null)
            return null;
        Collection<Map<String,String>> vals = new ArrayList<Map<String,String>>();
        for (T struct : l) {
            if (struct == null) {
                continue;
            }
            vals.add(serializeStruct(struct, klass));
        }
        return vals;
    }

    private <T> Map<String,String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        Map<String,String> hash = new LinkedHashMap<String,String>();
        for (Field f : klass.getDeclaredFields()) {
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
            try {
                Object fieldValue = f.get(struct);
                Object defaultFieldValue = f.get(structPrototype);
                if (fieldValue != null) {
                    if (f.getAnnotation(writeExplicitly.class) != null || !Utils.equal(fieldValue, defaultFieldValue)) {
                        hash.put(f.getName().replace("_", "-"), fieldValue.toString());
                    }
                }
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException();
            }
        }
        return hash;
    }

    private <T> T deserializeStruct(Map<String,String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException();
        } catch (IllegalAccessException ex) {
            throw new RuntimeException();
        }
        for (Entry<String,String> key_value : hash.entrySet()) {
            Object value = null;
            Field f;
            try {
                f = klass.getDeclaredField(key_value.getKey().replace("-", "_"));
            } catch (NoSuchFieldException ex) {
                continue;
            } catch (SecurityException ex) {
                throw new RuntimeException();
            }
            if (f.getAnnotation(pref.class) == null) {
                continue;
            }
            f.setAccessible(true);
            if (f.getType() == Boolean.class || f.getType() == boolean.class) {
                value = Boolean.parseBoolean(key_value.getValue());
            } else if (f.getType() == Integer.class || f.getType() == int.class) {
                try {
                    value = Integer.parseInt(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                try {
                    value = Double.parseDouble(key_value.getValue());
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else  if (f.getType() == String.class) {
                value = key_value.getValue();
            } else
                throw new RuntimeException("unsupported preference primitive type");

            try {
                f.set(struct, value);
            } catch (IllegalArgumentException ex) {
                throw new AssertionError();
            } catch (IllegalAccessException ex) {
                throw new RuntimeException();
            }
        }
        return struct;
    }

    public boolean putSetting(final String key, Setting value) {
        if (value == null) return false;
        class PutVisitor implements SettingVisitor {
            public boolean changed;
            public void visit(StringSetting setting) {
                changed = put(key, setting.getValue());
            }
            public void visit(ListSetting setting) {
                changed = putCollection(key, setting.getValue());
            }
            public void visit(ListListSetting setting) {
                changed = putArray(key, (Collection) setting.getValue());
            }
            public void visit(MapListSetting setting) {
                changed = putListOfStructs(key, setting.getValue());
            }
        };
        PutVisitor putVisitor = new PutVisitor();
        value.visit(putVisitor);
        return putVisitor.changed;
    }

    public Map<String, Setting> getAllSettings() {
        Map<String, Setting> settings = new TreeMap<String, Setting>();

        for (Entry<String, String> e : properties.entrySet()) {
            settings.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionProperties.entrySet()) {
            settings.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayProperties.entrySet()) {
            settings.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsProperties.entrySet()) {
            settings.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        return settings;
    }

    public Map<String, Setting> getAllDefaults() {
        Map<String, Setting> allDefaults = new TreeMap<String, Setting>();

        for (Entry<String, String> e : defaults.entrySet()) {
            allDefaults.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsDefaults.entrySet()) {
            allDefaults.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        return allDefaults;
    }

    /**
     * Updates system properties with the current values in the preferences.
     *
     */
    public void updateSystemProperties() {
        Properties sysProp = System.getProperties();
        sysProp.put("http.agent", Version.getInstance().getAgentString());
        System.setProperties(sysProp);
    }

    /**
     * The default plugin site
     */
    private final static String[] DEFAULT_PLUGIN_SITE = {
    "http://josm.openstreetmap.de/plugin%<?plugins=>"};

    /**
     * Replies the collection of plugin site URLs from where plugin lists can be downloaded
     *
     * @return
     */
    public Collection<String> getPluginSites() {
        return getCollection("pluginmanager.sites", Arrays.asList(DEFAULT_PLUGIN_SITE));
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

    public void validateXML(Reader in) throws Exception {
        SchemaFactory factory =  SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = factory.newSchema(new StreamSource(new MirroredInputStream("resource://data/preferences.xsd")));
        XMLStreamReader parser = XMLInputFactory.newInstance().createXMLStreamReader(in);
        Validator validator = schema.newValidator();
        validator.validate(new StAXSource(parser));
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
                if (parser.getLocalName().equals("tag")) {
                    properties.put(parser.getAttributeValue(null, "key"), parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else if (parser.getLocalName().equals("list") || parser.getLocalName().equals("collection")) {
                    parseToplevelList();
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
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
        List<String> entries = null;
        List<List<String>> lists = null;
        List<Map<String, String>> maps = null;
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("entry")) {
                    if (entries == null) {
                        entries = new ArrayList<String>();
                    }
                    entries.add(parser.getAttributeValue(null, "value"));
                    jumpToEnd();
                } else if (parser.getLocalName().equals("list")) {
                    if (lists == null) {
                        lists = new ArrayList<List<String>>();
                    }
                    lists.add(parseInnerList());
                } else if (parser.getLocalName().equals("map")) {
                    if (maps == null) {
                        maps = new ArrayList<Map<String, String>>();
                    }
                    maps.add(parseMap());
                } else {
                    throwException("Unexpected element: "+parser.getLocalName());
                }
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        if (entries != null) {
            collectionProperties.put(key, Collections.unmodifiableList(entries));
        }
        if (lists != null) {
            arrayProperties.put(key, Collections.unmodifiableList(lists));
        }
        if (maps != null) {
            listOfStructsProperties.put(key, Collections.unmodifiableList(maps));
        }
    }

    protected List<String> parseInnerList() throws XMLStreamException {
        List<String> entries = new ArrayList<String>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("entry")) {
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
        Map<String, String> map = new LinkedHashMap<String, String>();
        while (true) {
            int event = parser.next();
            if (event == XMLStreamConstants.START_ELEMENT) {
                if (parser.getLocalName().equals("tag")) {
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
        throw new RuntimeException(msg + tr(" (at line {0}, column {1})", parser.getLocation().getLineNumber(), parser.getLocation().getColumnNumber()));
    }

    private class SettingToXml implements SettingVisitor {
        private StringBuilder b;
        private boolean noPassword;
        private String key;

        public SettingToXml(StringBuilder b, boolean noPassword) {
            this.b = b;
            this.noPassword = noPassword;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void visit(StringSetting setting) {
            if (noPassword && key.equals("osm-server.password"))
                return; // do not store plain password.
            String r = setting.getValue();
            String s = defaults.get(key);
            /* don't save default values */
            if(s == null || !s.equals(r)) {
                if(r.contains("\u001e"))
                {
                    b.append("  <list key='");
                    b.append(XmlWriter.encode(key));
                    b.append("'>\n");
                    for (String val : r.split("\u001e", -1))
                    {
                        b.append("    <entry value='");
                        b.append(XmlWriter.encode(val));
                        b.append("'/>\n");
                    }
                    b.append("  </list>\n");
                }
                else
                {
                    b.append("  <tag key='");
                    b.append(XmlWriter.encode(key));
                    b.append("' value='");
                    b.append(XmlWriter.encode(setting.getValue()));
                    b.append("'/>\n");
                }
            }
        }

        public void visit(ListSetting setting) {
            b.append("  <list key='").append(XmlWriter.encode(key)).append("'>\n");
            for (String s : setting.getValue()) {
                b.append("    <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
            }
            b.append("  </list>\n");
        }

        public void visit(ListListSetting setting) {
            b.append("  <list key='").append(XmlWriter.encode(key)).append("'>\n");
            for (List<String> list : setting.getValue()) {
                b.append("    <list>\n");
                for (String s : list) {
                    b.append("      <entry value='").append(XmlWriter.encode(s)).append("'/>\n");
                }
                b.append("    </list>\n");
            }
            b.append("  </list>\n");
        }

        public void visit(MapListSetting setting) {
            b.append("  <list key='").append(XmlWriter.encode(key)).append("'>\n");
            for (Map<String, String> struct : setting.getValue()) {
                b.append("    <map>\n");
                for (Entry<String, String> e : struct.entrySet()) {
                    b.append("      <tag key='").append(XmlWriter.encode(e.getKey())).append("' value='").append(XmlWriter.encode(e.getValue())).append("'/>\n");
                }
                b.append("    </map>\n");
            }
            b.append("  </list>\n");
        }
    }

    public String toXML(boolean nopass) {
        StringBuilder b = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<preferences xmlns=\"http://josm.openstreetmap.de/preferences-1.0\" version=\""+
                Version.getInstance().getVersion() + "\">\n");
        SettingToXml toXml = new SettingToXml(b, nopass);
        Map<String, Setting> settings = new TreeMap<String, Setting>();

        for (Entry<String, String> e : properties.entrySet()) {
            settings.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : collectionProperties.entrySet()) {
            settings.put(e.getKey(), new ListSetting(e.getValue()));
        }
        for (Entry<String, List<List<String>>> e : arrayProperties.entrySet()) {
            settings.put(e.getKey(), new ListListSetting(e.getValue()));
        }
        for (Entry<String, List<Map<String, String>>> e : listOfStructsProperties.entrySet()) {
            settings.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        for (Entry<String, Setting> e : settings.entrySet()) {
            toXml.setKey(e.getKey());
            e.getValue().visit(toXml);
        }
        b.append("</preferences>\n");
        return b.toString();
    }
}
