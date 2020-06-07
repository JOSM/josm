// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

/**
 * An interface allowing injection of hashcode and equality implementation
 * based on some inner state of an object for a set.
 * It supports two type parameters to implement effective foreign key implementation
 * inside {@link Storage}, but for basic use, both type parameters are the same.
 *
 * For use cases, see {@link Storage}.
 * @author nenik
 * @param <K> type for hashCode and first equals parameter
 * @param <T> type for second equals parameter
 */
public interface Hash<K, T> {

    /**
     * Get hashcode for given instance, based on some inner state of the
     * instance. The returned hashcode should remain constant over the time,
     * so it should be based on some instance invariant.
     *
     * @param k the object to compute hashcode for
     * @return computed hashcode
     */
    int getHashCode(K k);

    /**
     * Compare two instances for semantic or lookup equality. For use cases
     * where it compares different types, refer to {@link Storage}.
     *
     * @param k the object to compare
     * @param t the object to compare
     * @return true if the objects are semantically equivalent, or if k
     * uniquely identifies t in given class.
     */
    boolean equals(K k, T t);
}
