// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.io;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;

import org.openstreetmap.josm.gui.util.CellEditorSupport;
import org.openstreetmap.josm.tools.GBC;

/**
 * This class creates a table cell that features two checkboxes, Upload and Save. It
 * handles everything on its own, in other words it renders itself and also functions
 * as editor so the checkboxes may be set by the user.
 *
 * Intended usage is like this:
 * <code>
 * <br>ActionFlagsTableCell aftc = new ActionFlagsTableCell();
 * <br>col = new TableColumn(0);
 * <br>col.setCellRenderer(aftc);
 * <br>col.setCellEditor(aftc);
 * </code>
 */
class ActionFlagsTableCell extends JPanel implements TableCellRenderer, TableCellEditor {
    private final JCheckBox[] checkBoxes = new JCheckBox[2];
    private final transient CellEditorSupport cellEditorSupport = new CellEditorSupport(this);

    private transient ActionListener al = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            cellEditorSupport.fireEditingStopped();
        }
    };

    /**
     * Constructs a new {@code ActionFlagsTableCell}.
     */
    ActionFlagsTableCell() {
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
                    cellEditorSupport.fireEditingStopped();
                }
            });
        }

        setToolTipText(tr("<html>"+
            "Select which actions to perform for this layer, if you click the leftmost button.<br/>"+
            "Check \"upload\" to upload the changes to the OSM server.<br/>"+
            "Check \"Save\" to save the layer to the file specified on the left."+
            "</html>"));
    }

    protected void updateCheckboxes(Object v) {
        if (v != null && checkBoxes[0] != null && checkBoxes[1] != null) {
            boolean[] values;
            if (v instanceof SaveLayerInfo) {
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
        cellEditorSupport.addCellEditorListener(l);
    }

    @Override
    public void cancelCellEditing() {
        cellEditorSupport.fireEditingCanceled();
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
        cellEditorSupport.removeCellEditorListener(l);
    }

    @Override
    public boolean shouldSelectCell(EventObject anEvent) {
        return true;
    }

    @Override
    public boolean stopCellEditing() {
        cellEditorSupport.fireEditingStopped();
        return true;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        updateCheckboxes(value);
        return this;
    }
}
