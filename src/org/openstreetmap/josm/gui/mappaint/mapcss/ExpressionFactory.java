// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.awt.Color;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.util.RotationAngle;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.RightAndLefthandTraffic;
import org.openstreetmap.josm.tools.Utils;

/**
 * Factory to generate Expressions.
 *
 * See {@link #createFunctionExpression}.
 */
public final class ExpressionFactory {

    /**
     * Marks functions which should be executed also when one or more arguments are null.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    static @interface NullableArguments {}

    private static final List<Method> arrayFunctions = new ArrayList<>();
    private static final List<Method> parameterFunctions = new ArrayList<>();
    private static final List<Method> parameterFunctionsEnv = new ArrayList<>();

    static {
        for (Method m : Functions.class.getDeclaredMethods()) {
            Class<?>[] paramTypes = m.getParameterTypes();
            if (paramTypes.length == 1 && paramTypes[0].isArray()) {
                arrayFunctions.add(m);
            } else if (paramTypes.length >= 1 && paramTypes[0].equals(Environment.class)) {
                parameterFunctionsEnv.add(m);
            } else {
                parameterFunctions.add(m);
            }
        }
        try {
            parameterFunctions.add(Math.class.getMethod("abs", float.class));
            parameterFunctions.add(Math.class.getMethod("acos", double.class));
            parameterFunctions.add(Math.class.getMethod("asin", double.class));
            parameterFunctions.add(Math.class.getMethod("atan", double.class));
            parameterFunctions.add(Math.class.getMethod("atan2", double.class, double.class));
            parameterFunctions.add(Math.class.getMethod("ceil", double.class));
            parameterFunctions.add(Math.class.getMethod("cos", double.class));
            parameterFunctions.add(Math.class.getMethod("cosh", double.class));
            parameterFunctions.add(Math.class.getMethod("exp", double.class));
            parameterFunctions.add(Math.class.getMethod("floor", double.class));
            parameterFunctions.add(Math.class.getMethod("log", double.class));
            parameterFunctions.add(Math.class.getMethod("max", float.class, float.class));
            parameterFunctions.add(Math.class.getMethod("min", float.class, float.class));
            parameterFunctions.add(Math.class.getMethod("random"));
            parameterFunctions.add(Math.class.getMethod("round", float.class));
            parameterFunctions.add(Math.class.getMethod("signum", double.class));
            parameterFunctions.add(Math.class.getMethod("sin", double.class));
            parameterFunctions.add(Math.class.getMethod("sinh", double.class));
            parameterFunctions.add(Math.class.getMethod("sqrt", double.class));
            parameterFunctions.add(Math.class.getMethod("tan", double.class));
            parameterFunctions.add(Math.class.getMethod("tanh", double.class));
        } catch (NoSuchMethodException | SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ExpressionFactory() {
        // Hide default constructor for utils classes
    }

    /**
     * List of functions that can be used in MapCSS expressions.
     *
     * First parameter can be of type {@link Environment} (if needed). This is
     * automatically filled in by JOSM and the user only sees the remaining
     * arguments.
     * When one of the user supplied arguments cannot be converted the
     * expected type or is null, the function is not called and it returns null
     * immediately. Add the annotation {@link NullableArguments} to allow
     * null arguments.
     * Every method must be static.
     */
    @SuppressWarnings("UnusedDeclaration")
    public static class Functions {

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
         * @param args arguments
         * @return Sum of arguments
         */
        public static float plus(float... args) {
            float res = 0;
            for (float f : args) {
                res += f;
            }
            return res;
        }

        /**
         * Function associated to the numeric "-" operator.
         * @param args arguments
         * @return Substraction of arguments
         */
        public static Float minus(float... args) {
            if (args.length == 0) {
                return 0.0F;
            }
            if (args.length == 1) {
                return -args[0];
            }
            float res = args[0];
            for (int i = 1; i < args.length; ++i) {
                res -= args[i];
            }
            return res;
        }

        /**
         * Function associated to the numeric "*" operator.
         * @param args arguments
         * @return Multiplication of arguments
         */
        public static float times(float... args) {
            float res = 1;
            for (float f : args) {
                res *= f;
            }
            return res;
        }

        /**
         * Function associated to the numeric "/" operator.
         * @param args arguments
         * @return Division of arguments
         */
        public static Float divided_by(float... args) {
            if (args.length == 0) {
                return 1.0F;
            }
            float res = args[0];
            for (int i = 1; i < args.length; ++i) {
                if (Float.floatToRawIntBits(args[i]) == 0) {
                    return null;
                }
                res /= args[i];
            }
            return res;
        }

        /**
         * Creates a list of values, e.g., for the {@code dashes} property.
         * @param args The values to put in a list
         * @return list of values
         * @see Arrays#asList(Object[])
         */
        public static List<Object> list(Object... args) {
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
         * @param args arguments
         * @return the first non-null object
         * @see #coalesce(Object...)
         * @see Utils#firstNonNull(Object[])
         */
        @NullableArguments
        public static Object any(Object... args) {
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
                return null;
            }
        }

        /**
         * Creates a color value with the specified amounts of {@code r}ed, {@code g}reen, {@code b}lue, {@code alpha} (arguments from 0.0 to 1.0)
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
            return Utils.color_int2float(c.getRed());
        }

        /**
         * Get the value of the green color channel in the rgb color model
         * @param c color
         * @return the green color channel in the range [0;1]
         * @see java.awt.Color#getGreen()
         */
        public static float green(Color c) {
            return Utils.color_int2float(c.getGreen());
        }

        /**
         * Get the value of the blue color channel in the rgb color model
         * @param c color
         * @return the blue color channel in the range [0;1]
         * @see java.awt.Color#getBlue()
         */
        public static float blue(Color c) {
            return Utils.color_int2float(c.getBlue());
        }

        /**
         * Get the value of the alpha channel in the rgba color model
         * @param c color
         * @return the alpha channel in the range [0;1]
         * @see java.awt.Color#getAlpha()
         */
        public static float alpha(Color c) {
            return Utils.color_int2float(c.getAlpha());
        }

        /**
         * Assembles the strings to one.
         * @param args arguments
         * @return assembled string
         * @see Utils#join
         */
        @NullableArguments
        public static String concat(Object... args) {
            return Utils.join("", Arrays.asList(args));
        }

        /**
         * Assembles the strings to one, where the first entry is used as separator.
         * @param args arguments. First one is used as separator
         * @return assembled string
         * @see Utils#join
         */
        @NullableArguments
        public static String join(String... args) {
            return Utils.join(args[0], Arrays.asList(args).subList(1, args.length));
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
         * Gets the first non-null value of the key {@code key} from the object's parent(s).
         * @param env the environment
         * @param key the OSM key
         * @return first non-null value of the key {@code key} from the object's parent(s)
         */
        public static String parent_tag(final Environment env, String key) {
            if (env.parent == null) {
                if (env.osm != null) {
                    // we don't have a matched parent, so just search all referrers
                    for (OsmPrimitive parent : env.osm.getReferrers()) {
                        String value = parent.get(key);
                        if (value != null) {
                            return value;
                        }
                    }
                }
                return null;
            }
            return env.parent.get(key);
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
         * Determines whether the object has a tag with the given key.
         * @param env the environment
         * @param key the OSM key
         * @return {@code true} if the object has a tag with the given key, {@code false} otherwise
         */
        public static boolean has_tag_key(final Environment env, String key) {
            return env.osm.hasKey(key);
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
            return new Float(env.index + 1);
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
         * Returns the area of a closed way in square meters or {@code null}.
         * @param env the environment
         * @return the area of a closed way in square meters or {@code null}
         * @see Geometry#closedWayArea(Way)
         */
        public static Float areasize(final Environment env) {
            if (env.osm instanceof Way && ((Way) env.osm).isClosed()) {
                return (float) Geometry.closedWayArea((Way) env.osm);
            } else {
                return null;
            }
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
         * Function associated to the logical ">=" operator.
         * @param a first value
         * @param b second value
         * @return {@code true} if {@code a >= b}
         */
        public static boolean greater_equal(float a, float b) {
            return a >= b;
        }

        /**
         * Function associated to the logical "<=" operator.
         * @param a first value
         * @param b second value
         * @return {@code true} if {@code a <= b}
         */
        public static boolean less_equal(float a, float b) {
            return a <= b;
        }

        /**
         * Function associated to the logical ">" operator.
         * @param a first value
         * @param b second value
         * @return {@code true} if {@code a > b}
         */
        public static boolean greater(float a, float b) {
            return a > b;
        }

        /**
         * Function associated to the logical "<" operator.
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
            return Math.toRadians(degree);
        }

        /**
         * Converts an angle diven in cardinal directions to radians.
         * The following values are supported: {@code n}, {@code north}, {@code ne}, {@code northeast},
         * {@code e}, {@code east}, {@code se}, {@code southeast}, {@code s}, {@code south},
         * {@code sw}, {@code southwest}, {@code w}, {@code west}, {@code nw}, {@code northwest}.
         * @param cardinal the angle in cardinal directions.
         * @see RotationAngle#parseCardinalRotation(String)
         * @return the angle in radians
         */
        public static Double cardinal_to_radians(String cardinal) {
            try {
                return RotationAngle.parseCardinalRotation(cardinal);
            } catch (IllegalArgumentException ignore) {
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
            return !equal(a,b);
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
                m = SearchCompiler.compile(searchStr, false, false);
            } catch (ParseError ex) {
                return null;
            }
            return m.match(env.osm);
        }

        /**
         * Obtains the JOSM'key {@link org.openstreetmap.josm.data.Preferences} string for key {@code key},
         * and defaults to {@code def} if that is null.
         * @param key Key in JOSM preference
         * @param def Default value
         * @return value for key, or default value if not found
         * @see org.openstreetmap.josm.data.Preferences#get(String, String)
         */
        public static String JOSM_pref(String key, String def) {
            String res = Main.pref.get(key, null);
            return res != null ? res : def;
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
         * @see OsmPrimitive#getUniqueId()
         */
        public static long osm_id(final Environment env) {
            return env.osm.getUniqueId();
        }

        /**
         * Translates some text for the current locale. The first argument is the text to translate,
         * and the subsequent arguments are parameters for the string indicated by <code>{0}</code>, <code>{1}</code>, …
         * @param args arguments
         * @return the translated string
         */
        @NullableArguments
        public static String tr(String... args) {
            final String text = args[0];
            System.arraycopy(args, 1, args, 0, args.length - 1);
            return org.openstreetmap.josm.tools.I18n.tr(text, (Object[])args);
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
         * Percent-encode a string. (See https://en.wikipedia.org/wiki/Percent-encoding)
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
         *
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
            if (env.osm instanceof Node)
                return RightAndLefthandTraffic.isRightHandTraffic(((Node) env.osm).getCoor());
            return RightAndLefthandTraffic.isRightHandTraffic(env.osm.getBBox().getCenter());
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
            return env.osm.getNumKeys();
        }

        /**
         * Get value of a setting.
         * @param env the environment
         * @param key setting key (given as layer identifier, e.g. setting::mykey {...})
         * @return the value of the setting (calculated when the style is loaded)
         */
        public static Object setting(Environment env, String key) {
            return env.source.settingValues.get(key);
        }
    }

    /**
     * Main method to create an function-like expression.
     *
     * @param name the name of the function or operator
     * @param args the list of arguments (as expressions)
     * @return the generated Expression. If no suitable function can be found,
     * returns {@link NullExpression#INSTANCE}.
     */
    public static Expression createFunctionExpression(String name, List<Expression> args) {
        if ("cond".equals(name) && args.size() == 3)
            return new CondOperator(args.get(0), args.get(1), args.get(2));
        else if ("and".equals(name))
            return new AndOperator(args);
        else if ("or".equals(name))
            return new OrOperator(args);
        else if ("length".equals(name) && args.size() == 1)
            return new LengthFunction(args.get(0));
        else if ("max".equals(name) && !args.isEmpty())
            return new MinMaxFunction(args, true);
        else if ("min".equals(name) && !args.isEmpty())
            return new MinMaxFunction(args, false);

        for (Method m : arrayFunctions) {
            if (m.getName().equals(name))
                return new ArrayFunction(m, args);
        }
        for (Method m : parameterFunctions) {
            if (m.getName().equals(name) && args.size() == m.getParameterTypes().length)
                return new ParameterFunction(m, args, false);
        }
        for (Method m : parameterFunctionsEnv) {
            if (m.getName().equals(name) && args.size() == m.getParameterTypes().length-1)
                return new ParameterFunction(m, args, true);
        }
        return NullExpression.INSTANCE;
    }

    /**
     * Expression that always evaluates to null.
     */
    public static class NullExpression implements Expression {

        /**
         * The unique instance.
         */
        public static final NullExpression INSTANCE = new NullExpression();

        @Override
        public Object evaluate(Environment env) {
            return null;
        }
    }

    /**
     * Conditional operator.
     */
    public static class CondOperator implements Expression {

        private Expression condition, firstOption, secondOption;

        /**
         * Constructs a new {@code CondOperator}.
         * @param condition condition
         * @param firstOption first option
         * @param secondOption second option
         */
        public CondOperator(Expression condition, Expression firstOption, Expression secondOption) {
            this.condition = condition;
            this.firstOption = firstOption;
            this.secondOption = secondOption;
        }

        @Override
        public Object evaluate(Environment env) {
            Boolean b = Cascade.convertTo(condition.evaluate(env), boolean.class);
            if (b != null && b)
                return firstOption.evaluate(env);
            else
                return secondOption.evaluate(env);
        }
    }

    /**
     * "And" logical operator.
     */
    public static class AndOperator implements Expression {

        private List<Expression> args;

        /**
         * Constructs a new {@code AndOperator}.
         * @param args arguments
         */
        public AndOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            for (Expression arg : args) {
                Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                if (b == null || !b) {
                    return Boolean.FALSE;
                }
            }
            return Boolean.TRUE;
        }
    }

    /**
     * "Or" logical operator.
     */
    public static class OrOperator implements Expression {

        private List<Expression> args;

        /**
         * Constructs a new {@code OrOperator}.
         * @param args arguments
         */
        public OrOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            for (Expression arg : args) {
                Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                if (b != null && b) {
                    return Boolean.TRUE;
                }
            }
            return Boolean.FALSE;
        }
    }

    /**
     * Function to calculate the length of a string or list in a MapCSS eval expression.
     *
     * Separate implementation to support overloading for different argument types.
     *
     * The use for calculating the length of a list is deprecated, use
     * {@link Functions#count(java.util.List)} instead (see #10061).
     */
    public static class LengthFunction implements Expression {

        private Expression arg;

        /**
         * Constructs a new {@code LengthFunction}.
         * @param args arguments
         */
        public LengthFunction(Expression args) {
            this.arg = args;
        }

        @Override
        public Object evaluate(Environment env) {
            List<?> l = Cascade.convertTo(arg.evaluate(env), List.class);
            if (l != null)
                return l.size();
            String s = Cascade.convertTo(arg.evaluate(env), String.class);
            if (s != null)
                return s.length();
            return null;
        }
    }

    /**
     * Computes the maximum/minimum value an arbitrary number of floats, or a list of floats.
     */
    public static class MinMaxFunction implements Expression {

        private final List<Expression> args;
        private final boolean computeMax;

        /**
         * Constructs a new {@code MinMaxFunction}.
         * @param args arguments
         * @param computeMax if {@code true}, compute max. If {@code false}, compute min
         */
        public MinMaxFunction(final List<Expression> args, final boolean computeMax) {
            this.args = args;
            this.computeMax = computeMax;
        }

        public Float aggregateList(List<?> lst) {
            final List<Float> floats = Utils.transform(lst, new Utils.Function<Object, Float>() {
                @Override
                public Float apply(Object x) {
                    return Cascade.convertTo(x, float.class);
                }
            });
            final Collection<Float> nonNullList = Utils.filter(floats, Predicates.not(Predicates.isNull()));
            return computeMax ? Collections.max(nonNullList) : Collections.min(nonNullList);
        }

        @Override
        public Object evaluate(final Environment env) {
            List<?> l = Cascade.convertTo(args.get(0).evaluate(env), List.class);
            if (args.size() != 1 || l == null)
                l = Utils.transform(args, new Utils.Function<Expression, Object>() {
                    @Override
                    public Object apply(Expression x) {
                        return x.evaluate(env);
                    }
                });
            return aggregateList(l);
        }
    }

    /**
     * Function that takes a certain number of argument with specific type.
     *
     * Implementation is based on a Method object.
     * If any of the arguments evaluate to null, the result will also be null.
     */
    public static class ParameterFunction implements Expression {

        private final Method m;
        private final boolean nullable;
        private final List<Expression> args;
        private final Class<?>[] expectedParameterTypes;
        private final boolean needsEnvironment;

        /**
         * Constructs a new {@code ParameterFunction}.
         */
        public ParameterFunction(Method m, List<Expression> args, boolean needsEnvironment) {
            this.m = m;
            this.nullable = m.getAnnotation(NullableArguments.class) != null;
            this.args = args;
            this.expectedParameterTypes = m.getParameterTypes();
            this.needsEnvironment = needsEnvironment;
        }

        @Override
        public Object evaluate(Environment env) {
            Object[] convertedArgs;

            if (needsEnvironment) {
                convertedArgs = new Object[args.size()+1];
                convertedArgs[0] = env;
                for (int i = 1; i < convertedArgs.length; ++i) {
                    convertedArgs[i] = Cascade.convertTo(args.get(i-1).evaluate(env), expectedParameterTypes[i]);
                    if (convertedArgs[i] == null && !nullable) {
                        return null;
                    }
                }
            } else {
                convertedArgs = new Object[args.size()];
                for (int i = 0; i < convertedArgs.length; ++i) {
                    convertedArgs[i] = Cascade.convertTo(args.get(i).evaluate(env), expectedParameterTypes[i]);
                    if (convertedArgs[i] == null && !nullable) {
                        return null;
                    }
                }
            }
            Object result = null;
            try {
                result = m.invoke(null, convertedArgs);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Main.error(ex);
                return null;
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("ParameterFunction~");
            b.append(m.getName()).append('(');
            for (int i = 0; i < args.size(); ++i) {
                if (i > 0) b.append(',');
                b.append(expectedParameterTypes[i]).append(' ').append(args.get(i));
            }
            b.append(')');
            return b.toString();
        }
    }

    /**
     * Function that takes an arbitrary number of arguments.
     *
     * Currently, all array functions are static, so there is no need to
     * provide the environment, like it is done in {@link ParameterFunction}.
     * If any of the arguments evaluate to null, the result will also be null.
     */
    public static class ArrayFunction implements Expression {

        private final Method m;
        private final boolean nullable;
        private final List<Expression> args;
        private final Class<?>[] expectedParameterTypes;
        private final Class<?> arrayComponentType;

        /**
         * Constructs a new {@code ArrayFunction}.
         */
        public ArrayFunction(Method m, List<Expression> args) {
            this.m = m;
            this.nullable = m.getAnnotation(NullableArguments.class) != null;
            this.args = args;
            this.expectedParameterTypes = m.getParameterTypes();
            this.arrayComponentType = expectedParameterTypes[0].getComponentType();
        }

        @Override
        public Object evaluate(Environment env) {
            Object[] convertedArgs = new Object[expectedParameterTypes.length];
            Object arrayArg = Array.newInstance(arrayComponentType, args.size());
            for (int i = 0; i < args.size(); ++i) {
                Object o = Cascade.convertTo(args.get(i).evaluate(env), arrayComponentType);
                if (o == null && !nullable) {
                    return null;
                }
                Array.set(arrayArg, i, o);
            }
            convertedArgs[0] = arrayArg;

            Object result = null;
            try {
                result = m.invoke(null, convertedArgs);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Main.error(ex);
                return null;
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("ArrayFunction~");
            b.append(m.getName()).append('(');
            for (int i = 0; i < args.size(); ++i) {
                if (i > 0) b.append(',');
                b.append(arrayComponentType).append(' ').append(args.get(i));
            }
            b.append(')');
            return b.toString();
        }
    }
}
