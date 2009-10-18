// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;

import javax.swing.JTable;

public class MemberTableLinkedCellRenderer extends MemberTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        reset();

        renderForeground(isSelected);
        setText(value.toString());
        setToolTipText(((WayConnectionType)value).getToolTip());
        renderBackground(getModel(table), null, isSelected);
        return this;
    }
}
