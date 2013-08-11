// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.awt.Color;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import org.openstreetmap.josm.tools.ColorHelper;
import org.openstreetmap.josm.tools.Utils;

/**
 * Factory to generate Expressions.
 *
 * See {@link #createFunctionExpression}.
 */
public class ExpressionFactory {

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

    public static class Functions {

        Environment env;

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

        public static List list(Object... args) {
            return Arrays.asList(args);
        }

        public static Object get(List<? extends Object> objects, float index) {
            int idx = Math.round(index);
            if (idx >= 0 && idx < objects.size()) {
                return objects.get(idx);
            }
            return null;
        }

        public static List<String> split(String sep, String toSplit) {
            return Arrays.asList(toSplit.split(Pattern.quote(sep), -1));
        }

        public static Color rgb(float r, float g, float b) {
            Color c;
            try {
                c = new Color(r, g, b);
            } catch (IllegalArgumentException e) {
                return null;
            }
            return c;
        }

        public static Color html2color(String html) {
            return ColorHelper.html2color(html);
        }

        public static String color2html(Color c) {
            return ColorHelper.color2html(c);
        }

        public static float red(Color c) {
            return Utils.color_int2float(c.getRed());
        }

        public static float green(Color c) {
            return Utils.color_int2float(c.getGreen());
        }

        public static float blue(Color c) {
            return Utils.color_int2float(c.getBlue());
        }

        public static String concat(Object... args) {
            StringBuilder res = new StringBuilder();
            for (Object f : args) {
                res.append(f.toString());
            }
            return res.toString();
        }

        public Object prop(String key) {
            return prop(key, null);
        }

        public Object prop(String key, String layer) {
            Cascade c;
            if (layer == null) {
                c = env.mc.getCascade(env.layer);
            } else {
                c = env.mc.getCascade(layer);
            }
            return c.get(key);
        }

        public Boolean is_prop_set(String key) {
            return is_prop_set(key, null);
        }

        public Boolean is_prop_set(String key, String layer) {
            Cascade c;
            if (layer == null) {
                // env.layer is null if expression is evaluated
                // in ExpressionCondition, but MultiCascade.getCascade
                // handles this
                c = env.mc.getCascade(env.layer);
            } else {
                c = env.mc.getCascade(layer);
            }
            return c.containsKey(key);
        }

        public String tag(String key) {
            return env.osm.get(key);
        }

        public String parent_tag(String key) {
            if (env.parent == null) {
                // we don't have a matched parent, so just search all referrers
                for (OsmPrimitive parent : env.osm.getReferrers()) {
                    String value = parent.get(key);
                    if (value != null) {
                        return value;
                    }
                }
                return null;
            }
            return env.parent.get(key);
        }

        public boolean has_tag_key(String key) {
            return env.osm.hasKey(key);
        }

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

        public Boolean JOSM_search(String s) {
            Match m;
            try {
                m = SearchCompiler.compile(s, false, false);
            } catch (ParseError ex) {
                return null;
            }
            return m.match(env.osm);
        }

        public static String JOSM_pref(String s, String def) {
            String res = Main.pref.get(s, null);
            return res != null ? res : def;
        }

        public static Color JOSM_pref_color(String s, Color def) {
            Color res = Main.pref.getColor(s, null);
            return res != null ? res : def;
        }

        public static boolean regexp_test(String pattern, String target) {
            return Pattern.matches(pattern, target);
        }

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
            if (m.matches()) {
                List<String> result = new ArrayList<String>(m.groupCount() + 1);
                for (int i = 0; i <= m.groupCount(); i++) {
                    result.add(m.group(i));
                }
                return result;
            } else {
                return null;
            }
        }

        public static List<String> regexp_match(String pattern, String target) {
            Matcher m = Pattern.compile(pattern).matcher(target);
            if (m.matches()) {
                List<String> result = new ArrayList<String>(m.groupCount() + 1);
                for (int i = 0; i <= m.groupCount(); i++) {
                    result.add(m.group(i));
                }
                return result;
            } else {
                return null;
            }
        }

        public long osm_id() {
            return env.osm.getUniqueId();
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
                if (convertedArgs[i] == null) {
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
                System.err.println(ex);
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
                if (o == null) {
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
                System.err.println(ex);
                return null;
            }
            return result;
        }
    }

}
