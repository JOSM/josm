// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract superclass for combo box and multi-select list types.
 */
public abstract class ComboMultiSelect extends KeyedItem {

    private static final Renderer RENDERER = new Renderer();

    /**
     * A list of entries.
     * The list has to be separated by commas (for the {@link Combo} box) or by the specified delimiter (for the {@link MultiSelect}).
     * If a value contains the delimiter, the delimiter may be escaped with a backslash.
     * If a value contains a backslash, it must also be escaped with a backslash. */
    public String values; // NOSONAR
    /**
     * To use instead of {@link #values} if the list of values has to be obtained with a Java method of this form:
     * <p>{@code public static String[] getValues();}<p>
     * The value must be: {@code full.package.name.ClassName#methodName}.
     */
    public String values_from; // NOSONAR
    /** The context used for translating {@link #values} */
    public String values_context; // NOSONAR
    /** Disabled internationalisation for value to avoid mistakes, see #11696 */
    public boolean values_no_i18n; // NOSONAR
    /** Whether to sort the values, defaults to true. */
    public boolean values_sort = true; // NOSONAR
    /**
     * A list of entries that is displayed to the user.
     * Must be the same number and order of entries as {@link #values} and editable must be false or not specified.
     * For the delimiter character and escaping, see the remarks at {@link #values}.
     */
    public String display_values; // NOSONAR
    /** The localized version of {@link #display_values}. */
    public String locale_display_values; // NOSONAR
    /**
     * A delimiter-separated list of texts to be displayed below each {@code display_value}.
     * (Only if it is not possible to describe the entry in 2-3 words.)
     * Instead of comma separated list instead using {@link #values}, {@link #display_values} and {@link #short_descriptions},
     * the following form is also supported:<p>
     * {@code <list_entry value="" display_value="" short_description="" icon="" icon_size="" />}
     */
    public String short_descriptions; // NOSONAR
    /** The localized version of {@link #short_descriptions}. */
    public String locale_short_descriptions; // NOSONAR
    /** The default value for the item. If not specified, the current value of the key is chosen as default (if applicable).*/
    public String default_; // NOSONAR
    /**
     * The character that separates values.
     * In case of {@link Combo} the default is comma.
     * In case of {@link MultiSelect} the default is semicolon and this will also be used to separate selected values in the tag.
     */
    public char delimiter = ';'; // NOSONAR
    /** whether the last value is used as default.
     * Using "force" (2) enforces this behaviour also for already tagged objects. Default is "false" (0).*/
    public byte use_last_as_default; // NOSONAR
    /** whether to use values for search via {@link TaggingPresetSelector} */
    public boolean values_searchable; // NOSONAR

    protected JComponent component;
    protected final Set<PresetListEntry> presetListEntries = new CopyOnWriteArraySet<>();
    private boolean initialized;
    protected Usage usage;
    protected Object originalValue;

    private static final class Renderer implements ListCellRenderer<PresetListEntry> {

        private final JLabel lbl = new JLabel();

        @Override
        public Component getListCellRendererComponent(JList<? extends PresetListEntry> list, PresetListEntry item, int index,
                boolean isSelected, boolean cellHasFocus) {

            if (list == null || item == null) {
                return lbl;
            }

            if (index == -1) {
                // Take the longest element for the preferred width (#19321)
                // We do not want the editor to have the maximum height of all entries. Return a dummy with bogus height.
                IntStream.range(0, list.getModel().getSize())
                        .mapToObj(i -> getListCellRendererComponent(list, list.getModel().getElementAt(i), i, isSelected, cellHasFocus))
                        .map(Component::getPreferredSize)
                        .max(Comparator.comparingInt(dim -> dim.width))
                        .ifPresent(dim -> lbl.setPreferredSize(new Dimension(dim.width, 10)));
                return lbl;
            }

            // Only return cached size, item is not shown
            if (!list.isShowing() && item.preferredWidth != -1 && item.preferredHeight != -1) {
                lbl.setPreferredSize(new Dimension(item.preferredWidth, item.preferredHeight));
                return lbl;
            }

            lbl.setPreferredSize(null);

            if (isSelected) {
                lbl.setBackground(list.getSelectionBackground());
                lbl.setForeground(list.getSelectionForeground());
            } else {
                lbl.setBackground(list.getBackground());
                lbl.setForeground(list.getForeground());
            }

            lbl.setOpaque(true);
            lbl.setFont(lbl.getFont().deriveFont(Font.PLAIN));
            lbl.setText("<html>" + item.getListDisplay() + "</html>");
            lbl.setIcon(item.getIcon());
            lbl.setEnabled(list.isEnabled());

            // Cache size
            item.preferredWidth = (short) lbl.getPreferredSize().width;
            item.preferredHeight = (short) lbl.getPreferredSize().height;

            return lbl;
        }
    }

    /**
     * Preset list entry.
     */
    public static class PresetListEntry implements Comparable<PresetListEntry> {
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
            if (value.equals(DIFFERENT))
                return "<b>" + Utils.escapeReservedCharactersHTML(DIFFERENT) + "</b>";

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
            return icon == null ? null : loadImageIcon(icon, TaggingPresetReader.getZipIcons(), (int) icon_size);
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
            if (DIFFERENT.equals(value))
                return DIFFERENT;
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

    /**
     * allow escaped comma in comma separated list:
     * "A\, B\, C,one\, two" --&gt; ["A, B, C", "one, two"]
     * @param delimiter the delimiter, e.g. a comma. separates the entries and
     *      must be escaped within one entry
     * @param s the string
     * @return splitted items
     */
    public static String[] splitEscaped(char delimiter, String s) {
        if (s == null)
            return new String[0];
        List<String> result = new ArrayList<>();
        boolean backslash = false;
        StringBuilder item = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (backslash) {
                item.append(ch);
                backslash = false;
            } else if (ch == '\\') {
                backslash = true;
            } else if (ch == delimiter) {
                result.add(item.toString());
                item.setLength(0);
            } else {
                item.append(ch);
            }
        }
        if (item.length() > 0) {
            result.add(item.toString());
        }
        return result.toArray(new String[0]);
    }

    protected abstract Object getSelectedItem();

    protected abstract void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches);

    @Override
    public Collection<String> getValues() {
        initListEntries(false);
        return presetListEntries.stream().map(x -> x.value).collect(Collectors.toSet());
    }

    /**
     * Returns the values to display.
     * @return the values to display
     */
    public Collection<String> getDisplayValues() {
        initListEntries(false);
        return presetListEntries.stream().map(PresetListEntry::getDisplayValue).collect(Collectors.toList());
    }

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
        initListEntries(true);

        // find out if our key is already used in the selection.
        usage = determineTextUsage(sel, key);
        if (!usage.hasUniqueValue() && !usage.unused()) {
            presetListEntries.add(new PresetListEntry(DIFFERENT));
        }

        final JLabel label = new JLabel(tr("{0}:", locale_text));
        label.setToolTipText(getKeyTooltipText());
        label.setComponentPopupMenu(getPopupMenu());
        p.add(label, GBC.std().insets(0, 0, 10, 0));
        addToPanelAnchor(p, default_, presetInitiallyMatches);
        label.setLabelFor(component);
        component.setToolTipText(getKeyTooltipText());

        return true;
    }

    private void initListEntries(boolean cleanup) {
        if (initialized) {
            if (cleanup) { // do not cleanup for #getDisplayValues used in Combo#addToPanelAnchor
                presetListEntries.remove(new PresetListEntry(DIFFERENT)); // possibly added in #addToPanel
            }
            return;
        } else if (presetListEntries.isEmpty()) {
            initListEntriesFromAttributes();
        } else {
            if (values != null) {
                Logging.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "values", "list_entry"));
            }
            if (display_values != null || locale_display_values != null) {
                Logging.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "display_values", "list_entry"));
            }
            if (short_descriptions != null || locale_short_descriptions != null) {
                Logging.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "short_descriptions", "list_entry"));
            }
            for (PresetListEntry e : presetListEntries) {
                if (e.value_context == null) {
                    e.value_context = values_context;
                }
            }
        }
        initializeLocaleText(null);
        initialized = true;
    }

    private void initListEntriesFromAttributes() {

        String[] valueArray = null;

        if (values_from != null) {
            String[] classMethod = values_from.split("#", -1);
            if (classMethod.length == 2) {
                try {
                    Method method = Class.forName(classMethod[0]).getMethod(classMethod[1]);
                    // Check method is public static String[] methodName()
                    int mod = method.getModifiers();
                    if (Modifier.isPublic(mod) && Modifier.isStatic(mod)
                            && method.getReturnType().equals(String[].class) && method.getParameterTypes().length == 0) {
                        valueArray = (String[]) method.invoke(null);
                    } else {
                        Logging.error(tr("Broken tagging preset \"{0}-{1}\" - Java method given in ''values_from'' is not \"{2}\"", key, text,
                                "public static String[] methodName()"));
                    }
                } catch (ReflectiveOperationException e) {
                    Logging.error(tr("Broken tagging preset \"{0}-{1}\" - Java method given in ''values_from'' threw {2} ({3})", key, text,
                            e.getClass().getName(), e.getMessage()));
                    Logging.debug(e);
                }
            }
        }

        if (valueArray == null) {
            valueArray = splitEscaped(delimiter, values);
            values = null;
        }

        String[] displayArray = valueArray;
        if (!values_no_i18n) {
            final String displ = Utils.firstNonNull(locale_display_values, display_values);
            displayArray = displ == null ? valueArray : splitEscaped(delimiter, displ);
        }

        final String descr = Utils.firstNonNull(locale_short_descriptions, short_descriptions);
        String[] shortDescriptionsArray = descr == null ? null : splitEscaped(delimiter, descr);

        if (displayArray.length != valueArray.length) {
            Logging.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''",
                            key, text));
            Logging.error(tr("Detailed information: {0} <> {1}", Arrays.toString(displayArray), Arrays.toString(valueArray)));
            displayArray = valueArray;
        }

        if (shortDescriptionsArray != null && shortDescriptionsArray.length != valueArray.length) {
            Logging.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''",
                            key, text));
            Logging.error(tr("Detailed information: {0} <> {1}", Arrays.toString(shortDescriptionsArray), Arrays.toString(valueArray)));
            shortDescriptionsArray = null;
        }

        final List<PresetListEntry> entries = new ArrayList<>(valueArray.length);
        for (int i = 0; i < valueArray.length; i++) {
            final PresetListEntry e = new PresetListEntry(valueArray[i]);
            final String value = locale_display_values != null || values_no_i18n
                    ? displayArray[i]
                    : trc(values_context, fixPresetString(displayArray[i]));
            e.locale_display_value = value == null ? null : value.intern();
            if (shortDescriptionsArray != null) {
                final String description = locale_short_descriptions != null
                        ? shortDescriptionsArray[i]
                        : tr(fixPresetString(shortDescriptionsArray[i]));
                e.locale_short_description = description == null ? null : description.intern();
            }

            entries.add(e);
        }

        if (values_sort && Config.getPref().getBoolean("taggingpreset.sortvalues", true)) {
            Collections.sort(entries);
        }

        addListEntries(entries);
    }

    protected String getDisplayIfNull() {
        return null;
    }

    protected Object getItemToSelect(String def, boolean presetInitiallyMatches) {
        final Object itemToSelect;
        if (usage.hasUniqueValue()) {
            // all items have the same value (and there were no unset items)
            originalValue = getListEntry(usage.getFirst());
            itemToSelect = originalValue;
        } else if (def != null && usage.unused()) {
            // default is set and all items were unset
            if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || isForceUseLastAsDefault()) {
                // selected osm primitives are untagged or filling default feature is enabled
                itemToSelect = getListEntry(def).getDisplayValue();
            } else {
                // selected osm primitives are tagged and filling default feature is disabled
                itemToSelect = "";
            }
            originalValue = getListEntry(DIFFERENT);
        } else if (usage.unused()) {
            // all items were unset (and so is default)
            originalValue = getListEntry("");
            if (!presetInitiallyMatches && isUseLastAsDefault() && LAST_VALUES.containsKey(key)) {
                itemToSelect = getListEntry(LAST_VALUES.get(key));
            } else {
                itemToSelect = originalValue;
            }
        } else {
            originalValue = getListEntry(DIFFERENT);
            itemToSelect = originalValue;
        }
        return itemToSelect;
    }

    protected String getSelectedValue() {
        Object obj = getSelectedItem();
        String display = obj == null ? getDisplayIfNull() : obj.toString();

        if (display == null) {
            return "";
        }
        return presetListEntries.stream()
                .filter(entry -> Objects.equals(entry.toString(), display))
                .findFirst()
                .map(entry -> entry.value)
                .map(Utils::removeWhiteSpaces)
                .orElse(display);
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        String value = getSelectedValue();

        // no change if same as before
        if (originalValue == null) {
            if (value.isEmpty())
                return;
        } else if (value.equals(originalValue.toString()))
            return;

        if (isUseLastAsDefault()) {
            LAST_VALUES.put(key, value);
        }
        changedTags.add(new Tag(key, value));
    }

    /**
     * Sets whether the last value is used as default.
     * @param v Using "force" (2) enforces this behaviour also for already tagged objects. Default is "false" (0).
     */
    public void setUse_last_as_default(String v) { // NOPMD
        if ("force".equals(v)) {
            use_last_as_default = 2;
        } else if ("true".equals(v)) {
            use_last_as_default = 1;
        } else {
            use_last_as_default = 0;
        }
    }

    protected boolean isUseLastAsDefault() {
        return use_last_as_default > 0;
    }

    protected boolean isForceUseLastAsDefault() {
        return use_last_as_default == 2;
    }

    /**
     * Adds a preset list entry.
     * @param e list entry to add
     */
    public void addListEntry(PresetListEntry e) {
        presetListEntries.add(e);
    }

    /**
     * Adds a collection of preset list entries.
     * @param e list entries to add
     */
    public void addListEntries(Collection<PresetListEntry> e) {
        for (PresetListEntry i : e) {
            addListEntry(i);
        }
    }

    protected PresetListEntry getListEntry(String value) {
        return presetListEntries.stream().filter(e -> Objects.equals(e.value, value)).findFirst().orElse(null);
    }

    protected ListCellRenderer<PresetListEntry> getListCellRenderer() {
        return RENDERER;
    }

    @Override
    public MatchType getDefaultMatch() {
        return MatchType.NONE;
    }
}
