// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.tools;

import java.util.HashMap;
import java.util.LinkedHashSet;

/**
 * Maps keys to ordered sets of values.
 */
public class MultiMap<A, B> extends HashMap<A, LinkedHashSet<B>> {
    /**
     * Map a key to a value. Can be called multiple times with the same key, but different value.
     */
    public void put(A key, B value) {
        LinkedHashSet<B> vals = get(key);
        if (vals == null) {
            vals = new LinkedHashSet<B>();
            put(key, vals);
        }
        vals.add(value);
    }

    /**
     * Put a key that maps to nothing.
     */
    public void putVoid(A key) {
        if (containsKey(key))
            return;
        put(key, new LinkedHashSet<B>());
    }

    /**
     * Returns a list of values for the given key
     * or an empty list, if it maps to nothing.
     */
    public LinkedHashSet<B> getValues(A key) {
        if (!containsKey(key))
            return new LinkedHashSet<B>();
        return get(key);
    }
}
