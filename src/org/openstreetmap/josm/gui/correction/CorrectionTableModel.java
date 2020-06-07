// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.correction;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.correction.Correction;

/**
 * Abstract correction table model.
 * @param <C> type of correction
 */
public abstract class CorrectionTableModel<C extends Correction> extends AbstractTableModel {

    private final transient List<C> corrections;
    private boolean[] apply;
    private final int applyColumn;

    /**
     * Constructs a new {@code CorrectionTableModel}.
     * @param corrections list of corrections
     */
    protected CorrectionTableModel(List<C> corrections) {
        this.corrections = corrections;
        apply = new boolean[this.corrections.size()];
        Arrays.fill(apply, true);
        applyColumn = getColumnCount() - 1;
    }

    protected abstract boolean isBoldCell(int row, int column);

    /**
     * Returns the column name for columns other than "Apply".
     * @param colIndex column index
     * @return the translated column name for given index
     * @see #getApplyColumn
     */
    public abstract String getCorrectionColumnName(int colIndex);

    /**
     * Returns the correction value at given position.
     * @param rowIndex row index
     * @param colIndex column index
     * @return the correction value at given position
     */
    public abstract Object getCorrectionValueAt(int rowIndex, int colIndex);

    /**
     * Returns the list of corrections.
     * @return the list of corrections
     */
    public List<C> getCorrections() {
        return corrections;
    }

    /**
     * Returns the index of the "Apply" column.
     * @return the index of the "Apply" column
     */
    public int getApplyColumn() {
        return applyColumn;
    }

    /**
     * Returns the "Apply" flag for given index.
     * @param i index
     * @return the "Apply" flag for given index
     */
    public boolean getApply(int i) {
        return apply[i];
    }

    @Override
    public int getRowCount() {
        return corrections.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == applyColumn)
            return Boolean.class;
        return String.class;
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == applyColumn)
            return tr("Apply?");

        return getCorrectionColumnName(columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == applyColumn;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == applyColumn && aValue instanceof Boolean)
            apply[rowIndex] = (Boolean) aValue;
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        if (colIndex == applyColumn)
            return apply[rowIndex];

        return getCorrectionValueAt(rowIndex, colIndex);
    }
}
