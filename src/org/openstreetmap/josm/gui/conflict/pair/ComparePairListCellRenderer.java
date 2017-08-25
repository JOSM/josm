// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.gui.conflict.ConflictColors;

/**
 * This {@link ListCellRenderer} renders the value of a {@link ComparePairType}
 */
public class ComparePairListCellRenderer extends JLabel implements ListCellRenderer<ComparePairType> {

    /**
     * Constructs a new {@code ComparePairListCellRenderer}.
     */
    public ComparePairListCellRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends ComparePairType> list,
            ComparePairType value,
            int index,
            boolean isSelected,
            boolean cellHasFocus) {
        setText(value.getDisplayName());
        setBackground(isSelected ? ConflictColors.BGCOLOR_SELECTED.get() : ConflictColors.BGCOLOR.get());
        setForeground(ConflictColors.FGCOLOR.get());
        return this;
    }
}
