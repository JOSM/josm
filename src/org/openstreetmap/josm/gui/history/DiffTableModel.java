// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * Simple model storing "diff cells" in a list. Could probably have used a DefaultTableModel instead..
 *
 * {@see NodeListDiffTableCellRenderer}
 */
class DiffTableModel extends AbstractTableModel {
    private List<TwoColumnDiff.Item> rows;

    public void setRows(List<TwoColumnDiff.Item> rows) {
        this.rows = rows;
    }

    public DiffTableModel(List<TwoColumnDiff.Item> rows) {
        this.rows = rows;
    }
    public DiffTableModel() {
        this.rows = new ArrayList<TwoColumnDiff.Item>();
    }
    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public TwoColumnDiff.Item getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex);
    }
}
