// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionPriority;
import org.openstreetmap.josm.data.tagging.ac.AutoCompletionSet;
import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * AutoCompletionList manages a graphical list of {@link AutoCompletionItem}s.
 *
 * The list is sorted, items with higher priority first, then according to lexicographic order
 * on the value of the {@link AutoCompletionItem}.
 *
 * AutoCompletionList maintains two views on the list of {@link AutoCompletionItem}s.
 * <ol>
 *   <li>the bare, unfiltered view which includes all items</li>
 *   <li>a filtered view, which includes only items which match a current filter expression</li>
 * </ol>
 *
 * AutoCompletionList is an {@link AbstractTableModel} which serves the list of filtered
 * items to a {@link JTable}.
 * @since 1762
 */
public class AutoCompletionList extends AbstractTableModel {

    /** the bare list of AutoCompletionItems */
    private final transient AutoCompletionSet list;
    /**  the filtered list of AutoCompletionItems */
    private final transient ArrayList<AutoCompletionItem> filtered;
    /** the filter expression */
    private String filter;

    /**
     * constructor
     */
    public AutoCompletionList() {
        list = new AutoCompletionSet();
        filtered = new ArrayList<>();
    }

    /**
     * applies a filter expression to the list of {@link AutoCompletionItem}s.
     *
     * The matching criterion is a case insensitive substring match.
     *
     * @param filter  the filter expression; must not be null
     *
     * @throws IllegalArgumentException if filter is null
     */
    public void applyFilter(String filter) {
        CheckParameterUtil.ensureParameterNotNull(filter, "filter");
        this.filter = filter;
        filter();
    }

    /**
     * clears the current filter
     */
    public void clearFilter() {
        filter = null;
        filter();
    }

    /**
     * Returns the current filter expression.
     * @return the current filter expression; null, if no filter expression is set
     */
    public String getFilter() {
        return filter;
    }

    /**
     * adds an {@link AutoCompletionItem} to the list. Only adds the item if it
     * is not null and if not in the list yet.
     *
     * @param item the item
     * @since 12859
     */
    public void add(AutoCompletionItem item) {
        if (item != null && list.add(item)) {
            filter();
        }
    }

    /**
     * adds another {@link AutoCompletionList} to this list. An item is only
     * added it is not null and if it does not exist in the list yet.
     *
     * @param other another auto completion list; must not be null
     * @throws IllegalArgumentException if other is null
     */
    public void add(AutoCompletionList other) {
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        add(other.list);
    }

    /**
     * adds a colleciton of {@link AutoCompletionItem} to this list. An item is only
     * added it is not null and if it does not exist in the list yet.
     *
     * @param collection auto completion collection; must not be null
     * @throws IllegalArgumentException if other is null
     * @since 12859
     */
    public void add(Collection<AutoCompletionItem> collection) {
        CheckParameterUtil.ensureParameterNotNull(collection, "collection");
        if (list.addAll(collection)) {
            filter();
        }
    }

    /**
     * adds a list of strings to this list. Only strings which
     * are not null and which do not exist yet in the list are added.
     *
     * @param values a list of strings to add
     * @param priority the priority to use
     * @since 12859
     */
    public void add(Collection<String> values, AutoCompletionPriority priority) {
        if (values != null && list.addAll(values, priority)) {
            filter();
        }
    }

    /**
     * Adds values that have been entered by the user.
     * @param values values that have been entered by the user
     */
    public void addUserInput(Collection<String> values) {
        if (values != null && list.addUserInput(values)) {
            filter();
        }
    }

    /**
     * checks whether a specific item is already in the list. Matches for the
     * the value <strong>and</strong> the priority of the item
     *
     * @param item the item to check
     * @return true, if item is in the list; false, otherwise
     * @since 12859
     */
    public boolean contains(AutoCompletionItem item) {
        return list.contains(item);
    }

    /**
     * checks whether an item with the given value is already in the list. Ignores
     * priority of the items.
     *
     * @param value the value of an auto completion item
     * @return true, if value is in the list; false, otherwise
     */
    public boolean contains(String value) {
        return list.contains(value);
    }

    /**
     * removes the auto completion item with key <code>key</code>
     * @param key the key
     */
    public void remove(String key) {
        if (key != null) {
            list.remove(key);
        }
    }

    protected void filter() {
        filtered.clear();
        if (filter == null) {
            // Collections.copy throws an exception "Source does not fit in dest"
            filtered.ensureCapacity(list.size());
            filtered.addAll(list);
            return;
        }

        // apply the pattern to list of possible values. If it matches, add the
        // value to the list of filtered values
        //
        list.stream().filter(e -> e.getValue().startsWith(filter)).forEach(filtered::add);
        fireTableDataChanged();
    }

    /**
     * replies the number of filtered items
     *
     * @return the number of filtered items
     */
    public int getFilteredSize() {
        return filtered.size();
    }

    /**
     * replies the idx-th item from the list of filtered items
     * @param idx the index; must be in the range 0 &lt;= idx &lt; {@link #getFilteredSize()}
     * @return the item
     *
     * @throws IndexOutOfBoundsException if idx is out of bounds
     * @since 12859
     */
    public AutoCompletionItem getFilteredItemAt(int idx) {
        return filtered.get(idx);
    }

    AutoCompletionSet getSet() {
        return list;
    }

    Set<AutoCompletionItem> getUnmodifiableSet() {
        return Collections.unmodifiableSet(list);
    }

    /**
     * removes all elements from the auto completion list
     */
    public void clear() {
        list.clear();
        fireTableDataChanged();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public int getRowCount() {
        return list == null ? 0 : getFilteredSize();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return list == null ? null : getFilteredItemAt(rowIndex);
    }
}
