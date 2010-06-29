// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.Not;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;

public class FilterMatcher {

    private static class FilterInfo {
        final Match match;
        final boolean isDelete;

        FilterInfo(Filter filter) throws ParseError {
            if (filter.mode == SearchMode.remove || filter.mode == SearchMode.in_selection) {
                isDelete = true;
            } else {
                isDelete = false;
            }

            Match compiled = SearchCompiler.compile(filter.text, filter.caseSensitive, filter.regexSearch);
            this.match = filter.inverted?new Not(compiled):compiled;
        }
    }

    private final List<FilterInfo> hiddenFilters = new ArrayList<FilterInfo>();
    private final List<FilterInfo> disabledFilters = new ArrayList<FilterInfo>();

    public void update(Collection<Filter> filters) throws ParseError {
        hiddenFilters.clear();
        disabledFilters.clear();

        for (Filter filter: filters) {

            if (!filter.enable) {
                continue;
            }

            List<FilterInfo> list = filter.hiding?hiddenFilters:disabledFilters;

            if (filter.mode == SearchMode.replace) {
                // No point in evalutaing filter when value will get replaced anyway (and yes, there is no point in using replace mode with filters)
                list.clear();
            }

            list.add(new FilterInfo(filter));
        }
    }

    private boolean test(List<FilterInfo> filters, OsmPrimitive primitive) {
        boolean selected = false;
        for (FilterInfo fi: filters) {
            if (fi.isDelete && selected && fi.match.match(primitive)) {
                selected = false;
            } else if (!fi.isDelete && !selected && fi.match.match(primitive)) {
                selected = true;
            }
        }
        return selected;
    }

    public boolean isHidden(OsmPrimitive primitive) {
        return test(hiddenFilters, primitive);
    }

    public boolean isDisabled(OsmPrimitive primitive) {
        return test(disabledFilters, primitive);
    }

}
