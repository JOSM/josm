// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.tagging.ac;

/**
 * Represents an entry in the list of auto completion values.
 *
 *  An AutoCompletionListItem has a <em>priority</em> and a <em>value</em>.
 *
 *  The priority helps to sort the auto completion items according to their importance. For instance,
 *  in an auto completion list for tag names, standard tag names would be assigned a higher
 *  priority than arbitrary tag names present in the current data set. There are three priority levels,
 *  {@see AutoCompletionItemPritority}.
 *
 * The value is a string which will be displayed in the auto completion list.
 *
 */
public class AutoCompletionListItem implements Comparable<AutoCompletionListItem>{

    /** the pritority of this item */
    private  AutoCompletionItemPritority priority;
    /** the value of this item */
    private String value;

    /**
     * constructor
     */
    public AutoCompletionListItem() {
        value = "";
        priority = AutoCompletionItemPritority.UNKNOWN;
    }

    public AutoCompletionListItem(String value, AutoCompletionItemPritority priority) {
        this.value = value;
        this.priority = priority;
    }

    /**
     *
     * @return the priority
     */
    public AutoCompletionItemPritority getPriority() {
        return priority;
    }

    /**
     * sets the priority
     * @param priority  the priority
     */
    public void setPriority(AutoCompletionItemPritority priority) {
        this.priority = priority;
    }

    /**
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * sets the value
     * @param value the value; must not be null
     * @exception IllegalArgumentException thrown, if value if null
     */
    public void setValue(String value) {
        if (value == null)
            throw new IllegalArgumentException("argument 'value' must not be null");
        this.value = value;
    }

    @Override public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("<val='");
        sb.append(value);
        sb.append("',");
        sb.append(priority.toString());
        sb.append(">");
        return sb.toString();
    }

    @Override public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
        + ((priority == null) ? 0 : priority.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final AutoCompletionListItem other = (AutoCompletionListItem)obj;
        if (priority == null) {
            if (other.priority != null)
                return false;
        } else if (!priority.equals(other.priority))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    public int compareTo(AutoCompletionListItem other) {
        int ret = this.priority.compareTo(other.priority);
        if (ret != 0)
            return ret;
        else
            return this.value.compareTo(other.value);
    }
}
