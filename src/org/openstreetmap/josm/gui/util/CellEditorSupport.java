// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.CellEditor;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;

/**
 * Utility class used to ease implementation of {@link CellEditor} interface,
 * or one of its sub-interfaces, for classes that cannot extend {@link javax.swing.AbstractCellEditor}.
 * @since 6912
 */
public class CellEditorSupport {
    private final CellEditor editor;
    private final Set<CellEditorListener> listeners;

    /**
     * Constructs a new {@code CellEditorSupport}.
     * @param editor The cell editor backed by this
     */
    public CellEditorSupport(CellEditor editor) {
        this.editor = editor;
        this.listeners = new LinkedHashSet<>();
    }

    protected List<CellEditorListener> getListeners() {
        synchronized (this) {
            return new ArrayList<>(listeners);
        }
    }

    /**
     * Worker for {@link CellEditor#addCellEditorListener(CellEditorListener)} method.
     * @param l the CellEditorListener
     */
    public final void addCellEditorListener(CellEditorListener l) {
        synchronized (this) {
            if (l != null) {
                listeners.add(l);
            }
        }
    }

    /**
     * Worker for {@link CellEditor#removeCellEditorListener(CellEditorListener)} method.
     * @param l the CellEditorListener
     */
    public final void removeCellEditorListener(CellEditorListener l) {
        synchronized (this) {
            if (l != null) {
                listeners.remove(l);
            }
        }
    }

    /**
     * Fires "editing canceled" event to listeners.
     */
    public final void fireEditingCanceled() {
        for (CellEditorListener listener: getListeners()) {
            listener.editingCanceled(new ChangeEvent(editor));
        }
    }

    /**
     * Fires "editing stopped" event to listeners.
     */
    public final void fireEditingStopped() {
        for (CellEditorListener listener: getListeners()) {
            listener.editingStopped(new ChangeEvent(editor));
        }
    }
}
