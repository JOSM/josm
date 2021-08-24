// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.tagging.ac;

import java.util.Objects;

/**
 * Represents an entry in the set of auto completion values.
 *
 *  An AutoCompletionItem has a <em>priority</em> and a <em>value</em>.
 *
 *  The priority helps to sort the auto completion items according to their importance. For instance,
 *  in an auto completion set for tag names, standard tag names would be assigned a higher
 *  priority than arbitrary tag names present in the current data set. There are three priority levels,
 *  {@link AutoCompletionPriority}.
 *
 * The value is a string which will be displayed in the auto completion list.
 * @since 12859 (copied from {@code gui.tagging.ac.AutoCompletionListItem})
 */
public class AutoCompletionItem implements Comparable<AutoCompletionItem> {

    /** the priority of this item */
    private AutoCompletionPriority priority;
    /** the value of this item */
    private final String value;

    /**
     * Constructs a new {@code AutoCompletionItem} with the given value and priority.
     * @param value The value
     * @param priority The priority
     */
    public AutoCompletionItem(String value, AutoCompletionPriority priority) {
        this.value = value;
        this.priority = priority;
    }

    /**
     * Constructs a new {@code AutoCompletionItem} with the given value and unknown priority.
     * @param value The value
     */
    public AutoCompletionItem(String value) {
        this.value = value;
        priority = AutoCompletionPriority.UNKNOWN;
    }

    /**
     * Constructs a new {@code AutoCompletionItem}.
     */
    public AutoCompletionItem() {
        value = "";
        priority = AutoCompletionPriority.UNKNOWN;
    }

    /**
     * Returns the priority.
     * @return the priority
     */
    public AutoCompletionPriority getPriority() {
        return priority;
    }

    /**
     * Sets the priority.
     * @param priority  the priority
     */
    public void setPriority(AutoCompletionPriority priority) {
        this.priority = priority;
    }

    /**
     * Returns the value.
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Here we return the value instead of a representation of the inner object state because both
     * {@link javax.swing.plaf.basic.BasicComboBoxEditor#setItem(Object)} and
     * {@link javax.swing.DefaultListCellRenderer#getListCellRendererComponent}
     * expect it, thus making derived Editor and CellRenderer classes superfluous.
     */
    @Override
    public String toString() {
        return value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(priority, value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof AutoCompletionItem))
            return false;
        final AutoCompletionItem other = (AutoCompletionItem) obj;
        if (value == null ? other.value != null : !value.equals(other.value))
            return false;
        return priority == null ? other.priority == null : priority.equals(other.priority);
    }

    @Override
    public int compareTo(AutoCompletionItem other) {
        // sort on priority descending
        int ret = other.priority.compareTo(priority);
        // then alphabetic ascending
        return ret != 0 ? ret : this.value.compareTo(other.value);
    }
}
