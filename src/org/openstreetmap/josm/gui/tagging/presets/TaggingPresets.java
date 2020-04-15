// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.actions.PreferencesAction;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MainMenu;
import org.openstreetmap.josm.gui.MenuScroller;
import org.openstreetmap.josm.gui.preferences.ToolbarPreferences;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.presets.items.CheckGroup;
import org.openstreetmap.josm.gui.tagging.presets.items.KeyedItem;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles;
import org.openstreetmap.josm.gui.tagging.presets.items.Roles.Role;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.MultiMap;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Class holding Tagging Presets and allowing to manage them.
 * @since 7100
 */
public final class TaggingPresets {

    /** The collection of tagging presets */
    private static final Collection<TaggingPreset> taggingPresets = new ArrayList<>();

    /** cache for key/value pairs found in the preset */
    private static final MultiMap<String, String> PRESET_TAG_CACHE = new MultiMap<>();
    /** cache for roles found in the preset */
    private static final Set<String> PRESET_ROLE_CACHE = new HashSet<>();

    /** The collection of listeners */
    private static final Collection<TaggingPresetListener> listeners = new ArrayList<>();

    private TaggingPresets() {
        // Hide constructor for utility classes
    }

    /**
     * Initializes tagging presets from preferences.
     */
    public static void readFromPreferences() {
        taggingPresets.clear();
        taggingPresets.addAll(TaggingPresetReader.readFromPreferences(false, false));
        cachePresets(taggingPresets);
    }

    /**
     * Initialize the tagging presets (load and may display error)
     */
    public static void initialize() {
        MainMenu mainMenu = MainApplication.getMenu();
        JMenu presetsMenu = mainMenu.presetsMenu;
        if (presetsMenu.getComponentCount() == 0) {
            MainMenu.add(presetsMenu, mainMenu.presetSearchAction);
            MainMenu.add(presetsMenu, mainMenu.presetSearchPrimitiveAction);
            MainMenu.add(presetsMenu, PreferencesAction.forPreferenceSubTab(tr("Preset preferences..."),
                    tr("Click to open the tagging presets tab in the preferences"), TaggingPresetPreference.class));
            presetsMenu.addSeparator();
        }

        readFromPreferences();
        for (TaggingPreset tp: taggingPresets) {
            if (!(tp instanceof TaggingPresetSeparator)) {
                MainApplication.getToolbar().register(tp);
            }
        }
        if (taggingPresets.isEmpty()) {
            presetsMenu.setVisible(false);
        } else {
            Map<TaggingPresetMenu, JMenu> submenus = new HashMap<>();
            for (final TaggingPreset p : taggingPresets) {
                JMenu m = p.group != null ? submenus.get(p.group) : presetsMenu;
                if (m == null && p.group != null) {
                    Logging.error("No tagging preset submenu for " + p.group);
                } else if (m == null) {
                    Logging.error("No tagging preset menu. Tagging preset " + p + " won't be available there");
                } else if (p instanceof TaggingPresetSeparator) {
                    m.add(new JSeparator());
                } else if (p instanceof TaggingPresetMenu) {
                    JMenu submenu = new JMenu(p);
                    submenu.setText(p.getLocaleName());
                    ((TaggingPresetMenu) p).menu = submenu;
                    submenus.put((TaggingPresetMenu) p, submenu);
                    m.add(submenu);
                } else {
                    JMenuItem mi = new JMenuItem(p);
                    mi.setText(p.getLocaleName());
                    m.add(mi);
                }
            }
            for (JMenu submenu : submenus.values()) {
                if (submenu.getItemCount() >= Config.getPref().getInt("taggingpreset.min-elements-for-scroller", 15)) {
                    MenuScroller.setScrollerFor(submenu);
                }
            }
        }
        if (Config.getPref().getBoolean("taggingpreset.sortmenu")) {
            TaggingPresetMenu.sortMenu(presetsMenu);
        }
        listeners.forEach(TaggingPresetListener::taggingPresetsModified);
    }

    // Cannot implement Destroyable since this is static
    /**
     * Call to deconstruct the TaggingPresets menus and other information so that it
     * can be rebuilt later.
     *
     * @since 15582
     */
    public static void destroy() {
        ToolbarPreferences toolBar = MainApplication.getToolbar();
        taggingPresets.forEach(toolBar::unregister);
        taggingPresets.clear();
        PRESET_TAG_CACHE.clear();
        PRESET_ROLE_CACHE.clear();
        MainApplication.getMenu().presetsMenu.removeAll();
        listeners.forEach(TaggingPresetListener::taggingPresetsModified);
    }

    /**
     * Initialize the cache for presets. This is done only once.
     * @param presets Tagging presets to cache
     */
    public static void cachePresets(Collection<TaggingPreset> presets) {
        for (final TaggingPreset p : presets) {
            for (TaggingPresetItem item : p.data) {
                cachePresetItem(p, item);
            }
        }
    }

    private static void cachePresetItem(TaggingPreset p, TaggingPresetItem item) {
        if (item instanceof KeyedItem) {
            KeyedItem ki = (KeyedItem) item;
            if (ki.key != null && ki.getValues() != null) {
                PRESET_TAG_CACHE.putAll(ki.key, ki.getValues());
            }
        } else if (item instanceof Roles) {
            Roles r = (Roles) item;
            for (Role i : r.roles) {
                if (i.key != null) {
                    PRESET_ROLE_CACHE.add(i.key);
                }
            }
        } else if (item instanceof CheckGroup) {
            for (KeyedItem check : ((CheckGroup) item).checks) {
                cachePresetItem(p, check);
            }
        }
    }

    /**
     * Replies a new collection containing all tagging presets.
     * @return a new collection containing all tagging presets. Empty if presets are not initialized (never null)
     */
    public static Collection<TaggingPreset> getTaggingPresets() {
        return Collections.unmodifiableCollection(taggingPresets);
    }

    /**
     * Replies a set of all roles in the tagging presets.
     * @return a set of all roles in the tagging presets.
     */
    public static Set<String> getPresetRoles() {
        return Collections.unmodifiableSet(PRESET_ROLE_CACHE);
    }

    /**
     * Replies a set of all keys in the tagging presets.
     * @return a set of all keys in the tagging presets.
     */
    public static Set<String> getPresetKeys() {
        return Collections.unmodifiableSet(PRESET_TAG_CACHE.keySet());
    }

    /**
     * Return set of values for a key in the tagging presets
     * @param key the key
     * @return set of values for a key in the tagging presets or null if none is found
     */
    public static Set<String> getPresetValues(String key) {
        Set<String> values = PRESET_TAG_CACHE.get(key);
        if (values != null)
            return Collections.unmodifiableSet(values);
        return null;
    }

    /**
     * Replies a new collection of all presets matching the parameters.
     *
     * @param t the preset types to include
     * @param tags the tags to perform matching on, see {@link TaggingPresetItem#matches(Map)}
     * @param onlyShowable whether only {@link TaggingPreset#isShowable() showable} presets should be returned
     * @return a new collection of all presets matching the parameters.
     * @see TaggingPreset#matches(Collection, Map, boolean)
     * @since 9266
     */
    public static Collection<TaggingPreset> getMatchingPresets(final Collection<TaggingPresetType> t,
                                                               final Map<String, String> tags, final boolean onlyShowable) {
        return SubclassFilteredCollection.filter(getTaggingPresets(), preset -> preset.matches(t, tags, onlyShowable));
    }

    /**
     * Replies a new collection of all presets matching the given preset.
     *
     * @param primitive the primitive
     * @return a new collection of all presets matching the given preset.
     * @see TaggingPreset#test(IPrimitive)
     * @since 13623 (signature)
     */
    public static Collection<TaggingPreset> getMatchingPresets(final IPrimitive primitive) {
        return SubclassFilteredCollection.filter(getTaggingPresets(), preset -> preset.test(primitive));
    }

    /**
     * Adds a list of tagging presets to the current list.
     * @param presets The tagging presets to add
     */
    public static void addTaggingPresets(Collection<TaggingPreset> presets) {
        if (presets != null && taggingPresets.addAll(presets)) {
            listeners.forEach(TaggingPresetListener::taggingPresetsModified);
        }
    }

    /**
     * Adds a tagging preset listener.
     * @param listener The listener to add
     */
    public static void addListener(TaggingPresetListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a tagging preset listener.
     * @param listener The listener to remove
     */
    public static void removeListener(TaggingPresetListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }
}
