// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.spi.preferences.IPreferences;
import org.openstreetmap.josm.spi.preferences.ListListSetting;
import org.openstreetmap.josm.spi.preferences.ListSetting;
import org.openstreetmap.josm.spi.preferences.MapListSetting;
import org.openstreetmap.josm.spi.preferences.Setting;
import org.openstreetmap.josm.spi.preferences.StringSetting;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Helper class to do specific Preferences operation - appending, replacing, deletion by key and by value
 * @since 12634 (extracted from {@code CustomConfigurator})
 */
public final class PreferencesUtils {

    private static volatile StringBuilder summary = new StringBuilder();

    private PreferencesUtils() {
        // Hide implicit public constructor for utility class
    }

    /**
     * Log a formatted message.
     * @param fmt format
     * @param vars arguments
     * @see String#format
     * @since 12826
     */
    public static void log(String fmt, Object... vars) {
        summary.append(String.format(fmt, vars));
    }

    /**
     * Log a message.
     * @param s message to log
     * @since 12826
     */
    public static void log(String s) {
        summary.append(s).append('\n');
    }

    /**
     * Log an exception.
     * @param e exception to log
     * @param s message prefix
     * @since 12826
     */
    public static void log(Exception e, String s) {
        summary.append(s).append(' ').append(Logging.getErrorMessage(e)).append('\n');
    }

    /**
     * Returns the log.
     * @return the log
     * @since 12826
     */
    public static String getLog() {
        return summary.toString();
    }

    /**
     * Resets the log.
     * @since 12826
     */
    public static void resetLog() {
        summary = new StringBuilder();
    }

    public static void replacePreferences(Preferences fragment, Preferences mainpref) {
        for (Entry<String, Setting<?>> entry: fragment.settingsMap.entrySet()) {
            mainpref.putSetting(entry.getKey(), entry.getValue());
        }
    }

    public static void appendPreferences(Preferences fragment, Preferences mainpref) {
        for (Entry<String, Setting<?>> entry: fragment.settingsMap.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() instanceof StringSetting) {
                mainpref.putSetting(key, entry.getValue());
            } else if (entry.getValue() instanceof ListSetting) {
                ListSetting lSetting = (ListSetting) entry.getValue();
                List<String> newItems = getList(mainpref, key, true);
                if (newItems == null) continue;
                for (String item : lSetting.getValue()) {
                    // add nonexisting elements to then list
                    if (!newItems.contains(item)) {
                        newItems.add(item);
                    }
                }
                mainpref.putList(key, newItems);
            } else if (entry.getValue() instanceof ListListSetting) {
                ListListSetting llSetting = (ListListSetting) entry.getValue();
                List<List<String>> newLists = getListOfLists(mainpref, key, true);
                if (newLists == null) continue;

                for (List<String> list : llSetting.getValue()) {
                    // add nonexisting list (equals comparison for lists is used implicitly)
                    if (!newLists.contains(list)) {
                        newLists.add(list);
                    }
                }
                mainpref.putListOfLists(key, newLists);
            } else if (entry.getValue() instanceof MapListSetting) {
                MapListSetting mlSetting = (MapListSetting) entry.getValue();
                List<Map<String, String>> newMaps = getListOfStructs(mainpref, key, true);
                if (newMaps == null) continue;

                // get existing properties as list of maps

                for (Map<String, String> map : mlSetting.getValue()) {
                    // add nonexisting map (equals comparison for maps is used implicitly)
                    if (!newMaps.contains(map)) {
                        newMaps.add(map);
                    }
                }
                mainpref.putListOfMaps(entry.getKey(), newMaps);
            }
        }
    }

    /**
     * Delete items from {@code mainpref} collections that match items from {@code fragment} collections.
     * @param fragment preferences
     * @param mainpref main preferences
     */
    public static void deletePreferenceValues(Preferences fragment, Preferences mainpref) {

        for (Entry<String, Setting<?>> entry : fragment.settingsMap.entrySet()) {
            String key = entry.getKey();
            if (entry.getValue() instanceof StringSetting) {
                StringSetting sSetting = (StringSetting) entry.getValue();
                // if mentioned value found, delete it
                if (sSetting.equals(mainpref.settingsMap.get(key))) {
                    mainpref.put(key, null);
                }
            } else if (entry.getValue() instanceof ListSetting) {
                ListSetting lSetting = (ListSetting) entry.getValue();
                List<String> newItems = getList(mainpref, key, true);
                if (newItems == null) continue;

                // remove mentioned items from collection
                for (String item : lSetting.getValue()) {
                    log("Deleting preferences: from list %s: %s\n", key, item);
                    newItems.remove(item);
                }
                mainpref.putList(entry.getKey(), newItems);
            } else if (entry.getValue() instanceof ListListSetting) {
                ListListSetting llSetting = (ListListSetting) entry.getValue();
                List<List<String>> newLists = getListOfLists(mainpref, key, true);
                if (newLists == null) continue;

                // if items are found in one of lists, remove that list!
                Iterator<List<String>> listIterator = newLists.iterator();
                while (listIterator.hasNext()) {
                    Collection<String> list = listIterator.next();
                    for (Collection<String> removeList : llSetting.getValue()) {
                        if (list.containsAll(removeList)) {
                            // remove current list, because it matches search criteria
                            log("Deleting preferences: list from lists %s: %s\n", key, list);
                            listIterator.remove();
                        }
                    }
                }

                mainpref.putListOfLists(key, newLists);
            } else if (entry.getValue() instanceof MapListSetting) {
                MapListSetting mlSetting = (MapListSetting) entry.getValue();
                List<Map<String, String>> newMaps = getListOfStructs(mainpref, key, true);
                if (newMaps == null) continue;

                Iterator<Map<String, String>> mapIterator = newMaps.iterator();
                while (mapIterator.hasNext()) {
                    Map<String, String> map = mapIterator.next();
                    for (Map<String, String> removeMap : mlSetting.getValue()) {
                        if (map.entrySet().containsAll(removeMap.entrySet())) {
                            // the map contain all mentioned key-value pair, so it should be deleted from "maps"
                            log("Deleting preferences: deleting map from maps %s: %s\n", key, map);
                            mapIterator.remove();
                        }
                    }
                }
                mainpref.putListOfMaps(entry.getKey(), newMaps);
            }
        }
    }

    public static void deletePreferenceKeyByPattern(String pattern, Preferences pref) {
        Map<String, Setting<?>> allSettings = pref.getAllSettings();
        for (Entry<String, Setting<?>> entry : allSettings.entrySet()) {
            String key = entry.getKey();
            if (key.matches(pattern)) {
                log("Deleting preferences: deleting key from preferences: " + key);
                pref.putSetting(key, null);
            }
        }
    }

    public static void deletePreferenceKey(String key, Preferences pref) {
        Map<String, Setting<?>> allSettings = pref.getAllSettings();
        if (allSettings.containsKey(key)) {
            log("Deleting preferences: deleting key from preferences: " + key);
            pref.putSetting(key, null);
        }
    }

    private static List<String> getList(Preferences mainpref, String key, boolean warnUnknownDefault) {
        ListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListSetting.class);
        ListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListSetting.class);
        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static List<List<String>> getListOfLists(Preferences mainpref, String key, boolean warnUnknownDefault) {
        ListListSetting existing = Utils.cast(mainpref.settingsMap.get(key), ListListSetting.class);
        ListListSetting defaults = Utils.cast(mainpref.defaultsMap.get(key), ListListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }
        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static List<Map<String, String>> getListOfStructs(Preferences mainpref, String key, boolean warnUnknownDefault) {
        MapListSetting existing = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);
        MapListSetting defaults = Utils.cast(mainpref.settingsMap.get(key), MapListSetting.class);

        if (existing == null && defaults == null) {
            if (warnUnknownDefault) defaultUnknownWarning(key);
            return null;
        }

        if (existing != null)
            return new ArrayList<>(existing.getValue());
        else
            return defaults.getValue() == null ? null : new ArrayList<>(defaults.getValue());
    }

    private static void defaultUnknownWarning(String key) {
        log("Warning: Unknown default value of %s , skipped\n", key);
        JOptionPane.showMessageDialog(
                MainApplication.getMainFrame(),
                tr("<html>Settings file asks to append preferences to <b>{0}</b>,<br/> "+
                        "but its default value is unknown at this moment.<br/> " +
                        "Please activate corresponding function manually and retry importing.", key),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE);
    }

    public static void showPrefs(Preferences tmpPref) {
        Logging.info("properties: " + tmpPref.settingsMap);
    }

    /**
     * Gets an boolean that may be specialized
     * @param prefs the preferences
     * @param key The basic key
     * @param specName The sub-key to append to the key
     * @param def The default value
     * @return The boolean value or the default value if it could not be parsed
     * @since 12891
     */
    public static boolean getBoolean(IPreferences prefs, final String key, final String specName, final boolean def) {
        synchronized (prefs) {
            boolean generic = prefs.getBoolean(key, def);
            String skey = key+'.'+specName;
            String svalue = prefs.get(skey, null);
            if (svalue != null)
                return Boolean.parseBoolean(svalue);
            else
                return generic;
        }
    }

    /**
     * Gets an integer that may be specialized
     * @param prefs the preferences
     * @param key The basic key
     * @param specName The sub-key to append to the key
     * @param def The default value
     * @return The integer value or the default value if it could not be parsed
     * @since 12891
     */
    public static int getInteger(IPreferences prefs, String key, String specName, int def) {
        synchronized (prefs) {
            String v = prefs.get(key+'.'+specName);
            if (v.isEmpty())
                v = prefs.get(key, Integer.toString(def));
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
    }

    /**
     * Removes a value from a given String list
     * @param prefs the preferences
     * @param key The preference key the list is stored with
     * @param value The value that should be removed in the list
     * @since 12894
     */
    public static void removeFromList(IPreferences prefs, String key, String value) {
        synchronized (prefs) {
            List<String> a = new ArrayList<>(prefs.getList(key, Collections.<String>emptyList()));
            a.remove(value);
            prefs.putList(key, a);
        }
    }

    /**
     * Saves at most {@code maxsize} items of list {@code val}.
     * @param prefs the preferences
     * @param key key
     * @param maxsize max number of items to save
     * @param val value
     * @return {@code true}, if something has changed (i.e. value is different than before)
     * @since 12894
     */
    public static boolean putListBounded(IPreferences prefs, String key, int maxsize, List<String> val) {
        List<String> newCollection = new ArrayList<>(Math.min(maxsize, val.size()));
        for (String i : val) {
            if (newCollection.size() >= maxsize) {
                break;
            }
            newCollection.add(i);
        }
        return prefs.putList(key, newCollection);
    }

}
