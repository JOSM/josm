// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.relation;

import java.awt.Component;

import javax.swing.JTable;

public class MemberTableRoleCellRenderer extends MemberTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {

        reset();
        if (value == null)
            return this;

        renderForeground(isSelected);
        String role = (String) value;
        renderBackground(getModel(table), null, isSelected);
        setText(role);
        return this;
    }
}
