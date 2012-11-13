// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.ExtendedDialog;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.QuadStateCheckBox;
import org.openstreetmap.josm.gui.dialogs.relation.RelationEditor;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.preferences.SourceEntry;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference.PresetPrefHelper;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPritority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.util.GuiHelper;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.io.MirroredInputStream;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.UrlLabel;
import org.openstreetmap.josm.tools.Utils;
import org.openstreetmap.josm.tools.XmlObjectParser;
import org.openstreetmap.josm.tools.template_engine.ParseError;
import org.openstreetmap.josm.tools.template_engine.TemplateEntry;
import org.openstreetmap.josm.tools.template_engine.TemplateParser;
import org.xml.sax.SAXException;

/**
 * This class read encapsulate one tagging preset. A class method can
 * read in all predefined presets, either shipped with JOSM or that are
 * in the config directory.
 *
 * It is also able to construct dialogs out of preset definitions.
 */
public class TaggingPreset extends AbstractAction implements MapView.LayerChangeListener {

    public enum PresetType {
        NODE(/* ICON */"Mf_node", "node"),
        WAY(/* ICON */"Mf_way", "way"),
        RELATION(/* ICON */"Mf_relation", "relation"),
        CLOSEDWAY(/* ICON */"Mf_closedway", "closedway");

        private final String iconName;
        private final String name;

        PresetType(String iconName, String name) {
            this.iconName = iconName;
            this.name = name;
        }

        public String getIconName() {
            return iconName;
        }

        public String getName() {
            return name;
        }

        public static PresetType forPrimitive(OsmPrimitive p) {
            return forPrimitiveType(p.getDisplayType());
        }

        public static PresetType forPrimitiveType(org.openstreetmap.josm.data.osm.OsmPrimitiveType type) {
            switch (type) {
            case NODE:
                return NODE;
            case WAY:
                return WAY;
            case CLOSEDWAY:
                return CLOSEDWAY;
            case RELATION:
            case MULTIPOLYGON:
                return RELATION;
            default:
                throw new IllegalArgumentException("Unexpected primitive type: " + type);
            }
        }

        public static PresetType fromString(String type) {
            for (PresetType t : PresetType.values()) {
                if (t.getName().equals(type))
                    return t;
            }
            return null;
        }
    }

    /**
     * Enum denoting how a match (see {@link Item#matches}) is performed.
     */
    private enum MatchType {

        /**
         * Neutral, i.e., do not consider this item for matching.
         */
        NONE("none"),
        /**
         * Positive if key matches, neutral otherwise.
         */
        KEY("key"),
        /**
         * Positive if key matches, negative otherwise.
         */
        KEY_REQUIRED("key!"),
        /**
         * Positive if key and value matches, negative otherwise.
         */
        KEY_VALUE("keyvalue");

        private final String value;

        private MatchType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static MatchType ofString(String type) {
            for (MatchType i : EnumSet.allOf(MatchType.class)) {
                if (i.getValue().equals(type))
                    return i;
            }
            throw new IllegalArgumentException(type + " is not allowed");
        }
    }

    public static final int DIALOG_ANSWER_APPLY = 1;
    public static final int DIALOG_ANSWER_NEW_RELATION = 2;
    public static final int DIALOG_ANSWER_CANCEL = 3;

    public TaggingPresetMenu group = null;
    public String name;
    public String name_context;
    public String locale_name;
    public final static String OPTIONAL_TOOLTIP_TEXT = "Optional tooltip text";
    private static File zipIcons = null;
    private static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    public static abstract class Item {

        protected void initAutoCompletionField(AutoCompletingTextField field, String key) {
            OsmDataLayer layer = Main.main.getEditLayer();
            if (layer == null)
                return;
            AutoCompletionList list = new AutoCompletionList();
            Main.main.getEditLayer().data.getAutoCompletionManager().populateWithTagValues(list, key);
            field.setAutoCompletionList(list);
        }

        abstract boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel);

        abstract void addCommands(List<Tag> changedTags);

        boolean requestFocusInWindow() {
            return false;
        }

        /**
         * Tests whether the tags match this item.
         * Note that for a match, at least one positive and no negative is required.
         * @param tags the tags of an {@link OsmPrimitive}
         * @return {@code true} if matches (positive), {@code null} if neutral, {@code false} if mismatches (negative).
         */
        Boolean matches(Map<String, String> tags) {
            return null;
        }
    }

    public static abstract class KeyedItem extends Item {

        public String key;
        public String text;
        public String text_context;
        public String match = getDefaultMatch().getValue();

        public abstract MatchType getDefaultMatch();
        public abstract Collection<String> getValues();

        @Override
        Boolean matches(Map<String, String> tags) {
            switch (MatchType.ofString(match)) {
            case NONE:
                return null;
            case KEY:
                return tags.containsKey(key) ? true : null;
            case KEY_REQUIRED:
                return tags.containsKey(key);
            case KEY_VALUE:
                return tags.containsKey(key) && (getValues().contains(tags.get(key)));
            default:
                throw new IllegalStateException();
            }
        }

    }

    public static class Usage {
        TreeSet<String> values;
        boolean hadKeys = false;
        boolean hadEmpty = false;
        public boolean hasUniqueValue() {
            return values.size() == 1 && !hadEmpty;
        }

        public boolean unused() {
            return values.isEmpty();
        }
        public String getFirst() {
            return values.first();
        }

        public boolean hadKeys() {
            return hadKeys;
        }
    }

    public static final String DIFFERENT = tr("<different>");

    static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<String>();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null) {
                returnValue.values.add(v);
            } else {
                returnValue.hadEmpty = true;
            }
            if(s.hasKeys()) {
                returnValue.hadKeys = true;
            }
        }
        return returnValue;
    }

    static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {

        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<String>();
        for (OsmPrimitive s : sel) {
            String booleanValue = OsmUtils.getNamedOsmBoolean(s.get(key));
            if (booleanValue != null) {
                returnValue.values.add(booleanValue);
            }
        }
        return returnValue;
    }

    public static class PresetListEntry {
        public String value;
        public String value_context;
        public String display_value;
        public String short_description;
        public String icon;
        public String locale_display_value;
        public String locale_short_description;
        private final File zipIcons = TaggingPreset.zipIcons;

        // Cached size (currently only for Combo) to speed up preset dialog initialization
        private int prefferedWidth = -1;
        private int prefferedHeight = -1;

        public String getListDisplay() {
            if (value.equals(DIFFERENT))
                return "<b>"+DIFFERENT.replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</b>";

            if (value.equals(""))
                return "&nbsp;";

            final StringBuilder res = new StringBuilder("<b>");
            res.append(getDisplayValue(true));
            res.append("</b>");
            if (getShortDescription(true) != null) {
                // wrap in table to restrict the text width
                res.append("<div style=\"width:300px; padding:0 0 5px 5px\">");
                res.append(getShortDescription(true));
                res.append("</div>");
            }
            return res.toString();
        }

        public ImageIcon getIcon() {
            return icon == null ? null : loadImageIcon(icon, zipIcons, 24);
        }

        public PresetListEntry() {
        }

        public PresetListEntry(String value) {
            this.value = value;
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
    }

    public static class Text extends KeyedItem {

        public String locale_text;
        public String default_;
        public String originalValue;
        public String use_last_as_default = "false";
        public String length;

        private JComponent value;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineTextUsage(sel, key);
            AutoCompletingTextField textField = new AutoCompletingTextField();
            initAutoCompletionField(textField, key);
            if (length != null && !length.isEmpty()) {
                textField.setMaxChars(new Integer(length));
            }
            if (usage.unused()){
                if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || "force".equals(use_last_as_default)) {
                    // selected osm primitives are untagged or filling default values feature is enabled
                    if (!"false".equals(use_last_as_default) && lastValue.containsKey(key)) {
                        textField.setText(lastValue.get(key));
                    } else {
                        textField.setText(default_);
                    }
                } else {
                    // selected osm primitives are tagged and filling default values feature is disabled
                    textField.setText("");
                }
                value = textField;
                originalValue = null;
            } else if (usage.hasUniqueValue()) {
                // all objects use the same value
                textField.setText(usage.getFirst());
                value = textField;
                originalValue = usage.getFirst();
            } else {
                // the objects have different values
                JosmComboBox comboBox = new JosmComboBox(usage.values.toArray());
                comboBox.setEditable(true);
                comboBox.setEditor(textField);
                comboBox.getEditor().setItem(DIFFERENT);
                value=comboBox;
                originalValue = DIFFERENT;
            }
            if(locale_text == null) {
                if (text != null) {
                    if(text_context != null) {
                        locale_text = trc(text_context, fixPresetString(text));
                    } else {
                        locale_text = tr(fixPresetString(text));
                    }
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {

            // return if unchanged
            String v = (value instanceof JosmComboBox)
                    ? ((JosmComboBox) value).getEditor().getItem().toString()
                            : ((JTextField) value).getText();
                    v = v.trim();

                    if (!"false".equals(use_last_as_default)) {
                        lastValue.put(key, v);
                    }
                    if (v.equals(originalValue) || (originalValue == null && v.length() == 0))
                        return;

                    changedTags.add(new Tag(key, v));
        }

        @Override
        boolean requestFocusInWindow() {
            return value.requestFocusInWindow();
        }

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.NONE;
        }

        @Override
        public Collection<String> getValues() {
            if (default_ == null || default_.isEmpty())
                return Collections.emptyList();
            return Collections.singleton(default_);
        }
    }

    public static class Check extends KeyedItem {

        public String locale_text;
        public String value_on = OsmUtils.trueval;
        public String value_off = OsmUtils.falseval;
        public boolean default_ = false; // only used for tagless objects

        private QuadStateCheckBox check;
        private QuadStateCheckBox.State initialState;
        private boolean def;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            // find out if our key is already used in the selection.
            Usage usage = determineBooleanUsage(sel, key);
            def = default_;

            if(locale_text == null) {
                if(text_context != null) {
                    locale_text = trc(text_context, fixPresetString(text));
                } else {
                    locale_text = tr(fixPresetString(text));
                }
            }

            String oneValue = null;
            for (String s : usage.values) {
                oneValue = s;
            }
            if (usage.values.size() < 2 && (oneValue == null || value_on.equals(oneValue) || value_off.equals(oneValue))) {
                if (def && !PROP_FILL_DEFAULT.get()) {
                    // default is set and filling default values feature is disabled - check if all primitives are untagged
                    for (OsmPrimitive s : sel)
                        if(s.hasKeys()) {
                            def = false;
                        }
                }

                // all selected objects share the same value which is either true or false or unset,
                // we can display a standard check box.
                initialState = value_on.equals(oneValue) ?
                        QuadStateCheckBox.State.SELECTED :
                            value_off.equals(oneValue) ?
                                    QuadStateCheckBox.State.NOT_SELECTED :
                                        def ? QuadStateCheckBox.State.SELECTED
                                                : QuadStateCheckBox.State.UNSET;
                check = new QuadStateCheckBox(locale_text, initialState,
                        new QuadStateCheckBox.State[] {
                        QuadStateCheckBox.State.SELECTED,
                        QuadStateCheckBox.State.NOT_SELECTED,
                        QuadStateCheckBox.State.UNSET });
            } else {
                def = false;
                // the objects have different values, or one or more objects have something
                // else than true/false. we display a quad-state check box
                // in "partial" state.
                initialState = QuadStateCheckBox.State.PARTIAL;
                check = new QuadStateCheckBox(locale_text, QuadStateCheckBox.State.PARTIAL,
                        new QuadStateCheckBox.State[] {
                        QuadStateCheckBox.State.PARTIAL,
                        QuadStateCheckBox.State.SELECTED,
                        QuadStateCheckBox.State.NOT_SELECTED,
                        QuadStateCheckBox.State.UNSET });
            }
            p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        @Override public void addCommands(List<Tag> changedTags) {
            // if the user hasn't changed anything, don't create a command.
            if (check.getState() == initialState && !def) return;

            // otherwise change things according to the selected value.
            changedTags.add(new Tag(key,
                    check.getState() == QuadStateCheckBox.State.SELECTED ? value_on :
                        check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? value_off :
                            null));
        }
        @Override boolean requestFocusInWindow() {return check.requestFocusInWindow();}

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.NONE;
        }

        @Override
        public Collection<String> getValues() {
            return Arrays.asList(value_on, value_off);
        }
    }

    public static abstract class ComboMultiSelect extends KeyedItem {

        public String locale_text;
        public String values;
        public String values_context;
        public String display_values;
        public String locale_display_values;
        public String short_descriptions;
        public String locale_short_descriptions;
        public String default_;
        public String delimiter = ";";
        public String use_last_as_default = "false";

        protected JComponent component;
        protected Map<String, PresetListEntry> lhm = new LinkedHashMap<String, PresetListEntry>();
        private boolean initialized = false;
        protected Usage usage;
        protected Object originalValue;

        protected abstract Object getSelectedItem();
        protected abstract void addToPanelAnchor(JPanel p, String def);

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
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {

            initListEntries();

            // find out if our key is already used in the selection.
            usage = determineTextUsage(sel, key);
            if (!usage.hasUniqueValue() && !usage.unused()) {
                lhm.put(DIFFERENT, new PresetListEntry(DIFFERENT));
            }

            p.add(new JLabel(tr("{0}:", locale_text)), GBC.std().insets(0, 0, 10, 0));
            addToPanelAnchor(p, default_);

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
                    System.err.println(tr("Warning in tagging preset \"{0}-{1}\": "
                            + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                            key, text, "values", "list_entry"));
                }
                if (display_values != null || locale_display_values != null) {
                    System.err.println(tr("Warning in tagging preset \"{0}-{1}\": "
                            + "Ignoring ''{2}'' attribute as ''{3}'' elements are given.",
                            key, text, "display_values", "list_entry"));
                }
                if (short_descriptions != null || locale_short_descriptions != null) {
                    System.err.println(tr("Warning in tagging preset \"{0}-{1}\": "
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
                locale_text = trc(text_context, fixPresetString(text));
            }
            initialized = true;
        }

        private String[] initListEntriesFromAttributes() {
            char delChar = getDelChar();

            String[] value_array = splitEscaped(delChar, values);

            final String displ = Utils.firstNonNull(locale_display_values, display_values);
            String[] display_array = displ == null ? value_array : splitEscaped(delChar, displ);

            final String descr = Utils.firstNonNull(locale_short_descriptions, short_descriptions);
            String[] short_descriptions_array = descr == null ? null : splitEscaped(delChar, descr);

            if (display_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''", key, text));
                display_array = value_array;
            }

            if (short_descriptions_array != null && short_descriptions_array.length != value_array.length) {
                System.err.println(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''", key, text));
                short_descriptions_array = null;
            }

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
                        lhm.put(value_array[i], e);
                        display_array[i] = e.getDisplayValue(true);
            }

            return display_array;
        }

        protected String getDisplayIfNull(String display) {
            return display;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
            Object obj = getSelectedItem();
            String display = (obj == null) ? null : obj.toString();
            String value = null;
            if (display == null) {
                display = getDisplayIfNull(display);
            }

            if (display != null) {
                for (String key : lhm.keySet()) {
                    String k = lhm.get(key).toString();
                    if (k != null && k.equals(display)) {
                        value = key;
                        break;
                    }
                }
                if (value == null) {
                    value = display;
                }
            } else {
                value = "";
            }
            value = value.trim();

            // no change if same as before
            if (originalValue == null) {
                if (value.length() == 0)
                    return;
            } else if (value.equals(originalValue.toString()))
                return;

            if (!"false".equals(use_last_as_default)) {
                lastValue.put(key, value);
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

        @Override
        boolean requestFocusInWindow() {
            return component.requestFocusInWindow();
        }

        private static ListCellRenderer RENDERER = new ListCellRenderer() {

            JLabel lbl = new JLabel();

            public Component getListCellRendererComponent(
                    JList list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus) {
                PresetListEntry item = (PresetListEntry) value;
                
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


        protected ListCellRenderer getListCellRenderer() {
            return RENDERER;
        }

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.NONE;
        }
    }

    public static class Combo extends ComboMultiSelect {

        public boolean editable = true;
        protected JosmComboBox combo;

        public Combo() {
            delimiter = ",";
        }

        @Override
        protected void addToPanelAnchor(JPanel p, String def) {
            if (!usage.unused()) {
                for (String s : usage.values) {
                    if (!lhm.containsKey(s)) {
                        lhm.put(s, new PresetListEntry(s));
                    }
                }
            }
            if (def != null && !lhm.containsKey(def)) {
                lhm.put(def, new PresetListEntry(def));
            }
            lhm.put("", new PresetListEntry(""));

            combo = new JosmComboBox(lhm.values().toArray());
            component = combo;
            combo.setRenderer(getListCellRenderer());
            combo.setEditable(editable);
            combo.reinitialize(lhm.values());
            AutoCompletingTextField tf = new AutoCompletingTextField();
            initAutoCompletionField(tf, key);
            AutoCompletionList acList = tf.getAutoCompletionList();
            if (acList != null) {
                acList.add(getDisplayValues(), AutoCompletionItemPritority.IS_IN_STANDARD);
            }
            combo.setEditor(tf);

            if (usage.hasUniqueValue()) {
                // all items have the same value (and there were no unset items)
                originalValue = lhm.get(usage.getFirst());
                combo.setSelectedItem(originalValue);
            } else if (def != null && usage.unused()) {
                // default is set and all items were unset
                if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || "force".equals(use_last_as_default)) {
                    // selected osm primitives are untagged or filling default feature is enabled
                    combo.setSelectedItem(lhm.get(def).getDisplayValue(true));
                } else {
                    // selected osm primitives are tagged and filling default feature is disabled
                    combo.setSelectedItem("");
                }
                originalValue = lhm.get(DIFFERENT);
            } else if (usage.unused()) {
                // all items were unset (and so is default)
                originalValue = lhm.get("");
                if ("force".equals(use_last_as_default) && lastValue.containsKey(key)) {
                    combo.setSelectedItem(lhm.get(lastValue.get(key)));
                } else {
                    combo.setSelectedItem(originalValue);
                }
            } else {
                originalValue = lhm.get(DIFFERENT);
                combo.setSelectedItem(originalValue);
            }
            p.add(combo, GBC.eol().fill(GBC.HORIZONTAL));

        }

        @Override
        protected Object getSelectedItem() {
            return combo.getSelectedItem();

        }

        @Override
        protected String getDisplayIfNull(String display) {
            if (combo.isEditable())
                return combo.getEditor().getItem().toString();
            else
                return display;

        }
    }

    /**
     * Class that allows list values to be assigned and retrieved as a comma-delimited
     * string.
     */
    public static class ConcatenatingJList extends JList {
        private String delimiter;
        public ConcatenatingJList(String del, Object[] o) {
            super(o);
            delimiter = del;
        }
        public void setSelectedItem(Object o) {
            if (o == null) {
                clearSelection();
            } else {
                String s = o.toString();
                HashSet<String> parts = new HashSet<String>(Arrays.asList(s.split(delimiter)));
                ListModel lm = getModel();
                int[] intParts = new int[lm.getSize()];
                int j = 0;
                for (int i = 0; i < lm.getSize(); i++) {
                    if (parts.contains((((PresetListEntry)lm.getElementAt(i)).value))) {
                        intParts[j++]=i;
                    }
                }
                setSelectedIndices(Arrays.copyOf(intParts, j));
                // check if we have actually managed to represent the full
                // value with our presets. if not, cop out; we will not offer
                // a selection list that threatens to ruin the value.
                setEnabled(s.equals(getSelectedItem()));
            }
        }
        public String getSelectedItem() {
            ListModel lm = getModel();
            int[] si = getSelectedIndices();
            StringBuilder builder = new StringBuilder();
            for (int i=0; i<si.length; i++) {
                if (i>0) {
                    builder.append(delimiter);
                }
                builder.append(((PresetListEntry)lm.getElementAt(si[i])).value);
            }
            return builder.toString();
        }
    }

    public static class MultiSelect extends ComboMultiSelect {

        public long rows = -1;
        protected ConcatenatingJList list;

        @Override
        protected void addToPanelAnchor(JPanel p, String def) {
            list = new ConcatenatingJList(delimiter, lhm.values().toArray());
            component = list;
            ListCellRenderer renderer = getListCellRenderer();
            list.setCellRenderer(renderer);

            if (usage.hasUniqueValue() && !usage.unused()) {
                originalValue = usage.getFirst();
                list.setSelectedItem(originalValue);
            } else if (def != null && !usage.hadKeys() || PROP_FILL_DEFAULT.get() || "force".equals(use_last_as_default)) {
                originalValue = DIFFERENT;
                list.setSelectedItem(def);
            } else if (usage.unused()) {
                originalValue = null;
                list.setSelectedItem(originalValue);
            } else {
                originalValue = DIFFERENT;
                list.setSelectedItem(originalValue);
            }

            JScrollPane sp = new JScrollPane(list);
            // if a number of rows has been specified in the preset,
            // modify preferred height of scroll pane to match that row count.
            if (rows != -1) {
                double height = renderer.getListCellRendererComponent(list,
                        new PresetListEntry("x"), 0, false, false).getPreferredSize().getHeight() * rows;
                sp.setPreferredSize(new Dimension((int) sp.getPreferredSize().getWidth(), (int) height));
            }
            p.add(sp, GBC.eol().fill(GBC.HORIZONTAL));


        }

        @Override
        protected Object getSelectedItem() {
            return list.getSelectedItem();
        }
    }

    /**
     * allow escaped comma in comma separated list:
     * "A\, B\, C,one\, two" --> ["A, B, C", "one, two"]
     * @param delimiter the delimiter, e.g. a comma. separates the entries and
     *      must be escaped within one entry
     * @param s the string
     */
    private static String[] splitEscaped(char delimiter, String s) {
        if (s == null)
            return new String[0];
        List<String> result = new ArrayList<String>();
        boolean backslash = false;
        StringBuilder item = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
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

    public static class Label extends Item {

        public String text;
        public String text_context;
        public String locale_text;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if (locale_text == null) {
                if (text_context != null) {
                    locale_text = trc(text_context, fixPresetString(text));
                } else {
                    locale_text = tr(fixPresetString(text));
                }
            }
            p.add(new JLabel(locale_text), GBC.eol());
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Link extends Item {

        public String href;
        public String text;
        public String text_context;
        public String locale_text;
        public String locale_href;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            if (locale_text == null) {
                if (text == null) {
                    locale_text = tr("More information about this feature");
                } else if (text_context != null) {
                    locale_text = trc(text_context, fixPresetString(text));
                } else {
                    locale_text = tr(fixPresetString(text));
                }
            }
            String url = locale_href;
            if (url == null) {
                url = href;
            }
            if (url != null) {
                p.add(new UrlLabel(url, locale_text, 2), GBC.eol().anchor(GBC.WEST));
            }
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Role {
        public EnumSet<PresetType> types;
        public String key;
        public String text;
        public String text_context;
        public String locale_text;

        public boolean required = false;
        public long count = 0;

        public void setType(String types) throws SAXException {
            this.types = TaggingPreset.getType(types);
        }

        public void setRequisite(String str) throws SAXException {
            if("required".equals(str)) {
                required = true;
            } else if(!"optional".equals(str))
                throw new SAXException(tr("Unknown requisite: {0}", str));
        }

        /* return either argument, the highest possible value or the lowest
           allowed value */
        public long getValidCount(long c)
        {
            if(count > 0 && !required)
                return c != 0 ? count : 0;
            else if(count > 0)
                return count;
            else if(!required)
                return c != 0  ? c : 0;
            else
                return c != 0  ? c : 1;
        }
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            String cstring;
            if(count > 0 && !required) {
                cstring = "0,"+String.valueOf(count);
            } else if(count > 0) {
                cstring = String.valueOf(count);
            } else if(!required) {
                cstring = "0-...";
            } else {
                cstring = "1-...";
            }
            if(locale_text == null) {
                if (text != null) {
                    if(text_context != null) {
                        locale_text = trc(text_context, fixPresetString(text));
                    } else {
                        locale_text = tr(fixPresetString(text));
                    }
                }
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(new JLabel(key), GBC.std().insets(0,0,10,0));
            p.add(new JLabel(cstring), types == null ? GBC.eol() : GBC.std().insets(0,0,10,0));
            if(types != null){
                JPanel pp = new JPanel();
                for(PresetType t : types) {
                    pp.add(new JLabel(ImageProvider.get(t.getIconName())));
                }
                p.add(pp, GBC.eol());
            }
            return true;
        }
    }

    public static class Roles extends Item {

        public List<Role> roles = new LinkedList<Role>();

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            if (roles.size() > 0) {
                JPanel proles = new JPanel(new GridBagLayout());
                proles.add(new JLabel(tr("Available roles")), GBC.std().insets(0, 0, 10, 0));
                proles.add(new JLabel(tr("role")), GBC.std().insets(0, 0, 10, 0));
                proles.add(new JLabel(tr("count")), GBC.std().insets(0, 0, 10, 0));
                proles.add(new JLabel(tr("elements")), GBC.eol());
                for (Role i : roles) {
                    i.addToPanel(proles, sel);
                }
                p.add(proles, GBC.eol());
            }
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Optional extends Item {

        // TODO: Draw a box around optional stuff
        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            p.add(new JLabel(tr("Optional Attributes:")), GBC.eol());
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Space extends Item {

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Key extends KeyedItem {

        public String value;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel) {
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
            changedTags.add(new Tag(key, value));
        }

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.KEY_VALUE;
        }

        @Override
        public Collection<String> getValues() {
            return Collections.singleton(value);
        }
    }

    /**
     * The types as preparsed collection.
     */
    public EnumSet<PresetType> types;
    public List<Item> data = new LinkedList<Item>();
    public TemplateEntry nameTemplate;
    public Match nameTemplateFilter;
    private static final HashMap<String,String> lastValue = new HashMap<String,String>();

    /**
     * Create an empty tagging preset. This will not have any items and
     * will be an empty string as text. createPanel will return null.
     * Use this as default item for "do not select anything".
     */
    public TaggingPreset() {
        MapView.addLayerChangeListener(this);
        updateEnabledState();
    }

    /**
     * Change the display name without changing the toolbar value.
     */
    public void setDisplayName() {
        putValue(Action.NAME, getName());
        putValue("toolbar", "tagging_" + getRawName());
        putValue(OPTIONAL_TOOLTIP_TEXT, (group != null ?
                tr("Use preset ''{0}'' of group ''{1}''", getLocaleName(), group.getName()) :
                    tr("Use preset ''{0}''", getLocaleName())));
    }

    public String getLocaleName() {
        if(locale_name == null) {
            if(name_context != null) {
                locale_name = trc(name_context, fixPresetString(name));
            } else {
                locale_name = tr(fixPresetString(name));
            }
        }
        return locale_name;
    }

    public String getName() {
        return group != null ? group.getName() + "/" + getLocaleName() : getLocaleName();
    }
    public String getRawName() {
        return group != null ? group.getRawName() + "/" + name : name;
    }

    protected static ImageIcon loadImageIcon(String iconName, File zipIcons, Integer maxSize) {
        final Collection<String> s = Main.pref.getCollection("taggingpreset.icon.sources", null);
        ImageProvider imgProv = new ImageProvider(iconName).setDirs(s).setId("presets").setArchive(zipIcons).setOptional(true);
        if (maxSize != null) {
            imgProv.setMaxSize(24);
        }
        return imgProv.get();
    }

    /*
     * Called from the XML parser to set the icon.
     * This task is performed in the background in order to speedup startup.
     *
     * FIXME for Java 1.6 - use 24x24 icons for LARGE_ICON_KEY (button bar)
     * and the 16x16 icons for SMALL_ICON.
     */
    public void setIcon(final String iconName) {
        ImageProvider imgProv = new ImageProvider(iconName);
        final Collection<String> s = Main.pref.getCollection("taggingpreset.icon.sources", null);
        imgProv.setDirs(s);
        imgProv.setId("presets");
        imgProv.setArchive(TaggingPreset.zipIcons);
        imgProv.setOptional(true);
        imgProv.setMaxWidth(16).setMaxHeight(16);
        imgProv.getInBackground(new ImageProvider.ImageCallback() {
            @Override
            public void finished(final ImageIcon result) {
                if (result != null) {
                    GuiHelper.runInEDT(new Runnable() {
                        @Override
                        public void run() {
                            putValue(Action.SMALL_ICON, result);
                        }
                    });
                } else {
                    System.out.println("Could not get presets icon " + iconName);
                }
            }
        });
    }

    // cache the parsing of types using a LRU cache (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html)
    private static final Map<String,EnumSet<PresetType>> typeCache =
            new LinkedHashMap<String, EnumSet<PresetType>>(16, 1.1f, true);

    static public EnumSet<PresetType> getType(String types) throws SAXException {
        if (typeCache.containsKey(types))
            return typeCache.get(types);
        EnumSet<PresetType> result = EnumSet.noneOf(PresetType.class);
        for (String type : Arrays.asList(types.split(","))) {
            try {
                PresetType presetType = PresetType.fromString(type);
                result.add(presetType);
            } catch (IllegalArgumentException e) {
                throw new SAXException(tr("Unknown type: {0}", type));
            }
        }
        typeCache.put(types, result);
        return result;
    }

    /*
     * Called from the XML parser to set the types this preset affects.
     */
    public void setType(String types) throws SAXException {
        this.types = getType(types);
    }

    public void setName_template(String pattern) throws SAXException {
        try {
            this.nameTemplate = new TemplateParser(pattern).parse();
        } catch (ParseError e) {
            System.err.println("Error while parsing " + pattern + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }

    public void setName_template_filter(String filter) throws SAXException {
        try {
            this.nameTemplateFilter = SearchCompiler.compile(filter, false, false);
        } catch (org.openstreetmap.josm.actions.search.SearchCompiler.ParseError e) {
            System.err.println("Error while parsing" + filter + ": " + e.getMessage());
            throw new SAXException(e);
        }
    }


    public static List<TaggingPreset> readAll(Reader in, boolean validate) throws SAXException {
        XmlObjectParser parser = new XmlObjectParser();
        parser.mapOnStart("item", TaggingPreset.class);
        parser.mapOnStart("separator", TaggingPresetSeparator.class);
        parser.mapBoth("group", TaggingPresetMenu.class);
        parser.map("text", Text.class);
        parser.map("link", Link.class);
        parser.mapOnStart("optional", Optional.class);
        parser.mapOnStart("roles", Roles.class);
        parser.map("role", Role.class);
        parser.map("check", Check.class);
        parser.map("combo", Combo.class);
        parser.map("multiselect", MultiSelect.class);
        parser.map("label", Label.class);
        parser.map("space", Space.class);
        parser.map("key", Key.class);
        parser.map("list_entry", PresetListEntry.class);
        LinkedList<TaggingPreset> all = new LinkedList<TaggingPreset>();
        TaggingPresetMenu lastmenu = null;
        Roles lastrole = null;
        List<PresetListEntry> listEntries = new LinkedList<PresetListEntry>();

        if (validate) {
            parser.startWithValidation(in, "http://josm.openstreetmap.de/tagging-preset-1.0", "resource://data/tagging-preset.xsd");
        } else {
            parser.start(in);
        }
        while(parser.hasNext()) {
            Object o = parser.next();
            if (o instanceof TaggingPresetMenu) {
                TaggingPresetMenu tp = (TaggingPresetMenu) o;
                if(tp == lastmenu) {
                    lastmenu = tp.group;
                } else
                {
                    tp.group = lastmenu;
                    tp.setDisplayName();
                    lastmenu = tp;
                    all.add(tp);

                }
                lastrole = null;
            } else if (o instanceof TaggingPresetSeparator) {
                TaggingPresetSeparator tp = (TaggingPresetSeparator) o;
                tp.group = lastmenu;
                all.add(tp);
                lastrole = null;
            } else if (o instanceof TaggingPreset) {
                TaggingPreset tp = (TaggingPreset) o;
                tp.group = lastmenu;
                tp.setDisplayName();
                all.add(tp);
                lastrole = null;
            } else {
                if (all.size() != 0) {
                    if (o instanceof Roles) {
                        all.getLast().data.add((Item) o);
                        lastrole = (Roles) o;
                    } else if (o instanceof Role) {
                        if (lastrole == null)
                            throw new SAXException(tr("Preset role element without parent"));
                        lastrole.roles.add((Role) o);
                    } else if (o instanceof PresetListEntry) {
                        listEntries.add((PresetListEntry) o);
                    } else {
                        all.getLast().data.add((Item) o);
                        if (o instanceof ComboMultiSelect) {
                            ((ComboMultiSelect) o).addListEntries(listEntries);
                        }
                        listEntries = new LinkedList<PresetListEntry>();
                        lastrole = null;
                    }
                } else
                    throw new SAXException(tr("Preset sub element without parent"));
            }
        }
        return all;
    }

    public static Collection<TaggingPreset> readAll(String source, boolean validate) throws SAXException, IOException {
        Collection<TaggingPreset> tp;
        MirroredInputStream s = new MirroredInputStream(source);
        try {
            InputStream zip = s.getZipEntry("xml","preset");
            if(zip != null) {
                zipIcons = s.getFile();
            }
            InputStreamReader r;
            try {
                r = new InputStreamReader(zip == null ? s : zip, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                r = new InputStreamReader(zip == null ? s: zip);
            }
            try {
                tp = TaggingPreset.readAll(new BufferedReader(r), validate);
            } finally {
                r.close();
            }
        } finally {
            s.close();
        }
        return tp;
    }

    public static Collection<TaggingPreset> readAll(Collection<String> sources, boolean validate) {
        LinkedList<TaggingPreset> allPresets = new LinkedList<TaggingPreset>();
        for(String source : sources)  {
            try {
                allPresets.addAll(TaggingPreset.readAll(source, validate));
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Could not read tagging preset source: {0}",source),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            } catch (SAXException e) {
                System.err.println(e.getMessage());
                System.err.println(source);
                e.printStackTrace();
                JOptionPane.showMessageDialog(
                        Main.parent,
                        tr("Error parsing {0}: ", source)+e.getMessage(),
                        tr("Error"),
                        JOptionPane.ERROR_MESSAGE
                        );
            }
        }
        return allPresets;
    }

    public static LinkedList<String> getPresetSources() {
        LinkedList<String> sources = new LinkedList<String>();

        for (SourceEntry e : (new PresetPrefHelper()).get()) {
            sources.add(e.url);
        }

        return sources;
    }

    public static Collection<TaggingPreset> readFromPreferences(boolean validate) {
        return readAll(getPresetSources(), validate);
    }

    private static class PresetPanel extends JPanel {
        boolean hasElements = false;
        PresetPanel()
        {
            super(new GridBagLayout());
        }
    }

    public PresetPanel createPanel(Collection<OsmPrimitive> selected) {
        if (data == null)
            return null;
        PresetPanel p = new PresetPanel();
        LinkedList<Item> l = new LinkedList<Item>();
        if(types != null){
            JPanel pp = new JPanel();
            for(PresetType t : types){
                JLabel la = new JLabel(ImageProvider.get(t.getIconName()));
                la.setToolTipText(tr("Elements of type {0} are supported.", tr(t.getName())));
                pp.add(la);
            }
            p.add(pp, GBC.eol());
        }

        JPanel items = new JPanel(new GridBagLayout());
        for (Item i : data){
            if(i instanceof Link) {
                l.add(i);
            } else {
                if(i.addToPanel(items, selected)) {
                    p.hasElements = true;
                }
            }
        }
        p.add(items, GBC.eol().fill());
        if (selected.size() == 0 && !supportsRelation()) {
            GuiHelper.setEnabledRec(items, false);
        }

        for(Item link : l) {
            link.addToPanel(p, selected);
        }

        return p;
    }

    public boolean isShowable()
    {
        for(Item i : data)
        {
            if(!(i instanceof Optional || i instanceof Space || i instanceof Key))
                return true;
        }
        return false;
    }

    public void actionPerformed(ActionEvent e) {
        if (Main.main == null) return;
        if (Main.main.getCurrentDataSet() == null) return;

        Collection<OsmPrimitive> sel = createSelection(Main.main.getCurrentDataSet().getSelected());
        int answer = showDialog(sel, supportsRelation());

        if (sel.size() != 0 && answer == DIALOG_ANSWER_APPLY) {
            Command cmd = createCommand(sel, getChangedTags());
            if (cmd != null) {
                Main.main.undoRedo.add(cmd);
            }
        } else if (answer == DIALOG_ANSWER_NEW_RELATION) {
            final Relation r = new Relation();
            final Collection<RelationMember> members = new HashSet<RelationMember>();
            for(Tag t : getChangedTags()) {
                r.put(t.getKey(), t.getValue());
            }
            for(OsmPrimitive osm : Main.main.getCurrentDataSet().getSelected()) {
                RelationMember rm = new RelationMember("", osm);
                r.addMember(rm);
                members.add(rm);
            }
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    RelationEditor.getEditor(Main.main.getEditLayer(), r, members).setVisible(true);
                }
            });
        }
        Main.main.getCurrentDataSet().setSelected(Main.main.getCurrentDataSet().getSelected()); // force update

    }

    public int showDialog(Collection<OsmPrimitive> sel, final boolean showNewRelation) {
        PresetPanel p = createPanel(sel);
        if (p == null)
            return DIALOG_ANSWER_CANCEL;

        int answer = 1;
        if (p.getComponentCount() != 0 && (sel.size() == 0 || p.hasElements)) {
            String title = trn("Change {0} object", "Change {0} objects", sel.size(), sel.size());
            if(sel.size() == 0) {
                if(originalSelectionEmpty) {
                    title = tr("Nothing selected!");
                } else {
                    title = tr("Selection unsuitable!");
                }
            }

            class PresetDialog extends ExtendedDialog {
                public PresetDialog(Component content, String title, boolean disableApply) {
                    super(Main.parent,
                            title,
                            showNewRelation?
                                    new String[] { tr("Apply Preset"), tr("New relation"), tr("Cancel") }:
                                        new String[] { tr("Apply Preset"), tr("Cancel") },
                                        true);
                    contentInsets = new Insets(10,5,0,5);
                    if (showNewRelation) {
                        setButtonIcons(new String[] {"ok.png", "dialogs/addrelation.png", "cancel.png" });
                    } else {
                        setButtonIcons(new String[] {"ok.png", "cancel.png" });
                    }
                    setContent(content);
                    setDefaultButton(1);
                    setupDialog();
                    buttons.get(0).setEnabled(!disableApply);
                    buttons.get(0).setToolTipText(title);
                    // Prevent dialogs of being too narrow (fix #6261)
                    Dimension d = getSize();
                    if (d.width < 350) {
                        d.width = 350;
                        setSize(d);
                    }
                    showDialog();
                }
            }

            answer = new PresetDialog(p, title, (sel.size() == 0)).getValue();
        }
        if (!showNewRelation && answer == 2)
            return DIALOG_ANSWER_CANCEL;
        else
            return answer;
    }

    /**
     * True whenever the original selection given into createSelection was empty
     */
    private boolean originalSelectionEmpty = false;

    /**
     * Removes all unsuitable OsmPrimitives from the given list
     * @param participants List of possible OsmPrimitives to tag
     * @return Cleaned list with suitable OsmPrimitives only
     */
    public Collection<OsmPrimitive> createSelection(Collection<OsmPrimitive> participants) {
        originalSelectionEmpty = participants.size() == 0;
        Collection<OsmPrimitive> sel = new LinkedList<OsmPrimitive>();
        for (OsmPrimitive osm : participants)
        {
            if (types != null)
            {
                if(osm instanceof Relation)
                {
                    if(!types.contains(PresetType.RELATION) &&
                            !(types.contains(PresetType.CLOSEDWAY) && ((Relation)osm).isMultipolygon())) {
                        continue;
                    }
                }
                else if(osm instanceof Node)
                {
                    if(!types.contains(PresetType.NODE)) {
                        continue;
                    }
                }
                else if(osm instanceof Way)
                {
                    if(!types.contains(PresetType.WAY) &&
                            !(types.contains(PresetType.CLOSEDWAY) && ((Way)osm).isClosed())) {
                        continue;
                    }
                }
            }
            sel.add(osm);
        }
        return sel;
    }

    public List<Tag> getChangedTags() {
        List<Tag> result = new ArrayList<Tag>();
        for (Item i: data) {
            i.addCommands(result);
        }
        return result;
    }

    private static String fixPresetString(String s) {
        return s == null ? s : s.replaceAll("'","''");
    }

    public static Command createCommand(Collection<OsmPrimitive> sel, List<Tag> changedTags) {
        List<Command> cmds = new ArrayList<Command>();
        for (Tag tag: changedTags) {
            cmds.add(new ChangePropertyCommand(sel, tag.getKey(), tag.getValue()));
        }

        if (cmds.size() == 0)
            return null;
        else if (cmds.size() == 1)
            return cmds.get(0);
        else
            return new SequenceCommand(tr("Change Properties"), cmds);
    }

    private boolean supportsRelation() {
        return types == null || types.contains(PresetType.RELATION);
    }

    protected void updateEnabledState() {
        setEnabled(Main.main != null && Main.main.getCurrentDataSet() != null);
    }

    public void activeLayerChange(Layer oldLayer, Layer newLayer) {
        updateEnabledState();
    }

    public void layerAdded(Layer newLayer) {
        updateEnabledState();
    }

    public void layerRemoved(Layer oldLayer) {
        updateEnabledState();
    }

    @Override
    public String toString() {
        return (types == null?"":types) + " " + name;
    }

    public boolean typeMatches(Collection<PresetType> t) {
        return t == null || types == null || types.containsAll(t);
    }

    public boolean matches(Collection<PresetType> t, Map<String, String> tags, boolean onlyShowable) {
        if (onlyShowable && !isShowable())
            return false;
        else if (!typeMatches(t))
            return false;
        boolean atLeastOnePositiveMatch = false;
        for (Item item : data) {
            Boolean m = item.matches(tags);
            if (m != null && !m)
                return false;
            else if (m != null) {
                atLeastOnePositiveMatch = true;
            }
        }
        return atLeastOnePositiveMatch;
    }
}
