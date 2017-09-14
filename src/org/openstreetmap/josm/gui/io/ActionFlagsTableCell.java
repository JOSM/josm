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

    /**
     * Constructs a new {@code ActionFlagsTableCell}.
     */
    ActionFlagsTableCell() {
        checkBoxes[0] = new JCheckBox(tr("Upload"));
        checkBoxes[1] = new JCheckBox(tr("Save"));
        setLayout(new GridBagLayout());

        ActionListener al = e -> cellEditorSupport.fireEditingStopped();
        ActionMap am = getActionMap();
        for (final JCheckBox b : checkBoxes) {
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

    private void updatePanel(SaveLayerInfo info) {
        StringBuilder sb = new StringBuilder(128)
            .append("<html>")
            .append(tr("Select which actions to perform for this layer, if you click the leftmost button."));
        removeAll();
        if (info != null) {
            if (info.isUploadable()) {
                sb.append("<br/>")
                  .append(tr("Check \"Upload\" to upload the changes to the OSM server."));
                add(checkBoxes[0], GBC.eol().fill(GBC.HORIZONTAL));
            }
            if (info.isSavable()) {
                sb.append("<br/>")
                  .append(tr("Check \"Save\" to save the layer to the file specified on the left."));
                add(checkBoxes[1], GBC.eol().fill(GBC.HORIZONTAL));
            }
        }
        sb.append("</html>");
        setToolTipText(sb.toString());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return getTableCellEditorComponent(table, value, isSelected, row, column);
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
        updatePanel((SaveLayerInfo) value);
        updateCheckboxes(value);
        return this;
    }
}
