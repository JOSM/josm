// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.tools;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps keys to ordered sets of values.
 */
public class MultiMap<A, B>  {

    private final Map<A, LinkedHashSet<B>> map = new HashMap<A, LinkedHashSet<B>>();
    /**
     * Map a key to a value. Can be called multiple times with the same key, but different value.
     */
    public void put(A key, B value) {
        LinkedHashSet<B> vals = map.get(key);
        if (vals == null) {
            vals = new LinkedHashSet<B>();
            map.put(key, vals);
        }
        vals.add(value);
    }

    /**
     * Put a key that maps to nothing.
     */
    public void putVoid(A key) {
        if (map.containsKey(key))
            return;
        map.put(key, new LinkedHashSet<B>());
    }

    /**
     * Returns a list of values for the given key
     * or an empty list, if it maps to nothing.
     */
    public LinkedHashSet<B> getValues(A key) {
        if (!map.containsKey(key))
            return new LinkedHashSet<B>();
        return map.get(key);
    }

    public Set<A> keySet() {
        return map.keySet();
    }

    public Set<B> get(A key) {
        return map.get(key);
    }
}
