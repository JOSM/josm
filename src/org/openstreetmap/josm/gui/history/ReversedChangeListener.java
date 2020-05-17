// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.history;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;

/**
 * Updates the column header text and indicates a normal/reversed diff sorting
 */
final class ReversedChangeListener implements TableModelListener {
    private final TableColumnModel columnModel;
    private final JTable table;
    private Boolean reversed;
    private final String nonReversedText;
    private final String reversedText;
    private final String reversedTooltip;

    ReversedChangeListener(JTable table, TableColumnModel columnModel, String reversedTooltip) {
        this.columnModel = columnModel;
        this.table = table;
        Object columnName = columnModel.getColumn(0).getHeaderValue();
        this.nonReversedText = columnName + (table.getFont().canDisplay('\u25bc') ? " \u25bc" : " (1-n)");
        this.reversedText = columnName + (table.getFont().canDisplay('\u25b2') ? " \u25b2" : " (n-1)");
        this.reversedTooltip = reversedTooltip;
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        if (e.getSource() instanceof DiffTableModel) {
            final DiffTableModel mod = (DiffTableModel) e.getSource();
            if (reversed == null || reversed != mod.isReversed()) {
                reversed = mod.isReversed();
                columnModel.getColumn(0).setHeaderValue(reversed ? reversedText : nonReversedText);
                table.getTableHeader().setToolTipText(reversed ? reversedTooltip : null);
                table.getTableHeader().repaint();
            }
        }
    }
}
