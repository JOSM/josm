// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Comparator;

/**
 * Comparators for comparing {@link OsmPrimitive}.
 * @since 4113
 */
public final class OsmPrimitiveComparator {

    /**
     * Returns a comparator comparing primitives by their name using {@link DefaultNameFormatter}.
     *
     * {@linkplain DefaultNameFormatter#format(IPrimitive) Formatted names} are cached.
     *
     * @return a comparator comparing primitives by their name using {@link DefaultNameFormatter}
     */
    public static Comparator<OsmPrimitive> comparingNames() {
        return PrimitiveComparator.doComparingNames();
    }

    /**
     * Returns a comparator comparing primitives by their {@linkplain OsmPrimitive#getUniqueId unique id}.
     *
     * @return a comparator comparing primitives by their {@linkplain OsmPrimitive#getUniqueId unique id}.
     */
    public static Comparator<OsmPrimitive> comparingUniqueId() {
        return PrimitiveComparator.doComparingUniqueId();
    }

    /**
     * Returns a comparator ordering the primitives by type in the order NODE, WAY, RELATION
     *
     * @return a comparator ordering the primitives by type in the order NODE, WAY, RELATION
     */
    public static Comparator<OsmPrimitive> orderingNodesWaysRelations() {
        return PrimitiveComparator.doOrderingNodesWaysRelations();
    }

    /**
     * Returns a comparator ordering the primitives by type in the order WAY, RELATION, NODE
     *
     * @return a comparator ordering the primitives by type in the order WAY, RELATION, NODE
     */
    public static Comparator<OsmPrimitive> orderingWaysRelationsNodes() {
        return PrimitiveComparator.doOrderingWaysRelationsNodes();
    }

    /**
     * Returns a comparator ordering the primitives by type in the order RELATION, WAY, NODE
     *
     * @return a comparator ordering the primitives by type in the order RELATION, WAY, NODE
     * @since 11679
     */
    public static Comparator<OsmPrimitive> orderingRelationsWaysNodes() {
        return PrimitiveComparator.doOrderingRelationsWaysNodes();
    }

    private OsmPrimitiveComparator() {
    }
}
