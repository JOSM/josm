// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trc;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
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
import org.openstreetmap.josm.actions.search.SearchAction;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.preferences.BooleanProperty;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPriority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.gui.widgets.QuadStateCheckBox;
import org.openstreetmap.josm.gui.widgets.UrlLabel;
import org.openstreetmap.josm.tools.AlphanumComparator;
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
    private TaggingPresetItems() {
    }

    private static int auto_increment_selected;
    /** Translatation of "&lt;different&gt;". Use in combo boxes to display en entry matching several different values. */
    public static final String DIFFERENT = tr("<different>");

    private static final BooleanProperty PROP_FILL_DEFAULT = new BooleanProperty("taggingpreset.fill-default-for-tagged-primitives", false);

    // cache the parsing of types using a LRU cache (http://java-planet.blogspot.com/2005/08/how-to-set-up-simple-lru-cache-using.html)
    private static final Map<String, Set<TaggingPresetType>> TYPE_CACHE = new LinkedHashMap<>(16, 1.1f, true);

    /**
     * Last value of each key used in presets, used for prefilling corresponding fields
     */
    private static final Map<String, String> LAST_VALUES = new HashMap<>();

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
        private int prefferedWidth = -1;
        private int prefferedHeight = -1;

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

        /**
         * Constructs a new {@code PresetListEntry}, uninitialized.
         */
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

        @Override
        public int compareTo(PresetListEntry o) {
            return AlphanumComparator.getInstance().compare(this.getDisplayValue(true), o.getDisplayValue(true));
        }
    }

    public static class Role {
        public Set<TaggingPresetType> types;
        public String key;
        /** The text to display */
        public String text;
        /** The context used for translating {@link #text} */
        public String text_context;
        /** The localized version of {@link #text}. */
        public String locale_text;
        public SearchCompiler.Match memberExpression;

        public boolean required;
        private long count;

        public void setType(String types) throws SAXException {
            this.types = getType(types);
        }

        public void setRequisite(String str) throws SAXException {
            if ("required".equals(str)) {
                required = true;
            } else if (!"optional".equals(str))
                throw new SAXException(tr("Unknown requisite: {0}", str));
        }

        public void setMember_expression(String member_expression) throws SAXException {
            try {
                final SearchAction.SearchSetting searchSetting = new SearchAction.SearchSetting();
                searchSetting.text = member_expression;
                searchSetting.caseSensitive = true;
                searchSetting.regexSearch = true;
                this.memberExpression = SearchCompiler.compile(searchSetting);
            } catch (SearchCompiler.ParseError ex) {
                throw new SAXException(tr("Illegal member expression: {0}", ex.getMessage()), ex);
            }
        }

        public void setCount(String count) {
            this.count = Long.parseLong(count);
        }

        /**
         * Return either argument, the highest possible value or the lowest allowed value
         */
        public long getValidCount(long c) {
            if (count > 0 && !required)
                return c != 0 ? count : 0;
            else if (count > 0)
                return count;
            else if (!required)
                return c != 0 ? c : 0;
            else
                return c != 0 ? c : 1;
        }

        public boolean addToPanel(JPanel p) {
            String cstring;
            if (count > 0 && !required) {
                cstring = "0,"+count;
            } else if (count > 0) {
                cstring = String.valueOf(count);
            } else if (!required) {
                cstring = "0-...";
            } else {
                cstring = "1-...";
            }
            if (locale_text == null) {
                locale_text = getLocaleText(text, text_context, null);
            }
            p.add(new JLabel(locale_text+':'), GBC.std().insets(0, 0, 10, 0));
            p.add(new JLabel(key), GBC.std().insets(0, 0, 10, 0));
            p.add(new JLabel(cstring), types == null ? GBC.eol() : GBC.std().insets(0, 0, 10, 0));
            if (types != null) {
                JPanel pp = new JPanel();
                for (TaggingPresetType t : types) {
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
    public enum MatchType {

        /** Neutral, i.e., do not consider this item for matching. */
        NONE("none"),
        /** Positive if key matches, neutral otherwise. */
        KEY("key"),
        /** Positive if key matches, negative otherwise. */
        KEY_REQUIRED("key!"),
        /** Positive if key and value matches, neutral otherwise. */
        KEY_VALUE("keyvalue"),
        /** Positive if key and value matches, negative otherwise. */
        KEY_VALUE_REQUIRED("keyvalue!");

        private final String value;

        MatchType(String value) {
            this.value = value;
        }

        /**
         * Replies the associated textual value.
         * @return the associated textual value
         */
        public String getValue() {
            return value;
        }

        /**
         * Determines the {@code MatchType} for the given textual value.
         * @param type the textual value
         * @return the {@code MatchType} for the given textual value
         */
        public static MatchType ofString(String type) {
            for (MatchType i : EnumSet.allOf(MatchType.class)) {
                if (i.getValue().equals(type))
                    return i;
            }
            throw new IllegalArgumentException(type + " is not allowed");
        }
    }

    public static class Usage {
        private SortedSet<String> values;
        private boolean hadKeys;
        private boolean hadEmpty;

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
    public abstract static class TaggingPresetTextItem extends TaggingPresetItem {

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
        void addCommands(List<Tag> changedTags) {
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

    /**
     * Label type.
     */
    public static class Label extends TaggingPresetTextItem {

        /** The location of icon file to display (optional) */
        public String icon;
        /** The size of displayed icon. If not set, default is 16px */
        public String icon_size;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            initializeLocaleText(null);
            addLabel(p, getIcon(), locale_text);
            return true;
        }

        /**
         * Adds a new {@code JLabel} to the given panel.
         * @param p The panel
         * @param icon the icon (optional, can be null)
         * @param label The text label
         */
        public static void addLabel(JPanel p, Icon icon, String label) {
            p.add(new JLabel(label, icon, JLabel.LEADING), GBC.eol().fill(GBC.HORIZONTAL));
        }

        /**
         * Returns the label icon, if any.
         * @return the label icon, or {@code null}
         */
        public ImageIcon getIcon() {
            Integer size = parseInteger(icon_size);
            return icon == null ? null : loadImageIcon(icon, TaggingPresetReader.getZipIcons(), size != null ? size : 16);
        }
    }

    /**
     * Hyperlink type.
     */
    public static class Link extends TaggingPresetTextItem {

        /** The link to display. */
        public String href;

        /** The localized version of {@link #href}. */
        public String locale_href;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {
            initializeLocaleText(tr("More information about this feature"));
            String url = locale_href;
            if (url == null) {
                url = href;
            }
            if (url != null) {
                p.add(new UrlLabel(url, locale_text, 2), GBC.eol().insets(0, 10, 0, 0).fill(GBC.HORIZONTAL));
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
            final TaggingPreset t = Utils.filter(TaggingPresets.getTaggingPresets(), new Predicate<TaggingPreset>() {
                @Override
                public boolean evaluate(TaggingPreset object) {
                    return presetName.equals(object.name);
                }
            }).iterator().next();
            if (t == null) return false;
            JLabel lbl = new PresetLabel(t);
            lbl.addMouseListener(new MouseAdapter() {
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

        public final List<Role> roles = new LinkedList<>();

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
                    i.addToPanel(proles);
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

    /**
     * Horizontal separator type.
     */
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

    /**
     * Preset item associated to an OSM key.
     */
    public abstract static class KeyedItem extends TaggingPresetItem {

        public String key;
        /** The text to display */
        public String text;
        /** The context used for translating {@link #text} */
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
                return tags.containsKey(key) ? Boolean.TRUE : null;
            case KEY_REQUIRED:
                return tags.containsKey(key);
            case KEY_VALUE:
                return tags.containsKey(key) && getValues().contains(tags.get(key)) ? Boolean.TRUE : null;
            case KEY_VALUE_REQUIRED:
                return tags.containsKey(key) && getValues().contains(tags.get(key));
            default:
                throw new IllegalStateException();
            }
        }

        @Override
        public String toString() {
            return "KeyedItem [key=" + key + ", text=" + text
                    + ", text_context=" + text_context + ", match=" + match
                    + ']';
        }
    }

    /**
     * Invisible type allowing to hardcode an OSM key/value from the preset definition.
     */
    public static class Key extends KeyedItem {

        /** The hardcoded value for key */
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
            return MatchType.KEY_VALUE_REQUIRED;
        }

        @Override
        public Collection<String> getValues() {
            return Collections.singleton(value);
        }

        @Override
        public String toString() {
            return "Key [key=" + key + ", value=" + value + ", text=" + text
                    + ", text_context=" + text_context + ", match=" + match
                    + ']';
        }
    }

    /**
     * Text field type.
     */
    public static class Text extends KeyedItem {

        /** The localized version of {@link #text}. */
        public String locale_text;
        public String default_;
        public String originalValue;
        public String use_last_as_default = "false";
        public String auto_increment;
        public String length;
        public String alternative_autocomplete_keys;

        private JComponent value;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

            // find out if our key is already used in the selection.
            Usage usage = determineTextUsage(sel, key);
            AutoCompletingTextField textField = new AutoCompletingTextField();
            if (alternative_autocomplete_keys != null) {
                initAutoCompletionField(textField, (key + ',' + alternative_autocomplete_keys).split(","));
            } else {
                initAutoCompletionField(textField, key);
            }
            if (Main.pref.getBoolean("taggingpreset.display-keys-as-hint", true)) {
                textField.setHint(key);
            }
            if (length != null && !length.isEmpty()) {
                textField.setMaxChars(Integer.valueOf(length));
            }
            if (usage.unused()) {
                if (auto_increment_selected != 0  && auto_increment != null) {
                    try {
                        textField.setText(Integer.toString(Integer.parseInt(LAST_VALUES.get(key)) + auto_increment_selected));
                    } catch (NumberFormatException ex) {
                        // Ignore - cannot auto-increment if last was non-numeric
                        if (Main.isTraceEnabled()) {
                            Main.trace(ex.getMessage());
                        }
                    }
                } else if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || "force".equals(use_last_as_default)) {
                    // selected osm primitives are untagged or filling default values feature is enabled
                    if (!"false".equals(use_last_as_default) && LAST_VALUES.containsKey(key) && !presetInitiallyMatches) {
                        textField.setText(LAST_VALUES.get(key));
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
                JosmComboBox<String> comboBox = new JosmComboBox<>(usage.values.toArray(new String[0]));
                comboBox.setEditable(true);
                comboBox.setEditor(textField);
                comboBox.getEditor().setItem(DIFFERENT);
                value = comboBox;
                originalValue = DIFFERENT;
            }
            if (locale_text == null) {
                locale_text = getLocaleText(text, text_context, null);
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
                    aibutton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                    aibutton.setFocusable(false);
                    saveHorizontalSpace(aibutton);
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
                releasebutton.setMargin(new java.awt.Insets(0, 0, 0, 0));
                releasebutton.setFocusable(false);
                releasebutton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        auto_increment_selected = 0;
                        clearbutton.setSelected(true);
                    }
                });
                saveHorizontalSpace(releasebutton);
                pnl.add(releasebutton, GBC.eol());
                value = pnl;
            }
            p.add(new JLabel(locale_text+':'), GBC.std().insets(0, 0, 10, 0));
            p.add(value, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        private static void saveHorizontalSpace(AbstractButton button) {
            Insets insets = button.getBorder().getBorderInsets(button);
            // Ensure the current look&feel does not waste horizontal space (as seen in Nimbus & Aqua)
            if (insets != null && insets.left+insets.right > insets.top+insets.bottom) {
                int min = Math.min(insets.top, insets.bottom);
                button.setBorder(BorderFactory.createEmptyBorder(insets.top, min, insets.bottom, min));
            }
        }

        private static String getValue(Component comp) {
            if (comp instanceof JosmComboBox) {
                return ((JosmComboBox<?>) comp).getEditor().getItem().toString();
            } else if (comp instanceof JosmTextField) {
                return ((JosmTextField) comp).getText();
            } else if (comp instanceof JPanel) {
                return getValue(((JPanel) comp).getComponent(0));
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
                LAST_VALUES.put(key, v);
            }
            if (v.equals(originalValue) || (originalValue == null && v.isEmpty()))
                return;

            changedTags.add(new Tag(key, v));
            AutoCompletionManager.rememberUserInput(key, v, true);
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
        public final List<Check> checks = new LinkedList<>();

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
        Boolean matches(Map<String, String> tags) {
            for (Check check : checks) {
                if (Boolean.TRUE.equals(check.matches(tags))) {
                    return Boolean.TRUE;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "CheckGroup [columns=" + columns + ']';
        }
    }

    /**
     * Checkbox type.
     */
    public static class Check extends KeyedItem {

        /** The localized version of {@link #text}. */
        public String locale_text;
        /** the value to set when checked (default is "yes") */
        public String value_on = OsmUtils.trueval;
        /** the value to set when unchecked (default is "no") */
        public String value_off = OsmUtils.falseval;
        /** whether the off value is disabled in the dialog, i.e., only unset or yes are provided */
        public boolean disable_off;
        /** "on" or "off" or unset (default is unset) */
        public String default_; // only used for tagless objects

        private QuadStateCheckBox check;
        private QuadStateCheckBox.State initialState;
        private Boolean def;

        @Override
        public boolean addToPanel(JPanel p, Collection<OsmPrimitive> sel, boolean presetInitiallyMatches) {

            // find out if our key is already used in the selection.
            final Usage usage = determineBooleanUsage(sel, key);
            final String oneValue = usage.values.isEmpty() ? null : usage.values.last();
            def = "on".equals(default_) ? Boolean.TRUE : "off".equals(default_) ? Boolean.FALSE : null;

            if (locale_text == null) {
                locale_text = getLocaleText(text, text_context, null);
            }

            if (usage.values.size() < 2 && (oneValue == null || value_on.equals(oneValue) || value_off.equals(oneValue))) {
                if (def != null && !PROP_FILL_DEFAULT.get()) {
                    // default is set and filling default values feature is disabled - check if all primitives are untagged
                    for (OsmPrimitive s : sel) {
                        if (s.hasKeys()) {
                            def = null;
                        }
                    }
                }

                // all selected objects share the same value which is either true or false or unset,
                // we can display a standard check box.
                initialState = value_on.equals(oneValue) || Boolean.TRUE.equals(def)
                        ? QuadStateCheckBox.State.SELECTED
                        : value_off.equals(oneValue) || Boolean.FALSE.equals(def)
                        ? QuadStateCheckBox.State.NOT_SELECTED
                        : QuadStateCheckBox.State.UNSET;

            } else {
                def = null;
                // the objects have different values, or one or more objects have something
                // else than true/false. we display a quad-state check box
                // in "partial" state.
                initialState = QuadStateCheckBox.State.PARTIAL;
            }

            final List<QuadStateCheckBox.State> allowedStates = new ArrayList<>(4);
            if (QuadStateCheckBox.State.PARTIAL.equals(initialState))
                allowedStates.add(QuadStateCheckBox.State.PARTIAL);
            allowedStates.add(QuadStateCheckBox.State.SELECTED);
            if (!disable_off || value_off.equals(oneValue))
                allowedStates.add(QuadStateCheckBox.State.NOT_SELECTED);
            allowedStates.add(QuadStateCheckBox.State.UNSET);
            check = new QuadStateCheckBox(locale_text, initialState,
                    allowedStates.toArray(new QuadStateCheckBox.State[allowedStates.size()]));

            p.add(check, GBC.eol().fill(GBC.HORIZONTAL));
            return true;
        }

        @Override
        public void addCommands(List<Tag> changedTags) {
            // if the user hasn't changed anything, don't create a command.
            if (check.getState() == initialState && def == null) return;

            // otherwise change things according to the selected value.
            changedTags.add(new Tag(key,
                    check.getState() == QuadStateCheckBox.State.SELECTED ? value_on :
                        check.getState() == QuadStateCheckBox.State.NOT_SELECTED ? value_off :
                            null));
        }

        @Override
        boolean requestFocusInWindow() {
            return check.requestFocusInWindow();
        }

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.NONE;
        }

        @Override
        public Collection<String> getValues() {
            return disable_off ? Arrays.asList(value_on) : Arrays.asList(value_on, value_off);
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
                            + ", " : "") + "def=" + def + ']';
        }
    }

    /**
     * Abstract superclass for combo box and multi-select list types.
     */
    public abstract static class ComboMultiSelect extends KeyedItem {

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

        @Override
        boolean requestFocusInWindow() {
            return component.requestFocusInWindow();
        }

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

        protected ListCellRenderer<PresetListEntry> getListCellRenderer() {
            return RENDERER;
        }

        @Override
        public MatchType getDefaultMatch() {
            return MatchType.NONE;
        }
    }

    /**
     * Combobox type.
     */
    public static class Combo extends ComboMultiSelect {

        public boolean editable = true;
        protected JosmComboBox<PresetListEntry> combo;
        public String length;

        /**
         * Constructs a new {@code Combo}.
         */
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

            combo = new JosmComboBox<>(lhm.values().toArray(new PresetListEntry[0]));
            component = combo;
            combo.setRenderer(getListCellRenderer());
            combo.setEditable(editable);
            combo.reinitialize(lhm.values());
            AutoCompletingTextField tf = new AutoCompletingTextField();
            initAutoCompletionField(tf, key);
            if (Main.pref.getBoolean("taggingpreset.display-keys-as-hint", true)) {
                tf.setHint(key);
            }
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
                if ("force".equals(use_last_as_default) && LAST_VALUES.containsKey(key) && !presetInitiallyMatches) {
                    combo.setSelectedItem(lhm.get(LAST_VALUES.get(key)));
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

    /**
     * Multi-select list type.
     */
    public static class MultiSelect extends ComboMultiSelect {

        /**
         * Number of rows to display (positive integer, optional).
         */
        public String rows;
        protected ConcatenatingJList list;

        @Override
        protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
            list = new ConcatenatingJList(delimiter, lhm.values().toArray(new PresetListEntry[0]));
            component = list;
            ListCellRenderer<PresetListEntry> renderer = getListCellRenderer();
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
            if (rows != null) {
                double height = renderer.getListCellRendererComponent(list,
                        new PresetListEntry("x"), 0, false, false).getPreferredSize().getHeight() * Integer.parseInt(rows);
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
    private static class ConcatenatingJList extends JList<PresetListEntry> {
        private String delimiter;

        ConcatenatingJList(String del, PresetListEntry[] o) {
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

    public static Set<TaggingPresetType> getType(String types) throws SAXException {
        if (types == null || types.isEmpty()) {
            throw new SAXException(tr("Unknown type: {0}", types));
        }
        if (TYPE_CACHE.containsKey(types))
            return TYPE_CACHE.get(types);
        Set<TaggingPresetType> result = EnumSet.noneOf(TaggingPresetType.class);
        for (String type : Arrays.asList(types.split(","))) {
            try {
                TaggingPresetType presetType = TaggingPresetType.fromString(type);
                result.add(presetType);
            } catch (IllegalArgumentException e) {
                throw new SAXException(tr("Unknown type: {0}", type), e);
            }
        }
        TYPE_CACHE.put(types, result);
        return result;
    }

    static String fixPresetString(String s) {
        return s == null ? s : s.replaceAll("'", "''");
    }

    private static String getLocaleText(String text, String text_context, String defaultText) {
        if (text == null) {
            return defaultText;
        } else if (text_context != null) {
            return trc(text_context, fixPresetString(text));
        } else {
            return tr(fixPresetString(text));
        }
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

    static Usage determineTextUsage(Collection<OsmPrimitive> sel, String key) {
        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<>();
        for (OsmPrimitive s : sel) {
            String v = s.get(key);
            if (v != null) {
                returnValue.values.add(v);
            } else {
                returnValue.hadEmpty = true;
            }
            if (s.hasKeys()) {
                returnValue.hadKeys = true;
            }
        }
        return returnValue;
    }

    static Usage determineBooleanUsage(Collection<OsmPrimitive> sel, String key) {

        Usage returnValue = new Usage();
        returnValue.values = new TreeSet<>();
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

    protected static Integer parseInteger(String str) {
        if (str == null || str.isEmpty())
            return null;
        try {
            return Integer.valueOf(str);
        } catch (Exception e) {
            if (Main.isTraceEnabled()) {
                Main.trace(e.getMessage());
            }
        }
        return null;
    }
}
