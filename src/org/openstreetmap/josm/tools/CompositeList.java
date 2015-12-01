// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.AbstractList;
import java.util.List;

/**
 * Joined List build from two Lists (read-only).
 *
 * Extremely simple single-purpose implementation.
 * @param <T> item type
 * @since 7109
 */
public class CompositeList<T> extends AbstractList<T> {
    private final List<? extends T> a, b;

    /**
     * Constructs a new {@code CompositeList} from two lists.
     * @param a First list
     * @param b Second list
     */
    public CompositeList(List<? extends T> a, List<? extends T> b) {
        this.a = a;
        this.b = b;
    }

    @Override
    public T get(int index) {
        return index < a.size() ? a.get(index) : b.get(index - a.size());
    }

    @Override
    public int size() {
        return a.size() + b.size();
    }
}
