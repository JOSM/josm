// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.widgets;

import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;

import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.table.TableModel;

/**
 * Table offering easier scroll to a given row/column.
 * @since 11881
 */
public class ScrollableTable extends JTable {

    /**
     * Constructs a <code>ScrollableTable</code> that is initialized with
     * <code>dm</code> as the data model, a default column model,
     * and a default selection model.
     *
     * @param dm the data model for the table
     * @see #createDefaultColumnModel
     * @see #createDefaultSelectionModel
     */
    public ScrollableTable(TableModel dm) {
        super(dm);
    }

    /**
     * Scrolls this table to make sure the (row,col) is visible.
     * @param row row index
     * @param col column index
     */
    public void scrollToVisible(int row, int col) {
        Container parent = getParent();
        if (parent instanceof JViewport) {
            JViewport viewport = (JViewport) parent;
            Rectangle rect = getCellRect(row, col, true);
            Point pt = viewport.getViewPosition();
            rect.setLocation(rect.x - pt.x, rect.y - pt.y);
            viewport.scrollRectToVisible(rect);
        }
    }
}
