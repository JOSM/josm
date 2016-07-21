// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Used to identify objects that fulfill a certain condition, e.g. when filtering a collection.
 *
 * @param <T> The objects type
 * @since 3177
 * @deprecated Use {@link java.util.function.Predicate} instead.
 */
@Deprecated
@FunctionalInterface
public interface Predicate<T> extends java.util.function.Predicate<T> {

    /**
     * Determines whether the object passes the test or not
     * @param object The object to evaluate
     * @return {@code true} if the object passes the test, {@code false} otherwise
     */
    boolean evaluate(T object);

    @Override
    default boolean test(T t) {
        return evaluate(t);
    }
}
