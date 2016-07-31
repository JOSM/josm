// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * The same as SubclassFilteredCollection, but does not restrict the type
 * of the collection to a certain subclass.
 * @param <T> element type of the underlying collection
 * @since 3802
 */
public class FilteredCollection<T> extends SubclassFilteredCollection<T, T> {

    /**
     * Constructs a new {@code FilteredCollection}.
     * @param collection The base collection to filter
     * @param predicate The predicate to use as filter
     */
    public FilteredCollection(Collection<? extends T> collection, Predicate<? super T> predicate) {
        super(collection, predicate);
    }
}
