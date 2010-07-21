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
        final boolean isInverted;

        FilterInfo(Filter filter) throws ParseError {
            if (filter.mode == SearchMode.remove || filter.mode == SearchMode.in_selection) {
                isDelete = true;
            } else {
                isDelete = false;
            }

            Match compiled = SearchCompiler.compile(filter.text, filter.caseSensitive, filter.regexSearch);
            this.match = filter.inverted?new Not(compiled):compiled;
            this.isInverted = filter.inverted;
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

            FilterInfo fi = new FilterInfo(filter);
            if (fi.isDelete) {
                if (filter.hiding) {
                    // Remove only hide flag
                    hiddenFilters.add(fi);
                } else {
                    // Remove both flags
                    disabledFilters.add(fi);
                    hiddenFilters.add(fi);
                }
            } else {
                if (filter.mode == SearchMode.replace) {
                    if (filter.hiding) {
                        hiddenFilters.clear();
                        disabledFilters.clear();
                    }
                }

                disabledFilters.add(fi);
                if (filter.hiding) {
                    hiddenFilters.add(fi);
                }
            }
        }
    }

    private boolean getState(OsmPrimitive primitive, boolean hidden) {
        return hidden?primitive.isDisabledAndHidden():primitive.isDisabled();
    }

    private boolean allParentWaysFiltered(OsmPrimitive primitive, boolean hidden) {
        List<OsmPrimitive> refs = primitive.getReferrers();
        boolean foundWay = false;

        for (OsmPrimitive p: refs) {
            if (p instanceof Way) {
                foundWay = true;
                if (!getState(p, hidden))
                    return false;
            }
        }

        return foundWay;
    }

    private boolean oneParentWayNotFiltered(OsmPrimitive primitive, boolean hidden) {
        List<OsmPrimitive> refs = primitive.getReferrers();
        for (OsmPrimitive p: refs) {
            if (p instanceof Way && !getState(p, hidden))
                return true;
        }

        return false;
    }

    private boolean test(List<FilterInfo> filters, OsmPrimitive primitive, boolean hidden) {
        boolean selected = false;
        boolean onlyInvertedFilters = true;

        for (FilterInfo fi: filters) {
            if (fi.isDelete && selected && fi.match.match(primitive)) {
                selected = false;
            } else if (!fi.isDelete && (!selected || (onlyInvertedFilters && !fi.isInverted)) && fi.match.match(primitive)) {
                selected = true;
                onlyInvertedFilters = onlyInvertedFilters && fi.isInverted;
            }
        }

        if (primitive instanceof Node) {
            if (!selected)
                return !primitive.isTagged() && allParentWaysFiltered(primitive, hidden);
            if (onlyInvertedFilters)
                return selected && !oneParentWayNotFiltered(primitive, hidden);
            return true;
        } else
            return selected;

    }

    public boolean isHidden(OsmPrimitive primitive) {
        return test(hiddenFilters, primitive, true);
    }

    public boolean isDisabled(OsmPrimitive primitive) {
        return test(disabledFilters, primitive, false);
    }

}
