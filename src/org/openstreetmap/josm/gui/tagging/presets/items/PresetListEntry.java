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
     * Returns the contents displayed in the dropdown list.
     *
     * This is the contents that would be displayed in the current view plus a short description to
     * aid the user.  The whole contents is wrapped to {@code width}.
     *
     * @param width the width in px
     * @return HTML formatted contents
     */
    public String getListDisplay(int width) {
        if (value.equals(KeyedItem.DIFFERENT)) {
            return "<b>" + KeyedItem.DIFFERENT + "</b>";
        }

        String shortDescription = getShortDescription(true);
        String displayValue = getDisplayValue();

        if (shortDescription.isEmpty()) {
            if (displayValue.isEmpty()) {
                return " ";
            }
            return displayValue;
        }

        // RTL not supported in HTML. See: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4866977
        return String.format("<html><div style=\"width: %d\"><b>%s</b><p style=\"padding-left: 10\">%s</p></div></html>",
                width,
                displayValue,
                Utils.escapeReservedCharactersHTML(shortDescription));
    }

    /**
     * Returns the entry icon, if any.
     * @return the entry icon, or {@code null}
     */
    public ImageIcon getIcon() {
        return icon == null ? null : TaggingPresetItem.loadImageIcon(icon, TaggingPresetReader.getZipIcons(), (int) icon_size);
    }

    /**
     * Returns the contents of the current item view.
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
        String shortDesc = translated
                ? Utils.firstNonNull(locale_short_description, tr(short_description))
                        : short_description;
        return shortDesc == null ? "" : shortDesc;
    }

    // toString is mainly used to initialize the Editor
    @Override
    public String toString() {
        if (KeyedItem.DIFFERENT.equals(value))
            return KeyedItem.DIFFERENT;
        String displayValue = getDisplayValue();
        return displayValue != null ? displayValue.replaceAll("\\s*<.*>\\s*", " ") : ""; // remove additional markup, e.g. <br>
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
