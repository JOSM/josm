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

import org.openstreetmap.josm.gui.widgets.JosmComboBox;

/**
 * This class renders {@link RelationMemberConflictDecisionType} entries of a list/table
 */
public class RelationMemberConflictDecisionRenderer extends JLabel
implements TableCellRenderer, ListCellRenderer<RelationMemberConflictDecisionType> {

    private final JosmComboBox<RelationMemberConflictDecisionType> cbDecisionTypes;

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

    /**
     * Constructs a new {@code RelationMemberConflictDecisionRenderer}.
     */
    public RelationMemberConflictDecisionRenderer() {
        cbDecisionTypes = new JosmComboBox<>(RelationMemberConflictDecisionType.values());
        cbDecisionTypes.setRenderer(this);
    }

    /* --------------------------------------------------------------------------------- */
    /* TableCellRenderer                                                                 */
    /* --------------------------------------------------------------------------------- */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        resetTableRenderer();
        if (value == null)
            return this;

        if (isSelected) {
            setBackground(UIManager.getColor("Table.selectionBackground"));
            setForeground(UIManager.getColor("Table.selectionForeground"));
        }
        RelationMemberConflictDecisionType decision = (RelationMemberConflictDecisionType) value;
        cbDecisionTypes.setSelectedItem(decision);
        return cbDecisionTypes;
    }

    /* --------------------------------------------------------------------------------- */
    /* ListCellRenderer                                                                  */
    /* --------------------------------------------------------------------------------- */
    @Override
    public Component getListCellRendererComponent(
            JList<? extends RelationMemberConflictDecisionType> list,
            RelationMemberConflictDecisionType decision, int index, boolean isSelected,
            boolean cellHasFocus) {
        resetListRenderer();
        if (isSelected) {
            setBackground(UIManager.getColor("ComboBox.selectionBackground"));
            setForeground(UIManager.getColor("ComboBox.selectionForeground"));
        }
        RelationMemberConflictDecisionType.prepareLabel(decision, this);
        if (RelationMemberConflictDecisionType.UNDECIDED.equals(decision)) {
            setFont(getFont().deriveFont(Font.ITALIC));
        }
        return this;
    }
}
