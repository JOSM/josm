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

    public static boolean tagIsInt(String name) {
        if (name.equals("tiger:tlid"))
            return true;
        return false;
    }

    public static Object tagObj(String name) {
        if (tagIsInt(name))
            return Integer.valueOf(name);
        return name;
    }

    public static String combineTags(String name, Set<String> values) {
        TreeSet<Object> resultSet = new TreeSet<Object>();
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
                combined.append(":");
            }
            combined.append(part);
        }
        return combined.toString();
    }

    public static String combineTags(String name, String t1, String t2) {
        Set<String> set = new TreeSet<String>();
        set.add(t1);
        set.add(t2);
        return TigerUtils.combineTags(name, set);
    }
}
