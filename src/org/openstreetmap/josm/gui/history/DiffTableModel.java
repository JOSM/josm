// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.gui.history.TwoColumnDiff.Item.DiffItemType;

/**
 * Simple model storing "diff cells" in a list. Could probably have
 * used a {@link javax.swing.table.DefaultTableModel} instead.
 */
class DiffTableModel extends AbstractTableModel {
    private transient List<TwoColumnDiff.Item> rows = new ArrayList<>();
    private transient int[] rowNumbers;
    private boolean reversed;

    public void setRows(List<TwoColumnDiff.Item> rows, boolean reversed) {
        this.rows = rows;
        this.reversed = reversed;
        computeRowNumbers();
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
        return IntStream.range(0, rows.size())
                .filter(i -> rows.get(i).state != DiffItemType.SAME)
                .findFirst().orElse(-1);
    }

    void computeRowNumbers() {
        rowNumbers = new int[rows.size()];
        int rowNumber = reversed ? rows.size() : 1;
        int i = 0;
        for (TwoColumnDiff.Item item : rows) {
            if (item.state == DiffItemType.EMPTY) {
                rowNumbers[i] = -1;
            } else if (reversed) {
                rowNumbers[i] = rowNumber--;
            } else {
                rowNumbers[i] = rowNumber++;
            }
            i++;
        }
    }

    public int getRowNumber(int rowIndex) {
        return rowNumbers[rowIndex];
    }
}
