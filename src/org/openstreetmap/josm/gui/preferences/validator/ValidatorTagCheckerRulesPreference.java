// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.preferences.validator;

import static org.openstreetmap.josm.tools.I18n.marktr;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.preferences.sources.ExtendedSourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceEntry;
import org.openstreetmap.josm.data.preferences.sources.SourceProvider;
import org.openstreetmap.josm.data.preferences.sources.SourceType;
import org.openstreetmap.josm.data.preferences.sources.ValidatorPrefHelper;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker;
import org.openstreetmap.josm.gui.preferences.PreferenceSetting;
import org.openstreetmap.josm.gui.preferences.PreferenceSettingFactory;
import org.openstreetmap.josm.gui.preferences.PreferenceTabbedPane;
import org.openstreetmap.josm.gui.preferences.SourceEditor;
import org.openstreetmap.josm.gui.preferences.SubPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.TabPreferenceSetting;
import org.openstreetmap.josm.gui.preferences.map.MapPaintPreference;

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

    private static final List<SourceProvider> ruleSourceProviders = new ArrayList<>();

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

        TagCheckerRulesSourceEditor() {
            super(SourceType.TAGCHECKER_RULE, Main.getJOSMWebsite()+"/rules", ruleSourceProviders, false);
        }

        @Override
        public Collection<? extends SourceEntry> getInitialSourcesList() {
            return ValidatorPrefHelper.INSTANCE.get();
        }

        @Override
        public boolean finish() {
            return ValidatorPrefHelper.INSTANCE.put(activeSourcesModel.getSources());
        }

        @Override
        public Collection<ExtendedSourceEntry> getDefault() {
            return ValidatorPrefHelper.INSTANCE.getDefault();
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

        @Override
        protected String getTitleForSourceEntry(SourceEntry entry) {
            final String title = MapPaintPreference.getTitleFromSourceEntry(entry);
            return title != null ? title : super.getTitleForSourceEntry(entry);
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
