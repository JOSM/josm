// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import javax.swing.JPanel;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.spi.preferences.Config;
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
                if (!lhm.containsKey(s)) {
                    lhm.put(s, new PresetListEntry(s));
                }
            }
        }
        if (def != null && !lhm.containsKey(def)) {
            lhm.put(def, new PresetListEntry(def));
        }
        if (!lhm.containsKey("")) {
            lhm.put("", new PresetListEntry(""));
        }

        combobox = new JosmComboBox<>(lhm.values().toArray(new PresetListEntry[lhm.size()]));
        component = combobox;
        combobox.setRenderer(getListCellRenderer());
        combobox.setEditable(editable);
        combobox.reinitialize(lhm.values());
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
            originalValue = lhm.get(usage.getFirst());
            combobox.setSelectedItem(originalValue);
        } else if (def != null && usage.unused()) {
            // default is set and all items were unset
            if (!usage.hadKeys() || PROP_FILL_DEFAULT.get() || isForceUseLastAsDefault()) {
                // selected osm primitives are untagged or filling default feature is enabled
                combobox.setSelectedItem(lhm.get(def).getDisplayValue(true));
            } else {
                // selected osm primitives are tagged and filling default feature is disabled
                combobox.setSelectedItem("");
            }
            originalValue = lhm.get(DIFFERENT);
        } else if (usage.unused()) {
            // all items were unset (and so is default)
            originalValue = lhm.get("");
            if (!presetInitiallyMatches && isForceUseLastAsDefault() && LAST_VALUES.containsKey(key)) {
                combobox.setSelectedItem(lhm.get(LAST_VALUES.get(key)));
            } else {
                combobox.setSelectedItem(originalValue);
            }
        } else {
            originalValue = lhm.get(DIFFERENT);
            combobox.setSelectedItem(originalValue);
        }
        p.add(combobox, GBC.eol().fill(GBC.HORIZONTAL));
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
