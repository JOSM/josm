// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.io.XmlWriter;
import org.openstreetmap.josm.tools.ColorHelper;
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

    private static final List<Method> arrayFunctions;
    private static final List<Method> parameterFunctions;
    private static final Functions FUNCTIONS_INSTANCE = new Functions();

    static {
        arrayFunctions = new ArrayList<Method>();
        parameterFunctions = new ArrayList<Method>();
        for (Method m : Functions.class.getDeclaredMethods()) {
            Class<?>[] paramTypes = m.getParameterTypes();
            if (paramTypes.length == 1 && paramTypes[0].isArray()) {
                arrayFunctions.add(m);
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
        } catch (NoSuchMethodException ex) {
            throw new RuntimeException(ex);
        } catch (SecurityException ex) {
            throw new RuntimeException(ex);
        }
    }

    private ExpressionFactory() {
        // Hide default constructor for utils classes
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class Functions {

        Environment env;

        /**
         * Identity function for compatibility with MapCSS specification.
         * @param o any object
         * @return {@code o} unchanged
         */
        public static Object eval(Object o) {
            return o;
        }

        public static float plus(float... args) {
            float res = 0;
            for (float f : args) {
                res += f;
            }
            return res;
        }

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

        public static float times(float... args) {
            float res = 1;
            for (float f : args) {
                res *= f;
            }
            return res;
        }

        public static Float divided_by(float... args) {
            if (args.length == 0) {
                return 1.0F;
            }
            float res = args[0];
            for (int i = 1; i < args.length; ++i) {
                if (args[i] == 0.0F) {
                    return null;
                }
                res /= args[i];
            }
            return res;
        }

        /**
         * Creates a list of values, e.g., for the {@code dashes} property.
         * @see Arrays#asList(Object[])
         */
        public static List list(Object... args) {
            return Arrays.asList(args);
        }

        /**
         * Returns the first non-null object. The name originates from the {@code COALESCE} SQL function.
         * @see Utils#firstNonNull(Object[])
         */
        @NullableArguments
        public static Object coalesce(Object... args) {
            return Utils.firstNonNull(args);
        }

        /**
         * Get the {@code n}th element of the list {@code lst} (counting starts at 0).
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
         * @see String#split(String)
         * @since 5699
         */
        public static List<String> split(String sep, String toSplit) {
            return Arrays.asList(toSplit.split(Pattern.quote(sep), -1));
        }

        /**
         * Creates a color value with the specified amounts of {@code r}ed, {@code g}reen, {@code b}lue (arguments from 0.0 to 1.0)
         * @see Color#Color(float, float, float)
         */
        public static Color rgb(float r, float g, float b) {
            Color c;
            try {
                c = new Color(r, g, b);
            } catch (IllegalArgumentException e) {
                return null;
            }
            return c;
        }

        /**
         * Creates a color value from an HTML notation, i.e., {@code #rrggbb}.
         */
        public static Color html2color(String html) {
            return ColorHelper.html2color(html);
        }

        /**
         * Computes the HTML notation ({@code #rrggbb}) for a color value).
         */
        public static String color2html(Color c) {
            return ColorHelper.color2html(c);
        }

        /**
         * Get the value of the red color channel in the rgb color model
         * @return the red color channel in the range [0;1]
         * @see java.awt.Color#getRed()
         */
        public static float red(Color c) {
            return Utils.color_int2float(c.getRed());
        }

        /**
         * Get the value of the green color channel in the rgb color model
         * @return the green color channel in the range [0;1]
         * @see java.awt.Color#getGreen()
         */
        public static float green(Color c) {
            return Utils.color_int2float(c.getGreen());
        }

        /**
         * Get the value of the blue color channel in the rgb color model
         * @return the blue color channel in the range [0;1]
         * @see java.awt.Color#getBlue()
         */
        public static float blue(Color c) {
            return Utils.color_int2float(c.getBlue());
        }

        /**
         * Get the value of the alpha channel in the rgba color model
         * @return the alpha channel in the range [0;1]
         * @see java.awt.Color#getAlpha()
         */
        public static float alpha(Color c) {
            return Utils.color_int2float(c.getAlpha());
        }

        /**
         * Assembles the strings to one.
         * @see Utils#join
         */
        @NullableArguments
        public static String concat(Object... args) {
            return Utils.join("", Arrays.asList(args));
        }

        /**
         * Assembles the strings to one, where the first entry is used as separator.
         * @see Utils#join
         */
        @NullableArguments
        public static String join(String... args) {
            return Utils.join(args[0], Arrays.asList(args).subList(1, args.length));
        }

        /**
         * Returns the value of the property {@code key}, e.g., {@code prop("width")}.
         */
        public Object prop(String key) {
            return prop(key, null);
        }

        /**
         * Returns the value of the property {@code key} from layer {@code layer}.
         */
        public Object prop(String key, String layer) {
            return env.getCascade(layer).get(key);
        }

        /**
         * Determines whether property {@code key} is set.
         */
        public Boolean is_prop_set(String key) {
            return is_prop_set(key, null);
        }

        /**
         * Determines whether property {@code key} is set on layer {@code layer}.
         */
        public Boolean is_prop_set(String key, String layer) {
            return env.getCascade(layer).containsKey(key);
        }

        /**
         * Gets the value of the key {@code key} from the object in question.
         */
        public String tag(String key) {
            return env.osm == null ? null : env.osm.get(key);
        }

        /**
         * Gets the first non-null value of the key {@code key} from the object's parent(s).
         */
        public String parent_tag(String key) {
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
         * Determines whether the object has a tag with the given key.
         */
        public boolean has_tag_key(String key) {
            return env.osm.hasKey(key);
        }

        /**
         * Returns the index of node in parent way or member in parent relation.
         */
        public Float index() {
            if (env.index == null) {
                return null;
            }
            return new Float(env.index + 1);
        }

        public String role() {
            return env.getRole();
        }

        public static boolean not(boolean b) {
            return !b;
        }

        public static boolean greater_equal(float a, float b) {
            return a >= b;
        }

        public static boolean less_equal(float a, float b) {
            return a <= b;
        }

        public static boolean greater(float a, float b) {
            return a > b;
        }

        public static boolean less(float a, float b) {
            return a < b;
        }

        /**
         * Determines if the objects {@code a} and {@code b} are equal.
         * @see Object#equals(Object)
         */
        public static boolean equal(Object a, Object b) {
            // make sure the casts are done in a meaningful way, so
            // the 2 objects really can be considered equal
            for (Class<?> klass : new Class[]{Float.class, Boolean.class, Color.class, float[].class, String.class}) {
                Object a2 = Cascade.convertTo(a, klass);
                Object b2 = Cascade.convertTo(b, klass);
                if (a2 != null && b2 != null && a2.equals(b2)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Determines whether the JOSM search with {@code searchStr} applies to the object.
         */
        public Boolean JOSM_search(String searchStr) {
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
         * @see org.openstreetmap.josm.data.Preferences#get(String, String)
         */
        public static String JOSM_pref(String key, String def) {
            String res = Main.pref.get(key, null);
            return res != null ? res : def;
        }

        /**
         * Obtains the JOSM'key {@link org.openstreetmap.josm.data.Preferences} color for key {@code key},
         * and defaults to {@code def} if that is null.
         * @see org.openstreetmap.josm.data.Preferences#getColor(String, java.awt.Color)
         */
        public static Color JOSM_pref_color(String key, Color def) {
            Color res = Main.pref.getColor(key, null);
            return res != null ? res : def;
        }

        /**
         * Tests if string {@code target} matches pattern {@code pattern}
         * @see Pattern#matches(String, CharSequence)
         * @since 5699
         */
        public static boolean regexp_test(String pattern, String target) {
            return Pattern.matches(pattern, target);
        }

        /**
         * Tests if string {@code target} matches pattern {@code pattern}
         * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
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
         * @param flags a string that may contain "i" (case insensitive), "m" (multiline) and "s" ("dot all")
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
            Matcher m = Pattern.compile(pattern, f).matcher(target);
            return Utils.getMatches(m);
        }

        /**
         * Tries to match string against pattern regexp and returns a list of capture groups in case of success.
         * The first element (index 0) is the complete match (i.e. string).
         * Further elements correspond to the bracketed parts of the regular expression.
         * @since 5701
         */
        public static List<String> regexp_match(String pattern, String target) {
            Matcher m = Pattern.compile(pattern).matcher(target);
            return Utils.getMatches(m);
        }

        /**
         * Returns the OSM id of the current object.
         * @see OsmPrimitive#getUniqueId()
         */
        public long osm_id() {
            return env.osm.getUniqueId();
        }

        /**
         * Translates some text for the current locale. The first argument is the text to translate,
         * and the subsequent arguments are parameters for the string indicated by {@code {0}}, {@code {1}}, …
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
         * * @see String#replace(CharSequence, CharSequence)
         */
        public static String replace(String s, String target, String replacement) {
            return s == null ? null : s.replace(target, replacement);
        }

        /**
         * Percent-encode a string. (See https://en.wikipedia.org/wiki/Percent-encoding)
         * This is especially useful for data urls, e.g.
         * <code>icon-image: concat("data:image/svg+xml,", URL_encode("&lt;svg&gt;...&lt;/svg&gt;"));</code>
         * @param s arbitrary string
         * @return the encoded string
         */
        public static String URL_encode(String s) {
            try {
                return s == null ? null : URLEncoder.encode(s, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                throw new RuntimeException(ex);
            }
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
        if (equal(name, "cond") && args.size() == 3)
            return new CondOperator(args.get(0), args.get(1), args.get(2));
        else if (equal(name, "and"))
            return new AndOperator(args);
        else if (equal(name, "or"))
            return new OrOperator(args);
        else if (equal(name, "length") && args.size() == 1)
            return new LengthFunction(args.get(0));

        for (Method m : arrayFunctions) {
            if (m.getName().equals(name))
                return new ArrayFunction(m, args);
        }
        for (Method m : parameterFunctions) {
            if (m.getName().equals(name) && args.size() == m.getParameterTypes().length)
                return new ParameterFunction(m, args);
        }
        return NullExpression.INSTANCE;
    }

    /**
     * Expression that always evaluates to null.
     */
    public static class NullExpression implements Expression {

        final public static NullExpression INSTANCE = new NullExpression();

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

    public static class AndOperator implements Expression {

        private List<Expression> args;

        public AndOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            for (Expression arg : args) {
                Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                if (b == null || !b) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class OrOperator implements Expression {

        private List<Expression> args;

        public OrOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            for (Expression arg : args) {
                Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                if (b != null && b) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Function to calculate the length of a string or list in a MapCSS eval
     * expression.
     *
     * Separate implementation to support overloading for different
     * argument types.
     */
    public static class LengthFunction implements Expression {

        private Expression arg;

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
     * Function that takes a certain number of argument with specific type.
     *
     * Implementation is based on a Method object.
     * If any of the arguments evaluate to null, the result will also be null.
     */
    public static class ParameterFunction implements Expression {

        private final Method m;
        private final List<Expression> args;
        private final Class<?>[] expectedParameterTypes;

        public ParameterFunction(Method m, List<Expression> args) {
            this.m = m;
            this.args = args;
            expectedParameterTypes = m.getParameterTypes();
        }

        @Override
        public Object evaluate(Environment env) {
            FUNCTIONS_INSTANCE.env = env;
            Object[] convertedArgs = new Object[expectedParameterTypes.length];
            for (int i = 0; i < args.size(); ++i) {
                convertedArgs[i] = Cascade.convertTo(args.get(i).evaluate(env), expectedParameterTypes[i]);
                if (convertedArgs[i] == null && m.getAnnotation(NullableArguments.class) == null) {
                    return null;
                }
            }
            Object result = null;
            try {
                result = m.invoke(FUNCTIONS_INSTANCE, convertedArgs);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Main.error(ex);
                return null;
            }
            return result;
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
        private final List<Expression> args;
        private final Class<?> arrayComponentType;
        private final Object[] convertedArgs;

        public ArrayFunction(Method m, List<Expression> args) {
            this.m = m;
            this.args = args;
            Class<?>[] expectedParameterTypes = m.getParameterTypes();
            convertedArgs = new Object[expectedParameterTypes.length];
            arrayComponentType = expectedParameterTypes[0].getComponentType();
        }

        @Override
        public Object evaluate(Environment env) {
            Object arrayArg = Array.newInstance(arrayComponentType, args.size());
            for (int i = 0; i < args.size(); ++i) {
                Object o = Cascade.convertTo(args.get(i).evaluate(env), arrayComponentType);
                if (o == null && m.getAnnotation(NullableArguments.class) == null) {
                    return null;
                }
                Array.set(arrayArg, i, o);
            }
            convertedArgs[0] = arrayArg;

            Object result = null;
            try {
                result = m.invoke(null, convertedArgs);
            } catch (IllegalAccessException ex) {
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Main.error(ex);
                return null;
            }
            return result;
        }
    }

}
