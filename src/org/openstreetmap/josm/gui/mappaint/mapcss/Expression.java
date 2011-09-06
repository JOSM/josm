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

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.actions.search.SearchCompiler.Match;
import org.openstreetmap.josm.actions.search.SearchCompiler.ParseError;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Utils;

public interface Expression {
    public Object evaluate(Environment env);

    public static class LiteralExpression implements Expression {
        Object literal;

        public LiteralExpression(Object literal) {
            CheckParameterUtil.ensureParameterNotNull(literal);
            this.literal = literal;
        }

        @Override
        public Object evaluate(Environment env) {
            return literal;
        }

        @Override
        public String toString() {
            if (literal instanceof float[])
                return Arrays.toString((float[]) literal);
            return "<"+literal.toString()+">";
        }
    }

    public static class FunctionExpression implements Expression {
        String name;
        List<Expression> args;

        public FunctionExpression(String name, List<Expression> args) {
            this.name = name;
            this.args = args;
        }

        public static class EvalFunctions {
            Environment env;

            public Object eval(Object o) {
                return o;
            }

            public static float plus(float... args) {
                float res = 0;
                for (float f : args) {
                    res += f;
                }
                return res;
            }

            public Float minus(float... args) {
                if (args.length == 0)
                    return 0f;
                if (args.length == 1)
                    return -args[0];
                float res = args[0];
                for (int i=1; i<args.length; ++i) {
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

            public Float divided_by(float... args) {
                if (args.length == 0)
                    return 1f;
                float res = args[0];
                for (int i=1; i<args.length; ++i) {
                    if (args[i] == 0f)
                        return null;
                    res /= args[i];
                }
                return res;
            }

            public static List list(Object... args) {
                return Arrays.asList(args);
            }

            public Color rgb(float r, float g, float b) {
                Color c = null;
                try {
                    c = new Color(r, g, b);
                } catch (IllegalArgumentException e) {
                    return null;
                }
                return c;
            }

            public float red(Color c) {
                return Utils.color_int2float(c.getRed());
            }

            public float green(Color c) {
                return Utils.color_int2float(c.getGreen());
            }

            public float blue(Color c) {
                return Utils.color_int2float(c.getBlue());
            }

            public String concat(Object... args) {
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
                        if (value != null)
                            return value;
                    }
                    return null;
                }
                return env.parent.get(key);
            }

            public boolean has_tag_key(String key) {
                return env.osm.hasKey(key);
            }

            public Float index() {
                if (env.index == null)
                    return null;
                return new Float(env.index + 1);
            }

            public String role() {
                return env.getRole();
            }

            public boolean not(boolean b) {
                return !b;
            }

            public boolean greater_equal(float a, float b) {
                return a >= b;
            }

            public boolean less_equal(float a, float b) {
                return a <= b;
            }

            public boolean greater(float a, float b) {
                return a > b;
            }

            public boolean less(float a, float b) {
                return a < b;
            }

            public int length(String s) {
                return s.length();
            }

            @SuppressWarnings("unchecked")
            public boolean equal(Object a, Object b) {
                // make sure the casts are done in a meaningful way, so
                // the 2 objects really can be considered equal
                for (Class klass : new Class[] {
                        Float.class, Boolean.class, Color.class, float[].class, String.class }) {
                    Object a2 = Cascade.convertTo(a, klass);
                    Object b2 = Cascade.convertTo(b, klass);
                    if (a2 != null && b2 != null && a2.equals(b2))
                        return true;
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

            public String JOSM_pref(String s, String def) {
                String res = Main.pref.get(s, null);
                return res != null ? res : def;
            }

            public Color JOSM_pref_color(String s, Color def) {
                Color res = Main.pref.getColor(s, null);
                return res != null ? res : def;
            }
        }

        @Override
        public Object evaluate(Environment env) {
            if (equal(name, "cond")) { // this needs special handling since only one argument should be evaluated
                if (args.size() != 3)
                    return null;
                Boolean b = Cascade.convertTo(args.get(0).evaluate(env), boolean.class);
                if (b == null)
                    return null;
                return args.get(b ? 1 : 2).evaluate(env);
            }
            if (equal(name, "and")) {
                for (Expression arg : args) {
                    Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                    if (b == null || !b)
                        return false;
                }
                return true;
            }
            if (equal(name, "or")) {
                for (Expression arg : args) {
                    Boolean b = Cascade.convertTo(arg.evaluate(env), boolean.class);
                    if (b != null && b)
                        return true;
                }
                return false;
            }
            EvalFunctions fn = new EvalFunctions();
            fn.env = env;
            Method[] customMethods = EvalFunctions.class.getDeclaredMethods();
            List<Method> allMethods = new ArrayList<Method>();
            allMethods.addAll(Arrays.asList(customMethods));
            try {
                allMethods.add(Math.class.getMethod("abs", float.class));
                allMethods.add(Math.class.getMethod("acos", double.class));
                allMethods.add(Math.class.getMethod("asin", double.class));
                allMethods.add(Math.class.getMethod("atan", double.class));
                allMethods.add(Math.class.getMethod("atan2", double.class, double.class));
                allMethods.add(Math.class.getMethod("ceil", double.class));
                allMethods.add(Math.class.getMethod("cos", double.class));
                allMethods.add(Math.class.getMethod("cosh", double.class));
                allMethods.add(Math.class.getMethod("exp", double.class));
                allMethods.add(Math.class.getMethod("floor", double.class));
                allMethods.add(Math.class.getMethod("log", double.class));
                allMethods.add(Math.class.getMethod("max", float.class, float.class));
                allMethods.add(Math.class.getMethod("min", float.class, float.class));
                allMethods.add(Math.class.getMethod("random"));
                allMethods.add(Math.class.getMethod("round", float.class));
                allMethods.add(Math.class.getMethod("signum", double.class));
                allMethods.add(Math.class.getMethod("sin", double.class));
                allMethods.add(Math.class.getMethod("sinh", double.class));
                allMethods.add(Math.class.getMethod("sqrt", double.class));
                allMethods.add(Math.class.getMethod("tan", double.class));
                allMethods.add(Math.class.getMethod("tanh", double.class));
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException(ex);
            } catch (SecurityException ex) {
                throw  new RuntimeException(ex);
            }
            for (Method m : allMethods) {
                if (!m.getName().equals(name)) {
                    continue;
                }
                Class<?>[] expectedParameterTypes = m.getParameterTypes();
                Object[] convertedArgs = new Object[expectedParameterTypes.length];

                if (expectedParameterTypes.length == 1 && expectedParameterTypes[0].isArray())
                {
                    Class<?> arrayComponentType = expectedParameterTypes[0].getComponentType();
                    Object arrayArg = Array.newInstance(arrayComponentType, args.size());
                    for (int i=0; i<args.size(); ++i)
                    {
                        Object o = Cascade.convertTo(args.get(i).evaluate(env), arrayComponentType);
                        if (o == null)
                            return null;
                        Array.set(arrayArg, i, o);
                    }
                    convertedArgs[0] = arrayArg;
                } else {
                    if (args.size() != expectedParameterTypes.length) {
                        continue;
                    }
                    for (int i=0; i<args.size(); ++i) {
                        convertedArgs[i] = Cascade.convertTo(args.get(i).evaluate(env), expectedParameterTypes[i]);
                        if (convertedArgs[i] == null)
                            return null;
                    }
                }
                Object result = null;
                try {
                    result = m.invoke(fn, convertedArgs);
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
            return null;
        }

        @Override
        public String toString() {
            return name + "(" + Utils.join(", ", args) + ")";
        }

    }
}
