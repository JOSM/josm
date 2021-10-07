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
 * <p>
 * Used for controls that offer a list of items to choose from like {@link Combo} and
 * {@link MultiSelect}.
 */
public class PresetListEntry implements Comparable<PresetListEntry> {
    /** Used to display an entry matching several different values. */
    protected static final PresetListEntry ENTRY_DIFFERENT = new PresetListEntry(KeyedItem.DIFFERENT, null);
    /** Used to display an empty entry used to clear values. */
    protected static final PresetListEntry ENTRY_EMPTY = new PresetListEntry("", null);

    /**
     * This is the value that is going to be written to the tag on the selected primitive(s). Except
     * when the value is {@code "<different>"}, which is never written, or the value is empty, which
     * deletes the tag.  {@code value} is never translated.
     */
    public String value; // NOSONAR
    /** The ComboMultiSelect that displays the list */
    public ComboMultiSelect cms; // NOSONAR
    /** Text displayed to the user instead of {@link #value}. */
    public String display_value; // NOSONAR
    /** Text to be displayed below {@link #display_value} in the combobox list. */
    public String short_description; // NOSONAR
    /** The location of icon file to display */
    public String icon; // NOSONAR
    /** The size of displayed icon. If not set, default is size from icon file */
    public short icon_size; // NOSONAR
    /** The localized version of {@link #display_value}. */
    public String locale_display_value; // NOSONAR
    /** The localized version of {@link #short_description}. */
    public String locale_short_description; // NOSONAR

    private String cachedDisplayValue;
    private String cachedShortDescription;
    private ImageIcon cachedIcon;

    /**
     * Constructs a new {@code PresetListEntry}, uninitialized.
     *
     * Public default constructor is needed by {@link org.openstreetmap.josm.tools.XmlObjectParser.Parser#startElement}
     */
    public PresetListEntry() {
    }

    /**
     * Constructs a new {@code PresetListEntry}, initialized with a value and
     * {@link ComboMultiSelect} context.
     *
     * @param value value
     * @param cms the ComboMultiSelect
     */
    public PresetListEntry(String value, ComboMultiSelect cms) {
        this.value = value;
        this.cms = cms;
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
        String displayValue = getDisplayValue();
        Integer count = getCount();

        if (count > 0 && cms.usage.getSelectedCount() > 1) {
            displayValue = tr("{0} ({1})", displayValue, count);
        }

        if (this.equals(ENTRY_DIFFERENT)) {
            return "<html><b>" + Utils.escapeReservedCharactersHTML(displayValue) + "</b></html>";
        }

        String shortDescription = getShortDescription();

        if (shortDescription.isEmpty()) {
            // avoids a collapsed list entry if value == ""
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
        if (icon != null && cachedIcon == null) {
            cachedIcon = TaggingPresetItem.loadImageIcon(icon, TaggingPresetReader.getZipIcons(), (int) icon_size);
        }
        return cachedIcon;
    }

    /**
     * Returns the contents displayed in the current item view.
     * @return the value to display
     */
    public String getDisplayValue() {
        if (cachedDisplayValue == null) {
            if (cms != null && cms.values_no_i18n) {
                cachedDisplayValue = Utils.firstNonNull(value, " ");
            } else {
                cachedDisplayValue = Utils.firstNonNull(
                    locale_display_value, tr(display_value), trc(cms == null ? null : cms.values_context, value), " ");
            }
        }
        return cachedDisplayValue;
    }

    /**
     * Returns the short description to display.
     * @return the short description to display
     */
    public String getShortDescription() {
        if (cachedShortDescription == null) {
            cachedShortDescription = Utils.firstNonNull(locale_short_description, tr(short_description), "");
        }
        return cachedShortDescription;
    }

    /**
     * Returns the tooltip for this entry.
     * @param key the tag key
     * @return the tooltip
     */
    public String getToolTipText(String key) {
        if (this.equals(ENTRY_DIFFERENT)) {
            return tr("Keeps the original values of the selected objects unchanged.");
        }
        if (value != null && !value.isEmpty()) {
            return tr("Sets the key ''{0}'' to the value ''{1}''.", key, value);
        }
        return tr("Clears the key ''{0}''.", key);
    }

    // toString is mainly used to initialize the Editor
    @Override
    public String toString() {
        if (this.equals(ENTRY_DIFFERENT))
            return getDisplayValue();
        return getDisplayValue().replaceAll("\\s*<.*>\\s*", " "); // remove additional markup, e.g. <br>
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

    /**
     * Returns how many selected primitives had this value set.
     * @return see above
     */
    public int getCount() {
        Integer count = cms == null ? null : cms.usage.map.get(value);
        return count == null ? 0 : count;
    }

    @Override
    public int compareTo(PresetListEntry o) {
        return AlphanumComparator.getInstance().compare(this.value, o.value);
    }
}
