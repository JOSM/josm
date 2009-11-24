// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;

/**
 * This is {@see TableCellEditor} for a boolean flag. It is used in two table columns of
 * {@see SaveLayersTable} and renders the values of {@see SaveLayerInfo#isDoSaveToFile()}
 * and {@see SaveLayerInfo#isDoUploadToServer()}
 *
 */
class SaveFlagCellEditor extends JCheckBox implements TableCellEditor {
    private CopyOnWriteArrayList<CellEditorListener> listeners;
    private boolean value;

    public SaveFlagCellEditor() {
        listeners = new CopyOnWriteArrayList<CellEditorListener>();
        addMouseListener(
                new MouseAdapter() {
                    @Override
                    public void mouseExited(MouseEvent e) {
                        stopCellEditing();
                    }
                }
        );
    }

    public void addCellEditorListener(CellEditorListener l) {
        if (!listeners.contains(l)) {
            listeners.add(l);
        }
    }

    protected void fireEditingCanceled() {
        for (CellEditorListener l: listeners) {
            l.editingCanceled(new ChangeEvent(this));
        }
    }

    protected void fireEditingStopped() {
        for (CellEditorListener l: listeners) {
            l.editingStopped(new ChangeEvent(this));
        }
    }

    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    public Object getCellEditorValue() {
        return value;
    }

    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    public void removeCellEditorListener(CellEditorListener l) {
        if (listeners.contains(l)) {
            listeners.remove(l);
        }
    }

    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    public void setInitialValue(boolean value) {
        this.value = value;
        setSelected(value);
    }

    public boolean stopCellEditing() {
        this.value = isSelected();
        fireEditingStopped();
        return true;
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        SaveLayerInfo info = (SaveLayerInfo)value;
        switch(column) {
            case 4: setInitialValue(info.isDoUploadToServer()); break;
            case 5: setInitialValue(info.isDoSaveToFile()); break;
        }
        return this;
    }
}
