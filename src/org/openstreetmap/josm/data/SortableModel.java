// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data;

/**
 * Defines a model that can be sorted.
 * @param <T> item type
 * @since 15226
 */
public interface SortableModel<T> extends ReorderableModel<T> {

    /**
     * Sort the items.
     */
    void sort();

    /**
     * Reverse the items order.
     */
    void reverse();
}
