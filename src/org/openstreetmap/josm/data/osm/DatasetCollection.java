// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

abstract class DatasetCollection extends AbstractCollection<OsmPrimitive> {

    private class FilterIterator implements Iterator<OsmPrimitive> {

        private final Iterator<OsmPrimitive> iterator;
        private OsmPrimitive current;

        public FilterIterator(Iterator<OsmPrimitive> iterator) {
            this.iterator = iterator;
        }

        private void findNext() {
            if (current == null) {
                while (iterator.hasNext()) {
                    current = iterator.next();
                    if (filter(current))
                        return;
                }
                current = null;
            }
        }

        public boolean hasNext() {
            findNext();
            return current != null;
        }

        public OsmPrimitive next() {
            findNext();
            OsmPrimitive old = current;
            current = null;
            return old;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private final Collection<OsmPrimitive> primitives;

    public DatasetCollection(Collection<OsmPrimitive> primitives) {
        this.primitives = primitives;
    }

    protected abstract boolean filter(OsmPrimitive primitive);

    @Override
    public Iterator<OsmPrimitive> iterator() {
        return new FilterIterator(primitives.iterator());
    }

    @Override
    public int size() {
        int size = 0;
        Iterator<OsmPrimitive> it = iterator();
        while (it.hasNext()) {
            size++;
            it.next();
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    public static class AllNonDeleted extends DatasetCollection {

        public AllNonDeleted(Collection<OsmPrimitive> primitives) {
            super(primitives);
        }

        @Override
        protected boolean filter(OsmPrimitive primitive) {
            return primitive.isVisible() && !primitive.isDeleted();
        }

    }

    public static class AllNonDeletedComplete extends DatasetCollection {

        public AllNonDeletedComplete(Collection<OsmPrimitive> primitives) {
            super(primitives);
        }

        @Override
        protected boolean filter(OsmPrimitive primitive) {
            return primitive.isVisible() && !primitive.isDeleted() && !primitive.isIncomplete();
        }

    }

    public static class AllNonDeletedPhysical extends DatasetCollection {

        public AllNonDeletedPhysical(Collection<OsmPrimitive> primitives) {
            super(primitives);
        }

        @Override
        protected boolean filter(OsmPrimitive primitive) {
            return primitive.isVisible() && !primitive.isDeleted() && !primitive.isIncomplete() && !(primitive instanceof Relation);
        }

    }

    public static class AllModified extends DatasetCollection {

        public AllModified(Collection<OsmPrimitive> primitives) {
            super(primitives);
        }

        @Override
        protected boolean filter(OsmPrimitive primitive) {
            return primitive.isVisible() && primitive.isModified();
        }

    }

}