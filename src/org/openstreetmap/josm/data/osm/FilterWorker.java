// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Collection;

/**
 *
 * @author Petr_Dlouhý
 */
public class FilterWorker {
    /**
     * Apply the filters to the primitives of the data set.
     *
     * @param all the collection of primitives for that the filter state should
     * be updated
     * @param filterMatcher the FilterMatcher
     * @return true, if the filter state of any primitive has changed in the process
     */
    public static boolean executeFilters(Collection<OsmPrimitive> all, FilterMatcher filterMatcher) {

        boolean changed = false;

        // First relation and ways
        for (OsmPrimitive primitive: all) {
            if (!(primitive instanceof Node)) {
                if (filterMatcher.isHidden(primitive)) {
                    changed = changed | primitive.setDisabledState(true);
                } else if (filterMatcher.isDisabled(primitive)) {
                    changed = changed | primitive.setDisabledState(false);
                } else {
                    changed = changed | primitive.unsetDisabledState();
                }
            }
        }

        // Then nodes (because they state may depend on parent ways)
        for (OsmPrimitive primitive: all) {
            if (primitive instanceof Node) {
                if (filterMatcher.isHidden(primitive)) {
                    changed = changed | primitive.setDisabledState(true);
                } else if (filterMatcher.isDisabled(primitive)) {
                    changed = changed | primitive.setDisabledState(false);
                } else {
                    changed = changed | primitive.unsetDisabledState();
                }
            }
        }

        return changed;
    }

    public static boolean executeFilters(OsmPrimitive primitive, FilterMatcher filterMatcher) {
        boolean changed = false;
        if (filterMatcher.isHidden(primitive)) {
            changed = changed | primitive.setDisabledState(true);
        } else if (filterMatcher.isDisabled(primitive)) {
            changed = changed | primitive.setDisabledState(false);
        } else {
            changed = changed | primitive.unsetDisabledState();
        }
        return changed;
    }

    public static void clearFilterFlags(Collection<OsmPrimitive> prims) {
        for (OsmPrimitive osm : prims) {
            osm.unsetDisabledState();
        }
    }
}
