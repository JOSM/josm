// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.conflict.tags;

import java.awt.Component;
import java.util.EventObject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;

import org.openstreetmap.josm.gui.util.TableCellEditorSupport;

public class RelationMemberConflictDecisionEditor extends JComboBox implements TableCellEditor {

    public RelationMemberConflictDecisionEditor() {
        setOpaque(true);
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(RelationMemberConflictDecisionType.KEEP);
        model.addElement(RelationMemberConflictDecisionType.REMOVE);
        model.addElement(RelationMemberConflictDecisionType.UNDECIDED);
        setModel(model);
        setRenderer(new RelationMemberConflictDecisionRenderer());
        tableCellEditorSupport = new TableCellEditorSupport(this);
    }
    /* --------------------------------------------------------------------------------- */
    /* TableCellEditor                                                                   */
    /* --------------------------------------------------------------------------------- */
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        setSelectedItem(value);
        this.originalValue = (RelationMemberConflictDecisionType)value;
        return this;
    }

    private TableCellEditorSupport tableCellEditorSupport;
    private RelationMemberConflictDecisionType originalValue;

    public void addCellEditorListener(CellEditorListener l) {
        tableCellEditorSupport.addCellEditorListener(l);
    }

    public void cancelCellEditing() {
        setSelectedItem(originalValue);
        tableCellEditorSupport.fireEditingCanceled();
    }

    public Object getCellEditorValue() {
        return getSelectedItem();
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
        tableCellEditorSupport.removeCellEditorListener(l);
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public boolean stopCellEditing() {
        tableCellEditorSupport.fireEditingStopped();
        return true;
    }
}
