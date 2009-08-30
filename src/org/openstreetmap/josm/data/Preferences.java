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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.AboutAction;
import org.openstreetmap.josm.gui.preferences.ProxyPreferences;
import org.openstreetmap.josm.tools.ColorHelper;

/**
 * This class holds all preferences for JOSM.
 *
 * Other classes can register their beloved properties here. All properties will be
 * saved upon set-access.
 *
 * @author imi
 */
public class Preferences {

    /**
     * Internal storage for the preferenced directory.
     * Do not access this variable directly!
     * @see #getPreferencesDirFile()
     */
    private File preferencesDirFile = null;

    public static interface PreferenceChangedListener {
        void preferenceChanged(String key, String newValue);
    }

    /**
     * Class holding one bookmarkentry.
     * @author imi
     */
    public static class Bookmark implements Comparable<Bookmark> {
        public String name;
        public double[] latlon = new double[4]; // minlat, minlon, maxlat, maxlon
        @Override public String toString() {
            return name;
        }
        public int compareTo(Bookmark b) {
            return name.toLowerCase().compareTo(b.name.toLowerCase());
        }
    }

    public final ArrayList<PreferenceChangedListener> listener = new ArrayList<PreferenceChangedListener>();

    /**
     * Map the property name to the property object.
     */
    protected final SortedMap<String, String> properties = new TreeMap<String, String>();
    protected final SortedMap<String, String> defaults = new TreeMap<String, String>();

    /**
     * Override some values on read. This is intended to be used for technology previews
     * where we want to temporarily modify things without changing the user's preferences
     * file.
     */
    protected static final SortedMap<String, String> override = new TreeMap<String, String>();
    static {
        //override.put("osm-server.version", "0.5");
        //override.put("osm-server.additional-versions", "");
        //override.put("osm-server.url", "http://openstreetmap.gryph.de/api");
        //override.put("plugins", null);
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

    public File getPluginsDirFile() {
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
        return override.containsKey(key) ? override.get(key) != null : properties.containsKey(key);
    }

    synchronized public String get(final String key) {
        putDefault(key, null);
        if (override.containsKey(key))
            return override.get(key);
        if (!properties.containsKey(key))
            return "";
        return properties.get(key);
    }

    synchronized public String get(final String key, final String def) {
        putDefault(key, def);
        if (override.containsKey(key))
            return override.get(key);
        final String prop = properties.get(key);
        if (prop == null || prop.equals(""))
            return def;
        return prop;
    }

    synchronized public Map<String, String> getAllPrefix(final String prefix) {
        final Map<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : properties.entrySet())
            if (e.getKey().startsWith(prefix)) {
                all.put(e.getKey(), e.getValue());
            }
        for (final Entry<String,String> e : override.entrySet())
            if (e.getKey().startsWith(prefix))
                if (e.getValue() == null) {
                    all.remove(e.getKey());
                } else {
                    all.put(e.getKey(), e.getValue());
                }
        return all;
    }

    synchronized public TreeMap<String, String> getAllColors() {
        final TreeMap<String,String> all = new TreeMap<String,String>();
        for (final Entry<String,String> e : defaults.entrySet())
            if (e.getKey().startsWith("color.") && e.getValue() != null) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        for (final Entry<String,String> e : properties.entrySet())
            if (e.getKey().startsWith("color.")) {
                all.put(e.getKey().substring(6), e.getValue());
            }
        for (final Entry<String,String> e : override.entrySet())
            if (e.getKey().startsWith("color."))
                if (e.getValue() == null) {
                    all.remove(e.getKey().substring(6));
                } else {
                    all.put(e.getKey().substring(6), e.getValue());
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
        if (override.containsKey(key))
            return override.get(key) == null ? false : Boolean.parseBoolean(override.get(key));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : false;
    }

    synchronized public boolean getBoolean(final String key, final boolean def) {
        putDefault(key, Boolean.toString(def));
        if (override.containsKey(key))
            return override.get(key) == null ? def : Boolean.parseBoolean(override.get(key));
        return properties.containsKey(key) ? Boolean.parseBoolean(properties.get(key)) : def;
    }

    synchronized public boolean put(final String key, String value) {
        String oldvalue = properties.get(key);
        if(value != null && value.length() == 0) {
            value = null;
        }
        if(!((oldvalue == null && (value == null || value.equals(defaults.get(key))))
                || (value != null && oldvalue != null && oldvalue.equals(value))))
        {
            if (value == null) {
                properties.remove(key);
            } else {
                properties.put(key, value);
            }
            save();
            firePreferenceChanged(key, value);
            return true;
        }
        return false;
    }

    synchronized public boolean put(final String key, final boolean value) {
        return put(key, Boolean.toString(value));
    }

    synchronized public boolean putInteger(final String key, final Integer value) {
        return put(key, Integer.toString(value));
    }

    synchronized public boolean putDouble(final String key, final Double value) {
        return put(key, Double.toString(value));
    }

    synchronized public boolean putLong(final String key, final Long value) {
        return put(key, Long.toString(value));
    }

    private final void firePreferenceChanged(final String key, final String value) {
        for (final PreferenceChangedListener l : listener) {
            l.preferenceChanged(key, value);
        }
    }

    /**
     * Called after every put. In case of a problem, do nothing but output the error
     * in log.
     */
    public void save() {
        /* currently unused, but may help to fix configuration issues in future */
        properties.put("josm.version", AboutAction.getVersionString());
        try {
            setSystemProperties();
            final PrintWriter out = new PrintWriter(new OutputStreamWriter(
                    new FileOutputStream(getPreferencesDir() + "preferences"), "utf-8"), false);
            for (final Entry<String, String> e : properties.entrySet()) {
                String s = defaults.get(e.getKey());
                /* don't save default values */
                if(s == null || !s.equals(e.getValue())) {
                    out.println(e.getKey() + "=" + e.getValue());
                }
            }
            out.close();
        } catch (final IOException e) {
            e.printStackTrace();
            // do not message anything, since this can be called from strange
            // places.
        }
    }

    public void load() throws IOException {
        properties.clear();
        final BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(getPreferencesDir()+"preferences"), "utf-8"));
        int lineNumber = 0;
        ArrayList<Integer> errLines = new ArrayList<Integer>();
        for (String line = in.readLine(); line != null; line = in.readLine(), lineNumber++) {
            final int i = line.indexOf('=');
            if (i == -1 || i == 0) {
                errLines.add(lineNumber);
                continue;
            }
            properties.put(line.substring(0,i), line.substring(i+1));
        }
        if (!errLines.isEmpty())
            throw new IOException(tr("Malformed config file at lines {0}", errLines));
        setSystemProperties();
    }

    public void init(Boolean reset)
    {
        // get the preferences.
        File prefDir = getPreferencesDirFile();
        if (prefDir.exists()) {
            if(!prefDir.isDirectory()) {
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Cannot open preferences directory: {0}",Main.pref.getPreferencesDir()),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                );
                return;
            }
        } else {
            prefDir.mkdirs();
        }

        if (!new File(getPreferencesDir()+"preferences").exists()) {
            resetToDefault();
        }

        try {
            if (reset) {
                resetToDefault();
            } else {
                load();
            }
        } catch (final IOException e1) {
            e1.printStackTrace();
            String backup = getPreferencesDir() + "preferences.bak";
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Preferences file had errors. Making backup of old one to {0}.", backup),
                    tr("Error"),
                    JOptionPane.ERROR_MESSAGE
            );
            new File(getPreferencesDir() + "preferences").renameTo(new File(backup));
            save();
        }
    }

    public final void resetToDefault() {
        properties.clear();
        put("layerlist.visible", true);
        put("propertiesdialog.visible", true);
        put("selectionlist.visible", true);
        put("commandstack.visible", true);
        if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") == -1) {
            put("laf", "javax.swing.plaf.metal.MetalLookAndFeel");
        } else {
            put("laf", "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        }
        save();
    }

    public Collection<Bookmark> loadBookmarks() throws IOException {
        File bookmarkFile = new File(getPreferencesDir()+"bookmarks");
        if (!bookmarkFile.exists()) {
            bookmarkFile.createNewFile();
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(
                new FileInputStream(bookmarkFile), "utf-8"));

        LinkedList<Bookmark> bookmarks = new LinkedList<Bookmark>();
        for (String line = in.readLine(); line != null; line = in.readLine()) {
            // FIXME: legacy code using ',' sign, should be \u001e only
            Matcher m = Pattern.compile("^(.+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)[,\u001e](-?\\d+.\\d+)$").matcher(line);
            if(m.matches())
            {
                Bookmark b = new Bookmark();
                b.name = m.group(1);
                for (int i = 0; i < b.latlon.length; ++i) {
                    b.latlon[i] = Double.parseDouble(m.group(i+2));
                }
                bookmarks.add(b);
            }
        }
        in.close();
        Collections.sort(bookmarks);
        return bookmarks;
    }

    public void saveBookmarks(Collection<Bookmark> bookmarks) throws IOException {
        File bookmarkFile = new File(Main.pref.getPreferencesDir()+"bookmarks");
        if (!bookmarkFile.exists()) {
            bookmarkFile.createNewFile();
        }
        PrintWriter out = new PrintWriter(new OutputStreamWriter(
                new FileOutputStream(bookmarkFile), "utf-8"));
        for (Bookmark b : bookmarks) {
            out.print(b.name+"\u001e");
            for (int i = 0; i < b.latlon.length; ++i) {
                out.print(b.latlon[i]+(i<b.latlon.length-1?"\u001e":""));
            }
            out.println();
        }
        out.close();
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

    /**
     * Convenience method for accessing colour preferences.
     *
     * @param colName name of the colour
     * @param specName name of the special colour settings
     * @param def default value
     * @return a Color object for the configured colour, or the default value if none configured.
     */
    synchronized public Color getColor(String colName, String specName, Color def) {
        putDefault("color."+colName, ColorHelper.color2html(def));
        String colStr = specName != null ? get("color."+specName) : "";
        if(colStr.equals("")) {
            colStr = get("color."+colName);
        }
        return colStr.equals("") ? def : ColorHelper.html2color(colStr);
    }

    synchronized public Color getDefaultColor(String colName) {
        String colStr = defaults.get("color."+colName);
        return colStr.equals("") ? null : ColorHelper.html2color(colStr);
    }

    synchronized public boolean putColor(String colName, Color val) {
        return put("color."+colName, val != null ? ColorHelper.color2html(val) : null);
    }

    synchronized public int getInteger(String key, int def) {
        putDefault(key, Integer.toString(def));
        String v = get(key);
        if(null == v)
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

    synchronized public Collection<String> getCollection(String key, Collection<String> def) {
        String s = get(key);
        if(def != null)
        {
            String d = null;
            for(String a : def)
            {
                if(d != null) {
                    d += "\u001e" + a;
                } else {
                    d = a;
                }
            }
            putDefault(key, d);
        }
        if(s != null && s.length() != 0)
        {
            if(s.indexOf("\u001e") < 0) /* FIXME: legacy code, remove later */
            {
                String r =s;
                if(r.indexOf("§§§") > 0) {
                    r = r.replaceAll("§§§","\u001e");
                } else {
                    r = r.replace(';','\u001e');
                }
                if(!r.equals(s)) /* save the converted string */
                {
                    put(key,r);
                    s = r;
                }
            }
            return Arrays.asList(s.split("\u001e"));
        }
        return def;
    }
    synchronized public void removeFromCollection(String key, String value) {
        ArrayList<String> a = new ArrayList<String>(getCollection(key, null));
        if(a != null)
        {
            a.remove(value);
            putCollection(key, a);
        }
    }
    synchronized public boolean putCollection(String key, Collection<String> val) {
        String s = null;
        if(val != null)
        {
            for(String a : val)
            {
                if(s != null) {
                    s += "\u001e" + a;
                } else {
                    s = a;
                }
            }
        }

        return put(key, s);
    }

    private void setSystemProperties() {
        if (getBoolean(ProxyPreferences.PROXY_ENABLE)) {
            Properties sysProp = System.getProperties();
            sysProp.put("proxySet", "true");
            sysProp.put("http.proxyHost", get(ProxyPreferences.PROXY_HOST));
            sysProp.put("proxyPort", get(ProxyPreferences.PROXY_PORT));
            if (!getBoolean(ProxyPreferences.PROXY_ANONYMOUS)) {
                sysProp.put("proxyUser", get(ProxyPreferences.PROXY_USER));
                sysProp.put("proxyPassword", get(ProxyPreferences.PROXY_PASS));
            }
            System.setProperties(sysProp);
        }
        AboutAction.setUserAgent();
    }
}
