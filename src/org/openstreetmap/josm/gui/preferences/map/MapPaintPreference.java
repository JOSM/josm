// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SourceEditor;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.SourceProvider;
import org.openstreetmap.josm.gui.preferences.SourceType;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Preference settings for map paint styles.
 */
public class MapPaintPreference implements SubPreferenceSetting {
    private SourceEditor sources;
    private JCheckBox enableIconDefault;

    private static final List<SourceProvider> styleSourceProviders = new ArrayList<>();

    /**
     * Registers a new additional style source provider.
     * @param provider The style source provider
     * @return {@code true}, if the provider has been added, {@code false} otherwise
     */
    public static boolean registerSourceProvider(SourceProvider provider) {
        if (provider != null)
            return styleSourceProviders.add(provider);
        return false;
    }

    /**
     * Factory used to create a new {@code MapPaintPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new MapPaintPreference();
        }
    }

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        enableIconDefault = new JCheckBox(tr("Enable built-in icon defaults"),
                Main.pref.getBoolean("mappaint.icon.enable-defaults", true));

        sources = new MapPaintSourceEditor();

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));

        panel.add(sources, GBC.eol().fill(GBC.BOTH));
        panel.add(enableIconDefault, GBC.eol().insets(11, 2, 5, 0));

        final MapPreference mapPref = gui.getMapPreference();
        mapPref.addSubTab(this, tr("Map Paint Styles"), panel);
        sources.deferLoading(mapPref, panel);
    }

    static class MapPaintSourceEditor extends SourceEditor {

        private static final String ICONPREF = "mappaint.icon.sources";

        MapPaintSourceEditor() {
            super(SourceType.MAP_PAINT_STYLE, Main.getJOSMWebsite()+"/styles", styleSourceProviders, true);
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return MapPaintPrefHelper.INSTANCE.get();
        }

        @Override
        public boolean finish() {
            return doFinish(MapPaintPrefHelper.INSTANCE, ICONPREF);
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            return MapPaintPrefHelper.INSTANCE.getDefault();
        }

        @Override
        public Collection<String> getInitialIconPathsList() {
            return Main.pref.getCollection(ICONPREF, null);
        }

        @Override
        public String getStr(I18nString ident) {
            switch (ident) {
            case AVAILABLE_SOURCES:
                return tr("Available styles:");
            case ACTIVE_SOURCES:
                return tr("Active styles:");
            case NEW_SOURCE_ENTRY_TOOLTIP:
                return tr("Add a new style by entering filename or URL");
            case NEW_SOURCE_ENTRY:
                return tr("New style entry:");
            case REMOVE_SOURCE_TOOLTIP:
                return tr("Remove the selected styles from the list of active styles");
            case EDIT_SOURCE_TOOLTIP:
                return tr("Edit the filename or URL for the selected active style");
            case ACTIVATE_TOOLTIP:
                return tr("Add the selected available styles to the list of active styles");
            case RELOAD_ALL_AVAILABLE:
                return marktr("Reloads the list of available styles from ''{0}''");
            case LOADING_SOURCES_FROM:
                return marktr("Loading style sources from ''{0}''");
            case FAILED_TO_LOAD_SOURCES_FROM:
                return marktr("<html>Failed to load the list of style sources from<br>"
                        + "''{0}''.<br>"
                        + "<br>"
                        + "Details (untranslated):<br>{1}</html>");
            case FAILED_TO_LOAD_SOURCES_FROM_HELP_TOPIC:
                return "/Preferences/Styles#FailedToLoadStyleSources";
            case ILLEGAL_FORMAT_OF_ENTRY:
                return marktr("Warning: illegal format of entry in style list ''{0}''. Got ''{1}''");
            default: throw new AssertionError();
            }
        }

        @Override
        protected String getTitleForSourceEntry(SourceEntry entry) {
            final String title = getTitleFromSourceEntry(entry);
            return title != null ? title : super.getTitleForSourceEntry(entry);
        }
    }

    /**
     * Returns title from a source entry.
     * @param entry source entry
     * @return title
     * @see MapCSSStyleSource#title
     */
    public static String getTitleFromSourceEntry(SourceEntry entry) {
        try {
            final MapCSSStyleSource css = new MapCSSStyleSource(entry);
            css.loadStyleSource();
            if (css.title != null && !css.title.isEmpty()) {
                return css.title;
            }
        } catch (RuntimeException ignore) { // NOPMD
            Logging.debug(ignore);
        }
        return null;
    }

    @Override
    public boolean ok() {
        boolean reload = Main.pref.put("mappaint.icon.enable-defaults", enableIconDefault.isSelected());
        reload |= sources.finish();
        if (reload) {
            MapPaintStyles.readFromPreferences();
        }
        if (MainApplication.isDisplayingMapView()) {
            MapPaintStyles.getStyles().clearCached();
        }
        return false;
    }

    /**
     * Initialize the styles
     */
    public static void initialize() {
        MapPaintStyles.readFromPreferences();
    }

    /**
     * Helper class for map paint styles preferences.
     */
    public static class MapPaintPrefHelper extends SourceEditor.SourcePrefHelper {

        /**
         * The unique instance.
         */
        public static final MapPaintPrefHelper INSTANCE = new MapPaintPrefHelper();

        /**
         * Constructs a new {@code MapPaintPrefHelper}.
         */
        public MapPaintPrefHelper() {
            super("mappaint.style.entries");
        }

        @Override
        public List<SourceEntry> get() {
            List<SourceEntry> ls = super.get();
            if (insertNewDefaults(ls)) {
                put(ls);
            }
            return ls;
        }

        /**
         * If the selection of default styles changes in future releases, add
         * the new entries to the user-configured list. Remember the known URLs,
         * so an item that was deleted explicitly is not added again.
         * @param list new defaults
         * @return {@code true} if a change occurred
         */
        private boolean insertNewDefaults(List<SourceEntry> list) {
            boolean changed = false;

            Collection<String> knownDefaults = new TreeSet<>(Main.pref.getCollection("mappaint.style.known-defaults"));

            Collection<ExtendedSourceEntry> defaults = getDefault();
            int insertionIdx = 0;
            for (final SourceEntry def : defaults) {
                int i = Utils.indexOf(list, se -> Objects.equals(def.url, se.url));
                if (i == -1 && !knownDefaults.contains(def.url)) {
                    def.active = false;
                    list.add(insertionIdx, def);
                    insertionIdx++;
                    changed = true;
                } else {
                    if (i >= insertionIdx) {
                        insertionIdx = i + 1;
                    }
                }
                knownDefaults.add(def.url);
            }
            Main.pref.putCollection("mappaint.style.known-defaults", knownDefaults);

            // XML style is not bundled anymore
            list.remove(Utils.find(list, se -> "resource://styles/standard/elemstyles.xml".equals(se.url)));

            return changed;
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            ExtendedSourceEntry defJosmMapcss = new ExtendedSourceEntry("elemstyles.mapcss", "resource://styles/standard/elemstyles.mapcss");
            defJosmMapcss.active = true;
            defJosmMapcss.name = "standard";
            defJosmMapcss.title = tr("JOSM default (MapCSS)");
            defJosmMapcss.description = tr("Internal style to be used as base for runtime switchable overlay styles");
            ExtendedSourceEntry defPL2 = new ExtendedSourceEntry("potlatch2.mapcss", "resource://styles/standard/potlatch2.mapcss");
            defPL2.active = false;
            defPL2.name = "standard";
            defPL2.title = tr("Potlatch 2");
            defPL2.description = tr("the main Potlatch 2 style");

            return Arrays.asList(defJosmMapcss, defPL2);
        }

        @Override
        public Map<String, String> serialize(SourceEntry entry) {
            Map<String, String> res = new HashMap<>();
            res.put("url", entry.url == null ? "" : entry.url);
            res.put("title", entry.title == null ? "" : entry.title);
            res.put("active", Boolean.toString(entry.active));
            if (entry.name != null) {
                res.put("ptoken", entry.name);
            }
            return res;
        }

        @Override
        public SourceEntry deserialize(Map<String, String> s) {
            return new SourceEntry(s.get("url"), s.get("ptoken"), s.get("title"), Boolean.parseBoolean(s.get("active")));
        }
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getMapPreference();
    }
}
