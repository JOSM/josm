// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

public class Utils {

    public static <T> boolean exists(Iterable<? extends T> coll, Predicate<? super T> pred) {
        for (T el : coll) {
            if (pred.evaluate(el))
                return true;
        }
        return false;
    }
}
