// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.FilterMatcher.FilterType;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;

/**
 * Class for applying {@link Filter}s to {@link IPrimitive}s.
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
     * @param <T> The primitive type
     * @param all the collection of primitives for that the filter state should be updated
     * @param filters the filters
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     * @throws SearchParseError if the search expression in a filter cannot be parsed
     * @since 12383, xxx (generics)
     */
    public static <T extends IPrimitive & IFilterablePrimitive> boolean executeFilters(Collection<T> all, Filter... filters)
            throws SearchParseError {
        return executeFilters(all, FilterMatcher.of(filters));
    }

    /**
     * Apply the filters to the primitives of the data set.
     *
     * @param <T> The primitive type
     * @param all the collection of primitives for that the filter state should be updated
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     * @since 17862 (generics)
     */
    public static <T extends IPrimitive & IFilterablePrimitive> boolean executeFilters(Collection<T> all, FilterMatcher filterMatcher) {
        boolean changed;
        // first relations, then ways and nodes last; this is required to resolve dependencies
        changed = doExecuteFilters(SubclassFilteredCollection.filter(all, IRelation.class::isInstance), filterMatcher);
        changed |= doExecuteFilters(SubclassFilteredCollection.filter(all, IWay.class::isInstance), filterMatcher);
        changed |= doExecuteFilters(SubclassFilteredCollection.filter(all, INode.class::isInstance), filterMatcher);
        return changed;
    }

    private static <T extends IPrimitive & IFilterablePrimitive> boolean doExecuteFilters(Collection<T> all, FilterMatcher filterMatcher) {

        boolean changed = false;

        for (T primitive : all) {
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
     * @param <T> the primitive type
     * @param primitive the primitive
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state (normal / disabled / hidden)
     * of the primitive has changed in the process
     * @since 17862 (generics)
     */
    public static <T extends IPrimitive & IFilterablePrimitive> boolean executeFilters(T primitive, FilterMatcher filterMatcher) {
        return doExecuteFilters(Collections.singleton(primitive), filterMatcher);
    }

    /**
     * Clear all filter flags, i.e.&nbsp;turn off filters.
     * @param <T> the primitive type
     * @param prims the primitives
     * @return true, if the filter state (normal / disabled / hidden) of any primitive has changed in the process
     * @since 12388 (signature)
     */
    public static <T extends IPrimitive & IFilterablePrimitive> boolean clearFilterFlags(Collection<T> prims) {
        boolean changed = false;
        for (T osm : prims) {
            changed |= osm.unsetDisabledState();
        }
        return changed;
    }
}
