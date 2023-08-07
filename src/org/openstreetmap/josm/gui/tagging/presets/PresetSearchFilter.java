// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import java.util.EnumMap;
import java.util.Map;

import static org.openstreetmap.josm.tools.I18n.marktr;

/**
 * This enum defines different filters for searching presets.
 */
public enum PresetSearchFilter {
    ONLY_APPLICABLE(marktr("Show only applicable to selection")),
    SEARCH_IN_TAGS(marktr("Search in tags")),
    DEPRECATED_TAGS(marktr("Show deprecated tags"));

    /**
     * Map containing the preferences for the filters
     */
    private Map<PresetSearchFilter, Boolean> filtersPreference;

    static {
        for (PresetSearchFilter filter : values()) {
            filter.filtersPreference = new EnumMap<>(PresetSearchFilter.class);
            filter.filtersPreference.put(filter, true);
        }
    }

    /**
     * Sets the preference for the filter
     * @param filter the filter to set the preference for
     * @param pref true if the filter is enabled, false otherwise
     * @since xxx
     */
    public void setPref(PresetSearchFilter filter, Boolean pref) {
        filtersPreference.put(filter, pref);
    }

    /**
     * Gets the preference for the filter
     * @param filter the filter to get the preference for
     * @return true if the filter is enabled, false otherwise
     * @since xxx
     */
    public Boolean getPref(PresetSearchFilter filter) {
        return filtersPreference.get(filter);
    }

    /**
     * The translated text associated with the enum constant.
     */
    private final String translatedText;

    /**
     * Constructor for the PresetSearchFilter enum.
     * Initializes an enum constant with its corresponding translated text.
     *
     * @param translatedText The translated text associated with the enum constant.
     */
    PresetSearchFilter(String translatedText) {
        this.translatedText = translatedText;
    }

    /**
     * Returns the text associated with the filter
     * @return the text marked for translation
     */
    public String getText() {
        return translatedText;
    }

}
