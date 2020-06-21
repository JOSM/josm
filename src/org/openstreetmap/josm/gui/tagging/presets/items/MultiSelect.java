// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.presets.items;

import java.awt.Dimension;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.tools.GBC;

/**
 * Multi-select list type.
 */
public class MultiSelect extends ComboMultiSelect {

    /**
     * Number of rows to display (positive integer, optional).
     */
    public short rows; // NOSONAR

    protected ConcatenatingJList list;

    @Override
    protected void addToPanelAnchor(JPanel p, String def, boolean presetInitiallyMatches) {
        list = new ConcatenatingJList(delimiter, presetListEntries.toArray(new PresetListEntry[0]));
        component = list;
        ListCellRenderer<PresetListEntry> renderer = getListCellRenderer();
        list.setCellRenderer(renderer);

        if (usage.hasUniqueValue() && !usage.unused()) {
            originalValue = usage.getFirst();
            list.setSelectedItem(originalValue);
        } else if ((def != null && !usage.hadKeys()) || PROP_FILL_DEFAULT.get() || isForceUseLastAsDefault()) {
            originalValue = DIFFERENT;
            list.setSelectedItem(def);
        } else if (usage.unused()) {
            originalValue = null;
            if (!presetInitiallyMatches && isUseLastAsDefault() && LAST_VALUES.containsKey(key)) {
                list.setSelectedItem(getListEntry(LAST_VALUES.get(key)));
            } else {
                list.setSelectedItem(originalValue);
            }
        } else {
            originalValue = DIFFERENT;
            list.setSelectedItem(originalValue);
        }

        JScrollPane sp = new JScrollPane(list);
        // if a number of rows has been specified in the preset,
        // modify preferred height of scroll pane to match that row count.
        if (rows > 0) {
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
