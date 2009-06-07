// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ComparePairListCellRenderer extends JLabel implements ListCellRenderer {
    public final static Color BGCOLOR_SELECTED = new Color(143,170,255);

    public ComparePairListCellRenderer() {
        setOpaque(true);
    }
    public Component getListCellRendererComponent(
            JList list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus)
    {
        ComparePairType type = (ComparePairType)value;
        setText(type.getDisplayName());
        setBackground(isSelected ? BGCOLOR_SELECTED : Color.WHITE);
        setForeground(Color.BLACK);
        return this;
    }
}
