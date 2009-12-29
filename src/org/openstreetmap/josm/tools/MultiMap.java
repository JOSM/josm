// License: GPL. See LICENSE file for details.
package org.openstreetmap.josm.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

/**
 * Maps keys to a list of values. Partial implementation. Extend if you need more!
 */
public class MultiMap<A, B> extends HashMap<A, List<B>> {
    public void add(A key, B value) {
        List<B> vals = get(key);
        if (vals == null) {
            vals = new ArrayList<B>();
            put(key, vals);
        }
        vals.add(value);
    }
}
