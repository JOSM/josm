// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.openstreetmap.josm.gui.dialogs.IEnabledStateUpdating;

/**
 * The class that provide common JTable customization methods
 * @since 5785
 */
public final class TableHelper {

    private TableHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * Wires <code>listener</code> to <code>listSelectionModel</code> in such a way, that
     * <code>listener</code> receives a {@link IEnabledStateUpdating#updateEnabledState()}
     * on every {@link ListSelectionEvent}.
     *
     * @param listener  the listener
     * @param listSelectionModel  the source emitting {@link ListSelectionEvent}s
     * @since 15226
     */
    public static void adaptTo(final IEnabledStateUpdating listener, ListSelectionModel listSelectionModel) {
        listSelectionModel.addListSelectionListener(e -> listener.updateEnabledState());
    }

    /**
     * Wires <code>listener</code> to <code>listModel</code> in such a way, that
     * <code>listener</code> receives a {@link IEnabledStateUpdating#updateEnabledState()}
     * on every {@link ListDataEvent}.
     *
     * @param listener the listener
     * @param listModel the source emitting {@link ListDataEvent}s
     * @since 15226
     */
    public static void adaptTo(final IEnabledStateUpdating listener, AbstractTableModel listModel) {
        listModel.addTableModelListener(e -> listener.updateEnabledState());
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

    /**
     * Returns an array of all of the selected indices in the selection model, in increasing order.
     * Unfortunately this method is not available in OpenJDK before version 11, see
     * https://bugs.openjdk.java.net/browse/JDK-8199395
     * Code taken from OpenJDK 11. To be removed when we switch to Java 11 or later.
     *
     * @param selectionModel list selection model.
     *
     * @return all of the selected indices, in increasing order,
     *         or an empty array if nothing is selected
     * @since 15226
     */
    public static int[] getSelectedIndices(ListSelectionModel selectionModel) {
        int iMin = selectionModel.getMinSelectionIndex();
        int iMax = selectionModel.getMaxSelectionIndex();

        if (iMin < 0 || iMax < 0) {
            return new int[0];
        }

        int[] rvTmp = new int[1 + iMax - iMin];
        int n = 0;
        for (int i = iMin; i <= iMax; i++) {
            if (selectionModel.isSelectedIndex(i)) {
                rvTmp[n++] = i;
            }
        }
        int[] rv = new int[n];
        System.arraycopy(rvTmp, 0, rv, 0, n);
        return rv;
    }
}
