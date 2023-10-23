// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.CRC32;

import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxDistance;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.SimplePrimitiveId;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.Match;
import org.openstreetmap.josm.data.osm.search.SearchParseError;
import org.openstreetmap.josm.data.preferences.NamedColorProperty;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.MapPaintStyles;
import org.openstreetmap.josm.gui.mappaint.mapcss.ExpressionFactory.NullableArguments;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.AlphanumComparator;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.RotationAngle;
import org.openstreetmap.josm.tools.RotationAngle.WayDirectionRotationAngle;
import org.openstreetmap.josm.tools.StreamUtils;
import org.openstreetmap.josm.tools.Territories;
import org.openstreetmap.josm.tools.Utils;

/**
 * List of functions that can be used in MapCSS expressions.
 * <p>
 * First parameter can be of type {@link Environment} (if needed). This is
 * automatically filled in by JOSM and the user only sees the remaining arguments.
 * When one of the user supplied arguments cannot be converted the
 * expected type or is null, the function is not called and it returns null
 * immediately. Add the annotation {@link NullableArguments} to allow null arguments.
 * Every method must be static.
 *
 * @since 15245 (extracted from {@link ExpressionFactory})
 */
@SuppressWarnings({"UnusedDeclaration", "squid:S100"})
public final class Functions {

    private Functions() {
        // Hide implicit public constructor for utility classes
    }

    /**
     * Identity function for compatibility with MapCSS specification.
     * @param o any object
     * @return {@code o} unchanged
     */
    public static Object eval(Object o) {
        return o;
    }

    /**
     * Function associated to the numeric "+" operator.
     * @param a the first operand
     * @param b the second operand
     * @return Sum of arguments
     * @see Float#sum
     */
    public static double plus(double a, double b) {
        return a + b;
    }

    /**
     * Function associated to the numeric "-" operator.
     * @param a the first operand
     * @param b the second operand
     * @return Subtraction of arguments
     */
    public static double minus(double a, double b) {
        return a - b;
    }

    /**
     * Function associated to the numeric "*" operator.
     * @param a the first operand
     * @param b the second operand
     * @return Multiplication of arguments
     */
    public static double times(double a, double b) {
        return a * b;
    }

    /**
     * Function associated to the numeric "/" operator.
     * @param a the first operand
     * @param b the second operand
     * @return Division of arguments
     */
    public static double divided_by(double a, double b) {
        return a / b;
    }

    /**
     * Function associated to the math modulo "%" operator.
     * @param a first value
     * @param b second value
     * @return {@code a mod b}, e.g., {@code mod(7, 5) = 2}
     */
    public static float mod(float a, float b) {
        return a % b;
    }

    /**
     * Creates a list of values, e.g., for the {@code dashes} property.
     * @param ignored The environment (ignored)
     * @param args The values to put in a list
     * @return list of values
     * @see Arrays#asList(Object[])
     */
    public static List<Object> list(Environment ignored, Object... args) {
        return Arrays.asList(args);
    }

    /**
     * Returns the number of elements in a list.
     * @param lst the list
     * @return length of the list
     */
    public static Integer count(List<?> lst) {
        return lst.size();
    }

    /**
     * Returns the first non-null object.
     * The name originates from <a href="http://wiki.openstreetmap.org/wiki/MapCSS/0.2/eval">MapCSS standard</a>.
     * @param ignored The environment (ignored)
     * @param args arguments
     * @return the first non-null object
     * @see Utils#firstNonNull(Object[])
     */
    @NullableArguments
    public static Object any(Environment ignored, Object... args) {
        return Utils.firstNonNull(args);
    }

    /**
     * Get the {@code n}th element of the list {@code lst} (counting starts at 0).
     * @param lst list
     * @param n index
     * @return {@code n}th element of the list, or {@code null} if index out of range
     * @since 5699
     */
    public static Object get(List<?> lst, float n) {
        int idx = Math.round(n);
        if (idx >= 0 && idx < lst.size()) {
            return lst.get(idx);
        }
        return null;
    }

    /**
     * Splits string {@code toSplit} at occurrences of the separator string {@code sep} and returns a list of matches.
     * @param sep separator string
     * @param toSplit string to split
     * @return list of matches
     * @see String#split(String)
     * @since 5699
     */
    public static List<String> split(String sep, String toSplit) {
        return Arrays.asList(toSplit.split(Pattern.quote(sep), -1));
    }

    /**
     * Creates a color value with the specified amounts of {@code r}ed, {@code g}reen, {@code b}lue (arguments from 0.0 to 1.0)
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @return color matching the given components
     * @see Color#Color(float, float, float)
     */
    public static Color rgb(float r, float g, float b) {
        try {
            return new Color(r, g, b);
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            return null;
        }
    }

    /**
     * Creates a color value with the specified amounts of {@code r}ed, {@code g}reen, {@code b}lue, {@code alpha}
     * (arguments from 0.0 to 1.0)
     * @param r the red component
     * @param g the green component
     * @param b the blue component
     * @param alpha the alpha component
     * @return color matching the given components
     * @see Color#Color(float, float, float, float)
     */
    public static Color rgba(float r, float g, float b, float alpha) {
        try {
            return new Color(r, g, b, alpha);
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            return null;
        }
    }

    /**
     * Create color from hsb color model. (arguments form 0.0 to 1.0)
     * @param h hue
     * @param s saturation
     * @param b brightness
     * @return the corresponding color
     */
    public static Color hsb_color(float h, float s, float b) {
        try {
            return Color.getHSBColor(h, s, b);
        } catch (IllegalArgumentException e) {
            Logging.trace(e);
            return null;
        }
    }

    /**
     * Creates a color value from an HTML notation, i.e., {@code #rrggbb}.
     * @param html HTML notation
     * @return color matching the given notation
     */
    public static Color html2color(String html) {
        return ColorHelper.html2color(html);
    }

    /**
     * Computes the HTML notation ({@code #rrggbb}) for a color value).
     * @param c color
     * @return HTML notation matching the given color
     */
    public static String color2html(Color c) {
        return ColorHelper.color2html(c);
    }

    /**
     * Get the value of the red color channel in the rgb color model
     * @param c color
     * @return the red color channel in the range [0;1]
     * @see java.awt.Color#getRed()
     */
    public static float red(Color c) {
        return ColorHelper.int2float(c.getRed());
    }

    /**
     * Get the value of the green color channel in the rgb color model
     * @param c color
     * @return the green color channel in the range [0;1]
     * @see java.awt.Color#getGreen()
     */
    public static float green(Color c) {
        return ColorHelper.int2float(c.getGreen());
    }

    /**
     * Get the value of the blue color channel in the rgb color model
     * @param c color
     * @return the blue color channel in the range [0;1]
     * @see java.awt.Color#getBlue()
     */
    public static float blue(Color c) {
        return ColorHelper.int2float(c.getBlue());
    }

    /**
     * Get the value of the alpha channel in the rgba color model
     * @param c color
     * @return the alpha channel in the range [0;1]
     * @see java.awt.Color#getAlpha()
     */
    public static float alpha(Color c) {
        return ColorHelper.int2float(c.getAlpha());
    }

    /**
     * Assembles the strings to one.
     * @param ignored The environment (ignored)
     * @param args arguments
     * @return assembled string
     * @see Collectors#joining
     */
    @NullableArguments
    public static String concat(Environment ignored, Object... args) {
        return Arrays.stream(args)
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .collect(Collectors.joining());
    }

    /**
     * Assembles the strings to one, where the first entry is used as separator.
     * @param ignored The environment (ignored)
     * @param args arguments. First one is used as separator
     * @return assembled string
     * @see String#join(CharSequence, CharSequence...)
     */
    @NullableArguments
    public static String join(Environment ignored, String... args) {
        return String.join(args[0], Arrays.asList(args).subList(1, args.length));
    }

    /**
     * Joins a list of {@code values} into a single string with fields separated by {@code separator}.
     * @param separator the separator
     * @param values collection of objects
     * @return assembled string
     * @see String#join(CharSequence, Iterable)
     */
    public static String join_list(final String separator, final List<String> values) {
        return String.join(separator, values);
    }

    /**
     * Returns the value of the property {@code key}, e.g., {@code prop("width")}.
     * @param env the environment
     * @param key the property key
     * @return the property value
     */
    public static Object prop(final Environment env, String key) {
        return prop(env, key, null);
    }

    /**
     * Returns the value of the property {@code key} from layer {@code layer}.
     * @param env the environment
     * @param key the property key
     * @param layer layer
     * @return the property value
     */
    public static Object prop(final Environment env, String key, String layer) {
        return env.getCascade(layer).get(key);
    }

    /**
     * Determines whether property {@code key} is set.
     * @param env the environment
     * @param key the property key
     * @return {@code true} if the property is set, {@code false} otherwise
     */
    public static Boolean is_prop_set(final Environment env, String key) {
        return is_prop_set(env, key, null);
    }

    /**
     * Determines whether property {@code key} is set on layer {@code layer}.
     * @param env the environment
     * @param key the property key
     * @param layer layer
     * @return {@code true} if the property is set, {@code false} otherwise
     */
    public static Boolean is_prop_set(final Environment env, String key, String layer) {
        return env.getCascade(layer).containsKey(key);
    }

    /**
     * Gets the value of the key {@code key} from the object in question.
     * @param env the environment
     * @param key the OSM key
     * @return the value for given key
     */
    public static String tag(final Environment env, String key) {
        return env.osm == null ? null : env.osm.get(key);
    }

    /**
     * Get keys that follow a regex
     * @param env the environment
     * @param keyRegex the pattern that the key must match
     * @return the values for the keys that match the pattern
     * @see Functions#tag_regex(Environment, String, String)
     * @since 15315
     */
    public static List<String> tag_regex(final Environment env, String keyRegex) {
        return tag_regex(env, keyRegex, "");
    }

    /**
     * Get keys that follow a regex
     * @param env the environment
     * @param keyRegex the pattern that the key must match
     * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
     * @return the values for the keys that match the pattern
     * @see Pattern#CASE_INSENSITIVE
     * @see Pattern#DOTALL
     * @see Pattern#MULTILINE
     * @since 15315
     */
    public static List<String> tag_regex(final Environment env, String keyRegex, String flags) {
        if (env.osm == null) {
            return Collections.emptyList();
        }
        int f = parse_regex_flags(flags);
        Pattern compiled = Pattern.compile(keyRegex, f);
        return env.osm.getKeys().entrySet().stream()
                .filter(object -> compiled.matcher(object.getKey()).find())
                .map(Entry::getValue).collect(Collectors.toList());
    }

    /**
     * Parse flags for regex usage. Shouldn't be used in mapcss
     * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
     * @return An int that can be used by a {@link Pattern} object
     * @see Pattern#CASE_INSENSITIVE
     * @see Pattern#DOTALL
     * @see Pattern#MULTILINE
     */
    private static int parse_regex_flags(String flags) {
        int f = 0;
        if (flags.contains("i")) {
            f |= Pattern.CASE_INSENSITIVE;
        }
        if (flags.contains("s")) {
            f |= Pattern.DOTALL;
        }
        if (flags.contains("m")) {
            f |= Pattern.MULTILINE;
        }
        return f;
    }

    /**
     * Gets the first non-null value of the key {@code key} from the object's parent(s).
     * @param env the environment
     * @param key the OSM key
     * @return first non-null value of the key {@code key} from the object's parent(s)
     */
    public static String parent_tag(final Environment env, String key) {
        if (env.parent == null) {
            if (env.osm != null) {
                // we don't have a matched parent, so just search all referrers
                return env.osm.getReferrers().stream()
                        .map(parent -> parent.get(key))
                        .filter(Objects::nonNull)
                        .findFirst().orElse(null);
            }
            return null;
        }
        return env.parent.get(key);
    }

    /**
     * Gets a list of all non-null values of the key {@code key} from the object's parent(s).
     * <p>
     * The values are sorted according to {@link AlphanumComparator}.
     * @param env the environment
     * @param key the OSM key
     * @return a list of non-null values of the key {@code key} from the object's parent(s)
     */
    public static List<String> parent_tags(final Environment env, String key) {
        if (env.parent == null) {
            if (env.osm != null) {
                // we don't have a matched parent, so just search all referrers
                return env.osm.getReferrers().stream().map(parent -> parent.get(key))
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted(AlphanumComparator.getInstance())
                        .collect(StreamUtils.toUnmodifiableList());
            }
            return Collections.emptyList();
        }
        return Collections.singletonList(env.parent.get(key));
    }

    /**
     * Get the rotation angle of the preceding parent way segment at the node location.
     * If there is no preceding parent way segment, the following way segment is used instead.
     * Requires a parent way object matched via
     * <a href="https://josm.openstreetmap.de/wiki/Help/Styles/MapCSSImplementation#LinkSelector">child selector</a>.
     * 
     * @param env the environment
     * @return the rotation angle of the parent way segment at the node in radians,
     * otherwise null if there is no matching parent way or the object is not a node
     * @since 18664
     */
    public static Double parent_way_angle(final Environment env) {
        if (env.osm instanceof Node && env.parent instanceof Way) {
            return WayDirectionRotationAngle.getRotationAngleForNodeOnWay((Node) env.osm, (Way) env.parent);
        }
        return null;
    }

    /**
     * Gets the value of the key {@code key} from the object's child.
     * @param env the environment
     * @param key the OSM key
     * @return the value of the key {@code key} from the object's child, or {@code null} if there is no child
     */
    public static String child_tag(final Environment env, String key) {
        return env.child == null ? null : env.child.get(key);
    }

    /**
     * Returns the OSM id of the object's parent.
     * <p>
     * Parent must be matched by child selector.
     * @param env the environment
     * @return the OSM id of the object's parent, if available, or {@code null}
     * @see IPrimitive#getUniqueId()
     */
    public static Long parent_osm_id(final Environment env) {
        return env.parent == null ? null : env.parent.getUniqueId();
    }

    /**
     * Gets a list of all OSM id's of the object's parent(s) with a specified key.
     *
     * @param env      the environment
     * @param key      the OSM key
     * @param keyValue the regex value of the OSM key
     * @return a list of non-null values of the OSM id's from the object's parent(s)
     * @since 18829
     */
    @NullableArguments
    public static List<IPrimitive> parent_osm_primitives(final Environment env, String key, String keyValue) {
         if (env.parent == null) {
             if (env.osm != null) {
                final ArrayList<IPrimitive> parents = new ArrayList<>();
                for (IPrimitive parent : env.osm.getReferrers()) {
                    if ((key == null || parent.get(key) != null)
                            && (keyValue == null || regexp_test(keyValue, parent.get(key)))) {
                        parents.add(parent);
                    }
                }
                return Collections.unmodifiableList(parents);
            }
            return Collections.emptyList();
         }
         return Collections.singletonList(env.parent);
     }

     /**
      * Gets a list of all OSM id's of the object's parent(s) with a specified key.
      *
      * @param env the environment
      * @param key the OSM key
      * @return a list of non-null values of the OSM id's from the object's parent(s)
      * @since 18829
      */
     @NullableArguments
     public static List<IPrimitive> parent_osm_primitives(final Environment env, String key) {
         return parent_osm_primitives(env, key, null);
     }

    /**
     * Gets a list of all OSM id's of the object's parent(s).
     *
     * @param env the environment
     * @return a list of non-null values of the OSM id's from the object's parent(s)
     * @since 18829
     */
    public static List<IPrimitive> parent_osm_primitives(final Environment env) {
        return parent_osm_primitives(env, null, null);
    }

    /**
     * Convert Primitives to a string
     *
     * @param primitives The primitives to convert
     * @return A list of strings in the format type + id (in the list order)
     * @see SimplePrimitiveId#toSimpleId
     * @since 18829
     */
    public static List<String> convert_primitives_to_string(Iterable<PrimitiveId> primitives) {
        final List<String> primitiveStrings = new ArrayList<>(primitives instanceof Collection ?
                ((Collection<?>) primitives).size() : 0);
        for (PrimitiveId primitive : primitives) {
            primitiveStrings.add(convert_primitive_to_string(primitive));
        }
        return primitiveStrings;
    }

    /**
     * Convert a primitive to a string
     *
     * @param primitive The primitive to convert
     * @return A string in the format type + id
     * @see SimplePrimitiveId#toSimpleId
     * @since 18829
     */
    public static String convert_primitive_to_string(PrimitiveId primitive) {
        return SimplePrimitiveId.toSimpleId(primitive);
    }

    /**
     * Returns the lowest distance between the OSM object and a GPX point
     * <p>
     * @param env the environment
     * @return the distance between the object and the closest gpx point or {@code Double.MAX_VALUE}
     * @since 14802
     */
    public static double gpx_distance(final Environment env) {
        if (env.osm instanceof OsmPrimitive) {
            return MainApplication.getLayerManager().getAllGpxData().stream()
                    .mapToDouble(gpx -> GpxDistance.getLowestDistance((OsmPrimitive) env.osm, gpx))
                    .min().orElse(Double.MAX_VALUE);
        }
        return Double.MAX_VALUE;
    }

    /**
     * Determines whether the object has a tag with the given key.
     * @param env the environment
     * @param key the OSM key
     * @return {@code true} if the object has a tag with the given key, {@code false} otherwise
     */
    public static boolean has_tag_key(final Environment env, String key) {
        return env.osm != null ? env.osm.hasKey(key) : false;
    }

    /**
     * Returns the index of node in parent way or member in parent relation.
     * @param env the environment
     * @return the index as float. Starts at 1
     */
    public static Float index(final Environment env) {
        if (env.index == null) {
            return null;
        }
        return env.index + 1f;
    }

    /**
     * Sort an array of strings
     * @param ignored The environment (ignored)
     * @param sortables The array to sort
     * @return The sorted list
     * @since 15279
     */
    public static List<String> sort(Environment ignored, String... sortables) {
        Arrays.parallelSort(sortables);
        return Arrays.asList(sortables);
    }

    /**
     * Sort a list of strings
     * @param sortables The list to sort
     * @return The sorted list
     * @since 15279
     */
    public static List<String> sort_list(List<String> sortables) {
        Collections.sort(sortables);
        return sortables;
    }

    /**
     * Get unique values
     * @param ignored The environment (ignored)
     * @param values A list of values that may have duplicates
     * @return A list with no duplicates
     * @since 15323
     */
    public static List<String> uniq(Environment ignored, String... values) {
        return uniq_list(Arrays.asList(values));
    }

    /**
     * Get unique values
     * @param values A list of values that may have duplicates
     * @return A list with no duplicates
     * @since 15323
     */
    public static List<String> uniq_list(List<String> values) {
        return values.stream().distinct().collect(Collectors.toList());
    }

    /**
     * Returns the role of current object in parent relation, or role of child if current object is a relation.
     * @param env the environment
     * @return role of current object in parent relation, or role of child if current object is a relation
     * @see Environment#getRole()
     */
    public static String role(final Environment env) {
        return env.getRole();
    }

    /**
     * Returns the number of primitives in a relation with the specified roles.
     * @param env the environment
     * @param roles The roles to count in the relation
     * @return The number of relation members with the specified role
     * @since 15196
     */
    public static int count_roles(final Environment env, String... roles) {
        int rValue = 0;
        if (env.osm instanceof Relation) {
            List<String> roleList = Arrays.asList(roles);
            Relation rel = (Relation) env.osm;
            rValue = (int) rel.getMembers().stream()
                    .filter(member -> roleList.contains(member.getRole()))
                    .count();
        }
        return rValue;
    }

    /**
     * Returns the area of a closed way or multipolygon in square meters or {@code null}.
     * @param env the environment
     * @return the area of a closed way or multipolygon in square meters or {@code null}
     * @see Geometry#computeArea(IPrimitive)
     */
    public static Float areasize(final Environment env) {
        final Double area = Geometry.computeArea(env.osm);
        return area == null ? null : area.floatValue();
    }

    /**
     * Returns the length of the way in metres or {@code null}.
     * @param env the environment
     * @return the length of the way in metres or {@code null}.
     * @see Way#getLength()
     */
    public static Float waylength(final Environment env) {
        if (env.osm instanceof Way) {
            return (float) ((Way) env.osm).getLength();
        } else {
            return null;
        }
    }

    /**
     * Function associated to the logical "!" operator.
     * @param b boolean value
     * @return {@code true} if {@code !b}
     */
    public static boolean not(boolean b) {
        return !b;
    }

    /**
     * Function associated to the logical "&gt;=" operator.
     * @param a first value
     * @param b second value
     * @return {@code true} if {@code a >= b}
     */
    public static boolean greater_equal(float a, float b) {
        return a >= b;
    }

    /**
     * Function associated to the logical "&lt;=" operator.
     * @param a first value
     * @param b second value
     * @return {@code true} if {@code a <= b}
     */
    public static boolean less_equal(float a, float b) {
        return a <= b;
    }

    /**
     * Function associated to the logical "&gt;" operator.
     * @param a first value
     * @param b second value
     * @return {@code true} if {@code a > b}
     */
    public static boolean greater(float a, float b) {
        return a > b;
    }

    /**
     * Function associated to the logical "&lt;" operator.
     * @param a first value
     * @param b second value
     * @return {@code true} if {@code a < b}
     */
    public static boolean less(float a, float b) {
        return a < b;
    }

    /**
     * Converts an angle in degrees to radians.
     * @param degree the angle in degrees
     * @return the angle in radians
     * @see Math#toRadians(double)
     */
    public static double degree_to_radians(double degree) {
        return Utils.toRadians(degree);
    }

    /**
     * Converts an angle diven in cardinal directions to radians.
     * The following values are supported: {@code n}, {@code north}, {@code ne}, {@code northeast},
     * {@code e}, {@code east}, {@code se}, {@code southeast}, {@code s}, {@code south},
     * {@code sw}, {@code southwest}, {@code w}, {@code west}, {@code nw}, {@code northwest}.
     * @param cardinal the angle in cardinal directions.
     * @return the angle in radians
     * @see RotationAngle#parseCardinalRotation(String)
     */
    public static Double cardinal_to_radians(String cardinal) {
        try {
            return RotationAngle.parseCardinalRotation(cardinal);
        } catch (IllegalArgumentException illegalArgumentException) {
            Logging.trace(illegalArgumentException);
            return null;
        }
    }

    /**
     * Determines if the objects {@code a} and {@code b} are equal.
     * @param a First object
     * @param b Second object
     * @return {@code true} if objects are equal, {@code false} otherwise
     * @see Object#equals(Object)
     */
    @SuppressWarnings("squid:S1221")
    public static boolean equal(Object a, Object b) {
        if (a.getClass() == b.getClass()) return a.equals(b);
        if (a.equals(Cascade.convertTo(b, a.getClass()))) return true;
        return b.equals(Cascade.convertTo(a, b.getClass()));
    }

    /**
     * Determines if the objects {@code a} and {@code b} are not equal.
     * @param a First object
     * @param b Second object
     * @return {@code false} if objects are equal, {@code true} otherwise
     * @see Object#equals(Object)
     */
    public static boolean not_equal(Object a, Object b) {
        return !equal(a, b);
    }

    /**
     * Determines whether the JOSM search with {@code searchStr} applies to the object.
     * @param env the environment
     * @param searchStr the search string
     * @return {@code true} if the JOSM search with {@code searchStr} applies to the object
     * @see SearchCompiler
     */
    public static Boolean JOSM_search(final Environment env, String searchStr) {
        Match m;
        try {
            m = SearchCompiler.compile(searchStr);
        } catch (SearchParseError ex) {
            Logging.trace(ex);
            return null;
        }
        return m.match(env.osm);
    }

    /**
     * Obtains the JOSM key {@link org.openstreetmap.josm.data.Preferences} string for key {@code key},
     * and defaults to {@code def} if that is null.
     * <p>
     * If the default value can be {@linkplain Cascade#convertTo converted} to a {@link Color},
     * the {@link NamedColorProperty} is retrieved as string.
     *
     * @param env the environment
     * @param key Key in JOSM preference
     * @param def Default value
     * @return value for key, or default value if not found
     */
    public static String JOSM_pref(Environment env, String key, String def) {
        return MapPaintStyles.getStyles().getPreferenceCached(env != null ? env.source : null, key, def);
    }

    /**
     * Tests if string {@code target} matches pattern {@code pattern}
     * @param pattern The regex expression
     * @param target The character sequence to be matched
     * @return {@code true} if, and only if, the entire region sequence matches the pattern
     * @see Pattern#matches(String, CharSequence)
     * @since 5699
     */
    public static boolean regexp_test(String pattern, String target) {
        return Pattern.matches(pattern, target);
    }

    /**
     * Tests if string {@code target} matches pattern {@code pattern}
     * @param pattern The regex expression
     * @param target The character sequence to be matched
     * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
     * @return {@code true} if, and only if, the entire region sequence matches the pattern
     * @see Pattern#CASE_INSENSITIVE
     * @see Pattern#DOTALL
     * @see Pattern#MULTILINE
     * @since 5699
     */
    public static boolean regexp_test(String pattern, String target, String flags) {
        int f = parse_regex_flags(flags);
        return Pattern.compile(pattern, f).matcher(target).matches();
    }

    /**
     * Tries to match string against pattern regexp and returns a list of capture groups in case of success.
     * The first element (index 0) is the complete match (i.e. string).
     * Further elements correspond to the bracketed parts of the regular expression.
     * @param pattern The regex expression
     * @param target The character sequence to be matched
     * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
     * @return a list of capture groups if {@link Matcher#matches()}, or {@code null}.
     * @see Pattern#CASE_INSENSITIVE
     * @see Pattern#DOTALL
     * @see Pattern#MULTILINE
     * @since 5701
     */
    public static List<String> regexp_match(String pattern, String target, String flags) {
        int f = parse_regex_flags(flags);
        return Utils.getMatches(Pattern.compile(pattern, f).matcher(target));
    }

    /**
     * Tries to match string against pattern regexp and returns a list of capture groups in case of success.
     * The first element (index 0) is the complete match (i.e. string).
     * Further elements correspond to the bracketed parts of the regular expression.
     * @param pattern The regex expression
     * @param target The character sequence to be matched
     * @return a list of capture groups if {@link Matcher#matches()}, or {@code null}.
     * @since 5701
     */
    public static List<String> regexp_match(String pattern, String target) {
        return Utils.getMatches(Pattern.compile(pattern).matcher(target));
    }

    /**
     * Returns the OSM id of the current object.
     * @param env the environment
     * @return the OSM id of the current object
     * @see IPrimitive#getUniqueId()
     */
    public static long osm_id(final Environment env) {
        return env.osm != null ? env.osm.getUniqueId() : 0;
    }

    /**
     * Returns the OSM user name who last touched the current object.
     * @param env the environment
     * @return the OSM user name who last touched the current object
     * @see IPrimitive#getUser
     * @since 15246
     */
    public static String osm_user_name(final Environment env) {
        return env.osm != null ? env.osm.getUser().getName() : null;
    }

    /**
     * Returns the OSM user id who last touched the current object.
     * @param env the environment
     * @return the OSM user id who last touched the current object
     * @see IPrimitive#getUser
     * @since 15246
     */
    public static long osm_user_id(final Environment env) {
        return env.osm != null ? env.osm.getUser().getId() : 0;
    }

    /**
     * Returns the version number of the current object.
     * @param env the environment
     * @return the version number of the current object
     * @see IPrimitive#getVersion
     * @since 15246
     */
    public static int osm_version(final Environment env) {
        return env.osm != null ? env.osm.getVersion() : 0;
    }

    /**
     * Returns the id of the changeset the current object was last uploaded to.
     * @param env the environment
     * @return the id of the changeset the current object was last uploaded to
     * @see IPrimitive#getChangesetId
     * @since 15246
     */
    public static int osm_changeset_id(final Environment env) {
        return env.osm != null ? env.osm.getChangesetId() : 0;
    }

    /**
     * Returns the time of last modification to the current object, as timestamp.
     * @param env the environment
     * @return the time of last modification to the current object, as timestamp
     * @see IPrimitive#getRawTimestamp
     * @since 15246
     */
    public static int osm_timestamp(final Environment env) {
        return env.osm != null ? env.osm.getRawTimestamp() : 0;
    }

    /**
     * Translates some text for the current locale. The first argument is the text to translate,
     * and the subsequent arguments are parameters for the string indicated by <code>{0}</code>, <code>{1}</code>, …
     * @param ignored The environment (ignored)
     * @param args arguments
     * @return the translated string
     */
    @NullableArguments
    public static String tr(Environment ignored, String... args) {
        final String text = args[0];
        System.arraycopy(args, 1, args, 0, args.length - 1);
        return org.openstreetmap.josm.tools.I18n.tr(text, (Object[]) args);
    }

    /**
     * Returns the substring of {@code s} starting at index {@code begin} (inclusive, 0-indexed).
     * @param s The base string
     * @param begin The start index
     * @return the substring
     * @see String#substring(int)
     */
    public static String substring(String s, /* due to missing Cascade.convertTo for int*/ float begin) {
        return s == null ? null : s.substring((int) begin);
    }

    /**
     * Returns the substring of {@code s} starting at index {@code begin} (inclusive)
     * and ending at index {@code end}, (exclusive, 0-indexed).
     * @param s The base string
     * @param begin The start index
     * @param end The end index
     * @return the substring
     * @see String#substring(int, int)
     */
    public static String substring(String s, float begin, float end) {
        return s == null ? null : s.substring((int) begin, (int) end);
    }

    /**
     * Replaces in {@code s} every {@code} target} substring by {@code replacement}.
     * @param s The source string
     * @param target The sequence of char values to be replaced
     * @param replacement The replacement sequence of char values
     * @return The resulting string
     * @see String#replace(CharSequence, CharSequence)
     */
    public static String replace(String s, String target, String replacement) {
        return s == null ? null : s.replace(target, replacement);
    }

    /**
     * Converts string {@code s} to uppercase.
     * @param s The source string
     * @return The resulting string
     * @see String#toUpperCase(Locale)
     * @since 11756
     */
    public static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ENGLISH);
    }

    /**
     * Converts string {@code s} to lowercase.
     * @param s The source string
     * @return The resulting string
     * @see String#toLowerCase(Locale)
     * @since 11756
     */
    public static String lower(String s) {
        return s == null ? null : s.toLowerCase(Locale.ENGLISH);
    }

    /**
     * Returns a title-cased version of the string where words start with an uppercase character and the remaining characters are lowercase
     * <p>
     * Also known as "capitalize".
     * @param str The source string
     * @return The resulting string
     * @see Character#toTitleCase(char)
     * @since 17613
     */
    public static String title(String str) {
        // adapted from org.apache.commons.lang3.text.WordUtils.capitalize
        if (str == null) {
            return null;
        }
        final char[] buffer = str.toCharArray();
        boolean capitalizeNext = true;
        for (int i = 0; i < buffer.length; i++) {
            final char ch = buffer[i];
            if (Character.isWhitespace(ch)) {
                capitalizeNext = true;
            } else if (capitalizeNext) {
                buffer[i] = Character.toTitleCase(ch);
                capitalizeNext = false;
            } else {
                buffer[i] = Character.toLowerCase(ch);
            }
        }
        return new String(buffer);
    }

    /**
     * Trim whitespaces from the string {@code s}.
     * @param s The source string
     * @return The resulting string
     * @see Utils#strip
     * @since 11756
     */
    public static String trim(String s) {
        return Utils.strip(s);
    }

    /**
     * Trim whitespaces from the strings {@code strings}.
     *
     * @param strings The list of strings to strip
     * @return The resulting string
     * @see Utils#strip
     * @since 15591
     */
    public static List<String> trim_list(List<String> strings) {
        return strings.stream().map(Utils::strip).filter(str -> !str.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Check if two strings are similar, but not identical, i.e., have a Levenshtein distance of 1 or 2.
     * @param string1 first string to compare
     * @param string2 second string to compare
     * @return true if the normalized strings are different but only a "little bit"
     * @see Utils#isSimilar
     * @since 14371
     */
    public static boolean is_similar(String string1, String string2) {
        return Utils.isSimilar(string1, string2);
    }

    /**
     * Percent-decode a string. (See
     * <a href="https://en.wikipedia.org/wiki/Percent-encoding">https://en.wikipedia.org/wiki/Percent-encoding</a>)
     * <p>
     * This is especially useful for wikipedia titles
     * @param s url-encoded string
     * @return the decoded string, or original in case of an error
     * @since 11756
     */
    public static String URL_decode(String s) {
        if (s == null) return null;
        try {
            return Utils.decodeUrl(s);
        } catch (IllegalStateException e) {
            Logging.debug(e);
            return s;
        }
    }

    /**
     * Percent-encode a string.
     * (See <a href="https://en.wikipedia.org/wiki/Percent-encoding">https://en.wikipedia.org/wiki/Percent-encoding</a>)
     * <p>
     * This is especially useful for data urls, e.g.
     * <code>concat("data:image/svg+xml,", URL_encode("&lt;svg&gt;...&lt;/svg&gt;"));</code>
     * @param s arbitrary string
     * @return the encoded string
     */
    public static String URL_encode(String s) {
        return s == null ? null : Utils.encodeUrl(s);
    }

    /**
     * XML-encode a string.
     * <p>
     * Escapes special characters in xml. Alternative to using &lt;![CDATA[ ... ]]&gt; blocks.
     * @param s arbitrary string
     * @return the encoded string
     */
    public static String XML_encode(String s) {
        return s == null ? null : XmlWriter.encode(s);
    }

    /**
     * Calculates the CRC32 checksum from a string (based on RFC 1952).
     * @param s the string
     * @return long value from 0 to 2^32-1
     */
    public static long CRC32_checksum(String s) {
        CRC32 cs = new CRC32();
        cs.update(s.getBytes(StandardCharsets.UTF_8));
        return cs.getValue();
    }

    /**
     * check if there is right-hand traffic at the current location
     * @param env the environment
     * @return true if there is right-hand traffic
     * @since 7193
     */
    public static boolean is_right_hand_traffic(Environment env) {
        final LatLon center = center(env);
        if (center != null) {
            return RightAndLefthandTraffic.isRightHandTraffic(center);
        }
        return false;
    }

    /**
     * Determines whether the way is {@link Geometry#isClockwise closed and oriented clockwise},
     * or non-closed and the {@link Geometry#angleIsClockwise 1st, 2nd and last node are in clockwise order}.
     *
     * @param env the environment
     * @return true if the way is closed and oriented clockwise
     */
    public static boolean is_clockwise(Environment env) {
        if (!(env.osm instanceof Way)) {
            return false;
        }
        final Way way = (Way) env.osm;
        return (way.isClosed() && Geometry.isClockwise(way))
            || (!way.isClosed() && way.getNodesCount() > 2 && Geometry.angleIsClockwise(way.getNode(0), way.getNode(1), way.lastNode()));
    }

    /**
     * Determines whether the way is {@link Geometry#isClockwise closed and oriented anticlockwise},
     * or non-closed and the {@link Geometry#angleIsClockwise 1st, 2nd and last node are in anticlockwise order}.
     *
     * @param env the environment
     * @return true if the way is closed and oriented clockwise
     */
    public static boolean is_anticlockwise(Environment env) {
        if (!(env.osm instanceof Way)) {
            return false;
        }
        final Way way = (Way) env.osm;
        return (way.isClosed() && !Geometry.isClockwise(way))
            || (!way.isClosed() && way.getNodesCount() > 2 && !Geometry.angleIsClockwise(way.getNode(0), way.getNode(1), way.lastNode()));
    }

    /**
     * Prints the object to the command line (for debugging purpose).
     * @param o the object
     * @return the same object, unchanged
     */
    @NullableArguments
    public static Object print(Object o) {
        System.out.print(o == null ? "none" : o.toString());
        return o;
    }

    /**
     * Prints the object to the command line, with new line at the end
     * (for debugging purpose).
     * @param o the object
     * @return the same object, unchanged
     */
    @NullableArguments
    public static Object println(Object o) {
        System.out.println(o == null ? "none" : o.toString());
        return o;
    }

    /**
     * Get the number of tags for the current primitive.
     * @param env the environment
     * @return number of tags
     */
    public static int number_of_tags(Environment env) {
        return env.osm != null ? env.osm.getNumKeys() : 0;
    }

    /**
     * Get value of a setting.
     * @param env the environment
     * @param key setting key (given as layer identifier, e.g. setting::mykey {...})
     * @return the value of the setting (calculated when the style is loaded)
     */
    public static Object setting(Environment env, String key) {
        return env.source != null ? env.source.settingValues.get(key) : null;
    }

    /**
     * Returns the center of the environment OSM primitive.
     * @param env the environment
     * @return the center of the environment OSM primitive
     * @since 11247
     */
    public static LatLon center(Environment env) {
        if (env.osm instanceof ILatLon) {
            return new LatLon(((ILatLon) env.osm).lat(), ((ILatLon) env.osm).lon());
        } else if (env.osm != null) {
            return env.osm.getBBox().getCenter();
        }
        return null;
    }

    /**
     * Determines if the object is inside territories matching given ISO3166 codes.
     * @param env the environment
     * @param codes comma-separated list of ISO3166-1-alpha2 or ISO3166-2 country/subdivision codes
     * @return {@code true} if the object is inside territory matching given ISO3166 codes
     * @since 11247
     */
    public static boolean inside(Environment env, String codes) {
        return Arrays.stream(codes.toUpperCase(Locale.ENGLISH).split(",", -1))
                .anyMatch(code -> Territories.isIso3166Code(code.trim(), center(env)));
    }

    /**
     * Determines if the object is outside territories matching given ISO3166 codes.
     * @param env the environment
     * @param codes comma-separated list of ISO3166-1-alpha2 or ISO3166-2 country/subdivision codes
     * @return {@code true} if the object is outside territory matching given ISO3166 codes
     * @since 11247
     */
    public static boolean outside(Environment env, String codes) {
        return !inside(env, codes);
    }

    /**
     * Determines if the object centroid lies at given lat/lon coordinates.
     * @param env the environment
     * @param lat latitude, i.e., the north-south position in degrees
     * @param lon longitude, i.e., the east-west position in degrees
     * @return {@code true} if the object centroid lies at given lat/lon coordinates
     * @since 12514
     */
    public static boolean at(Environment env, double lat, double lon) {
        final ILatLon center = center(env);
        if (center != null) {
            return new LatLon(lat, lon).equalsEpsilon(center, ILatLon.MAX_SERVER_PRECISION);
        }
        return false;
    }

    /**
     * Parses the string argument as a boolean.
     * @param value the {@code String} containing the boolean representation to be parsed
     * @return the boolean represented by the string argument
     * @see Boolean#parseBoolean
     * @since 16110
     */
    public static boolean to_boolean(String value) {
        return Boolean.parseBoolean(value);
    }

    /**
     * Parses the string argument as a byte.
     * @param value the {@code String} containing the byte representation to be parsed
     * @return the byte represented by the string argument
     * @see Byte#parseByte
     * @since 16110
     */
    public static byte to_byte(String value) {
        return Byte.parseByte(value);
    }

    /**
     * Parses the string argument as a short.
     * @param value the {@code String} containing the short representation to be parsed
     * @return the short represented by the string argument
     * @see Short#parseShort
     * @since 16110
     */
    public static short to_short(String value) {
        return Short.parseShort(value);
    }

    /**
     * Parses the string argument as an int.
     * @param value the {@code String} containing the int representation to be parsed
     * @return the int represented by the string argument
     * @see Integer#parseInt
     * @since 16110
     */
    public static int to_int(String value) {
        return Integer.parseInt(value);
    }

    /**
     * Parses the string argument as a long.
     * @param value the {@code String} containing the long representation to be parsed
     * @return the long represented by the string argument
     * @see Long#parseLong
     * @since 16110
     */
    public static long to_long(String value) {
        return Long.parseLong(value);
    }

    /**
     * Parses the string argument as a float.
     * @param value the {@code String} containing the float representation to be parsed
     * @return the float represented by the string argument
     * @see Float#parseFloat
     * @since 16110
     */
    public static float to_float(String value) {
        return Float.parseFloat(value);
    }

    /**
     * Parses the string argument as a double.
     * @param value the {@code String} containing the double representation to be parsed
     * @return the double represented by the string argument
     * @see Double#parseDouble
     * @since 16110
     */
    public static double to_double(String value) {
        return Double.parseDouble(value);
    }
}
