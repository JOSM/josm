// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.TextTagParser;

/**
 * Utility methods/constants that are useful for generic OSM tag handling.
 */
public final class OsmUtils {

    private static final Set<String> TRUE_VALUES = new HashSet<>(Arrays
            .asList("true", "yes", "1", "on"));
    private static final Set<String> FALSE_VALUES = new HashSet<>(Arrays
            .asList("false", "no", "0", "off"));
    private static final Set<String> REVERSE_VALUES = new HashSet<>(Arrays
            .asList("reverse", "-1"));

    /**
     * A value that should be used to indicate true
     * @since 12186
     */
    public static final String TRUE_VALUE = "yes";
    /**
     * A value that should be used to indicate false
     * @since 12186
     */
    public static final String FALSE_VALUE = "no";
    /**
     * A value that should be used to indicate that a property applies reversed on the way
     * @since 12186
     */
    public static final String REVERSE_VALUE = "-1";

    /**
     * Discouraged synonym for {@link #TRUE_VALUE}
     */
    public static final String trueval = TRUE_VALUE;
    /**
     * Discouraged synonym for {@link #FALSE_VALUE}
     */
    public static final String falseval = FALSE_VALUE;
    /**
     * Discouraged synonym for {@link #REVERSE_VALUE}
     */
    public static final String reverseval = REVERSE_VALUE;

    private OsmUtils() {
        // Hide default constructor for utils classes
    }

    /**
     * Converts a string to a boolean value
     * @param value The string to convert
     * @return {@link Boolean#TRUE} if that string represents a true value,
     *         {@link Boolean#FALSE} if it represents a false value,
     *         <code>null</code> otherwise.
     */
    public static Boolean getOsmBoolean(String value) {
        if (value == null) return null;
        String lowerValue = value.toLowerCase(Locale.ENGLISH);
        if (TRUE_VALUES.contains(lowerValue)) return Boolean.TRUE;
        if (FALSE_VALUES.contains(lowerValue)) return Boolean.FALSE;
        return null;
    }

    /**
     * Normalizes the OSM boolean value
     * @param value The tag value
     * @return The best true/false value or the old value if the input cannot be converted.
     * @see #TRUE_VALUE
     * @see #FALSE_VALUE
     */
    public static String getNamedOsmBoolean(String value) {
        Boolean res = getOsmBoolean(value);
        return res == null ? value : (res ? trueval : falseval);
    }

    /**
     * Check if the value is a value indicating that a property applies reversed.
     * @param value The value to check
     * @return true if it is reversed.
     */
    public static boolean isReversed(String value) {
        return REVERSE_VALUES.contains(value);
    }

    /**
     * Check if a tag value represents a boolean true value
     * @param value The value to check
     * @return true if it is a true value.
     */
    public static boolean isTrue(String value) {
        return TRUE_VALUES.contains(value);
    }

    /**
     * Check if a tag value represents a boolean false value
     * @param value The value to check
     * @return true if it is a true value.
     */
    public static boolean isFalse(String value) {
        return FALSE_VALUES.contains(value);
    }

    /**
     * Creates a new OSM primitive around (0,0) according to the given assertion. Originally written for unit tests,
     * this can also be used in another places like validation of local MapCSS validator rules.
     * Ways and relations created using this method are empty.
     * @param assertion The assertion describing OSM primitive (ex: "way name=Foo railway=rail")
     * @return a new OSM primitive according to the given assertion
     * @throws IllegalArgumentException if assertion is null or if the primitive type cannot be deduced from it
     * @since 7356
     */
    public static OsmPrimitive createPrimitive(String assertion) {
        return createPrimitive(assertion, LatLon.ZERO, false);
    }

    /**
     * Creates a new OSM primitive according to the given assertion. Originally written for unit tests,
     * this can also be used in another places like validation of local MapCSS validator rules.
     * @param assertion The assertion describing OSM primitive (ex: "way name=Foo railway=rail")
     * @param around the coordinate at which the primitive will be located
     * @param enforceLocation if {@code true}, ways and relations will not be empty to force a physical location
     * @return a new OSM primitive according to the given assertion
     * @throws IllegalArgumentException if assertion is null or if the primitive type cannot be deduced from it
     * @since 14486
     */
    public static OsmPrimitive createPrimitive(String assertion, LatLon around, boolean enforceLocation) {
        CheckParameterUtil.ensureParameterNotNull(assertion, "assertion");
        final String[] x = assertion.split("\\s+", 2);
        final OsmPrimitive p = "n".equals(x[0]) || "node".equals(x[0])
                ? newNode(around)
                : "w".equals(x[0]) || "way".equals(x[0]) || /*for MapCSS related usage*/ "area".equals(x[0])
                ? newWay(around, enforceLocation)
                : "r".equals(x[0]) || "relation".equals(x[0])
                ? newRelation(around, enforceLocation)
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

    private static Node newNode(LatLon around) {
        return new Node(around);
    }

    private static Way newWay(LatLon around, boolean enforceLocation) {
        Way w = new Way();
        if (enforceLocation) {
            w.addNode(newNode(new LatLon(around.lat()+0.1, around.lon())));
            w.addNode(newNode(new LatLon(around.lat()-0.1, around.lon())));
        }
        return w;
    }

    private static Relation newRelation(LatLon around, boolean enforceLocation) {
        Relation r = new Relation();
        if (enforceLocation) {
            r.addMember(new RelationMember(null, newNode(around)));
        }
        return r;
    }

    /**
     * Returns the layer value of primitive (null for layer 0).
     * @param w OSM primitive
     * @return the value of "layer" key, or null if absent or set to 0 (default value)
     * @since 12986
     * @since 13637 (signature)
     */
    public static String getLayer(IPrimitive w) {
        String layer1 = w.get("layer");
        if ("0".equals(layer1)) {
            layer1 = null; // 0 is default value for layer.
        }
        return layer1;
    }

    /**
     * Determines if the given collection contains primitives, and that none of them belong to a locked layer.
     * @param collection collection of OSM primitives
     * @return {@code true} if the given collection is not empty and does not contain any primitive in a locked layer.
     * @since 13611
     * @since 13957 (signature)
     */
    public static boolean isOsmCollectionEditable(Collection<? extends IPrimitive> collection) {
        if (collection == null || collection.isEmpty()) {
            return false;
        }
        // see #16510: optimization: only consider the first primitive, as collection always refer to the same dataset
        OsmData<?, ?, ?, ?> ds = collection.iterator().next().getDataSet();
        return ds == null || !ds.isLocked();
    }

    /**
     * Splits a tag value by <a href="https://wiki.openstreetmap.org/wiki/Semi-colon_value_separator">semi-colon value separator</a>.
     * Spaces around the ; are ignored.
     *
     * @param value the value to separate
     * @return the separated values as Stream
     * @since 15671
     */
    public static Stream<String> splitMultipleValues(String value) {
        return Pattern.compile("\\s*;\\s*").splitAsStream(value);
    }
}
