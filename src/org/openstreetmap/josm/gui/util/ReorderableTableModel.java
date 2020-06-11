// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import java.util.Arrays;

import javax.swing.JList;
import javax.swing.JTable;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.data.ReorderableModel;

/**
 * Defines a list/table model that can be reordered.
 * @param <T> item type
 * @since 15226
 */
public interface ReorderableTableModel<T> extends ReorderableModel<T> {

    /**
     * Returns the selection model.
     * @return the selection model (never null)
     * @see JList#getSelectionModel()
     * @see JTable#getSelectionModel()
     */
    ListSelectionModel getSelectionModel();

    /**
     * Returns the number of rows in the list/table.
     * @return the number of rows in the list/table
     * @see ListModel#getSize()
     * @see TableModel#getRowCount()
     */
    int getRowCount();

    /**
     * Returns an array of all of the selected indices in the selection model, in increasing order.
     * @return an array of all of the selected indices in the selection model, in increasing order
     */
    default int[] getSelectedIndices() {
        return TableHelper.getSelectedIndices(getSelectionModel());
    }

    /**
     * Checks that the currently selected range of rows can be moved by a number of positions.
     * @param delta negative or positive delta
     * @return {@code true} if rows can be moved
     */
    default boolean canMove(int delta) {
        return canMove(delta, this::getRowCount, getSelectedIndices());
    }

    /**
     * Checks that the currently selected range of rows can be moved up.
     * @return {@code true} if rows can be moved up
     */
    default boolean canMoveUp() {
        return canMoveUp(getSelectedIndices());
    }

    /**
     * Checks that a range of rows can be moved up.
     * @param rows indexes of rows to move up
     * @return {@code true} if rows can be moved up
     */
    default boolean canMoveUp(int... rows) {
        return canMoveUp(this::getRowCount, rows);
    }

    /**
     * Checks that the currently selected range of rows can be moved down.
     * @return {@code true} if rows can be moved down
     */
    default boolean canMoveDown() {
        return canMoveDown(getSelectedIndices());
    }

    /**
     * Checks that a range of rows can be moved down.
     * @param rows indexes of rows to move down
     * @return {@code true} if rows can be moved down
     */
    default boolean canMoveDown(int... rows) {
        return canMoveDown(this::getRowCount, rows);
    }

    /**
     * Move up selected rows, if possible.
     * @return {@code true} if the move was performed
     * @see #canMoveUp
     */
    default boolean moveUp() {
        return moveUp(getSelectedIndices());
    }

    /**
     * Move up selected rows, if possible.
     * @param selectedRows rows to move up
     * @return {@code true} if the move was performed
     * @see #canMoveUp
     */
    default boolean moveUp(int... selectedRows) {
        return move(-1, selectedRows);
    }

    /**
     * Move down selected rows, if possible.
     * @return {@code true} if the move was performed
     * @see #canMoveDown
     */
    default boolean moveDown() {
        return moveDown(getSelectedIndices());
    }

    /**
     * Move down selected rows by 1 position, if possible.
     * @param selectedRows rows to move down
     * @return {@code true} if the move was performed
     * @see #canMoveDown
     */
    default boolean moveDown(int... selectedRows) {
        return move(1, selectedRows);
    }

    /**
     * Move selected rows by any number of positions, if possible.
     * @param delta negative or positive delta
     * @param selectedRows rows to move
     * @return {@code true} if the move was performed
     * @see #canMove
     */
    default boolean move(int delta, int... selectedRows) {
        if (!canMove(delta, this::getRowCount, selectedRows))
            return false;
        if (!doMove(delta, selectedRows))
            return false;
        final ListSelectionModel selectionModel = getSelectionModel();
        TableHelper.setSelectedIndices(selectionModel, Arrays.stream(selectedRows).map(i -> i + delta));
        return true;
    }
}
