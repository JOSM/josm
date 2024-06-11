// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

/**
 * This is a proxy of a collection that notifies a listener on every collection change
 * @author Michael Zangl
 *
 * @param <T> The entry type
 * @since 12267 (extracted from GpxData)
 * @since 12156
 */
public class ListeningCollection<T> extends AbstractCollection<T> {
    private final List<T> base;
    private final Runnable runOnModification;

    /**
     * Constructs a new {@code ListeningCollection}.
     * @param base base collection
     * @param runOnModification runnable run at each modification
     * @since 12269
     */
    public ListeningCollection(List<T> base, Runnable runOnModification) {
        this.base = base;
        this.runOnModification = runOnModification;
    }

    @Override
    public final Iterator<T> iterator() {
        Iterator<T> it = base.iterator();
        return new Iterator<>() {
            private T object;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                object = it.next();
                return object;
            }

            @Override
            public void remove() {
                if (object != null) {
                    removed(object);
                    object = null;
                }
                it.remove();
            }
        };
    }

    @Override
    public final int size() {
        return base.size();
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean remove(Object o) {
        boolean remove = base.remove(o);
        if (remove) {
            removed((T) o);
        }
        return remove;
    }

    @Override
    public final boolean add(T e) {
        boolean add = base.add(e);
        added(e);
        return add;
    }

    /**
     * Called when an object is removed.
     * @param object the removed object
     */
    protected void removed(T object) {
        runOnModification.run();
    }

    /**
     * Called when an object is added.
     * @param object the added object
     */
    protected void added(T object) {
        runOnModification.run();
    }
}
