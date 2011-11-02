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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.xml.sax.SAXException;

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
 * everywhere. null is a legitimate default value.
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

    /* NOTE: FIXME: Remove when saving XML enabled */
    private boolean loadedXML = false;

    public interface PreferenceChangeEvent{
        String getKey();
        String getOldValue();
        String getNewValue();
    }

    public interface PreferenceChangedListener {
        void preferenceChanged(PreferenceChangeEvent e);
    }

    private static class DefaultPreferenceChangeEvent implements PreferenceChangeEvent {
        private final String key;
        private final String oldValue;
        private final String newValue;

        public DefaultPreferenceChangeEvent(String key, String oldValue, String newValue) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getKey() {
            return key;
        }
        public String getOldValue() {
            return oldValue;
        }
        public String getNewValue() {
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

    protected void firePreferenceChanged(String key, String oldValue, String newValue) {
        PreferenceChangeEvent evt = new DefaultPreferenceChangeEvent(key, oldValue, newValue);
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
            firePreferenceChanged(key, oldValue, value);
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
        /* FIXME: NOTE: loadedXML - removed 01.12.2011 */
        if(loadedXML) {
            out.print(toXML(false));
        } else {
            for (final Entry<String, String> e : properties.entrySet()) {
                String s = defaults.get(e.getKey());
                /* don't save default values */
                if(s == null || !s.equals(e.getValue())) {
                    out.println(e.getKey() + "=" + e.getValue());
                }
              }
        }
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

    public void load() throws Exception {
        properties.clear();
        if(!Main.applet) {
            final BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(getPreferencesDir()+"preferences"), "utf-8"));
            /* FIXME: TODO: remove old style config file end of 2012 */
            in.mark(1);
            int v = in.read();
            in.reset();
            if(v == '<') {
                fromXML(in);
                loadedXML = true;
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
                System.out.println(tr("Warning: Missing preference file ''{0}''. Creating a default preference file.", preferenceFile.getAbsoluteFile()));
                resetToDefault();
                save();
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
            File backupFile = new File(prefDir,"preferences.bak");
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("<html>Preferences file had errors.<br> Making backup of old one to <br>{0}<br> and creating a new default preference file.</html>", backupFile.getAbsoluteFile()),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            preferenceFile.renameTo(backupFile);
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

    synchronized public String getCollectionAsString(final String key) {
        String s = get(key);
        if(s != null && s.length() != 0) {
            s = s.replaceAll("\u001e",",");
        }
        return s;
    }

    public boolean isCollection(String key, boolean def) {
        String s = get(key);
        if (s != null && s.length() != 0)
            return s.indexOf("\u001e") >= 0;
            else
                return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @param def the default value.
     * @return the corresponding value if the property has been set before,
     *  def otherwise
     */
    synchronized public Collection<String> getCollection(String key, Collection<String> def) {
        putCollectionDefault(key, def);
        String s = get(key);
        if(s != null && s.length() != 0)
            return Arrays.asList(s.split("\u001e", -1));
        return def;
    }

    /**
     * Get a list of values for a certain key
     * @param key the identifier for the setting
     * @return the corresponding value if the property has been set before,
     *  an empty Collection otherwise.
     */
    synchronized public Collection<String> getCollection(String key) {
        putCollectionDefault(key, null);
        String s = get(key);
        if (s != null && s.length() != 0)
            return Arrays.asList(s.split("\u001e", -1));
        return Collections.emptyList();
    }

    /* old style conversion, replace by above call after some transition time */
    /* remove this function, when no more old-style preference collections in the code */
    @Deprecated
    synchronized public Collection<String> getCollectionOld(String key, String sep) {
        putCollectionDefault(key, null);
        String s = get(key);
        if (s != null && s.length() != 0) {
            if(!s.contains("\u001e") && s.contains(sep)) {
                s = s.replace(sep, "\u001e");
                put(key, s);
            }
            return Arrays.asList(s.split("\u001e", -1));
        }
        return Collections.emptyList();
    }

    synchronized public void removeFromCollection(String key, String value) {
        List<String> a = new ArrayList<String>(getCollection(key, Collections.<String>emptyList()));
        a.remove(value);
        putCollection(key, a);
    }

    synchronized public boolean putCollection(String key, Collection<String> val) {
        return put(key, Utils.join("\u001e", val));
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

    synchronized private void putCollectionDefault(String key, Collection<String> val) {
        putDefault(key, Utils.join("\u001e", val));
    }

    /**
     * Used to read a 2-dimensional array of strings from the preference file.
     * If not a single entry could be found, def is returned.
     */
    synchronized public Collection<Collection<String>> getArray(String key,
            Collection<Collection<String>> def)
            {
        if(def != null) {
            putArrayDefault(key, def);
        }
        key += ".";
        int num = 0;
        Collection<Collection<String>> col = new LinkedList<Collection<String>>();
        while(properties.containsKey(key+num)) {
            col.add(getCollection(key+num++, null));
        }
        return num == 0 ? def : col;
            }

    synchronized public boolean putArray(String key, Collection<Collection<String>> val) {
        boolean changed = false;
        key += ".";
        Collection<String> keys = getAllPrefix(key).keySet();
        if(val != null) {
            int num = 0;
            for(Collection<String> c : val) {
                keys.remove(key+num);
                changed |= putCollection(key+num++, c);
            }
        }
        int l = key.length();
        for(String k : keys) {
            try {
                Integer.valueOf(k.substring(l));
                changed |= put(k, null);
            } catch(NumberFormatException e) {
                /* everything which does not end with a number should not be deleted */
            }
        }
        return changed;
    }

    synchronized private void putArrayDefault(String key, Collection<Collection<String>> val) {
        key += ".";
        Collection<String> keys = getAllPrefixDefault(key).keySet();
        int num = 0;
        for(Collection<String> c : val) {
            keys.remove(key+num);
            putCollectionDefault(key+num++, c);
        }
        int l = key.length();
        for(String k : keys) {
            try {
                Integer.valueOf(k.substring(l));
                defaults.remove(k);
            } catch(Exception e) {
                /* everything which does not end with a number should not be deleted */
            }
        }
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
        Collection<Collection<String>> array =
            getArray(key, def == null ? null : serializeListOfStructs(def, klass));
        if (array == null)
            return def == null ? null : new ArrayList<T>(def);
        List<T> lst = new ArrayList<T>();
        for (Collection<String> entries : array) {
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
        return putArray(key, serializeListOfStructs(val, klass));
    }

    private <T> Collection<Collection<String>> serializeListOfStructs(Collection<T> l, Class<T> klass) {
        if (l == null)
            return null;
        Collection<Collection<String>> vals = new ArrayList<Collection<String>>();
        for (T struct : l) {
            if (struct == null) {
                continue;
            }
            vals.add(serializeStruct(struct, klass));
        }
        return vals;
    }

    private <T> Collection<String> serializeStruct(T struct, Class<T> klass) {
        T structPrototype;
        try {
            structPrototype = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException(ex);
        } catch (IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }

        Collection<String> hash = new ArrayList<String>();
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
                        hash.add(String.format("%s:%s", f.getName().replace("_", "-"), fieldValue.toString()));
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

    private <T> T deserializeStruct(Collection<String> hash, Class<T> klass) {
        T struct = null;
        try {
            struct = klass.newInstance();
        } catch (InstantiationException ex) {
            throw new RuntimeException();
        } catch (IllegalAccessException ex) {
            throw new RuntimeException();
        }
        for (String key_value : hash) {
            final int i = key_value.indexOf(':');
            if (i == -1 || i == 0) {
                continue;
            }
            String key = key_value.substring(0,i);
            String valueString = key_value.substring(i+1);

            Object value = null;
            Field f;
            try {
                f = klass.getDeclaredField(key.replace("-", "_"));
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
                value = Boolean.parseBoolean(valueString);
            } else if (f.getType() == Integer.class || f.getType() == int.class) {
                try {
                    value = Integer.parseInt(valueString);
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else if (f.getType() == Double.class || f.getType() == double.class) {
                try {
                    value = Double.parseDouble(valueString);
                } catch (NumberFormatException nfe) {
                    continue;
                }
            } else  if (f.getType() == String.class) {
                value = valueString;
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

    public static class XMLTag {
        public String key;
        public String value;
    }
    public static class XMLCollection {
        public String key;
    }
    public static class XMLEntry {
        public String value;
    }
    public void fromXML(Reader in) throws SAXException {
        XmlObjectParser parser = new XmlObjectParser();
        parser.map("tag", XMLTag.class);
        parser.map("entry", XMLEntry.class);
        parser.map("collection", XMLCollection.class);
        parser.startWithValidation(in,
                "http://josm.openstreetmap.de/preferences-1.0", "resource://data/preferences.xsd");
        LinkedList<String> vals = new LinkedList<String>();
        while(parser.hasNext()) {
            Object o = parser.next();
            if(o instanceof XMLTag) {
                properties.put(((XMLTag)o).key, ((XMLTag)o).value);
            } else if (o instanceof XMLEntry) {
                vals.add(((XMLEntry)o).value);
            } else if (o instanceof XMLCollection) {
                properties.put(((XMLCollection)o).key, Utils.join("\u001e", vals));
                vals = new LinkedList<String>();
            }
        }
    }

    public String toXML(boolean nopass) {
        StringBuilder b = new StringBuilder(
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<preferences xmlns=\"http://josm.openstreetmap.de/preferences-1.0\" version=\""+
                Version.getInstance().getVersion() + "\">\n");
        for (Entry<String, String> p : properties.entrySet()) {
            if (nopass && p.getKey().equals("osm-server.password")) {
                continue; // do not store plain password.
            }
            String r = p.getValue();
            String s = defaults.get(p.getKey());
            /* don't save default values */
            if(s == null || !s.equals(r)) {
                if(r.contains("\u001e"))
                {
                    b.append(" <collection key='");
                    b.append(XmlWriter.encode(p.getKey()));
                    b.append("'>\n");
                    for (String val : r.split("\u001e", -1))
                    {
                        b.append("  <entry value='");
                        b.append(XmlWriter.encode(val));
                        b.append("' />\n");
                    }
                    b.append(" </collection>\n");
                }
                else
                {
                    b.append(" <tag key='");
                    b.append(XmlWriter.encode(p.getKey()));
                    b.append("' value='");
                    b.append(XmlWriter.encode(p.getValue()));
                    b.append("' />\n");
                }
            }
        }
        b.append("</preferences>");
        return b.toString();
    }
}
