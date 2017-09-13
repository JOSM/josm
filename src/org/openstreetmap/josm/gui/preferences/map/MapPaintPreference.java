// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.preferences.sources.MapPaintPrefHelper;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceProvider;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.MapCSSStyleSource;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SourceEditor;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

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
                Config.getPref().getBoolean("mappaint.icon.enable-defaults", true));

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
            return Config.getPref().getList(ICONPREF, null);
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
        boolean reload = Config.getPref().putBoolean("mappaint.icon.enable-defaults", enableIconDefault.isSelected());
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

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(final PreferenceTabbedPane gui) {
        return gui.getMapPreference();
    }
}
