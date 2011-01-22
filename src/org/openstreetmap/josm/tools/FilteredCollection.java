// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Collection;

/**
 * The same as SubclassFilteredCollection, but does not restrict the type
 * of the collection to a certain subclass.
 */
public class FilteredCollection<T> extends SubclassFilteredCollection<T, T> {

    public FilteredCollection(Collection<? extends T> collection, Predicate<? super T> predicate) {
        super(collection, predicate);
    }

}
