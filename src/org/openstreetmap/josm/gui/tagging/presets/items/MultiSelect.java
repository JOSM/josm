// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
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
        seenValues.clear();
        initListEntries();

        model.clear();
        // disable if the selected primitives have different values
        list.setEnabled(usage.hasUniqueValue() || usage.unused());
        String initialValue = getInitialValue(usage);

        // Add values from the preset.
        presetListEntries.forEach(this::addEntry);

        // Add all values used in the selected primitives. This also adds custom values and makes
        // sure we won't lose them.
        usage = usage.splitValues(String.valueOf(delimiter));
        for (String value: usage.map.keySet()) {
            addEntry(new PresetListEntry(value, this));
        }

        // Select the values in the initial value.
        if (initialValue != null && !DIFFERENT.equals(initialValue)) {
            for (String value : initialValue.split(String.valueOf(delimiter), -1)) {
                PresetListEntry e = new PresetListEntry(value, this);
                addEntry(e);
                int i = model.indexOf(e);
                list.addSelectionInterval(i, i);
            }
        }

        ComboMultiSelectListCellRenderer renderer = new ComboMultiSelectListCellRenderer(list, list.getCellRenderer(), 200, key);
        list.setCellRenderer(renderer);
        JLabel label = addLabel(p);
        label.setLabelFor(list);
        JScrollPane sp = new JScrollPane(list);

        if (rows > 0) {
            list.setVisibleRowCount(rows);
            // setVisibleRowCount() only works when all cells have the same height, but sometimes we
            // have icons of different sizes. Calculate the size of the first {@code rows} entries
            // and size the scrollpane accordingly.
            Rectangle r = list.getCellBounds(0, Math.min(rows, model.size() - 1));
            Insets insets = list.getInsets();
            r.width += insets.left + insets.right;
            r.height += insets.top + insets.bottom;
            insets = sp.getInsets();
            r.width += insets.left + insets.right;
            r.height += insets.top + insets.bottom;
            sp.setPreferredSize(new Dimension(r.width, r.height));
        }
        p.add(sp, GBC.eol().fill(GBC.HORIZONTAL)); // NOSONAR

        list.addListSelectionListener(l -> support.fireItemValueModified(this, key, getSelectedItem().value));
        list.setToolTipText(getKeyTooltipText());
        list.applyComponentOrientation(OrientationAction.getValueOrientation(key));

        return true;
    }

    @Override
    protected PresetListEntry getSelectedItem() {
        return new PresetListEntry(list.getSelectedValuesList()
            .stream().map(e -> e.value).distinct().sorted().collect(Collectors.joining(String.valueOf(delimiter))), this);
    }
}
