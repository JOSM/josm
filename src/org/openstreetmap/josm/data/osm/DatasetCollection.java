// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import org.openstreetmap.josm.tools.Predicate;

public class DatasetCollection<T extends OsmPrimitive> extends AbstractCollection<T> {

    private class FilterIterator implements Iterator<T> {

        private final Iterator<? extends OsmPrimitive> iterator;
        private OsmPrimitive current;

        public FilterIterator(Iterator<? extends OsmPrimitive> iterator) {
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

        public boolean hasNext() {
            findNext();
            return current != null;
        }

        @SuppressWarnings("unchecked")
        public T next() {
            findNext();
            OsmPrimitive old = current;
            current = null;
            return (T)old;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final Collection<? extends OsmPrimitive> primitives;
    private final Predicate<OsmPrimitive> predicate;
    int size = -1;

    public DatasetCollection(Collection<? extends OsmPrimitive> primitives, Predicate<OsmPrimitive> predicate) {
        this.primitives = primitives;
        this.predicate = predicate;
    }

    @Override
    public Iterator<T> iterator() {
        return new FilterIterator(primitives.iterator());
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
