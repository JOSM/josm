// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
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

/**
 * The general validator preferences, allowing to enable/disable tests.
 * @since 6669
 */
public class ValidatorTagCheckerRulesPreference implements SubPreferenceSetting {

    /**
     * Factory used to create a new {@code ValidatorTagCheckerRulesPreference}.
     */
    public static class Factory implements PreferenceSettingFactory {
        @Override
        public PreferenceSetting createPreferenceSetting() {
            return new ValidatorTagCheckerRulesPreference();
        }
    }

    private static final List<SourceProvider> ruleSourceProviders = new ArrayList<SourceProvider>();

    /**
     * Registers a new additional rule source provider.
     * @param provider The rule source provider
     * @return {@code true}, if the provider has been added, {@code false} otherwise
     */
    public static final boolean registerSourceProvider(SourceProvider provider) {
        if (provider != null)
            return ruleSourceProviders.add(provider);
        return false;
    }
    
    static class TagCheckerRulesSourceEditor extends SourceEditor {

        public TagCheckerRulesSourceEditor() {
            super(SourceType.TAGCHECKER_RULE, Main.JOSM_WEBSITE+"/rules", ruleSourceProviders, false);
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return RulePrefHelper.INSTANCE.get();
        }

        @Override
        public boolean finish() {
            return RulePrefHelper.INSTANCE.put(activeSourcesModel.getSources());
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            return RulePrefHelper.INSTANCE.getDefault();
        }

        @Override
        public Collection<String> getInitialIconPathsList() {
            return null;
        }

        @Override
        public String getStr(I18nString ident) {
            switch (ident) {
            case AVAILABLE_SOURCES:
                return tr("Available rules:");
            case ACTIVE_SOURCES:
                return tr("Active rules:");
            case NEW_SOURCE_ENTRY_TOOLTIP:
                return tr("Add a new rule by entering filename or URL");
            case NEW_SOURCE_ENTRY:
                return tr("New rule entry:");
            case REMOVE_SOURCE_TOOLTIP:
                return tr("Remove the selected rules from the list of active rules");
            case EDIT_SOURCE_TOOLTIP:
                return tr("Edit the filename or URL for the selected active rule");
            case ACTIVATE_TOOLTIP:
                return tr("Add the selected available rules to the list of active rules");
            case RELOAD_ALL_AVAILABLE:
                return marktr("Reloads the list of available rules from ''{0}''");
            case LOADING_SOURCES_FROM:
                return marktr("Loading rule sources from ''{0}''");
            case FAILED_TO_LOAD_SOURCES_FROM:
                return marktr("<html>Failed to load the list of rule sources from<br>"
                        + "''{0}''.<br>"
                        + "<br>"
                        + "Details (untranslated):<br>{1}</html>");
            case FAILED_TO_LOAD_SOURCES_FROM_HELP_TOPIC:
                return "/Preferences/Rules#FailedToLoadRuleSources";
            case ILLEGAL_FORMAT_OF_ENTRY:
                return marktr("Warning: illegal format of entry in rule list ''{0}''. Got ''{1}''");
            default: throw new AssertionError();
            }
        }
    }
    
    /**
     * Helper class for validator tag checker rules preferences.
     */
    public static class RulePrefHelper extends SourceEditor.SourcePrefHelper {

        /**
         * The unique instance.
         */
        public static final RulePrefHelper INSTANCE = new RulePrefHelper();

        /**
         * Constructs a new {@code PresetPrefHelper}.
         */
        public RulePrefHelper() {
            super(MapCSSTagChecker.ENTRIES_PREF_KEY);
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            List<ExtendedSourceEntry> def = new ArrayList<ExtendedSourceEntry>();
            
            addDefault(def, "combinations", tr("Tag combinations"),    tr("Checks for missing tag or suspicious combinations"));
            addDefault(def, "deprecated",   tr("Deprecated features"), tr("Checks for deprecated features"));
            addDefault(def, "geometry",     tr("Geometry"),            tr("Checks for geometry errors"));
            addDefault(def, "highway",      tr("Highways"),            tr("Checks for errors on highways"));
            addDefault(def, "numeric",      tr("Numeric values"),      tr("Checks for wrong numeric values"));
            addDefault(def, "power",        tr("Power"),               tr("Checks for errors on power infrastructures"));
            addDefault(def, "religion",     tr("Religion"),            tr("Checks for errors on religious objects"));
            addDefault(def, "relation",     tr("Relations"),           tr("Checks for errors on relations"));
            addDefault(def, "unnecessary",  tr("Unnecessary tags"),    tr("Checks for unnecessary tags"));
            addDefault(def, "wikipedia",    tr("Wikipedia"),           tr("Checks for wrong wikipedia tags"));
            
            return def;
        }
        
        private void addDefault(List<ExtendedSourceEntry> defaults, String filename, String title, String description) {
            ExtendedSourceEntry i = new ExtendedSourceEntry(filename+".mapcss", "resource://data/validator/"+filename+".mapcss");
            i.title = title;
            i.description = description;
            defaults.add(i);
        }

        @Override
        public Map<String, String> serialize(SourceEntry entry) {
            Map<String, String> res = new HashMap<String, String>();
            res.put("url", entry.url);
            res.put("title", entry.title == null ? "" : entry.title);
            res.put("active", Boolean.toString(entry.active));
            return res;
        }

        @Override
        public SourceEntry deserialize(Map<String, String> s) {
            return new SourceEntry(s.get("url"), null, s.get("title"), Boolean.parseBoolean(s.get("active")));
        }
    }

    private SourceEditor sources;

    @Override
    public void addGui(PreferenceTabbedPane gui) {
        final ValidatorPreference valPref = gui.getValidatorPreference();
        sources = new TagCheckerRulesSourceEditor();
        
        valPref.addSubTab(this, tr("Tag checker rules"),
                sources, tr("Choose Tag checker rules to enable"));
        sources.deferLoading(valPref, sources);
    }

    @Override
    public boolean ok() {
        if (sources.finish()) {
            // Reload sources
            MapCSSTagChecker tagChecker = OsmValidator.getTest(MapCSSTagChecker.class);
            if (tagChecker != null) {
                OsmValidator.initializeTests(Collections.singleton(tagChecker));
            }
        }

        return false;
    }

    @Override
    public boolean isExpert() {
        return false;
    }

    @Override
    public TabPreferenceSetting getTabPreferenceSetting(PreferenceTabbedPane gui) {
        return gui.getValidatorPreference();
    }
}
