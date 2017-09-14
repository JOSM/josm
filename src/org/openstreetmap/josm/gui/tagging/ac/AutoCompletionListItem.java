// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

import org.openstreetmap.josm.data.tagging.ac.AutoCompletionItem;

/**
 * Represents an entry in the list of auto completion values.
 *
 *  An AutoCompletionListItem has a <em>priority</em> and a <em>value</em>.
 *
 *  The priority helps to sort the auto completion items according to their importance. For instance,
 *  in an auto completion list for tag names, standard tag names would be assigned a higher
 *  priority than arbitrary tag names present in the current data set. There are three priority levels,
 *  {@link AutoCompletionItemPriority}.
 *
 * The value is a string which will be displayed in the auto completion list.
 * @deprecated To be removed end of 2017. Use {@link AutoCompletionItem} instead
 */
@Deprecated
public class AutoCompletionListItem implements Comparable<AutoCompletionListItem> {

    /** the item */
    private final AutoCompletionItem item;

    /**
     * Constructs a new {@code AutoCompletionListItem} with the given value and priority.
     * @param value The value
     * @param priority The priority
     */
    public AutoCompletionListItem(String value, AutoCompletionItemPriority priority) {
        this.item = new AutoCompletionItem(value, priority.getPriority());
    }

    /**
     * Constructs a new {@code AutoCompletionListItem} with the given value and unknown priority.
     * @param value The value
     */
    public AutoCompletionListItem(String value) {
        this.item = new AutoCompletionItem(value);
    }

    /**
     * Constructs a new {@code AutoCompletionListItem}.
     */
    public AutoCompletionListItem() {
        this.item = new AutoCompletionItem();
    }

    /**
     * Constructs a new {@code AutoCompletionListItem} from an existing {@link AutoCompletionItem}.
     * @param other {@code AutoCompletionItem} to convert
     * @since 12859
     */
    public AutoCompletionListItem(AutoCompletionItem other) {
        this.item = other;
    }

    /**
     * Returns the priority.
     * @return the priority
     */
    public AutoCompletionItemPriority getPriority() {
        return new AutoCompletionItemPriority(item.getPriority());
    }

    /**
     * Sets the priority.
     * @param priority  the priority
     */
    public void setPriority(AutoCompletionItemPriority priority) {
        item.setPriority(priority.getPriority());
    }

    /**
     * Returns the value.
     * @return the value
     */
    public String getValue() {
        return item.getValue();
    }

    /**
     * sets the value
     * @param value the value; must not be null
     * @throws IllegalArgumentException if value if null
     */
    public void setValue(String value) {
        item.setValue(value);
    }

    @Override
    public String toString() {
        return item.toString();
    }

    @Override
    public int hashCode() {
        return item.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (obj instanceof String)
            return obj.equals(item.getValue());
        if (getClass() != obj.getClass())
            return false;
        final AutoCompletionListItem other = (AutoCompletionListItem) obj;
        return item.equals(other.item);
    }

    @Override
    public int compareTo(AutoCompletionListItem other) {
        return item.compareTo(other.item);
    }

    /**
     * Returns the underlying item.
     * @return the underlying item
     * @since 12859
     */
    public AutoCompletionItem getItem() {
        return item;
    }
}
