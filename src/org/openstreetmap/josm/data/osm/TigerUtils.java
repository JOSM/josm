// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Set;
import java.util.TreeSet;

/**
 * A simple class to keep helper functions for merging TIGER data
 *
 * @author daveh
 * @since 529
 */
public final class TigerUtils {

    private TigerUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Determines if the given tag is a TIGER one
     * @param tag The tag to check
     * @return {@code true} if {@code tag} starts with {@code tiger:} namespace
     */
    public static boolean isTigerTag(String tag) {
        if (tag.indexOf("tiger:") == -1)
            return false;
        return true;
    }

    /**
     * Determines if the given key denotes an integer value.
     * @param name The key to determine
     * @return {@code true} if the given key denotes an integer value
     */
    public static boolean tagIsInt(String name) {
        if ("tiger:tlid".equals(name))
            return true;
        return false;
    }

    public static Object tagObj(String name) {
        if (tagIsInt(name))
            return Integer.valueOf(name);
        return name;
    }

    public static String combineTags(Set<String> values) {
        Set<Object> resultSet = new TreeSet<>();
        for (String value: values) {
            String[] parts = value.split(":");
            for (String part: parts) {
               resultSet.add(tagObj(part));
            }
            // Do not produce useless changeset noise if a single value is used and does not contain redundant splitted parts (fix #7405)
            if (values.size() == 1 && resultSet.size() == parts.length) {
                return value;
            }
        }
        StringBuilder combined = new StringBuilder();
        for (Object part : resultSet) {
            if (combined.length() > 0) {
                combined.append(':');
            }
            combined.append(part);
        }
        return combined.toString();
    }
}
