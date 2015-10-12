// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionManager;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.gui.widgets.JosmTextField;
import org.openstreetmap.josm.tools.GBC;

/**
 * Text field type.
 */
public class Text extends KeyedItem {

    private static int auto_increment_selected;

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
                    textField.setText(Integer.toString(Integer.parseInt(
                            LAST_VALUES.get(key)) + auto_increment_selected));
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
