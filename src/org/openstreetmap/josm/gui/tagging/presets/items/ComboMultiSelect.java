// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Font;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresets;
import org.openstreetmap.josm.gui.widgets.JosmListCellRenderer;
import org.openstreetmap.josm.gui.widgets.OrientationAction;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Logging;

/**
 * Abstract superclass for combo box and multi-select list types.
 */
public abstract class ComboMultiSelect extends KeyedItem {

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

    /**
     * The standard entries in the combobox dropdown or multiselect list. These entries are defined
     * in {@code defaultpresets.xml} (or in other custom preset files).
     */
    protected final List<PresetListEntry> presetListEntries = new ArrayList<>();
    /** Helps avoid duplicate list entries */
    protected final Map<String, PresetListEntry> seenValues = new TreeMap<>();
    protected Usage usage;
    /** Used to see if the user edited the value. May be null. */
    protected String originalValue;

    /**
     * A list cell renderer that paints a short text in the current value pane and and a longer text
     * in the dropdown list.
     */
    static class ComboMultiSelectListCellRenderer extends JosmListCellRenderer<PresetListEntry> {
        int width;
        private String key;

        ComboMultiSelectListCellRenderer(Component component, ListCellRenderer<? super PresetListEntry> renderer, int width, String key) {
            super(component, renderer);
            this.key = key;
            setWidth(width);
        }

        /**
         * Sets the width to format the dropdown list to
         *
         * Note: This is not the width of the list, but the width to which we format any multi-line
         * label in the list.  We cannot use the list's width because at the time the combobox
         * measures its items, it is not guaranteed that the list is already sized, the combobox may
         * not even be layed out yet.  Set this to {@code combobox.getWidth()}
         *
         * @param width the width
         */
        public void setWidth(int width) {
            if (width <= 0)
                width = 200;
            this.width = width - 20;
        }

        @Override
        public JLabel getListCellRendererComponent(
            JList<? extends PresetListEntry> list, PresetListEntry value, int index, boolean isSelected, boolean cellHasFocus) {

            JLabel l = (JLabel) renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            l.setComponentOrientation(component.getComponentOrientation());
            if (index != -1) {
                // index -1 is set when measuring the size of the cell and when painting the
                // editor-ersatz of a readonly combobox. fixes #6157
                l.setText(value.getListDisplay(width));
            }
            if (value.getCount() > 0) {
                l.setFont(l.getFont().deriveFont(Font.ITALIC + Font.BOLD));
            }
            l.setIcon(value.getIcon());
            l.setToolTipText(value.getToolTipText(key));
            return l;
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
    public static List<String> splitEscaped(char delimiter, String s) {
        if (s == null)
            return null; // NOSONAR

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
        return result;
    }

    /**
     * Returns the value selected in the combobox or a synthetic value if a multiselect.
     *
     * @return the value
     */
    protected abstract PresetListEntry getSelectedItem();

    @Override
    public Collection<String> getValues() {
        initListEntries();
        return presetListEntries.stream().map(x -> x.value).collect(Collectors.toSet());
    }

    /**
     * Returns the values to display.
     * @return the values to display
     */
    public Collection<String> getDisplayValues() {
        initListEntries();
        return presetListEntries.stream().map(PresetListEntry::getDisplayValue).collect(Collectors.toList());
    }

    /**
     * Adds the label to the panel
     *
     * @param p the panel
     * @return the label
     */
    protected JLabel addLabel(JPanel p) {
        final JLabel label = new JLabel(tr("{0}:", locale_text));
        addIcon(label);
        label.setToolTipText(getKeyTooltipText());
        label.setComponentPopupMenu(getPopupMenu());
        label.applyComponentOrientation(OrientationAction.getDefaultComponentOrientation());
        p.add(label, GBC.std().insets(0, 0, 10, 0));
        return label;
    }

    protected void initListEntries() {
        if (presetListEntries.isEmpty()) {
            initListEntriesFromAttributes();
        }
    }

    private List<String> getValuesFromCode(String valuesFrom) {
        // get the values from a Java function
        String[] classMethod = valuesFrom.split("#", -1);
        if (classMethod.length == 2) {
            try {
                Method method = Class.forName(classMethod[0]).getMethod(classMethod[1]);
                // Check method is public static String[] methodName()
                int mod = method.getModifiers();
                if (Modifier.isPublic(mod) && Modifier.isStatic(mod)
                        && method.getReturnType().equals(String[].class) && method.getParameterTypes().length == 0) {
                    return Arrays.asList((String[]) method.invoke(null));
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
        return null; // NOSONAR
    }

    /**
     * Checks if list {@code a} is either null or the same length as list {@code b}.
     *
     * @param a The list to check
     * @param b The other list
     * @param name The name of the list for error reporting
     * @return {@code a} if both lists have the same length or {@code null}
     */
    private List<String> checkListsSameLength(List<String> a, List<String> b, String name) {
        if (a != null && a.size() != b.size()) {
            Logging.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''{2}'' must be the same as in ''values''",
                            key, text, name));
            Logging.error(tr("Detailed information: {0} <> {1}", a, b));
            return null; // NOSONAR
        }
        return a;
    }

    protected void initListEntriesFromAttributes() {
        List<String> valueList = null;
        List<String> displayList = null;
        List<String> localeDisplayList = null;

        if (values_from != null) {
            valueList = getValuesFromCode(values_from);
        }

        if (valueList == null) {
            // get from {@code values} attribute
            valueList = splitEscaped(delimiter, values);
        }
        if (valueList == null) {
            return;
        }

        if (!values_no_i18n) {
            localeDisplayList = splitEscaped(delimiter, locale_display_values);
            displayList = splitEscaped(delimiter, display_values);
        }
        List<String> localeShortDescriptionsList = splitEscaped(delimiter, locale_short_descriptions);
        List<String> shortDescriptionsList = splitEscaped(delimiter, short_descriptions);

        displayList = checkListsSameLength(displayList, valueList, "display_values");
        localeDisplayList = checkListsSameLength(localeDisplayList, valueList, "locale_display_values");
        shortDescriptionsList = checkListsSameLength(shortDescriptionsList, valueList, "short_descriptions");
        localeShortDescriptionsList = checkListsSameLength(localeShortDescriptionsList, valueList, "locale_short_descriptions");

        for (int i = 0; i < valueList.size(); i++) {
            final PresetListEntry e = new PresetListEntry(valueList.get(i), this);
            if (displayList != null)
                e.display_value = displayList.get(i);
            if (localeDisplayList != null)
                e.locale_display_value = localeDisplayList.get(i);
            if (shortDescriptionsList != null)
                e.short_description = shortDescriptionsList.get(i);
            if (localeShortDescriptionsList != null)
                e.locale_short_description = localeShortDescriptionsList.get(i);
            addListEntry(e);
        }

        if (values_sort && TaggingPresets.SORT_MENU.get()) {
            Collections.sort(presetListEntries, (a, b) -> AlphanumComparator.getInstance().compare(a.getDisplayValue(), b.getDisplayValue()));
        }
    }

    /**
     * Returns the initial value to use for this preset.
     * <p>
     * The initial value is the value shown in the control when the preset dialogs opens.
     *
     * @param usage The key Usage
     * @return The initial value to use.
     */
    protected String getInitialValue(Usage usage) {
        String initialValue = null;
        originalValue = null;

        if (usage.hasUniqueValue()) {
            // all selected primitives have the same not empty value for this key
            initialValue = usage.getFirst();
            originalValue = initialValue;
        } else if (!usage.unused()) {
            // at least one primitive has a value for this key (but not all have the same one)
            initialValue = DIFFERENT;
            originalValue = initialValue;
        } else if (PROP_FILL_DEFAULT.get() || isForceUseLastAsDefault()) {
            // at this point no primitive had any value for this key
            // use the last value no matter what
            initialValue = LAST_VALUES.get(key);
        } else if (!usage.hadKeys() && isUseLastAsDefault()) {
            // use the last value only on objects with no keys at all
            initialValue = LAST_VALUES.get(key);
        } else if (!usage.hadKeys()) {
            // use the default only on objects with no keys at all
            initialValue = default_;
        }
        return initialValue;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        String value = getSelectedItem().value;

        // no change if same as before
        if (value.isEmpty() && originalValue == null)
            return;
        if (value.equals(originalValue))
            return;
        changedTags.add(new Tag(key, value));

        if (isUseLastAsDefault()) {
            LAST_VALUES.put(key, value);
        }
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

    /**
     * Returns true if the last entered value should be used as default.
     * <p>
     * Note: never used in {@code defaultpresets.xml}.
     *
     * @return true if the last entered value should be used as default.
     */
    protected boolean isUseLastAsDefault() {
        return use_last_as_default > 0;
    }

    /**
     * Returns true if the last entered value should be used as default also on primitives that
     * already have tags.
     * <p>
     * Note: used for {@code addr:*} tags in {@code defaultpresets.xml}.
     *
     * @return true if see above
     */
    protected boolean isForceUseLastAsDefault() {
        return use_last_as_default == 2;
    }

    /**
     * Adds a preset list entry.
     * @param e list entry to add
     */
    public void addListEntry(PresetListEntry e) {
        presetListEntries.add(e);
        // we need to fix the entries because the XML Parser
        // {@link org.openstreetmap.josm.tools.XmlObjectParser.Parser#startElement} has used the
        // default standard constructor for {@link PresetListEntry} if the list entry was defined
        // using XML {@code <list_entry>}.
        e.cms = this;
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

    @Override
    public MatchType getDefaultMatch() {
        return MatchType.NONE;
    }
}
