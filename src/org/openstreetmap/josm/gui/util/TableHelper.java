// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * The class that provide common JTable customization methods
 */
public final class TableHelper {

    private TableHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * adjust the preferred width of column col to the maximum preferred width of the cells
     * requires JTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
     * @param tbl table
     * @param col column index
     * @param maxColumnWidth maximum column width
     */
    public static void adjustColumnWidth(JTable tbl, int col, int maxColumnWidth) {
        int maxwidth = 0;
        for (int row = 0; row < tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
            Object val = tbl.getValueAt(row, col);
            Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
            maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
        }
        tbl.getColumnModel().getColumn(col).setPreferredWidth(Math.min(maxwidth+10, maxColumnWidth));
    }

    /**
     * adjust the table's columns to fit their content best
     * requires JTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
     * @param tbl table
     * @since 14476
     */
    public static void computeColumnsWidth(JTable tbl) {
        for (int column = 0; column < tbl.getColumnCount(); column++) {
            adjustColumnWidth(tbl, column, Integer.MAX_VALUE);
        }
    }
}
