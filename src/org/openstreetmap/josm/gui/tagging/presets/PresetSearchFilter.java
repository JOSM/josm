// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets;

import static org.openstreetmap.josm.tools.I18n.marktr;

/**
 * This enum defines different filters for searching presets.
 */
public enum PresetSearchFilter {
    ONLY_APPLICABLE(marktr("Show only applicable to selection")),
    SEARCH_IN_TAGS(marktr("Search in tags")),
    DEPRECATED_TAGS(marktr("Show deprecated tags"));

    /**
     * The translated text associated with the enum constant.
     */
    private final String textToBeTranslated;

    /**
     * Constructor for the PresetSearchFilter enum.
     * Initializes an enum constant with its corresponding translated text.
     *
     * @param textToBeTranslated The translated text associated with the enum constant.
     */
    PresetSearchFilter(String textToBeTranslated) {
        this.textToBeTranslated = textToBeTranslated;
    }

    /**
     * Returns the text associated with the filter
     * @return the text marked for translation
     */
    public String getText() {
        return textToBeTranslated;
    }

}
