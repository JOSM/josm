// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.corrector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public abstract class CorrectionTable<T extends CorrectionTableModel<?>>
        extends JTable {

    private static final int MAX_VISIBLE_LINES = 10;

    public static class BoldRenderer extends JLabel implements
            TableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table,
                Object value, boolean isSelected, boolean hasFocus, int row,
                int column) {

            Font f = getFont();
            setFont(new Font(f.getName(), f.getStyle() | Font.BOLD, f.getSize()));

            setText((String)value);

            return this;
        }
    }

    private static BoldRenderer boldRenderer = null;

    protected CorrectionTable(T correctionTableModel) {
        super(correctionTableModel);

        final int correctionsSize = correctionTableModel.getCorrections().size();
        final int lines = correctionsSize > MAX_VISIBLE_LINES ? MAX_VISIBLE_LINES
                : correctionsSize;
        setPreferredScrollableViewportSize(new Dimension(400, lines
                * getRowHeight()));
        getColumnModel().getColumn(correctionTableModel.getApplyColumn())
                .setPreferredWidth(40);
        setRowSelectionAllowed(false);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        if (getCorrectionTableModel().isBoldCell(row, column)) {
            if (boldRenderer == null)
                boldRenderer = new BoldRenderer();
            return boldRenderer;
        }
        return super.getCellRenderer(row, column);
    }

    @SuppressWarnings("unchecked")
    public T getCorrectionTableModel() {
        return (T)getModel();
    }

}
