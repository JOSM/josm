// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JPanel;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.mapcss.CSSColors;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.GBC;

/**
 * Combobox type.
 */
public class Combo extends ComboMultiSelect {

    /**
     * Whether the combo box is editable, which means that the user can add other values as text.
     * Default is {@code true}. If {@code false} it is readonly, which means that the user can only select an item in the list.
     */
    public boolean editable = true; // NOSONAR
    /** The length of the combo box (number of characters allowed). */
    public short length; // NOSONAR

    protected JosmComboBox<PresetListEntry> combobox;

    /**
     * Constructs a new {@code Combo}.
     */
    public Combo() {
        delimiter = ',';
    }

    @Override
    protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
        if (!usage.unused()) {
            for (String s : usage.values) {
                presetListEntries.add(new PresetListEntry(s));
            }
        }
        if (def != null) {
            presetListEntries.add(new PresetListEntry(def));
        }
        presetListEntries.add(new PresetListEntry(""));

        combobox = new JosmComboBox<>(presetListEntries.toArray(new PresetListEntry[0]));
        component = combobox;
        combobox.setRenderer(getListCellRenderer());
        combobox.setEditable(true); // fix incorrect height, see #6157
        combobox.reinitialize(presetListEntries);
        combobox.setEditable(editable); // see #6157
        AutoCompletingTextField tf = new AutoCompletingTextField();
        initAutoCompletionField(tf, key);
        if (Config.getPref().getBoolean("taggingpreset.display-keys-as-hint", true)) {
            tf.setHint(key);
        }
        if (length > 0) {
            tf.setMaxChars((int) length);
        }
        AutoCompletionList acList = tf.getAutoCompletionList();
        if (acList != null) {
            acList.add(getDisplayValues(), AutoCompletionPriority.IS_IN_STANDARD);
        }
        combobox.setEditor(tf);

        if (usage.hasUniqueValue()) {
            // all items have the same value (and there were no unset items)
            originalValue = getListEntry(usage.getFirst());
            combobox.setSelectedItem(originalValue);
        } else if (def != null && usage.unused()) {
            // default is set and all items were unset
            if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || isForceUseLastAsDefault()) {
                // selected osm primitives are untagged or filling default feature is enabled
                combobox.setSelectedItem(getListEntry(def).getDisplayValue());
            } else {
                // selected osm primitives are tagged and filling default feature is disabled
                combobox.setSelectedItem("");
            }
            originalValue = getListEntry(DIFFERENT);
        } else if (usage.unused()) {
            // all items were unset (and so is default)
            originalValue = getListEntry("");
            if (!presetInitiallyMatches && isForceUseLastAsDefault() && LAST_VALUES.containsKey(key)) {
                combobox.setSelectedItem(getListEntry(LAST_VALUES.get(key)));
            } else {
                combobox.setSelectedItem(originalValue);
            }
        } else {
            originalValue = getListEntry(DIFFERENT);
            combobox.setSelectedItem(originalValue);
        }
        if (key != null && ("colour".equals(key) || key.startsWith("colour:") || key.endsWith(":colour"))) {
            p.add(combobox, GBC.std().fill(GBC.HORIZONTAL));
            JButton button = new JButton(new ChooseColorAction());
            button.setOpaque(true);
            button.setBorderPainted(false);
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            p.add(button, GBC.eol().fill(GBC.VERTICAL));
            ActionListener updateColor = ignore -> button.setBackground(getColor());
            updateColor.actionPerformed(null);
            combobox.addActionListener(updateColor);
        } else {
            p.add(combobox, GBC.eol().fill(GBC.HORIZONTAL));
        }
    }

    class ChooseColorAction extends AbstractAction {
        ChooseColorAction() {
            putValue(SHORT_DESCRIPTION, tr("Choose a color"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Color color = getColor();
            color = JColorChooser.showDialog(MainApplication.getMainPanel(), tr("Choose a color"), color);
            setColor(color);
        }
    }

    protected void setColor(Color color) {
        if (color != null) {
            combobox.setSelectedItem(ColorHelper.color2html(color));
        }
    }

    protected Color getColor() {
        String colorString = String.valueOf(getSelectedValue());
        return colorString.startsWith("#")
                ? ColorHelper.html2color(colorString)
                : CSSColors.get(colorString);
    }

    @Override
    protected Object getSelectedItem() {
        return combobox.getSelectedItem();
    }

    @Override
    protected String getDisplayIfNull() {
        if (combobox.isEditable())
            return combobox.getEditor().getItem().toString();
        else
            return null;
    }
}
