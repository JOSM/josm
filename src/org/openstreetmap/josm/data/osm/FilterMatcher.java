// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openstreetmap.josm.actions.search.SearchAction.SearchMode;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.Not;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;

/**
 * Class that encapsulates the filter logic, i.e.&nbsp;applies a list of
 * filters to a primitive.
 *
 * Uses {@link SearchCompiler.Match#match} to see if the filter expression matches,
 * cares for "inverted-flag" of the filters and combines the results of all active
 * filters.
 *
 * There are two major use cases:
 *
 * (1) Hide features that you don't like to edit but get in the way, e.g.
 * <code>landuse</code> or power lines. It is expected, that the inverted flag
 * if false for these kind of filters.
 *
 * (2) Highlight certain features, that are currently interesting and hide everything
 * else. This can be thought of as an improved search (Ctrl-F), where you can
 * continue editing and don't loose the current selection. It is expected that
 * the inverted flag of the filter is true in this case.
 *
 * In addition to the formal application of filter rules, some magic is applied
 * to (hopefully) match the expectations of the user:
 *
 * (1) non-inverted: When hiding a way, all its untagged nodes are hidden as well.
 * This avoids a "cloud of nodes", that normally isn't useful without the
 * corresponding way.
 *
 * (2) inverted: When displaying a way, we show all its nodes, although the
 * individual nodes do not match the filter expression. The reason is, that a
 * way without its nodes cannot be edited properly.
 *
 */
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

        if (primitive.isIncomplete())
            return false;

        boolean selected = false;
        // If the primitive is "explicitly" hidden by a non-inverted filter.
        // Only interesting for nodes.
        boolean explicitlyHidden = false;

        for (FilterInfo fi: filters) {
            if (fi.isDelete) {
                if (selected && fi.match.match(primitive)) {
                    selected = false;
                }
            } else {
                if ((!selected || (!explicitlyHidden && !fi.isInverted)) && fi.match.match(primitive)) {
                    selected = true;
                    if (!fi.isInverted) {
                        explicitlyHidden = true;
                    }
                }
            }
        }

        if (primitive instanceof Node) {
            // Technically not hidden by any filter, but we hide it anyway, if
            // it is untagged and all parent ways are hidden.
            if (!selected)
                return !primitive.isTagged() && allParentWaysFiltered(primitive, hidden);
            // At this point, selected == true, so the node is hidden.
            // However, if there is a parent way, that is not hidden, we ignore
            // this and show the node anyway, unless there is no non-inverted
            // filter that applies to the node directly.
            if (!explicitlyHidden)
                return !oneParentWayNotFiltered(primitive, hidden);
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
