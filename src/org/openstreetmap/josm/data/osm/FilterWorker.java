// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;
import java.util.Collections;

import org.openstreetmap.josm.data.osm.FilterMatcher.FilterType;
import org.openstreetmap.josm.tools.Utils;

/**
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
     * @param all the collection of primitives for that the filter state should
     * be updated
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state (normal / disabled / hidden)
     * of any primitive has changed in the process
     */
    public static boolean executeFilters(Collection<OsmPrimitive> all, FilterMatcher filterMatcher) {
        boolean changed = false;
        // first relations, then ways and nodes last; this is required to resolve dependencies
        changed = doExecuteFilters(Utils.filter(all, OsmPrimitive.relationPredicate), filterMatcher);
        changed |= doExecuteFilters(Utils.filter(all, OsmPrimitive.wayPredicate), filterMatcher);
        changed |= doExecuteFilters(Utils.filter(all, OsmPrimitive.nodePredicate), filterMatcher);
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

    public static boolean executeFilters(OsmPrimitive primitive, FilterMatcher filterMatcher) {
        return doExecuteFilters(Collections.singleton(primitive), filterMatcher);
    }

    public static void clearFilterFlags(Collection<OsmPrimitive> prims) {
        for (OsmPrimitive osm : prims) {
            osm.unsetDisabledState();
        }
    }
}
