// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;

/**
 * A {@link RowFilter} implementation which matches tags w.r.t. the specified filter's
 * {@link org.openstreetmap.josm.data.osm.search.SearchCompiler.Match#match(org.openstreetmap.josm.data.osm.Tagged)} method.
 *
 * <p>An {@link javax.swing.RowFilter.Entry}'s column 0 is considered as key, and column 1 is considered as value.</p>
 */
class SearchBasedRowFilter extends RowFilter<TableModel, Integer> {

    final SearchCompiler.Match filter;

    /**
     * Constructs a new {@code SearchBasedRowFilter} with the given filter.
     * @param filter the filter used to match tags
     */
    SearchBasedRowFilter(SearchCompiler.Match filter) {
        this.filter = filter;
    }

    @Override
    public boolean include(Entry entry) {
        final String key = entry.getStringValue(0);
        final String value = entry.getStringValue(1);
        return filter.match(new Tag(key, value));
    }

}
