// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

/**
 * Used to identify objects that fulfill a certain condition, e.g. when filtering a collection.
 *
 * @param <T> The objects type
 * @since 3177
 */
public interface Predicate<T> {
    
    /**
     * Determines whether the object passes the test or not
     * @param object The object to evaluate
     * @return {@code true} if the object passes the test, {@code false} otherwise
     */
    public boolean evaluate(T object);
}
