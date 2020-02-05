// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import java.util.Objects;

/**
 * An auto filter is a graphical shortcut to enable a filter for a specific tag.
 * @since 12400
 */
public class AutoFilter {
    private final String label;
    private final String description;
    private final AutoFilterManager.CompiledFilter filter;

    /**
     * Constructs a new {@code AutoFilter}.
     * @param label button label
     * @param description button tooltip
     * @param filter associated filter
     */
    public AutoFilter(String label, String description, AutoFilterManager.CompiledFilter filter) {
        this.label = label;
        this.description = description;
        this.filter = filter;
    }

    /**
     * Returns the button label.
     * @return the button label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Returns the button tooltip.
     * @return the button tooltip
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the filter.
     * @return the filter
     */
    public AutoFilterManager.CompiledFilter getFilter() {
        return filter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(filter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        AutoFilter other = (AutoFilter) obj;
        return Objects.equals(filter, other.filter);
    }

    @Override
    public String toString() {
        return "AutoFilter [label=" + label + ", description=" + description + ", filter=" + filter + ']';
    }
}
