// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.openstreetmap.josm.tools.CheckParameterUtil;

/**
 * AutoCompletionList manages a list of {@link AutoCompletionListItem}s.
 *
 * The list is sorted, items with higher priority first, then according to lexicographic order
 * on the value of the {@link AutoCompletionListItem}.
 *
 * AutoCompletionList maintains two views on the list of {@link AutoCompletionListItem}s.
 * <ol>
 *   <li>the bare, unfiltered view which includes all items</li>
 *   <li>a filtered view, which includes only items which match a current filter expression</li>
 * </ol>
 *
 * AutoCompletionList is an {@link AbstractTableModel} which serves the list of filtered
 * items to a {@link JTable}.
 *
 */
public class AutoCompletionList extends AbstractTableModel {

    /** the bare list of AutoCompletionItems */
    private transient List<AutoCompletionListItem> list;
    /**  the filtered list of AutoCompletionItems */
    private transient ArrayList<AutoCompletionListItem> filtered;
    /** the filter expression */
    private String filter;
    /** map from value to priority */
    private transient Map<String, AutoCompletionListItem> valutToItemMap;

    /**
     * constructor
     */
    public AutoCompletionList() {
        list = new ArrayList<>();
        filtered = new ArrayList<>();
        valutToItemMap = new HashMap<>();
    }

    /**
     * applies a filter expression to the list of {@link AutoCompletionListItem}s.
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
     *
     */
    public void clearFilter() {
        filter = null;
        filter();
    }

    /**
     * @return the current filter expression; null, if no filter expression is set
     */
    public String getFilter() {
        return filter;
    }

    /**
     * adds an AutoCompletionListItem to the list. Only adds the item if it
     * is not null and if not in the list yet.
     *
     * @param item the item
     */
    public void add(AutoCompletionListItem item) {
        if (item == null)
            return;
        appendOrUpdatePriority(item);
        sort();
        filter();
    }

    /**
     * adds another AutoCompletionList to this list. An item is only
     * added it is not null and if it does not exist in the list yet.
     *
     * @param other another auto completion list; must not be null
     * @throws IllegalArgumentException if other is null
     */
    public void add(AutoCompletionList other) {
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        for (AutoCompletionListItem item : other.list) {
            appendOrUpdatePriority(item);
        }
        sort();
        filter();
    }

    /**
     * adds a list of AutoCompletionListItem to this list. Only items which
     * are not null and which do not exist yet in the list are added.
     *
     * @param other a list of AutoCompletionListItem; must not be null
     * @throws IllegalArgumentException if other is null
     */
    public void add(List<AutoCompletionListItem> other) {
        CheckParameterUtil.ensureParameterNotNull(other, "other");
        for (AutoCompletionListItem toadd : other) {
            appendOrUpdatePriority(toadd);
        }
        sort();
        filter();
    }

    /**
     * adds a list of strings to this list. Only strings which
     * are not null and which do not exist yet in the list are added.
     *
     * @param values a list of strings to add
     * @param priority the priority to use
     */
    public void add(Collection<String> values, AutoCompletionItemPriority priority) {
        if (values == null) return;
        for (String value: values) {
            if (value == null) {
                continue;
            }
            AutoCompletionListItem item = new AutoCompletionListItem(value, priority);
            appendOrUpdatePriority(item);

        }
        sort();
        filter();
    }

    public void addUserInput(Collection<String> values) {
        if (values == null) return;
        int i = 0;
        for (String value: values) {
            if (value == null) {
                continue;
            }
            AutoCompletionListItem item = new AutoCompletionListItem(value, new AutoCompletionItemPriority(false, false, false, i));
            appendOrUpdatePriority(item);
            i++;
        }
        sort();
        filter();
    }

    protected void appendOrUpdatePriority(AutoCompletionListItem toAdd) {
        AutoCompletionListItem item = valutToItemMap.get(toAdd.getValue());
        if (item == null) {
            // new item does not exist yet. Add it to the list
            list.add(toAdd);
            valutToItemMap.put(toAdd.getValue(), toAdd);
        } else {
            item.setPriority(item.getPriority().mergeWith(toAdd.getPriority()));
        }
    }

    /**
     * checks whether a specific item is already in the list. Matches for the
     * the value <strong>and</strong> the priority of the item
     *
     * @param item the item to check
     * @return true, if item is in the list; false, otherwise
     */
    public boolean contains(AutoCompletionListItem item) {
        if (item == null)
            return false;
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
        if (value == null)
            return false;
        for (AutoCompletionListItem item: list) {
            if (item.getValue().equals(value))
                return true;
        }
        return false;
    }

    /**
     * removes the auto completion item with key <code>key</code>
     * @param key  the key;
     */
    public void remove(String key) {
        if (key == null)
            return;
        for (int i = 0; i < list.size(); i++) {
            AutoCompletionListItem item = list.get(i);
            if (item.getValue().equals(key)) {
                list.remove(i);
                return;
            }
        }
    }

    /**
     * sorts the list
     */
    protected void sort() {
        Collections.sort(list);
    }

    protected void filter() {
        filtered.clear();
        if (filter == null) {
            // Collections.copy throws an exception "Source does not fit in dest"
            filtered.ensureCapacity(list.size());
            for (AutoCompletionListItem item: list) {
                filtered.add(item);
            }
            return;
        }

        // apply the pattern to list of possible values. If it matches, add the
        // value to the list of filtered values
        //
        for (AutoCompletionListItem item : list) {
            if (item.getValue().startsWith(filter)) {
                filtered.add(item);
            }
        }
        fireTableDataChanged();
    }

    /**
     * replies the number of filtered items
     *
     * @return the number of filtered items
     */
    public int getFilteredSize() {
        return this.filtered.size();
    }

    /**
     * replies the idx-th item from the list of filtered items
     * @param idx the index; must be in the range 0 &lt;= idx &lt; {@link #getFilteredSize()}
     * @return the item
     *
     * @throws IndexOutOfBoundsException if idx is out of bounds
     */
    public AutoCompletionListItem getFilteredItem(int idx) {
        if (idx < 0 || idx >= getFilteredSize())
            throw new IndexOutOfBoundsException("idx out of bounds. idx=" + idx);
        return filtered.get(idx);
    }

    List<AutoCompletionListItem> getList() {
        return list;
    }

    List<AutoCompletionListItem> getUnmodifiableList() {
        return Collections.unmodifiableList(list);
    }

    /**
     * removes all elements from the auto completion list
     *
     */
    public void clear() {
        valutToItemMap.clear();
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
        return list == null ? null : getFilteredItem(rowIndex);
    }
}
