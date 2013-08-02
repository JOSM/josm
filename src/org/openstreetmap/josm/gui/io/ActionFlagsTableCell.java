// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.tools.GBC;

/**
 * This class creates a table cell that features two checkboxes, Upload and Save. It
 * handles everything on its own, in other words it renders itself and also functions
 * as editor so the checkboxes may be set by the user.
 *
 * Intended usage is like this:
 * ActionFlagsTableCell aftc = new ActionFlagsTableCell();
 * col = new TableColumn(0);
 * col.setCellRenderer(aftc);
 * col.setCellEditor(aftc);
 */
class ActionFlagsTableCell extends JPanel implements TableCellRenderer, TableCellEditor {
    protected final JCheckBox[] checkBoxes = new JCheckBox[2];
    private CopyOnWriteArrayList<CellEditorListener> listeners;

    private ActionListener al = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            fireEditingStopped();
        }
    };

    public ActionFlagsTableCell() {
        super();
        listeners = new CopyOnWriteArrayList<CellEditorListener>();

        checkBoxes[0] = new JCheckBox(tr("Upload"));
        checkBoxes[1] = new JCheckBox(tr("Save"));
        setLayout(new GridBagLayout());

        ActionMap am = getActionMap();
        for (final JCheckBox b : checkBoxes) {
            add(b, GBC.eol().fill(GBC.HORIZONTAL));
            b.setPreferredSize(new Dimension(b.getPreferredSize().width, 19));
            b.addActionListener(al);
            am.put(b.getText(), new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    b.setSelected(!b.isSelected());
                    fireEditingStopped();
                }
            });
        }

        setToolTipText(tr("<html>Select which actions to perform for this layer, if you click the leftmost button.<br/>Check \"upload\" to upload the changes to the OSM server.<br/>Check \"Save\" to save the layer to the file specified on the left.</html>"));
    }

    protected void updateCheckboxes(Object v) {
        if (checkBoxes[0] != null && checkBoxes[1] != null) {
            boolean[] values;
            if(v instanceof SaveLayerInfo) {
                values = new boolean[2];
                values[0] = ((SaveLayerInfo) v).isDoUploadToServer();
                values[1] = ((SaveLayerInfo) v).isDoSaveToFile();
            } else {
                values = (boolean[]) v;
            }
            checkBoxes[0].setSelected(values[0]);
            checkBoxes[1].setSelected(values[1]);
        }
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        updateCheckboxes(value);
        return this;
    }

    @Override
    public void addCellEditorListener(CellEditorListener l) {
        if (l != null) {
            listeners.addIfAbsent(l);
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

    @Override
    public void cancelCellEditing() {
        fireEditingCanceled();
    }

    @Override
    public Object getCellEditorValue() {
        boolean[] values = new boolean[2];
        values[0] = checkBoxes[0].isSelected();
        values[1] = checkBoxes[1].isSelected();
        return values;
    }

    @Override
    public boolean isCellEditable(EventObject anEvent) {
        return true;
    }

    @Override
    public void removeCellEditorListener(CellEditorListener l) {
        listeners.remove(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        fireEditingStopped();
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        updateCheckboxes(value);
        return this;
    }
}