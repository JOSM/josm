// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetReader;
import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetSelector;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Utils;

/**
 * Abstract superclass for combo box and multi-select list types.
 */
public abstract class ComboMultiSelect extends KeyedItem {

    private static final ListCellRenderer<PresetListEntry> RENDERER = new ListCellRenderer<PresetListEntry>() {

        private final JLabel lbl = new JLabel();

        @Override
        public Component getListCellRendererComponent(JList<? extends PresetListEntry> list, PresetListEntry item, int index,
                boolean isSelected, boolean cellHasFocus) {

            // Only return cached size, item is not shown
            if (!list.isShowing() && item.prefferedWidth != -1 && item.prefferedHeight != -1) {
                if (index == -1) {
                    lbl.setPreferredSize(new Dimension(item.prefferedWidth, 10));
                } else {
                    lbl.setPreferredSize(new Dimension(item.prefferedWidth, item.prefferedHeight));
                }
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
            item.prefferedWidth = lbl.getPreferredSize().width;
            item.prefferedHeight = lbl.getPreferredSize().height;

            // We do not want the editor to have the maximum height of all
            // entries. Return a dummy with bogus height.
            if (index == -1) {
                lbl.setPreferredSize(new Dimension(lbl.getPreferredSize().width, 10));
            }
            return lbl;
        }
    };

    /** The localized version of {@link #text}. */
    public String locale_text;
    public String values;
    public String values_from;
    /** The context used for translating {@link #values} */
    public String values_context;
    public String display_values;
    /** The localized version of {@link #display_values}. */
    public String locale_display_values;
    public String short_descriptions;
    /** The localized version of {@link #short_descriptions}. */
    public String locale_short_descriptions;
    public String default_;
    public String delimiter = ";";
    public String use_last_as_default = "false";
    /** whether to use values for search via {@link TaggingPresetSelector} */
    public String values_searchable = "false";

    protected JComponent component;
    protected final Map<String, PresetListEntry> lhm = new LinkedHashMap<>();
    private boolean initialized;
    protected Usage usage;
    protected Object originalValue;

    /**
     * Class that allows list values to be assigned and retrieved as a comma-delimited
     * string (extracted from TaggingPreset)
     */
    protected static class ConcatenatingJList extends JList<PresetListEntry> {
        private final String delimiter;

        protected ConcatenatingJList(String del, PresetListEntry[] o) {
            super(o);
            delimiter = del;
        }

        public void setSelectedItem(Object o) {
            if (o == null) {
                clearSelection();
            } else {
                String s = o.toString();
                Set<String> parts = new TreeSet<>(Arrays.asList(s.split(delimiter)));
                ListModel<PresetListEntry> lm = getModel();
                int[] intParts = new int[lm.getSize()];
                int j = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    final String value = lm.getElementAt(i).value;
                    if (parts.contains(value)) {
                        intParts[j++] = i;
                        parts.remove(value);
                    }
                }
                setSelectedIndices(Arrays.copyOf(intParts, j));
                // check if we have actually managed to represent the full
                // value with our presets. if not, cop out; we will not offer
                // a selection list that threatens to ruin the value.
                setEnabled(parts.isEmpty());
            }
        }

        public String getSelectedItem() {
            ListModel<PresetListEntry> lm = getModel();
            int[] si = getSelectedIndices();
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < si.length; i++) {
                if (i > 0) {
                    builder.append(delimiter);
                }
                builder.append(lm.getElementAt(si[i]).value);
            }
            return builder.toString();
        }
    }

    public static class PresetListEntry implements Comparable<PresetListEntry> {
        public String value;
        /** The context used for translating {@link #value} */
        public String value_context;
        public String display_value;
        public String short_description;
        /** The location of icon file to display */
        public String icon;
        /** The size of displayed icon. If not set, default is size from icon file */
        public String icon_size;
        /** The localized version of {@link #display_value}. */
        public String locale_display_value;
        /** The localized version of {@link #short_description}. */
        public String locale_short_description;
        private final File zipIcons = TaggingPresetReader.getZipIcons();

        // Cached size (currently only for Combo) to speed up preset dialog initialization
        public int prefferedWidth = -1;
        public int prefferedHeight = -1;

        /**
         * Constructs a new {@code PresetListEntry}, uninitialized.
         */
        public PresetListEntry() {
        }

        public PresetListEntry(String value) {
            this.value = value;
        }

        public String getListDisplay() {
            if (value.equals(DIFFERENT))
                return "<b>"+DIFFERENT.replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</b>";

            if (value.isEmpty())
                return "&nbsp;";

            final StringBuilder res = new StringBuilder("<b>");
            res.append(getDisplayValue(true).replaceAll("<", "&lt;").replaceAll(">", "&gt;"))
               .append("</b>");
            if (getShortDescription(true) != null) {
                // wrap in table to restrict the text width
                res.append("<div style=\"width:300px; padding:0 0 5px 5px\">")
                   .append(getShortDescription(true))
                   .append("</div>");
            }
            return res.toString();
        }

        /**
         * Returns the entry icon, if any.
         * @return the entry icon, or {@code null}
         */
        public ImageIcon getIcon() {
            return icon == null ? null : loadImageIcon(icon, zipIcons, parseInteger(icon_size));
        }

        public String getDisplayValue(boolean translated) {
            return translated
                    ? Utils.firstNonNull(locale_display_value, tr(display_value), trc(value_context, value))
                            : Utils.firstNonNull(display_value, value);
        }

        public String getShortDescription(boolean translated) {
            return translated
                    ? Utils.firstNonNull(locale_short_description, tr(short_description))
                            : short_description;
        }

        // toString is mainly used to initialize the Editor
        @Override
        public String toString() {
            if (value.equals(DIFFERENT))
                return DIFFERENT;
            return getDisplayValue(true).replaceAll("<.*>", ""); // remove additional markup, e.g. <br>
        }

        @Override
        public int compareTo(PresetListEntry o) {
            return AlphanumComparator.getInstance().compare(this.getDisplayValue(true), o.getDisplayValue(true));
        }
    }

    /**
     * allow escaped comma in comma separated list:
     * "A\, B\, C,one\, two" --&gt; ["A, B, C", "one, two"]
     * @param delimiter the delimiter, e.g. a comma. separates the entries and
     *      must be escaped within one entry
     * @param s the string
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
        return result.toArray(new String[result.size()]);
    }

    protected abstract Object getSelectedItem();

    protected abstract void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches);

    protected char getDelChar() {
        return delimiter.isEmpty() ? ';' : delimiter.charAt(0);
    }

    @Override
    public Collection<String> getValues() {
        initListEntries();
        return lhm.keySet();
    }

    public Collection<String> getDisplayValues() {
        initListEntries();
        return Utils.transform(lhm.values(), new Utils.Function<PresetListEntry, String>() {
            @Override
            public String apply(PresetListEntry x) {
                return x.getDisplayValue(true);
            }
        });
    }

    @Override
    public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

        initListEntries();

        // find out if our key is already used in the selection.
        usage = determineTextUsage(sel, key);
        if (!usage.hasUniqueValue() && !usage.unused()) {
            lhm.put(DIFFERENT, new PresetListEntry(DIFFERENT));
        }

        p.add(new JLabel(tr("{0}:", locale_text)), GBC.std().insets(0, 0, 10, 0));
        addToPanelAnchor(p, default_, presetInitiallyMatches);

        return true;

    }

    private void initListEntries() {
        if (initialized) {
            lhm.remove(DIFFERENT); // possibly added in #addToPanel
            return;
        } else if (lhm.isEmpty()) {
            initListEntriesFromAttributes();
        } else {
            if (values != null) {
                Main.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "values", "list_entry"));
            }
            if (display_values != null || locale_display_values != null) {
                Main.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "display_values", "list_entry"));
            }
            if (short_descriptions != null || locale_short_descriptions != null) {
                Main.warn(tr("Warning in tagging preset \"{0}-{1}\": "
                        + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                        key, text, "short_descriptions", "list_entry"));
            }
            for (PresetListEntry e : lhm.values()) {
                if (e.value_context == null) {
                    e.value_context = values_context;
                }
            }
        }
        if (locale_text == null) {
            locale_text = getLocaleText(text, text_context, null);
        }
        initialized = true;
    }

    private void initListEntriesFromAttributes() {
        char delChar = getDelChar();

        String[] value_array = null;

        if (values_from != null) {
            String[] class_method = values_from.split("#");
            if (class_method != null && class_method.length == 2) {
                try {
                    Method method = Class.forName(class_method[0]).getMethod(class_method[1]);
                    // Check method is public static String[] methodName()
                    int mod = method.getModifiers();
                    if (Modifier.isPublic(mod) && Modifier.isStatic(mod)
                            && method.getReturnType().equals(String[].class) && method.getParameterTypes().length == 0) {
                        value_array = (String[]) method.invoke(null);
                    } else {
                        Main.error(tr("Broken tagging preset \"{0}-{1}\" - Java method given in ''values_from'' is not \"{2}\"", key, text,
                                "public static String[] methodName()"));
                    }
                } catch (Exception e) {
                    Main.error(tr("Broken tagging preset \"{0}-{1}\" - Java method given in ''values_from'' threw {2} ({3})", key, text,
                            e.getClass().getName(), e.getMessage()));
                }
            }
        }

        if (value_array == null) {
            value_array = splitEscaped(delChar, values);
        }

        final String displ = Utils.firstNonNull(locale_display_values, display_values);
        String[] display_array = displ == null ? value_array : splitEscaped(delChar, displ);

        final String descr = Utils.firstNonNull(locale_short_descriptions, short_descriptions);
        String[] short_descriptions_array = descr == null ? null : splitEscaped(delChar, descr);

        if (display_array.length != value_array.length) {
            Main.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''",
                            key, text));
            display_array = value_array;
        }

        if (short_descriptions_array != null && short_descriptions_array.length != value_array.length) {
            Main.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''",
                            key, text));
            short_descriptions_array = null;
        }

        final List<PresetListEntry> entries = new ArrayList<>(value_array.length);
        for (int i = 0; i < value_array.length; i++) {
            final PresetListEntry e = new PresetListEntry(value_array[i]);
            e.locale_display_value = locale_display_values != null
                    ? display_array[i]
                    : trc(values_context, fixPresetString(display_array[i]));
            if (short_descriptions_array != null) {
                e.locale_short_description = locale_short_descriptions != null
                        ? short_descriptions_array[i]
                        : tr(fixPresetString(short_descriptions_array[i]));
            }

            entries.add(e);
        }

        if (Main.pref.getBoolean("taggingpreset.sortvalues", true)) {
            Collections.sort(entries);
        }

        for (PresetListEntry i : entries) {
            lhm.put(i.value, i);
        }
    }

    protected String getDisplayIfNull() {
        return null;
    }

    @Override
    public void addCommands(List<Tag> changedTags) {
        Object obj = getSelectedItem();
        String display = (obj == null) ? null : obj.toString();
        String value = null;
        if (display == null) {
            display = getDisplayIfNull();
        }

        if (display != null) {
            for (Entry<String, PresetListEntry> entry : lhm.entrySet()) {
                String k = entry.getValue().toString();
                if (k != null && k.equals(display)) {
                    value = entry.getKey();
                    break;
                }
            }
            if (value == null) {
                value = display;
            }
        } else {
            value = "";
        }
        value = Tag.removeWhiteSpaces(value);

        // no change if same as before
        if (originalValue == null) {
            if (value.isEmpty())
                return;
        } else if (value.equals(originalValue.toString()))
            return;

        if (!"false".equals(use_last_as_default)) {
            LAST_VALUES.put(key, value);
        }
        changedTags.add(new Tag(key, value));
    }

    public void addListEntry(PresetListEntry e) {
        lhm.put(e.value, e);
    }

    public void addListEntries(Collection<PresetListEntry> e) {
        for (PresetListEntry i : e) {
            addListEntry(i);
        }
    }

    protected ListCellRenderer<PresetListEntry> getListCellRenderer() {
        return RENDERER;
    }

    @Override
    public MatchType getDefaultMatch() {
        return MatchType.NONE;
    }
}
