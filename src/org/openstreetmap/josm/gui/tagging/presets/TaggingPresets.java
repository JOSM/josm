// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;

/**
 * Class holding Tagging Presets and allowing to manage them.
 * @since 7100
 */
public final class TaggingPresets {

    /** The collection of tagging presets */
    private static final Collection<TaggingPreset> taggingPresets = new ArrayList<>();

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
    }

    /**
     * Initialize the tagging presets (load and may display error)
     */
    public static void initialize() {
        readFromPreferences();
        for (TaggingPreset tp: taggingPresets) {
            if (!(tp instanceof TaggingPresetSeparator)) {
                Main.toolbar.register(tp);
            }
        }
        if (taggingPresets.isEmpty()) {
            Main.main.menu.presetsMenu.setVisible(false);
        } else {
            AutoCompletionManager.cachePresets(taggingPresets);
            Map<TaggingPresetMenu, JMenu> submenus = new HashMap<>();
            for (final TaggingPreset p : taggingPresets) {
                JMenu m = p.group != null ? submenus.get(p.group) : Main.main.menu.presetsMenu;
                if (m == null && p.group != null) {
                    Main.error("No tagging preset submenu for " + p.group);
                } else if (m == null) {
                    Main.error("No tagging preset menu. Tagging preset " + p + " won't be available there");
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
        }
        if (Main.pref.getBoolean("taggingpreset.sortmenu")) {
            TaggingPresetMenu.sortMenu(Main.main.menu.presetsMenu);
        }
    }

    /**
     * Replies a new collection containing all tagging presets.
     * @return a new collection containing all tagging presets. Empty if presets are not initialized (never null)
     */
    public static Collection<TaggingPreset> getTaggingPresets() {
        return new ArrayList<>(taggingPresets);
    }

    /**
     * Adds a list of tagging presets to the current list.
     * @param presets The tagging presets to add
     */
    public static void addTaggingPresets(Collection<TaggingPreset> presets) {
        if (presets != null) {
            if (taggingPresets.addAll(presets)) {
                for (TaggingPresetListener listener : listeners) {
                    listener.taggingPresetsModified();
                }
            }
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
