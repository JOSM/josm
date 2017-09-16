// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.ListListSetting;
import org.openstreetmap.josm.data.preferences.ListSetting;
import org.openstreetmap.josm.data.preferences.MapListSetting;
import org.openstreetmap.josm.data.preferences.Setting;
import org.openstreetmap.josm.data.preferences.StringSetting;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Helper class to do specific Preferences operation - appending, replacing, deletion by key and by value
 * Also contains functions that convert preferences object to JavaScript object and back
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
                Main.parent,
                tr("<html>Settings file asks to append preferences to <b>{0}</b>,<br/> "+
                        "but its default value is unknown at this moment.<br/> " +
                        "Please activate corresponding function manually and retry importing.", key),
                tr("Warning"),
                JOptionPane.WARNING_MESSAGE);
    }

    public static void showPrefs(Preferences tmpPref) {
        Logging.info("properties: " + tmpPref.settingsMap);
    }

    public static void modifyPreferencesByScript(ScriptEngine engine, Preferences tmpPref, String js) throws ScriptException {
        loadPrefsToJS(engine, tmpPref, "API.pref", true);
        engine.eval(js);
        readPrefsFromJS(engine, tmpPref, "API.pref");
    }

    /**
     * Convert JavaScript preferences object to preferences data structures
     * @param engine - JS engine to put object
     * @param tmpPref - preferences to fill from JS
     * @param varInJS - JS variable name, where preferences are stored
     * @throws ScriptException if the evaluation fails
     */
    public static void readPrefsFromJS(ScriptEngine engine, Preferences tmpPref, String varInJS) throws ScriptException {
        String finish =
            "stringMap = new java.util.TreeMap ;"+
            "listMap =  new java.util.TreeMap ;"+
            "listlistMap = new java.util.TreeMap ;"+
            "listmapMap =  new java.util.TreeMap ;"+
            "for (key in "+varInJS+") {"+
            "  val = "+varInJS+"[key];"+
            "  type = typeof val == 'string' ? 'string' : val.type;"+
            "  if (type == 'string') {"+
            "    stringMap.put(key, val);"+
            "  } else if (type == 'list') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      l.add(java.lang.String.valueOf(val[i]));"+
            "    }"+
            "    listMap.put(key, l);"+
            "  } else if (type == 'listlist') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      list=val[i];"+
            "      jlist=new java.util.ArrayList;"+
            "      for (j=0; j<list.length; j++) {"+
            "         jlist.add(java.lang.String.valueOf(list[j]));"+
            "      }"+
            "      l.add(jlist);"+
            "    }"+
            "    listlistMap.put(key, l);"+
            "  } else if (type == 'listmap') {"+
            "    l = new java.util.ArrayList;"+
            "    for (i=0; i<val.length; i++) {"+
            "      map=val[i];"+
            "      jmap=new java.util.TreeMap;"+
            "      for (var key2 in map) {"+
            "         jmap.put(key2,java.lang.String.valueOf(map[key2]));"+
            "      }"+
            "      l.add(jmap);"+
            "    }"+
            "    listmapMap.put(key, l);"+
            "  }  else {" +
            "   " + PreferencesUtils.class.getName() + ".log('Unknown type:'+val.type+ '- use list, listlist or listmap'); }"+
            "  }";
        engine.eval(finish);

        @SuppressWarnings("unchecked")
        Map<String, String> stringMap = (Map<String, String>) engine.get("stringMap");
        @SuppressWarnings("unchecked")
        Map<String, List<String>> listMap = (Map<String, List<String>>) engine.get("listMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Collection<String>>> listlistMap = (Map<String, List<Collection<String>>>) engine.get("listlistMap");
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, String>>> listmapMap = (Map<String, List<Map<String, String>>>) engine.get("listmapMap");

        tmpPref.settingsMap.clear();

        Map<String, Setting<?>> tmp = new HashMap<>();
        for (Entry<String, String> e : stringMap.entrySet()) {
            tmp.put(e.getKey(), new StringSetting(e.getValue()));
        }
        for (Entry<String, List<String>> e : listMap.entrySet()) {
            tmp.put(e.getKey(), new ListSetting(e.getValue()));
        }

        for (Entry<String, List<Collection<String>>> e : listlistMap.entrySet()) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            List<List<String>> value = (List) e.getValue();
            tmp.put(e.getKey(), new ListListSetting(value));
        }
        for (Entry<String, List<Map<String, String>>> e : listmapMap.entrySet()) {
            tmp.put(e.getKey(), new MapListSetting(e.getValue()));
        }
        for (Entry<String, Setting<?>> e : tmp.entrySet()) {
            if (e.getValue().equals(tmpPref.defaultsMap.get(e.getKey()))) continue;
            tmpPref.settingsMap.put(e.getKey(), e.getValue());
        }
    }

    /**
     * Convert preferences data structures to JavaScript object
     * @param engine - JS engine to put object
     * @param tmpPref - preferences to convert
     * @param whereToPutInJS - variable name to store preferences in JS
     * @param includeDefaults - include known default values to JS objects
     * @throws ScriptException if the evaluation fails
     */
    public static void loadPrefsToJS(ScriptEngine engine, Preferences tmpPref, String whereToPutInJS, boolean includeDefaults)
            throws ScriptException {
        Map<String, String> stringMap = new TreeMap<>();
        Map<String, List<String>> listMap = new TreeMap<>();
        Map<String, List<List<String>>> listlistMap = new TreeMap<>();
        Map<String, List<Map<String, String>>> listmapMap = new TreeMap<>();

        if (includeDefaults) {
            for (Map.Entry<String, Setting<?>> e: tmpPref.defaultsMap.entrySet()) {
                Setting<?> setting = e.getValue();
                if (setting instanceof StringSetting) {
                    stringMap.put(e.getKey(), ((StringSetting) setting).getValue());
                } else if (setting instanceof ListSetting) {
                    listMap.put(e.getKey(), ((ListSetting) setting).getValue());
                } else if (setting instanceof ListListSetting) {
                    listlistMap.put(e.getKey(), ((ListListSetting) setting).getValue());
                } else if (setting instanceof MapListSetting) {
                    listmapMap.put(e.getKey(), ((MapListSetting) setting).getValue());
                }
            }
        }
        tmpPref.settingsMap.entrySet().removeIf(e -> e.getValue().getValue() == null);

        for (Map.Entry<String, Setting<?>> e: tmpPref.settingsMap.entrySet()) {
            Setting<?> setting = e.getValue();
            if (setting instanceof StringSetting) {
                stringMap.put(e.getKey(), ((StringSetting) setting).getValue());
            } else if (setting instanceof ListSetting) {
                listMap.put(e.getKey(), ((ListSetting) setting).getValue());
            } else if (setting instanceof ListListSetting) {
                listlistMap.put(e.getKey(), ((ListListSetting) setting).getValue());
            } else if (setting instanceof MapListSetting) {
                listmapMap.put(e.getKey(), ((MapListSetting) setting).getValue());
            }
        }

        engine.put("stringMap", stringMap);
        engine.put("listMap", listMap);
        engine.put("listlistMap", listlistMap);
        engine.put("listmapMap", listmapMap);

        String init =
            "function getJSList( javaList ) {"+
            " var jsList; var i; "+
            " if (javaList == null) return null;"+
            "jsList = [];"+
            "  for (i = 0; i < javaList.size(); i++) {"+
            "    jsList.push(String(list.get(i)));"+
            "  }"+
            "return jsList;"+
            "}"+
            "function getJSMap( javaMap ) {"+
            " var jsMap; var it; var e; "+
            " if (javaMap == null) return null;"+
            " jsMap = {};"+
            " for (it = javaMap.entrySet().iterator(); it.hasNext();) {"+
            "    e = it.next();"+
            "    jsMap[ String(e.getKey()) ] = String(e.getValue()); "+
            "  }"+
            "  return jsMap;"+
            "}"+
            "for (it = stringMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            whereToPutInJS+"[String(e.getKey())] = String(e.getValue());"+
            "}\n"+
            "for (it = listMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            "  list = e.getValue();"+
            "  jslist = getJSList(list);"+
            "  jslist.type = 'list';"+
            whereToPutInJS+"[String(e.getKey())] = jslist;"+
            "}\n"+
            "for (it = listlistMap.entrySet().iterator(); it.hasNext(); ) {"+
            "  e = it.next();"+
            "  listlist = e.getValue();"+
            "  jslistlist = [];"+
            "  for (it2 = listlist.iterator(); it2.hasNext(); ) {"+
            "    list = it2.next(); "+
            "    jslistlist.push(getJSList(list));"+
            "    }"+
            "  jslistlist.type = 'listlist';"+
            whereToPutInJS+"[String(e.getKey())] = jslistlist;"+
            "}\n"+
            "for (it = listmapMap.entrySet().iterator(); it.hasNext();) {"+
            "  e = it.next();"+
            "  listmap = e.getValue();"+
            "  jslistmap = [];"+
            "  for (it2 = listmap.iterator(); it2.hasNext();) {"+
            "    map = it2.next();"+
            "    jslistmap.push(getJSMap(map));"+
            "    }"+
            "  jslistmap.type = 'listmap';"+
            whereToPutInJS+"[String(e.getKey())] = jslistmap;"+
            "}\n";

        // Execute conversion script
        engine.eval(init);
    }
}
