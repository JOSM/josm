// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.TextTagParser;

public final class OsmUtils {

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays
            .asList("true", "yes", "1", "on"));
    private static final Set<String> FALSE_VALUES = new HashSet<>(Arrays
            .asList("false", "no", "0", "off"));
    private static final Set<String> REVERSE_VALUES = new HashSet<>(Arrays
            .asList("reverse", "-1"));

    public static final String trueval = "yes";
    public static final String falseval = "no";
    public static final String reverseval = "-1";

    private OsmUtils() {
        // Hide default constructor for utils classes
    }

    public static Boolean getOsmBoolean(String value) {
        if (value == null) return null;
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

    /**
     * Creates a new OSM primitive according to the given assertion. Originally written for unit tests,
     * this can also be used in another places like validation of local MapCSS validator rules.
     * @param assertion The assertion describing OSM primitive (ex: "way name=Foo railway=rail")
     * @return a new OSM primitive according to the given assertion
     * @throws IllegalArgumentException if assertion is null or if the primitive type cannot be deduced from it
     * @since 7356
     */
    public static OsmPrimitive createPrimitive(String assertion) {
        CheckParameterUtil.ensureParameterNotNull(assertion, "assertion");
        final String[] x = assertion.split("\\s+", 2);
        final OsmPrimitive p = "n".equals(x[0]) || "node".equals(x[0])
                ? new Node(LatLon.ZERO)
                : "w".equals(x[0]) || "way".equals(x[0]) || /*for MapCSS related usage*/ "area".equals(x[0])
                ? new Way()
                : "r".equals(x[0]) || "relation".equals(x[0])
                ? new Relation()
                : null;
        if (p == null) {
            throw new IllegalArgumentException("Expecting n/node/w/way/r/relation/area, but got '" + x[0] + '\'');
        }
        if (x.length > 1) {
            for (final Map.Entry<String, String> i : TextTagParser.readTagsFromText(x[1]).entrySet()) {
                p.put(i.getKey(), i.getValue());
            }
        }
        return p;
    }
}
