// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.preferences;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.tools.GBC;

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
            super("http://josm.openstreetmap.de/styles");
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return (new MapPaintPrefMigration()).get();
        }

        @Override
        public boolean finish() {
            List<SourceEntry> activeStyles = activeSourcesModel.getSources();

            boolean changed = (new MapPaintPrefMigration()).put(activeStyles);

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
            return (new MapPaintPrefMigration()).getDefault();
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
        Boolean restart = false;
        if(Main.pref.put("mappaint.icon.enable-defaults", enableIconDefault.isSelected())) {
            restart = true;
        }
        if(sources.finish()) {
            restart = true;
        }
        if(Main.isDisplayingMapView())
        {
            MapPaintStyles.getStyles().clearCached();
        }
        return restart;
    }

    /**
     * Initialize the styles
     */
    public static void initialize() {
        MapPaintStyles.readFromPreferences();
    }

    public static class MapPaintPrefMigration extends SourceEditor.SourcePrefMigration {

        public MapPaintPrefMigration() {
            super("mappaint.style.sources",
                  "mappaint.style.enable-defaults",
                  "mappaint.style.sources-list");
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            ExtendedSourceEntry i = new ExtendedSourceEntry("elemstyles.xml", "resource://data/elemstyles.xml");
            i.name = "standard";
            i.shortdescription = tr("Internal Style");
            i.description = tr("Internal style to be used as base for runtime switchable overlay styles");
            return Collections.singletonList(i);
        }

        @Override
        public Collection<String> serialize(SourceEntry entry) {
            return Arrays.asList(new String[] {entry.url, entry.name, entry.shortdescription, Boolean.toString(entry.active)});
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
