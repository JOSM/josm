// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

/**
 * Filtered view of a collection.
 * (read-only collection, but elements can be changed, of course)
 * Lets you iterate through those elements of a given collection that satisfy a
 * certain condition (imposed by a predicate).
 * @param <S> element type of the underlying collection
 * @param <T> element type of filtered collection (and subclass of S). The predicate
 *      must accept only objects of type T.
 */
public class SubclassFilteredCollection<S, T extends S> extends AbstractCollection<T> {

    private final Collection<? extends S> collection;
    private final Predicate<? super S> predicate;
    int size = -1;

    private class FilterIterator implements Iterator<T> {

        private final Iterator<? extends S> iterator;
        private S current;

        public FilterIterator(Iterator<? extends S> iterator) {
            this.iterator = iterator;
        }

        private void findNext() {
            if (current == null) {
                while (iterator.hasNext()) {
                    current = iterator.next();
                    if (predicate.evaluate(current))
                        return;
                }
                current = null;
            }
        }

        @Override
        public boolean hasNext() {
            findNext();
            return current != null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next() {
            findNext();
            S old = current;
            current = null;
            // we are save because predicate only accepts objects of type T
            return (T) old;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    public SubclassFilteredCollection(Collection<? extends S> collection, Predicate<? super S> predicate) {
        this.collection = collection;
        this.predicate = predicate;
    }

    @Override
    public Iterator<T> iterator() {
        return new FilterIterator(collection.iterator());
    }

    @Override
    public int size() {
        if (size == -1) {
            size = 0;
            Iterator<T> it = iterator();
            while (it.hasNext()) {
                size++;
                it.next();
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

}
