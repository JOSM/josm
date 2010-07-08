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
     * There are certain rules to ensure that a way is not displayed "naked"
     * without its nodes (1) and on the other hand to avoid hiding a way but
     * leaving its nodes visible as a cloud of points (2).
     *
     * In normal (non-inverted) mode only problem (2) is relevant.
     * Untagged child nodes of filtered ways that are not used by other
     * unfiltered ways are filtered as well.
     *
     * If a filter applies explicitly to a node, (2) is ignored and it
     * is filtered in any case.
     *
     * In inverted mode usually only problem (1) is relevant.
     * If the inverted filter applies explicitly to a node, this no longer
     * means it is filtered in any case:
     * E.g. the filter [searchtext="highway=footway", inverted=true] displays
     * the footways only. But that does not mean, the nodes of the footway
     * (which do not have the highway tag) should be filtered as well.
     *
     * So first the Filter is applied for ways and relations. Then to nodes
     * (but hides them only if they are not used by any unfiltered way).
     */
    public static void executeFilters(Collection<OsmPrimitive> all, FilterMatcher filterMatcher) {

        // First relation and ways
        for (OsmPrimitive primitive: all) {
            if (!(primitive instanceof Node)) {
                if (filterMatcher.isHidden(primitive)) {
                    primitive.setDisabledState(true);
                } else if (filterMatcher.isDisabled(primitive)) {
                    primitive.setDisabledState(false);
                } else {
                    primitive.unsetDisabledState();
                }
            }
        }

        // Then nodes (because they state may depend on parent ways)
        for (OsmPrimitive primitive: all) {
            if (primitive instanceof Node) {
                if (filterMatcher.isHidden(primitive)) {
                    primitive.setDisabledState(true);
                } else if (filterMatcher.isDisabled(primitive)) {
                    primitive.setDisabledState(false);
                } else {
                    primitive.unsetDisabledState();
                }
            }
        }
    }

    public static void clearFilterFlags(Collection<OsmPrimitive> prims) {
        for (OsmPrimitive osm : prims) {
            osm.unsetDisabledState();
        }
    }
}
