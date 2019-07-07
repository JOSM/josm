// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

import java.util.Arrays;
import java.util.function.IntSupplier;

import org.openstreetmap.josm.tools.Utils;

/**
 * Defines a model that can be reordered.
 * @param <T> item type
 * @since 15226
 */
public interface ReorderableModel<T> {

    /**
     * Get object value at given index.
     * @param index index
     * @return object value at given index
     */
    T getValue(int index);

    /**
     * Set object value at given index.
     * @param index index
     * @param value new value
     * @return the value previously at the specified position
     */
    T setValue(int index, T value);

    /**
     * Checks that a range of rows can be moved by a given number of positions.
     * @param delta negative or positive delta
     * @param rowCount row count supplier
     * @param rows indexes of rows to move
     * @return {@code true} if rows can be moved
     */
    default boolean canMove(int delta, IntSupplier rowCount, int... rows) {
        if (rows == null || rows.length == 0)
            return false;
        int[] sortedRows = Utils.copyArray(rows);
        Arrays.sort(sortedRows);
        if (delta < 0)
            return sortedRows[0] >= -delta;
        else if (delta > 0)
            return sortedRows[sortedRows.length-1] <= rowCount.getAsInt()-1 - delta;
        else
            return true;
    }

    /**
     * Checks that a range of rows can be moved up (by 1 position).
     * @param rowCount row count supplier
     * @param rows indexes of rows to move up
     * @return {@code true} if rows can be moved up
     */
    default boolean canMoveUp(IntSupplier rowCount, int... rows) {
        return canMove(-1, rowCount, rows);
    }

    /**
     * Checks that a range of rows can be moved down (by 1 position).
     * @param rowCount row count supplier
     * @param rows indexes of rows to move down
     * @return {@code true} if rows can be moved down
     */
    default boolean canMoveDown(IntSupplier rowCount, int... rows) {
        return canMove(1, rowCount, rows);
    }

    /**
     * Performs the move operation, without any check nor selection handling.
     * @param delta negative or positive delta
     * @param selectedRows rows to move
     * @return {@code true} if rows have been moved down
     */
    default boolean doMove(int delta, int... selectedRows) {
        for (int row: selectedRows) {
            T t1 = getValue(row);
            T t2 = getValue(row + delta);
            setValue(row, t2);
            setValue(row + delta, t1);
        }
        return true;
    }
}
