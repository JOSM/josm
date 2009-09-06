// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.Component;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

public class RelationMemberConflictDecisionRenderer extends JLabel implements TableCellRenderer, ListCellRenderer{

    protected void resetTableRenderer() {
        setOpaque(true);
        setFont(UIManager.getFont("Table.font"));
        setBackground(UIManager.getColor("Table.background"));
        setForeground(UIManager.getColor("Table.foreground"));
    }

    protected void resetListRenderer() {
        setOpaque(true);
        setFont(UIManager.getFont("ComboBox.font"));
        setBackground(UIManager.getColor("ComboBox.background"));
        setForeground(UIManager.getColor("ComboBox.foreground"));
    }


    /* --------------------------------------------------------------------------------- */
    /* TableCellRenderer                                                                 */
    /* --------------------------------------------------------------------------------- */
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        resetTableRenderer();
        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        }
        RelationMemberConflictDecisionType decision = (RelationMemberConflictDecisionType)value;
        RelationMemberConflictDecisionType.prepareLabel(decision, this);
        return this;
    }

    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
        resetListRenderer();
        if (isSelected) {
            setBackground(UIManager.getColor("ComboBox.selectionBackground"));
            setForeground(UIManager.getColor("ComboBox.selectionForeground"));
        }
        RelationMemberConflictDecisionType decision = (RelationMemberConflictDecisionType)value;
        RelationMemberConflictDecisionType.prepareLabel(decision, this);
        if (RelationMemberConflictDecisionType.UNDECIDED.equals(decision)) {
            setFont(getFont().deriveFont(Font.ITALIC));
        }
        return this;
    }
}
