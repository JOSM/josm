// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Filtered view of a collection.
 * (read-only collection, but elements can be changed, of course)
 * Lets you iterate through those elements of a given collection that satisfy a
 * certain condition (imposed by a predicate).
 * <p>
 * The behaviour of this class is undefined if the underlying collection is changed.
 * @param <S> element type of the underlying collection
 * @param <T> element type of filtered collection (and subclass of S). The predicate
 *      must accept only objects of type T.
 * @since 3147
 */
public class SubclassFilteredCollection<S, T extends S> extends AbstractCollection<T> {

    private final Collection<? extends S> collection;
    private final Predicate<? super S> predicate;
    private int size = -1;

    private class FilterIterator implements Iterator<T> {

        private final Iterator<? extends S> iterator;
        private S current;

        FilterIterator(Iterator<? extends S> iterator) {
            this.iterator = iterator;
        }

        private void findNext() {
            if (current == null) {
                while (iterator.hasNext()) {
                    current = iterator.next();
                    if (predicate.test(current))
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
            if (!hasNext())
                throw new NoSuchElementException();
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

    /**
     * Constructs a new {@code SubclassFilteredCollection}.
     * @param collection The base collection to filter
     * @param predicate The predicate to use as filter
     * @see #filter(Collection, Predicate) for an alternative way to construct this.
     */
    public SubclassFilteredCollection(Collection<? extends S> collection, Predicate<? super S> predicate) {
        this.collection = Objects.requireNonNull(collection);
        this.predicate = Objects.requireNonNull(predicate);
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

    /**
     * Create a new filtered collection without any constraints on the predicate type.
     * @param <T> The collection type.
     * @param collection The collection to filter.
     * @param predicate The predicate to filter for.
     * @return The filtered collection. It is a {@code Collection<T>}.
     */
    public static <T> SubclassFilteredCollection<T, T> filter(Collection<? extends T> collection, Predicate<T> predicate) {
        return new SubclassFilteredCollection<>(collection, predicate);
    }
}
