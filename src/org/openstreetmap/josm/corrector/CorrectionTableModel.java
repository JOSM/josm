// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Arrays;
import java.util.List;

import javax.swing.table.AbstractTableModel;

public abstract class CorrectionTableModel<C extends Correction> extends
        AbstractTableModel {

    private List<C> corrections;
    private boolean[] apply;
    private int applyColumn;

    public CorrectionTableModel(List<C> corrections) {
        super();
        this.corrections = corrections;
        apply = new boolean[this.corrections.size()];
        Arrays.fill(apply, true);
        applyColumn = getColumnCount() - 1;
    }

    @Override
    abstract public int getColumnCount();

    abstract protected boolean isBoldCell(int row, int column);
    abstract public String getCorrectionColumnName(int colIndex);
    abstract public Object getCorrectionValueAt(int rowIndex, int colIndex);

    public List<C> getCorrections() {
        return corrections;
    }

    public int getApplyColumn() {
        return applyColumn;
    }

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
            apply[rowIndex] = (Boolean)aValue;
    }

    @Override
    public Object getValueAt(int rowIndex, int colIndex) {
        if (colIndex == applyColumn)
            return apply[rowIndex];

        return getCorrectionValueAt(rowIndex, colIndex);
    }
}
