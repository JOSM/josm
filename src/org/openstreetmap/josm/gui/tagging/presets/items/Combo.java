// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletingTextField;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionItemPriority;
import org.openstreetmap.josm.gui.tagging.ac.AutoCompletionList;
import org.openstreetmap.josm.gui.widgets.JosmComboBox;
import org.openstreetmap.josm.tools.GBC;

/**
 * Combobox type.
 */
public class Combo extends ComboMultiSelect {

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
