// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.autofilter;

import org.openstreetmap.josm.data.osm.Filter;

/**
 * An auto filter is a graphical shortcut to enable a filter for a specific tag.
 * @since 12400
 */
public class AutoFilter {
    private final String label;
    private final String description;
    private final Filter filter;

    /**
     * Constructs a new {@code AutoFilter}.
     * @param label button label
     * @param description button tooltip
     * @param filter associated filter
     */
    public AutoFilter(String label, String description, Filter filter) {
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
    public Filter getFilter() {
        return filter;
    }
}
