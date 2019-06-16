// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/**
 * The class that provide common JTable customization methods
 * @since 5785
 */
public final class TableHelper {

    private TableHelper() {
        // Hide default constructor for utils classes
    }

    static int getColumnHeaderWidth(JTable tbl, int col) {
        TableColumn tableColumn = tbl.getColumnModel().getColumn(col);
        TableCellRenderer renderer = tableColumn.getHeaderRenderer();

        if (renderer == null && tbl.getTableHeader() != null)
            renderer = tbl.getTableHeader().getDefaultRenderer();

        if (renderer == null)
            return 0;

        Component c = renderer.getTableCellRendererComponent(tbl, tableColumn.getHeaderValue(), false, false, -1, col);
        return c.getPreferredSize().width;
    }

    static int getMaxWidth(JTable tbl, int col) {
        int maxwidth = getColumnHeaderWidth(tbl, col);
        for (int row = 0; row < tbl.getRowCount(); row++) {
            TableCellRenderer tcr = tbl.getCellRenderer(row, col);
            Object val = tbl.getValueAt(row, col);
            Component comp = tcr.getTableCellRendererComponent(tbl, val, false, false, row, col);
            maxwidth = Math.max(comp.getPreferredSize().width, maxwidth);
        }
        return maxwidth;
    }

    /**
     * adjust the preferred width of column col to the maximum preferred width of the cells (including header)
     * @param tbl table
     * @param col column index
     * @param resizable if true, resizing is allowed
     * @since 15176
     */
    public static void adjustColumnWidth(JTable tbl, int col, boolean resizable) {
        int maxwidth = getMaxWidth(tbl, col);
        TableColumn column = tbl.getColumnModel().getColumn(col);
        column.setPreferredWidth(maxwidth);
        column.setResizable(resizable);
        if (!resizable) {
            column.setMaxWidth(maxwidth);
        }
    }

    /**
     * adjust the preferred width of column col to the maximum preferred width of the cells (including header)
     * requires JTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
     * @param tbl table
     * @param col column index
     * @param maxColumnWidth maximum column width
     */
    public static void adjustColumnWidth(JTable tbl, int col, int maxColumnWidth) {
        int maxwidth = getMaxWidth(tbl, col);
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
