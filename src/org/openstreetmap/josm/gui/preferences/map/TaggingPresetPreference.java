// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.map;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane.ValidationListener;
import org.openstreetmap.josm.gui.preferences.SourceEditor;
import org.openstreetmap.josm.gui.preferences.SourceEditor.ExtendedSourceEntry;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.SourceProvider;
import org.openstreetmap.josm.gui.preferences.SourceType;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.tagging.TaggingPreset;
import org.openstreetmap.josm.gui.tagging.TaggingPresetMenu;
import org.openstreetmap.josm.gui.tagging.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.TaggingPresetSeparator;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.tools.GBC;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Preference settings for tagging presets.
 */
public final class TaggingPresetPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code TaggingPresetPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new TaggingPresetPreference();
        }
    }

    private TaggingPresetPreference() {
        super();
    }

    private static final List<SourceProvider> presetSourceProviders = new ArrayList<SourceProvider>();
    
    /**
     * The collection of tagging presets.
     */
    public static Collection<TaggingPreset> taggingPresets;
    private SourceEditor sources;
    private JCheckBox sortMenu;

    /**
     * Registers a new additional preset source provider.
     * @param provider The preset source provider
     * @return {@code true}, if the provider has been added, {@code false} otherwise
     */
    public static final boolean registerSourceProvider(SourceProvider provider) {
        if (provider != null)
            return presetSourceProviders.add(provider);
        return false;
    }

    private ValidationListener validationListener = new ValidationListener() {
        @Override
        public boolean validatePreferences() {
            if (sources.hasActiveSourcesChanged()) {
                List<Integer> sourcesToRemove = new ArrayList<Integer>();
                int i = -1;
                SOURCES:
                    for (SourceEntry source: sources.getActiveSources()) {
                        i++;
                        boolean canLoad = false;
                        try {
                            TaggingPresetReader.readAll(source.url, false);
                            canLoad = true;
                        } catch (IOException e) {
                            Main.warn(tr("Could not read tagging preset source: {0}", source));
                            ExtendedDialog ed = new ExtendedDialog(Main.parent, tr("Error"),
                                    new String[] {tr("Yes"), tr("No"), tr("Cancel")});
                            ed.setContent(tr("Could not read tagging preset source: {0}\nDo you want to keep it?", source));
                            switch (ed.showDialog().getValue()) {
                            case 1:
                                continue SOURCES;
                            case 2:
                                sourcesToRemove.add(i);
                                continue SOURCES;
                            default:
                                return false;
                            }
                        } catch (SAXException e) {
                            // We will handle this in step with validation
                        }

                        String errorMessage = null;

                        try {
                            TaggingPresetReader.readAll(source.url, true);
                        } catch (IOException e) {
                            // Should not happen, but at least show message
                            String msg = tr("Could not read tagging preset source {0}", source);
                            Main.error(msg);
                            JOptionPane.showMessageDialog(Main.parent, msg);
                            return false;
                        } catch (SAXParseException e) {
                            if (canLoad) {
                                errorMessage = tr("<html>Tagging preset source {0} can be loaded but it contains errors. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: [{1}:{2}] {3}</table></html>",
                                        source, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                            } else {
                                errorMessage = tr("<html>Unable to parse tagging preset source: {0}. " +
                                        "Do you really want to use it?<br><br><table width=400>Error is: [{1}:{2}] {3}</table></html>",
                                        source, e.getLineNumber(), e.getColumnNumber(), e.getMessage());
                            }
                        } catch (SAXException e) {
                            if (canLoad) {
                                errorMessage = tr("<html>Tagging preset source {0} can be loaded but it contains errors. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: {1}</table></html>",
                                        source,  e.getMessage());
                            } else {
                                errorMessage = tr("<html>Unable to parse tagging preset source: {0}. " +
                                        "Do you really want to use it?<br><br><table width=600>Error is: {1}</table></html>",
                                        source, e.getMessage());
                            }

                        }

                        if (errorMessage != null) {
                            Main.error(errorMessage);
                            int result = JOptionPane.showConfirmDialog(Main.parent, new JLabel(errorMessage), tr("Error"),
                                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.ERROR_MESSAGE);

                            switch (result) {
                            case JOptionPane.YES_OPTION:
                                continue SOURCES;
                            case JOptionPane.NO_OPTION:
                                sourcesToRemove.add(i);
                                continue SOURCES;
                            default:
                                return false;
                            }
                        }
                    }
                sources.removeSources(sourcesToRemove);
                return true;
            }  else
                return true;
        }
    };

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        sortMenu = new JCheckBox(tr("Sort presets menu"),
                Main.pref.getBoolean("taggingpreset.sortmenu", false));

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder( 0, 0, 0, 0 ));
        panel.add(sortMenu, GBC.eol().insets(5,5,5,0));
        sources = new TaggingPresetSourceEditor();
        panel.add(sources, GBC.eol().fill(GBC.BOTH));
        final MapPreference mapPref = gui.getMapPreference();
        mapPref.addSubTab(this, tr("Tagging Presets"), panel);
        sources.deferLoading(mapPref, panel);
        gui.addValidationListener(validationListener);
    }

    static class TaggingPresetSourceEditor extends SourceEditor {

        private static final String iconpref = "taggingpreset.icon.sources";

        public TaggingPresetSourceEditor() {
            super(SourceType.TAGGING_PRESET, Main.JOSM_WEBSITE+"/presets", presetSourceProviders, true);
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return PresetPrefHelper.INSTANCE.get();
        }

        @Override
        public boolean finish() {
            List<SourceEntry> activeStyles = activeSourcesModel.getSources();

            boolean changed = PresetPrefHelper.INSTANCE.put(activeStyles);

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
            return PresetPrefHelper.INSTANCE.getDefault();
        }

        @Override
        public Collection<String> getInitialIconPathsList() {
            return Main.pref.getCollection(iconpref, null);
        }

        @Override
        public String getStr(I18nString ident) {
            switch (ident) {
            case AVAILABLE_SOURCES:
                return tr("Available presets:");
            case ACTIVE_SOURCES:
                return tr("Active presets:");
            case NEW_SOURCE_ENTRY_TOOLTIP:
                return tr("Add a new preset by entering filename or URL");
            case NEW_SOURCE_ENTRY:
                return tr("New preset entry:");
            case REMOVE_SOURCE_TOOLTIP:
                return tr("Remove the selected presets from the list of active presets");
            case EDIT_SOURCE_TOOLTIP:
                return tr("Edit the filename or URL for the selected active preset");
            case ACTIVATE_TOOLTIP:
                return tr("Add the selected available presets to the list of active presets");
            case RELOAD_ALL_AVAILABLE:
                return marktr("Reloads the list of available presets from ''{0}''");
            case LOADING_SOURCES_FROM:
                return marktr("Loading preset sources from ''{0}''");
            case FAILED_TO_LOAD_SOURCES_FROM:
                return marktr("<html>Failed to load the list of preset sources from<br>"
                        + "''{0}''.<br>"
                        + "<br>"
                        + "Details (untranslated):<br>{1}</html>");
            case FAILED_TO_LOAD_SOURCES_FROM_HELP_TOPIC:
                return "/Preferences/Presets#FailedToLoadPresetSources";
            case ILLEGAL_FORMAT_OF_ENTRY:
                return marktr("Warning: illegal format of entry in preset list ''{0}''. Got ''{1}''");
            default: throw new AssertionError();
            }
        }
    }

    @Override
    public boolean ok() {
        boolean restart = Main.pref.put("taggingpreset.sortmenu", sortMenu.getSelectedObjects() != null);
        restart |= sources.finish();

        return restart;
    }

    /**
     * Initializes tagging presets from preferences.
     */
    public static void readFromPreferences() {
        taggingPresets = TaggingPresetReader.readFromPreferences(false);
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
            HashMap<TaggingPresetMenu,JMenu> submenus = new HashMap<TaggingPresetMenu,JMenu>();
            for (final TaggingPreset p : taggingPresets) {
                JMenu m = p.group != null ? submenus.get(p.group) : Main.main.menu.presetsMenu;
                if (p instanceof TaggingPresetSeparator) {
                    m.add(new JSeparator());
                } else if (p instanceof TaggingPresetMenu) {
                    JMenu submenu = new JMenu(p);
                    submenu.setText(p.getLocaleName());
                    ((TaggingPresetMenu)p).menu = submenu;
                    submenus.put((TaggingPresetMenu)p, submenu);
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
     * Helper class for tagging presets preferences.
     */
    public static class PresetPrefHelper extends SourceEditor.SourcePrefHelper {

        /**
         * The unique instance.
         */
        public final static PresetPrefHelper INSTANCE = new PresetPrefHelper();

        /**
         * Constructs a new {@code PresetPrefHelper}.
         */
        public PresetPrefHelper() {
            super("taggingpreset.entries");
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            ExtendedSourceEntry i = new ExtendedSourceEntry("defaultpresets.xml", "resource://data/defaultpresets.xml");
            i.title = tr("Internal Preset");
            i.description = tr("The default preset for JOSM");
            return Collections.singletonList(i);
        }

        @Override
        public Map<String, String> serialize(SourceEntry entry) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("url", entry.url);
            res.put("title", entry.title == null ? "" : entry.title);
            return res;
        }

        @Override
        public SourceEntry deserialize(Map<String, String> s) {
            return new SourceEntry(s.get("url"), null, s.get("title"), true);
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
