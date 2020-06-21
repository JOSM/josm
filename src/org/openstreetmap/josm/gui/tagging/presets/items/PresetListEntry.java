// License: GPL. For details, see LICENSE file.

package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.util.Objects;

import javax.swing.ImageIcon;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItem;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.Utils;

/**
 * Preset list entry.
 */
public class PresetListEntry implements Comparable<PresetListEntry> {
    /** Entry value */
    public String value; // NOSONAR
    /** The context used for translating {@link #value} */
    public String value_context; // NOSONAR
    /** Value displayed to the user */
    public String display_value; // NOSONAR
    /** Text to be displayed below {@code display_value}. */
    public String short_description; // NOSONAR
    /** The location of icon file to display */
    public String icon; // NOSONAR
    /** The size of displayed icon. If not set, default is size from icon file */
    public short icon_size; // NOSONAR
    /** The localized version of {@link #display_value}. */
    public String locale_display_value; // NOSONAR
    /** The localized version of {@link #short_description}. */
    public String locale_short_description; // NOSONAR

    /** Cached width (currently only for Combo) to speed up preset dialog initialization */
    public short preferredWidth = -1; // NOSONAR
    /** Cached height (currently only for Combo) to speed up preset dialog initialization */
    public short preferredHeight = -1; // NOSONAR

    /**
     * Constructs a new {@code PresetListEntry}, uninitialized.
     */
    public PresetListEntry() {
        // Public default constructor is needed
    }

    /**
     * Constructs a new {@code PresetListEntry}, initialized with a value.
     * @param value value
     */
    public PresetListEntry(String value) {
        this.value = value;
    }

    /**
     * Returns HTML formatted contents.
     * @return HTML formatted contents
     */
    public String getListDisplay() {
        if (value.equals(KeyedItem.DIFFERENT))
            return "<b>" + Utils.escapeReservedCharactersHTML(KeyedItem.DIFFERENT) + "</b>";

        String displayValue = Utils.escapeReservedCharactersHTML(getDisplayValue());
        String shortDescription = getShortDescription(true);

        if (displayValue.isEmpty() && (shortDescription == null || shortDescription.isEmpty()))
            return "&nbsp;";

        final StringBuilder res = new StringBuilder("<b>").append(displayValue).append("</b>");
        if (shortDescription != null) {
            // wrap in table to restrict the text width
            res.append("<div style=\"width:300px; padding:0 0 5px 5px\">")
               .append(shortDescription)
               .append("</div>");
        }
        return res.toString();
    }

    /**
     * Returns the entry icon, if any.
     * @return the entry icon, or {@code null}
     */
    public ImageIcon getIcon() {
        return icon == null ? null : TaggingPresetItem.loadImageIcon(icon, TaggingPresetReader.getZipIcons(), (int) icon_size);
    }

    /**
     * Returns the value to display.
     * @return the value to display
     */
    public String getDisplayValue() {
        return Utils.firstNonNull(locale_display_value, tr(display_value), trc(value_context, value));
    }

    /**
     * Returns the short description to display.
     * @param translated whether the text must be translated
     * @return the short description to display
     */
    public String getShortDescription(boolean translated) {
        return translated
                ? Utils.firstNonNull(locale_short_description, tr(short_description))
                        : short_description;
    }

    // toString is mainly used to initialize the Editor
    @Override
    public String toString() {
        if (KeyedItem.DIFFERENT.equals(value))
            return KeyedItem.DIFFERENT;
        String displayValue = getDisplayValue();
        return displayValue != null ? displayValue.replaceAll("<.*>", "") : ""; // remove additional markup, e.g. <br>
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PresetListEntry that = (PresetListEntry) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public int compareTo(PresetListEntry o) {
        return AlphanumComparator.getInstance().compare(this.getDisplayValue(), o.getDisplayValue());
    }
}
