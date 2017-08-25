// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.FilterMatcher.FilterType;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Class for applying {@link Filter}s to {@link OsmPrimitive}s.
 *
 * Provides a bridge between Filter GUI and the data.
 *
 * @author Petr_Dlouhý
 */
public final class FilterWorker {

    private FilterWorker() {
        // Hide default constructor for utils classes
    }

    /**
     * Apply the filters to the primitives of the data set.
     *
     * @param all the collection of primitives for that the filter state should be updated
     * @param filters the filters
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     * @throws SearchParseError if the search expression in a filter cannot be parsed
     * @since 12383
     */
    public static boolean executeFilters(Collection<OsmPrimitive> all, Filter... filters) throws SearchParseError {
        return executeFilters(all, FilterMatcher.of(filters));
    }

    /**
     * Apply the filters to the primitives of the data set.
     *
     * @param all the collection of primitives for that the filter state should be updated
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     */
    public static boolean executeFilters(Collection<OsmPrimitive> all, FilterMatcher filterMatcher) {
        boolean changed;
        // first relations, then ways and nodes last; this is required to resolve dependencies
        changed = doExecuteFilters(SubclassFilteredCollection.filter(all, Relation.class::isInstance), filterMatcher);
        changed |= doExecuteFilters(SubclassFilteredCollection.filter(all, Way.class::isInstance), filterMatcher);
        changed |= doExecuteFilters(SubclassFilteredCollection.filter(all, Node.class::isInstance), filterMatcher);
        return changed;
    }

    private static boolean doExecuteFilters(Collection<OsmPrimitive> all, FilterMatcher filterMatcher) {

        boolean changed = false;

        for (OsmPrimitive primitive: all) {
            FilterType hiddenType = filterMatcher.isHidden(primitive);
            if (hiddenType != FilterType.NOT_FILTERED) {
                changed |= primitive.setDisabledState(true);
                primitive.setHiddenType(hiddenType == FilterType.EXPLICIT);
            } else {
                FilterType disabledType = filterMatcher.isDisabled(primitive);
                if (disabledType != FilterType.NOT_FILTERED) {
                    changed |= primitive.setDisabledState(false);
                    primitive.setDisabledType(disabledType == FilterType.EXPLICIT);
                } else {
                    changed |= primitive.unsetDisabledState();
                }
            }
        }
        return changed;
    }

    /**
     * Apply the filters to a single primitive.
     *
     * @param primitive the primitive
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state (normal / disabled / hidden)
     * of the primitive has changed in the process
     */
    public static boolean executeFilters(OsmPrimitive primitive, FilterMatcher filterMatcher) {
        return doExecuteFilters(Collections.singleton(primitive), filterMatcher);
    }

    /**
     * Clear all filter flags, i.e.&nbsp;turn off filters.
     * @param prims the primitives
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     * @since 12388 (signature)
     */
    public static boolean clearFilterFlags(Collection<OsmPrimitive> prims) {
        boolean changed = false;
        for (OsmPrimitive osm : prims) {
            changed |= osm.unsetDisabledState();
        }
        return changed;
    }
}
