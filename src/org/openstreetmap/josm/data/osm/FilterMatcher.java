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
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Class that encapsulates the filter logic, i.e.&nbsp;applies a list of
 * filters to a primitive.
 *
 * Uses {@link Match#match} to see if the filter expression matches,
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
 * Multipolygons and (untagged) member ways are handled in a similar way.
 */
public class FilterMatcher {

    /**
     * Describes quality of the filtering.
     *
     * Depending on the context, this can either refer to disabled or
     * to hidden primitives.
     *
     * The distinction is necessary, because untagged nodes should only
     * "inherit" their filter property from the parent way, when the
     * parent way is hidden (or disabled) "explicitly" (i.e. by a non-inverted
     * filter). This way, filters like
     * <code>["child type:way", inverted, Add]</code> show the
     * untagged way nodes, as intended.
     *
     * This information is only needed for ways and relations, so nodes are
     * either <code>NOT_FILTERED</code> or <code>PASSIV</code>.
     */
    public enum FilterType {
        /** no filter applies */
        NOT_FILTERED,
        /** at least one non-inverted filter applies */
        EXPLICIT,
        /** at least one filter applies, but they are all inverted filters */
        PASSIV
    }

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

    /**
     * Check if primitive is filtered.
     * @param primitive the primitive to check
     * @param hidden the minimum level required for the primitive to count as filtered
     * @return when hidden is true, returns whether the primitive is hidden
     * when hidden is false, returns whether the primitive is disabled or hidden
     */
    private boolean isFiltered(OsmPrimitive primitive, boolean hidden) {
        return hidden ? primitive.isDisabledAndHidden() : primitive.isDisabled();
    }

    /**
     * Check if primitive is hidden explicitly.
     * Only used for ways and relations.
     * @param primitive the primitive to check
     * @param hidden the level where the check is performed
     * @return true, if at least one non-inverted filter applies to the primitive
     */
    private boolean isFilterExplicit(OsmPrimitive primitive, boolean hidden) {
        return hidden ? primitive.getHiddenType() : primitive.getDisabledType();
    }

    /**
     * Check if all parent ways are filtered.
     * @param primitive the primitive to check
     * @param hidden parameter that indicates the minimum level of filtering:
     * true when objects need to be hidden to count as filtered and
     * false when it suffices to be disabled to count as filtered
     * @return true if (a) there is at least one parent way
     * (b) all parent ways are filtered at least at the level indicated by the
     * parameter <code>hidden</code> and
     * (c) at least one of the parent ways is explicitly filtered
     */
    private boolean allParentWaysFiltered(OsmPrimitive primitive, boolean hidden) {
        List<OsmPrimitive> refs = primitive.getReferrers();
        boolean isExplicit = false;
        for (OsmPrimitive p: refs) {
            if (p instanceof Way) {
                if (!isFiltered(p, hidden))
                    return false;
                isExplicit |= isFilterExplicit(p, hidden);
            }
        }
        return isExplicit;
    }

    private boolean oneParentWayNotFiltered(OsmPrimitive primitive, boolean hidden) {
        List<OsmPrimitive> refs = primitive.getReferrers();
        for (OsmPrimitive p: refs) {
            if (p instanceof Way && !isFiltered(p, hidden))
                return true;
        }

        return false;
    }

    private boolean allParentMultipolygonsFiltered(OsmPrimitive primitive, boolean hidden) {
        boolean isExplicit = false;
        for (Relation r : new SubclassFilteredCollection<OsmPrimitive, Relation>(
                primitive.getReferrers(), OsmPrimitive.multipolygonPredicate)) {
            if (!isFiltered(r, hidden))
                return false;
            isExplicit |= isFilterExplicit(r, hidden);
        }
        return isExplicit;
    }

    private boolean oneParentMultipolygonNotFiltered(OsmPrimitive primitive, boolean hidden) {
        for (Relation r : new SubclassFilteredCollection<OsmPrimitive, Relation>(
                primitive.getReferrers(), OsmPrimitive.multipolygonPredicate)) {
            if (!isFiltered(r, hidden))
                return true;
        }
        return false;
    }

    private FilterType test(List<FilterInfo> filters, OsmPrimitive primitive, boolean hidden) {

        if (primitive.isIncomplete())
            return FilterType.NOT_FILTERED;

        boolean filtered = false;
        // If the primitive is "explicitly" hidden by a non-inverted filter.
        // Only interesting for nodes.
        boolean explicitlyFiltered = false;

        for (FilterInfo fi: filters) {
            if (fi.isDelete) {
                if (filtered && fi.match.match(primitive)) {
                    filtered = false;
                }
            } else {
                if ((!filtered || (!explicitlyFiltered && !fi.isInverted)) && fi.match.match(primitive)) {
                    filtered = true;
                    if (!fi.isInverted) {
                        explicitlyFiltered = true;
                    }
                }
            }
        }

        if (primitive instanceof Node) {
            if (filtered) {
                // If there is a parent way, that is not hidden, we  show the
                // node anyway, unless there is no non-inverted filter that
                // applies to the node directly.
                if (explicitlyFiltered)
                    return FilterType.PASSIV;
                else {
                    if (oneParentWayNotFiltered(primitive, hidden))
                        return FilterType.NOT_FILTERED;
                    else
                        return FilterType.PASSIV;
                }
            } else {
                if (!primitive.isTagged() && allParentWaysFiltered(primitive, hidden))
                    // Technically not hidden by any filter, but we hide it anyway, if
                    // it is untagged and all parent ways are hidden.
                    return FilterType.PASSIV;
                else
                    return FilterType.NOT_FILTERED;
            }
        } else if (primitive instanceof Way) {
            if (filtered) {
                if (explicitlyFiltered)
                    return FilterType.EXPLICIT;
                else {
                    if (oneParentMultipolygonNotFiltered(primitive, hidden))
                        return FilterType.NOT_FILTERED;
                    else
                        return FilterType.PASSIV;
                }
            } else {
                if (!primitive.isTagged() && allParentMultipolygonsFiltered(primitive, hidden))
                    return FilterType.EXPLICIT;
                else
                    return FilterType.NOT_FILTERED;
            }
        } else {
            if (filtered)
                return explicitlyFiltered ? FilterType.EXPLICIT : FilterType.PASSIV;
            else
                return FilterType.NOT_FILTERED;
        }

    }

    /**
     * Check if primitive is hidden.
     * The filter flags for all parent objects must be set correctly, when
     * calling this method.
     * @param primitive the primitive
     * @return FilterType.NOT_FILTERED when primitive is not hidden;
     * FilterType.EXPLICIT when primitive is hidden and there is a non-inverted
     * filter that applies;
     * FilterType.PASSIV when primitive is hidden and all filters that apply
     * are inverted
     */
    public FilterType isHidden(OsmPrimitive primitive) {
        return test(hiddenFilters, primitive, true);
    }

    /**
     * Check if primitive is disabled.
     * The filter flags for all parent objects must be set correctly, when
     * calling this method.
     * @param primitive the primitive
     * @return FilterType.NOT_FILTERED when primitive is not disabled;
     * FilterType.EXPLICIT when primitive is disabled and there is a non-inverted
     * filter that applies;
     * FilterType.PASSIV when primitive is disabled and all filters that apply
     * are inverted
     */
    public FilterType isDisabled(OsmPrimitive primitive) {
        return test(disabledFilters, primitive, false);
    }

}
