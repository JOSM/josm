// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.List;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A tagging preset item displaying a localizable text.
 * @since 6190
 */
public abstract class TextItem extends TaggingPresetItem {

    /** The text to display */
    public String text; // NOSONAR

    /** The context used for translating {@link #text} */
    public String text_context; // NOSONAR

    /** The localized version of {@link #text} */
    public String locale_text; // NOSONAR

    /** The location of icon file to display */
    public String icon; // NOSONAR
    /** The size of displayed icon. If not set, default is 16px */
    public short icon_size = 16; // NOSONAR


    protected final void initializeLocaleText(String defaultText) {
        if (locale_text == null) {
            locale_text = getLocaleText(text, text_context, defaultText);
        }
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        // Do nothing
    }

    protected String fieldsToString() {
        return (text != null ? "text=" + text + ", " : "")
                + (text_context != null ? "text_context=" + text_context + ", " : "")
                + (locale_text != null ? "locale_text=" + locale_text : "");
    }

    /**
     * Defines the label icon from this entry's icon
     * @param label the component
     * @since 17605
     */
    protected void addIcon(JLabel label) {
        label.setIcon(getIcon());
        label.setHorizontalAlignment(SwingConstants.LEADING);
    }

    /**
     * Returns the entry icon, if any.
     * @return the entry icon, or {@code null}
     * @since 17605
     */
    public ImageIcon getIcon() {
        return icon == null ? null : loadImageIcon(icon, TaggingPresetReader.getZipIcons(), (int) icon_size);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + fieldsToString() + ']';
    }
}
