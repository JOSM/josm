// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.dialogs.properties.PresetListPanel;
import org.openstreetmap.josm.gui.preferences.map.TaggingPresetPreference;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPriority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.QuadStateCheckBox;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.ImageProvider;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Utils;
import org.xml.sax.SAXException;

/**
 * Class that contains all subtypes of TaggingPresetItem, static supplementary data, types and methods
 * @since 6068
 */
public final class TaggingPresetItems {
    private TaggingPresetItems() {    }

    private static int auto_increment_selected = 0;
    public static final String DIFFERENT = tr("<different>");

    private static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    // cache the parsing of types using a LRU cache (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html)
    private static final Map<String,EnumSet<TaggingPresetType>> typeCache =
            new LinkedHashMap<String, EnumSet<TaggingPresetType>>(16, 1.1f, true);

    /**
     * Last value of each key used in presets, used for prefilling corresponding fields
     */
    private static final Map<String,String> lastValue = new HashMap<String,String>();

    public static class PresetListEntry {
        public String value;
        public String value_context;
        public String display_value;
        public String short_description;
        public String icon;
        public String icon_size;
        public String locale_display_value;
        public String locale_short_description;
        private final File zipIcons = TaggingPresetReader.getZipIcons();

        // Cached size (currently only for Combo) to speed up preset dialog initialization
        private int prefferedWidth = -1;
        private int prefferedHeight = -1;

        public String getListDisplay() {
            if (value.equals(DIFFERENT))
                return "<b>"+DIFFERENT.replaceAll("<", "&lt;").replaceAll(">", "&gt;")+"</b>";

            if (value.isEmpty())
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
            return icon == null ? null : loadImageIcon(icon, zipIcons, parseInteger(icon_size));
        }

        private Integer parseInteger(String str) {
            if (str == null || str.isEmpty())
                return null;
            try {
                return Integer.parseInt(str);
            } catch (Exception e) {
                //
            }
            return null;
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

    public static class Role {
        public EnumSet<TaggingPresetType> types;
        public String key;
        public String text;
        public String text_context;
        public String locale_text;
        public SearchCompiler.Match memberExpression;

        public boolean required = false;
        public long count = 0;

        public void setType(String types) throws SAXException {
            this.types = getType(types);
        }

        public void setRequisite(String str) throws SAXException {
            if("required".equals(str)) {
                required = true;
            } else if(!"optional".equals(str))
                throw new SAXException(tr("Unknown requisite: {0}", str));
        }

        public void setMember_expression(String member_expression) throws SAXException {
            try {
                this.memberExpression = SearchCompiler.compile(member_expression, true, true);
            } catch (SearchCompiler.ParseError ex) {
                throw new SAXException(tr("Illegal member expression: {0}", ex.getMessage()), ex);
            }
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
                cstring = "0,"+count;
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
                for(TaggingPresetType t : types) {
                    pp.add(new JLabel(ImageProvider.get(t.getIconName())));
                }
                p.add(pp, GBC.eol());
            }
            return true;
        }
    }

    /**
     * Enum denoting how a match (see {@link TaggingPresetItem#matches}) is performed.
     */
    public static enum MatchType {

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

    /**
     * A tagging preset item displaying a localizable text.
     * @since 6190
     */
    public static abstract class TaggingPresetTextItem extends TaggingPresetItem {

        /**
         * The text to display
         */
        public String text;

        /**
         * The context used for translating {@link #text}
         */
        public String text_context;

        /**
         * The localized version of {@link #text}
         */
        public String locale_text;

        protected final void initializeLocaleText(String defaultText) {
            if (locale_text == null) {
                if (text == null) {
                    locale_text = defaultText;
                } else if (text_context != null) {
                    locale_text = trc(text_context, fixPresetString(text));
                } else {
                    locale_text = tr(fixPresetString(text));
                }
            }
        }

        @Override
        void addCommands(List<Tag> changedTags) {
        }

        protected String fieldsToString() {
            return (text != null ? "text=" + text + ", " : "")
                    + (text_context != null ? "text_context=" + text_context + ", " : "")
                    + (locale_text != null ? "locale_text=" + locale_text : "");
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + " [" + fieldsToString() + "]";
        }
    }

    public static class Label extends TaggingPresetTextItem {

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            initializeLocaleText(null);
            addLabel(p, locale_text);
            return false;
        }

        public static void addLabel(JPanel p, String label) {
            p.add(new JLabel(label), GBC.eol());
        }
    }

    public static class Link extends TaggingPresetTextItem {

        /**
         * The link to display
         */
        public String href;

        /**
         * The localized version of {@link #href}
         */
        public String locale_href;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            initializeLocaleText(tr("More information about this feature"));
            String url = locale_href;
            if (url == null) {
                url = href;
            }
            if (url != null) {
                p.add(new UrlLabel(url, locale_text, 2), GBC.eol().insets(0, 10, 0, 0));
            }
            return false;
        }

        @Override
        protected String fieldsToString() {
            return super.fieldsToString()
                    + (href != null ? "href=" + href + ", " : "")
                    + (locale_href != null ? "locale_href=" + locale_href + ", " : "");
        }
    }

    public static class PresetLink extends TaggingPresetItem {

        public String preset_name = "";

        @Override
        boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            final String presetName = preset_name;
            final TaggingPreset t = Utils.filter(TaggingPresetPreference.taggingPresets, new Predicate<TaggingPreset>() {
                @Override
                public boolean evaluate(TaggingPreset object) {
                    return presetName.equals(object.name);
                }
            }).iterator().next();
            if (t == null) return false;
            JLabel lbl = PresetListPanel.createLabelForPreset(t);
            lbl.addMouseListener(new PresetListPanel.PresetLabelML(lbl, t, null) {
                @Override
                public void mouseClicked(MouseEvent arg0) {
                    t.actionPerformed(null);
                }
            });
            p.add(lbl, GBC.eol().fill(GBC.HORIZONTAL));
            return false;
        }

        @Override
        void addCommands(List<Tag> changedTags) {
        }
    }

    public static class Roles extends TaggingPresetItem {

        public final List<Role> roles = new LinkedList<Role>();

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            p.add(new JLabel(" "), GBC.eol()); // space
            if (!roles.isEmpty()) {
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

    public static class Optional extends TaggingPresetTextItem {

        // TODO: Draw a box around optional stuff
        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            initializeLocaleText(tr("Optional Attributes:"));
            p.add(new JLabel(" "), GBC.eol()); // space
            p.add(new JLabel(locale_text), GBC.eol());
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }
    }

    public static class Space extends TaggingPresetItem {

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            p.add(new JLabel(" "), GBC.eol()); // space
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }

        @Override
        public String toString() {
            return "Space";
        }
    }

    /**
     * Class used to represent a {@link JSeparator} inside tagging preset window.
     * @since 6198
     */
    public static class ItemSeparator extends TaggingPresetItem {

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            p.add(new JSeparator(), GBC.eol().fill(GBC.HORIZONTAL).insets(0, 5, 0, 5));
            return false;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
        }

        @Override
        public String toString() {
            return "ItemSeparator";
        }
    }

    public static abstract class KeyedItem extends TaggingPresetItem {

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

        @Override
        public String toString() {
            return "KeyedItem [key=" + key + ", text=" + text
                    + ", text_context=" + text_context + ", match=" + match
                    + "]";
        }
    }

    public static class Key extends KeyedItem {

        public String value;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
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

        @Override
        public String toString() {
            return "Key [key=" + key + ", value=" + value + ", text=" + text
                    + ", text_context=" + text_context + ", match=" + match
                    + "]";
        }
    }

    public static class Text extends KeyedItem {

        public String locale_text;
        public String default_;
        public String originalValue;
        public String use_last_as_default = "false";
        public String auto_increment;
        public String length;

        private JComponent value;

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

            // find out if our key is already used in the selection.
            Usage usage = determineTextUsage(sel, key);
            AutoCompletingTextField textField = new AutoCompletingTextField();
            initAutoCompletionField(textField, key);
            if (length != null && !length.isEmpty()) {
                textField.setMaxChars(Integer.valueOf(length));
            }
            if (usage.unused()){
                if (auto_increment_selected != 0  && auto_increment != null) {
                    try {
                        textField.setText(Integer.toString(Integer.parseInt(lastValue.get(key)) + auto_increment_selected));
                    } catch (NumberFormatException ex) {
                        // Ignore - cannot auto-increment if last was non-numeric
                    }
                }
                else if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || "force".equals(use_last_as_default)) {
                    // selected osm primitives are untagged or filling default values feature is enabled
                    if (!"false".equals(use_last_as_default) && lastValue.containsKey(key) && !presetInitiallyMatches) {
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
            if (locale_text == null) {
                if (text != null) {
                    if (text_context != null) {
                        locale_text = trc(text_context, fixPresetString(text));
                    } else {
                        locale_text = tr(fixPresetString(text));
                    }
                }
            }

            // if there's an auto_increment setting, then wrap the text field
            // into a panel, appending a number of buttons.
            // auto_increment has a format like -2,-1,1,2
            // the text box being the first component in the panel is relied
            // on in a rather ugly fashion further down.
            if (auto_increment != null) {
                ButtonGroup bg = new ButtonGroup();
                JPanel pnl = new JPanel(new GridBagLayout());
                pnl.add(value, GBC.std().fill(GBC.HORIZONTAL));

                // first, one button for each auto_increment value
                for (final String ai : auto_increment.split(",")) {
                    JToggleButton aibutton = new JToggleButton(ai);
                    aibutton.setToolTipText(tr("Select auto-increment of {0} for this field", ai));
                    aibutton.setMargin(new java.awt.Insets(0,0,0,0));
                    aibutton.setFocusable(false);
                    bg.add(aibutton);
                    try {
                        // TODO there must be a better way to parse a number like "+3" than this.
                        final int buttonvalue = (NumberFormat.getIntegerInstance().parse(ai.replace("+", ""))).intValue();
                        if (auto_increment_selected == buttonvalue) aibutton.setSelected(true);
                        aibutton.addActionListener(new ActionListener() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                auto_increment_selected = buttonvalue;
                            }
                        });
                        pnl.add(aibutton, GBC.std());
                    } catch (ParseException x) {
                        Main.error("Cannot parse auto-increment value of '" + ai + "' into an integer");
                    }
                }

                // an invisible toggle button for "release" of the button group
                final JToggleButton clearbutton = new JToggleButton("X");
                clearbutton.setVisible(false);
                clearbutton.setFocusable(false);
                bg.add(clearbutton);
                // and its visible counterpart. - this mechanism allows us to
                // have *no* button selected after the X is clicked, instead
                // of the X remaining selected
                JButton releasebutton = new JButton("X");
                releasebutton.setToolTipText(tr("Cancel auto-increment for this field"));
                releasebutton.setMargin(new java.awt.Insets(0,0,0,0));
                releasebutton.setFocusable(false);
                releasebutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        auto_increment_selected = 0;
                        clearbutton.setSelected(true);
                    }
                });
                pnl.add(releasebutton, GBC.eol());
                value = pnl;
            }
            p.add(new JLabel(locale_text+":"), GBC.std().insets(0,0,10,0));
            p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        private static String getValue(Component comp) {
            if (comp instanceof JosmComboBox) {
                return ((JosmComboBox) comp).getEditor().getItem().toString();
            } else if (comp instanceof JosmTextField) {
                return ((JosmTextField) comp).getText();
            } else if (comp instanceof JPanel) {
                return getValue(((JPanel)comp).getComponent(0));
            } else {
                return null;
            }
        }

        @Override
        public void addCommands(List<Tag> changedTags) {

            // return if unchanged
            String v = getValue(value);
            if (v == null) {
                Main.error("No 'last value' support for component " + value);
                return;
            }

            v = Tag.removeWhiteSpaces(v);

            if (!"false".equals(use_last_as_default) || auto_increment != null) {
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

    /**
     * A group of {@link Check}s.
     * @since 6114
     */
    public static class CheckGroup extends TaggingPresetItem {

        /**
         * Number of columns (positive integer)
         */
        public String columns;

        /**
         * List of checkboxes
         */
        public final List<Check> checks = new LinkedList<Check>();

        @Override
        boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            Integer cols = Integer.valueOf(columns);
            int rows = (int) Math.ceil(checks.size()/cols.doubleValue());
            JPanel panel = new JPanel(new GridLayout(rows, cols));

            for (Check check : checks) {
                check.addToPanel(panel, sel, presetInitiallyMatches);
            }

            p.add(panel, GBC.eol());
            return false;
        }

        @Override
        void addCommands(List<Tag> changedTags) {
            for (Check check : checks) {
                check.addCommands(changedTags);
            }
        }

        @Override
        public String toString() {
            return "CheckGroup [columns=" + columns + "]";
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

        @Override public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

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

        @Override
        public String toString() {
            return "Check ["
                    + (locale_text != null ? "locale_text=" + locale_text + ", " : "")
                    + (value_on != null ? "value_on=" + value_on + ", " : "")
                    + (value_off != null ? "value_off=" + value_off + ", " : "")
                    + "default_=" + default_ + ", "
                    + (check != null ? "check=" + check + ", " : "")
                    + (initialState != null ? "initialState=" + initialState
                            + ", " : "") + "def=" + def + "]";
        }
    }

    public static abstract class ComboMultiSelect extends KeyedItem {

        public String locale_text;
        public String values;
        public String values_from;
        public String values_context;
        public String display_values;
        public String locale_display_values;
        public String short_descriptions;
        public String locale_short_descriptions;
        public String default_;
        public String delimiter = ";";
        public String use_last_as_default = "false";
        /** whether to use values for search via {@link TaggingPresetSelector} */
        public String values_searchable = "false";

        protected JComponent component;
        protected final Map<String, PresetListEntry> lhm = new LinkedHashMap<String, PresetListEntry>();
        private boolean initialized = false;
        protected Usage usage;
        protected Object originalValue;

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
                locale_text = trc(text_context, fixPresetString(text));
            }
            initialized = true;
        }

        private String[] initListEntriesFromAttributes() {
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
                Main.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''display_values'' must be the same as in ''values''", key, text));
                display_array = value_array;
            }

            if (short_descriptions_array != null && short_descriptions_array.length != value_array.length) {
                Main.error(tr("Broken tagging preset \"{0}-{1}\" - number of items in ''short_descriptions'' must be the same as in ''values''", key, text));
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
                for (String val : lhm.keySet()) {
                    String k = lhm.get(val).toString();
                    if (k != null && k.equals(display)) {
                        value = val;
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

        private static final ListCellRenderer RENDERER = new ListCellRenderer() {

            JLabel lbl = new JLabel();

            @Override
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
        public String length;

        public Combo() {
            delimiter = ",";
        }

        @Override
        protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
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
            if (length != null && !length.isEmpty()) {
                tf.setMaxChars(Integer.valueOf(length));
            }
            AutoCompletionList acList = tf.getAutoCompletionList();
            if (acList != null) {
                acList.add(getDisplayValues(), AutoCompletionItemPriority.IS_IN_STANDARD);
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
                if ("force".equals(use_last_as_default) && lastValue.containsKey(key) && !presetInitiallyMatches) {
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
        protected String getDisplayIfNull() {
            if (combo.isEditable())
                return combo.getEditor().getItem().toString();
            else
                return null;
        }
    }
    public static class MultiSelect extends ComboMultiSelect {

        public long rows = -1;
        protected ConcatenatingJList list;

        @Override
        protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
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

        @Override
        public void addCommands(List<Tag> changedTags) {
            // Do not create any commands if list has been disabled because of an unknown value (fix #8605)
            if (list.isEnabled()) {
                super.addCommands(changedTags);
            }
        }
    }

    /**
    * Class that allows list values to be assigned and retrieved as a comma-delimited
    * string (extracted from TaggingPreset)
    */
    private static class ConcatenatingJList extends JList {
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
                TreeSet<String> parts = new TreeSet<String>(Arrays.asList(s.split(delimiter)));
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
                setEnabled(Utils.join(delimiter, parts).equals(getSelectedItem()));
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
    static public EnumSet<TaggingPresetType> getType(String types) throws SAXException {
        if (typeCache.containsKey(types))
            return typeCache.get(types);
        EnumSet<TaggingPresetType> result = EnumSet.noneOf(TaggingPresetType.class);
        for (String type : Arrays.asList(types.split(","))) {
            try {
                TaggingPresetType presetType = TaggingPresetType.fromString(type);
                result.add(presetType);
            } catch (IllegalArgumentException e) {
                throw new SAXException(tr("Unknown type: {0}", type), e);
            }
        }
        typeCache.put(types, result);
        return result;
    }

    static String fixPresetString(String s) {
        return s == null ? s : s.replaceAll("'","''");
    }

    /**
     * allow escaped comma in comma separated list:
     * "A\, B\, C,one\, two" --&gt; ["A, B, C", "one, two"]
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
    protected static ImageIcon loadImageIcon(String iconName, File zipIcons, Integer maxSize) {
        final Collection<String> s = Main.pref.getCollection("taggingpreset.icon.sources", null);
        ImageProvider imgProv = new ImageProvider(iconName).setDirs(s).setId("presets").setArchive(zipIcons).setOptional(true);
        if (maxSize != null) {
            imgProv.setMaxSize(maxSize);
        }
        return imgProv.get();
    }
}
