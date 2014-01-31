// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class OsmUtils {

    private OsmUtils() {
        // Hide default constructor for utils classes
    }

    static final List<String> TRUE_VALUES = new ArrayList<String>(Arrays
            .asList(new String[] { "true", "yes", "1", "on" }));
    static final List<String> FALSE_VALUES = new ArrayList<String>(Arrays
            .asList(new String[] { "false", "no", "0", "off" }));
    static final List<String> REVERSE_VALUES = new ArrayList<String>(Arrays
            .asList(new String[] { "reverse", "-1" }));

    public static final String trueval = "yes";
    public static final String falseval = "no";
    public static final String reverseval = "-1";

    public static Boolean getOsmBoolean(String value) {
        if(value == null) return null;
        String lowerValue = value.toLowerCase(Locale.ENGLISH);
        if (TRUE_VALUES.contains(lowerValue)) return Boolean.TRUE;
        if (FALSE_VALUES.contains(lowerValue)) return Boolean.FALSE;
        return null;
    }

    public static String getNamedOsmBoolean(String value) {
        Boolean res = getOsmBoolean(value);
        return res == null ? value : (res ? trueval : falseval);
    }

    public static boolean isReversed(String value) {
        return REVERSE_VALUES.contains(value);
    }

    public static boolean isTrue(String value) {
        return TRUE_VALUES.contains(value);
    }

    public static boolean isFalse(String value) {
        return FALSE_VALUES.contains(value);
    }
}
