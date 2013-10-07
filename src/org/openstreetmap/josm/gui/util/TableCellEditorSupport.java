// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

public class TableCellEditorSupport {
    private Object editor;
    private List<CellEditorListener> listeners;

    public TableCellEditorSupport(Object editor) {
        this.editor = editor;
        listeners = new LinkedList<CellEditorListener>();
    }

    protected List<CellEditorListener> getListeners() {
        synchronized (this) {
            return new ArrayList<CellEditorListener>(listeners);
        }
    }

    public void addCellEditorListener(CellEditorListener l) {
        synchronized (this) {
            if (l != null && ! listeners.contains(l)) {
                listeners.add(l);
            }
        }
    }
    public void removeCellEditorListener(CellEditorListener l) {
        synchronized (this) {
            if (l != null &&listeners.contains(l)) {
                listeners.remove(l);
            }
        }
    }

    public void fireEditingCanceled() {
        for (CellEditorListener listener: getListeners()) {
            listener.editingCanceled(new ChangeEvent(editor));
        }
    }

    public void fireEditingStopped() {
        for (CellEditorListener listener: getListeners()) {
            listener.editingStopped(new ChangeEvent(editor));
        }
    }
}
