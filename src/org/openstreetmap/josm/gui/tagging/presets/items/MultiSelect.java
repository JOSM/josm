// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.openstreetmap.josm.gui.tagging.presets.TaggingPresetItemGuiSupport;
import org.openstreetmap.josm.gui.widgets.OrientationAction;
import org.openstreetmap.josm.tools.GBC;

/**
 * Multi-select list type.
 */
public class MultiSelect extends ComboMultiSelect {

    /**
     * Number of rows to display (positive integer, optional).
     */
    public short rows; // NOSONAR

    /** The model for the JList */
    protected final DefaultListModel<PresetListEntry> model = new DefaultListModel<>();
    /** The swing component */
    protected final JList<PresetListEntry> list = new JList<>(model);

    private void addEntry(PresetListEntry entry) {
        if (!seenValues.containsKey(entry.value)) {
            model.addElement(entry);
            seenValues.put(entry.value, entry);
        }
    }

    @Override
    protected boolean addToPanel(JPanel p, TaggingPresetItemGuiSupport support) {
        initializeLocaleText(null);
        usage = determineTextUsage(support.getSelected(), key);
        seenValues = new TreeMap<>();
        initListEntries();

        model.clear();
        if (!usage.hasUniqueValue() && !usage.unused()) {
            addEntry(PresetListEntry.ENTRY_DIFFERENT);
        }

        String initialValue = getInitialValue(default_);
        if (initialValue != null) {
            // add all values already present to the list, otherwise we would remove all
            // custom entries unknown to the preset
            for (String value : initialValue.split(String.valueOf(delimiter), -1)) {
                PresetListEntry e = new PresetListEntry(value, this);
                addEntry(e);
                int i = model.indexOf(e);
                list.addSelectionInterval(i, i);
            }
        }

        presetListEntries.forEach(this::addEntry);

        ComboMultiSelectListCellRenderer renderer = new ComboMultiSelectListCellRenderer(list, list.getCellRenderer(), 200, key);
        list.setCellRenderer(renderer);

        if (rows > 0) {
            list.setVisibleRowCount(rows);
        }
        JLabel label = addLabel(p);
        p.add(new JScrollPane(list), GBC.eol().fill(GBC.HORIZONTAL)); // NOSONAR
        label.setLabelFor(list);

        list.addListSelectionListener(l -> support.fireItemValueModified(this, key, getSelectedItem().value));
        list.setToolTipText(getKeyTooltipText());
        list.applyComponentOrientation(OrientationAction.getValueOrientation(key));

        return true;
    }

    @Override
    protected PresetListEntry getSelectedItem() {
        return new PresetListEntry(list.getSelectedValuesList()
            .stream().map(e -> e.value).collect(Collectors.joining(String.valueOf(delimiter))), this);
    }
}
