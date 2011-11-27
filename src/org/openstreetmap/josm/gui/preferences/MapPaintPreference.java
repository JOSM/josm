// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;

public class MapPaintPreference implements PreferenceSetting {
    private SourceEditor sources;
    private JCheckBox enableIconDefault;

    public static class Factory implements PreferenceSettingFactory {
        public PreferenceSetting createPreferenceSetting() {
            return new MapPaintPreference();
        }
    }

    public void addGui(final PreferenceTabbedPane gui) {
        enableIconDefault = new JCheckBox(tr("Enable built-in icon defaults"),
                Main.pref.getBoolean("mappaint.icon.enable-defaults", true));

        sources = new MapPaintSourceEditor();

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));

        panel.add(sources, GBC.eol().fill(GBC.BOTH));
        panel.add(enableIconDefault, GBC.eol().insets(11,2,5,0));

        gui.mapcontent.addTab(tr("Map Paint Styles"), panel);

        // this defers loading of style sources to the first time the tab
        // with the map paint preferences is selected by the user
        //
        gui.mapcontent.addChangeListener(
                new ChangeListener() {
                    public void stateChanged(ChangeEvent e) {
                        if (gui.mapcontent.getSelectedComponent() == panel) {
                            sources.initiallyLoadAvailableSources();
                        }
                    }
                }
        );
    }

    class MapPaintSourceEditor extends SourceEditor {

        final private String iconpref = "mappaint.icon.sources";

        public MapPaintSourceEditor() {
            super(true, "http://josm.openstreetmap.de/styles");
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return MapPaintPrefMigration.INSTANCE.get();
        }

        @Override
        public boolean finish() {
            List<SourceEntry> activeStyles = activeSourcesModel.getSources();

            boolean changed = MapPaintPrefMigration.INSTANCE.put(activeStyles);

            if (tblIconPaths != null) {
                List<String> iconPaths = iconPathsModel.getIconPaths();

                if (!iconPaths.isEmpty()) {
                    if (Main.pref.putCollection(iconpref, iconPaths)) {
                        changed = true;
                    }
                } else if (Main.pref.putCollection(iconpref, null)) {
                    changed = true;
                }
            }
            return changed;
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            return MapPaintPrefMigration.INSTANCE.getDefault();
        }

        @Override
        public Collection<String> getInitialIconPathsList() {
            return Main.pref.getCollection(iconpref, null);
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

    }

    public boolean ok() {
        boolean reload = Main.pref.put("mappaint.icon.enable-defaults", enableIconDefault.isSelected());
        reload |= sources.finish();
        if (reload) {
            MapPaintStyles.readFromPreferences();
        }
        if (Main.isDisplayingMapView())
        {
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

    public static class MapPaintPrefMigration extends SourceEditor.SourcePrefMigration {

        public final static MapPaintPrefMigration INSTANCE = new MapPaintPrefMigration();

        public MapPaintPrefMigration() {
            super("mappaint.style.sources",
                  "mappaint.style.enable-defaults",
                  "mappaint.style.sources-list");
        }

        @Override
        public List<SourceEntry> get() {
            List<SourceEntry> ls = super.get();
            if (adapt_elemstyles_xml(ls)) {
                put(ls);
            }
            if (insertNewDefaults(ls)) {
                put(ls);
            }
            return ls;
        }

        /**
         * The internal path of elemstyles.xml has changed, this
         * can be removed when a few months have passed.
         */
        private boolean adapt_elemstyles_xml(List<SourceEntry> ls) {
            boolean changed = false;
            for (SourceEntry se : ls) {
                if (se.url.equals("resource://data/elemstyles.xml")) {
                    se.url = "resource://styles/standard/elemstyles.xml";
                    changed = true;
                }
            }
            return changed;
        }

        /**
         * If the selection of default styles changes in future releases, add
         * the new entries to the user-configured list. Remember the known URLs,
         * so an item that was deleted explicitly is not added again.
         */
        private boolean insertNewDefaults(List<SourceEntry> list) {
            boolean changed = false;
            
            Collection<String> knownDefaults = new TreeSet<String>(Main.pref.getCollection("mappaint.style.known-defaults"));

            Collection<ExtendedSourceEntry> defaults = getDefault();
            int insertionIdx = 0;
            for (final SourceEntry def : defaults) {
                int i = Utils.indexOf(list,
                        new Predicate<SourceEntry>() {
                            @Override
                            public boolean evaluate(SourceEntry se) {
                                return Utils.equal(def.url, se.url);
                            }
                        });
                if (i == -1 && !knownDefaults.contains(def.url)) {
                    list.add(insertionIdx, def);
                    insertionIdx++;
                    changed = true;
                } else {
                    if (i >= insertionIdx) {
                        insertionIdx = i + 1;
                    }
                }
            }

            for (SourceEntry def : defaults) {
                knownDefaults.add(def.url);
            }
            Main.pref.putCollection("mappaint.style.known-defaults", knownDefaults);

            return changed;
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            ExtendedSourceEntry defJOSM = new ExtendedSourceEntry("elemstyles.xml", "resource://styles/standard/elemstyles.xml");
            defJOSM.active = true;
            defJOSM.name = "standard";
            defJOSM.title = tr("JOSM Internal Style");
            defJOSM.description = tr("Internal style to be used as base for runtime switchable overlay styles");
            ExtendedSourceEntry defPL2 = new ExtendedSourceEntry("potlatch2.mapcss", "resource://styles/standard/potlatch2.mapcss");
            defPL2.active = false;
            defPL2.name = "standard";
            defPL2.title = tr("Potlatch 2");
            defPL2.description = tr("the main Potlatch 2 style");

            return Arrays.asList(new ExtendedSourceEntry[] { defJOSM, defPL2 });
        }

        @Override
        public Collection<String> serialize(SourceEntry entry) {
            return Arrays.asList(new String[] {
                    entry.url,
                    entry.name == null ? "" : entry.name,
                    entry.title == null ? "" : entry.title,
                    Boolean.toString(entry.active)
            });
        }

        @Override
        public SourceEntry deserialize(List<String> entryStr) {
            if (entryStr.size() < 4)
                return null;
            String url = entryStr.get(0);
            String name = entryStr.get(1);
            String shortdescription = entryStr.get(2);
            boolean active = Boolean.parseBoolean(entryStr.get(3));
            return new SourceEntry(url, name, shortdescription, active);
        }
    }
}
