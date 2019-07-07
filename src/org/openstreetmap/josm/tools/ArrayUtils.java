// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.tools;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility methods for arrays.
 * @since 15226
 */
public final class ArrayUtils {

    /**
     * Utility class
     */
    private ArrayUtils() {
        // Hide default constructor for utility classes
    }

    /**
     * Converts an array of int to a list of Integer.
     * @param array array of int
     * @return list of Integer
     */
    public static List<Integer> toList(int[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

    /**
     * Converts an array of long to a list of Long.
     * @param array array of long
     * @return list of Long
     */
    public static List<Long> toList(long[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }

    /**
     * Converts an array of double to a list of Double.
     * @param array array of double
     * @return list of Double
     */
    public static List<Double> toList(double[] array) {
        return Arrays.stream(array).boxed().collect(Collectors.toList());
    }
}
