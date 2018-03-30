// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static java.util.Comparator.comparing;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.openstreetmap.josm.tools.AlphanumComparator;

/**
 * Comparators for comparing primitives.
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
        final Comparator<String> digitsLast = comparing(str -> !str.isEmpty() && Character.isDigit(str.charAt(0)) ? 1 : 0);
        return comparing(memoize(DefaultNameFormatter.getInstance()::format),
                digitsLast.thenComparing(AlphanumComparator.getInstance()));
    }

    /**
     * Returns a comparator comparing primitives by their {@linkplain OsmPrimitive#getUniqueId unique id}.
     *
     * @return a comparator comparing primitives by their {@linkplain OsmPrimitive#getUniqueId unique id}.
     */
    public static Comparator<OsmPrimitive> comparingUniqueId() {
        return comparing(OsmPrimitive::getUniqueId);
    }

    /**
     * Returns a comparator ordering the primitives by type in the order NODE, WAY, RELATION
     *
     * @return a comparator ordering the primitives by type in the order NODE, WAY, RELATION
     */
    public static Comparator<OsmPrimitive> orderingNodesWaysRelations() {
        return comparingInt(osm -> osm.getType().ordinal());
    }

    /**
     * Returns a comparator ordering the primitives by type in the order WAY, RELATION, NODE
     *
     * @return a comparator ordering the primitives by type in the order WAY, RELATION, NODE
     */
    public static Comparator<OsmPrimitive> orderingWaysRelationsNodes() {
        return comparingInt(osm -> {
            switch (osm.getType()) {
                case WAY:
                    return 1;
                case RELATION:
                    return 2;
                case NODE:
                    return 3;
                default:
                    throw new IllegalStateException();
            }
        });
    }

    /**
     * Returns a comparator ordering the primitives by type in the order RELATION, WAY, NODE
     *
     * @return a comparator ordering the primitives by type in the order RELATION, WAY, NODE
     * @since 11679
     */
    public static Comparator<OsmPrimitive> orderingRelationsWaysNodes() {
        return comparingInt(osm -> {
            switch (osm.getType()) {
                case RELATION:
                    return 1;
                case WAY:
                    return 2;
                case NODE:
                    return 3;
                default:
                    throw new IllegalStateException();
            }
        });
    }

    private static <T, R> Function<T, R> memoize(Function<T, R> base) {
        final Map<T, R> cache = new HashMap<>();
        return t -> cache.computeIfAbsent(t, base);
    }

    private OsmPrimitiveComparator() {
    }
}
