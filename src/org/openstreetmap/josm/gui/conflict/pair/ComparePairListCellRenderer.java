// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.pair;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.openstreetmap.josm.gui.conflict.ConflictColors;

public class ComparePairListCellRenderer extends JLabel implements ListCellRenderer {
    public ComparePairListCellRenderer() {
        setOpaque(true);
    }
    @Override
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
    {
        ComparePairType type = (ComparePairType)value;
        setText(type.getDisplayName());
        setBackground(isSelected ? ConflictColors.BGCOLOR_SELECTED.get() : ConflictColors.BGCOLOR.get());
        setForeground(ConflictColors.FGCOLOR.get());
        return this;
    }
}
