// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item.DiffItemType;

/**
 * Simple model storing "diff cells" in a list. Could probably have
 * used a {@link javax.swing.table.DefaultTableModel} instead.
 */
class DiffTableModel extends AbstractTableModel {
    private transient List<TwoColumnDiff.Item> rows = new ArrayList<>();
    private boolean reversed;

    public void setRows(List<TwoColumnDiff.Item> rows, boolean reversed) {
        this.rows = rows;
        this.reversed = reversed;
        fireTableDataChanged();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    public boolean isReversed() {
        return reversed;
    }

    @Override
    public TwoColumnDiff.Item getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex);
    }

    public int getFirstChange() {
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).state != DiffItemType.SAME)
                return i;
        }
        return -1;
    }
}
