// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.dialogs.properties;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import javax.swing.RowFilter;
import javax.swing.table.TableModel;

import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.Tagged;

/**
 * A {@link RowFilter} implementation which matches tags w.r.t. the specified filter's
 * {@link SearchCompiler.Match#match(org.openstreetmap.josm.data.osm.Tagged)} method.
 *
 * <p>An {@link javax.swing.RowFilter.Entry}'s column 0 is considered as key, and column 1 is considered as value.</p>
 */
class SearchBasedRowFilter extends RowFilter<TableModel, Integer> {

    final SearchCompiler.Match filter;

    /**
     * Constructs a new {@code SearchBasedRowFilter} with the given filter.
     * @param filter the filter used to match tags
     */
    public SearchBasedRowFilter(SearchCompiler.Match filter) {
        this.filter = filter;
    }

    @Override
    public boolean include(Entry entry) {
        final String key = entry.getStringValue(0);
        final String value = entry.getStringValue(1);
        return filter.match(new OneKeyValue(key, value));
    }

    static class OneKeyValue implements Tagged {
        private final String key;
        private final String value;

        public OneKeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public void setKeys(Map<String, String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Map<String, String> getKeys() {
            return Collections.singletonMap(key, value);
        }

        @Override
        public void put(String key, String value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String get(String k) {
            return key.equals(k) ? value : null;
        }

        @Override
        public void remove(String key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasKeys() {
            return true;
        }

        @Override
        public Collection<String> keySet() {
            return Collections.singleton(key);
        }

        @Override
        public void removeAll() {
            throw new UnsupportedOperationException();
        }
    }
}
