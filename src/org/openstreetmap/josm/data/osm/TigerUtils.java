// License: GPL. Copyright 2007 by Dave Hansen and others
package org.openstreetmap.josm.data.osm;

import java.util.Set;
import java.util.TreeSet;

/**
 * A simple class to keep helper functions for merging TIGER data
 *
 * @author daveh
 *
 */
public class TigerUtils {

    public static boolean isTigerTag(String tag)
    {
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
            return new Integer(name);
        return name;
    }

    public static String combineTags(String name, Set<String> values) {
        TreeSet<Object> resultSet = new TreeSet<Object>();
        for (String value: values) {
            for (String part: value.split(":")) {
               resultSet.add(tagObj(part));
            }
        }
        String combined = "";
        for (Object part : resultSet) {
            if (combined.length() > 0)
                combined += ":";
            combined += part;
        }
        return combined;
    }

    public static String combineTags(String name, String t1, String t2) {
        Set<String> set = new TreeSet<String>();
        set.add(t1);
        set.add(t2);
        return TigerUtils.combineTags(name, set);
    }
}
