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

    /**
     * Get minimum of 3 values
     */
    public static int min(int a, int b, int c) {
        if (b < c) {
            if (a < b)
                return a;
            return b;
        } else {
            if (a < c) {
                return a;
            }
            return c;
        }
    }

    /**
     * return the modulus in the range [0, n)
     */
    public static int mod(int a, int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        int res = a % n;
        if (res < 0) {
            res += n;
        }
        return res;
    }

}
