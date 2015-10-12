// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.List;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;

/**
 * A tagging preset item displaying a localizable text.
 * @since 6190
 */
public abstract class TextItem extends TaggingPresetItem {

    /** The text to display */
    public String text;

    /** The context used for translating {@link #text} */
    public String text_context;

    /** The localized version of {@link #text} */
    public String locale_text;

    protected final void initializeLocaleText(String defaultText) {
        if (locale_text == null) {
            locale_text = getLocaleText(text, text_context, defaultText);
        }
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
    }

    protected String fieldsToString() {
        return (text != null ? "text=" + text + ", " : "")
                + (text_context != null ? "text_context=" + text_context + ", " : "")
                + (locale_text != null ? "locale_text=" + locale_text : "");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + fieldsToString() + ']';
    }
}
