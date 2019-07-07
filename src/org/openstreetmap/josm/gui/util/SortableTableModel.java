// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.util;

import org.openstreetmap.josm.data.SortableModel;

/**
 * Defines a table model that can be sorted.
 * @param <T> item type
 * @since 15226
 */
public interface SortableTableModel<T> extends ReorderableTableModel<T>, SortableModel<T> {

}
