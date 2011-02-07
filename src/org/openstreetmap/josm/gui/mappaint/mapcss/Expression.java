// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

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
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

public interface Expression {

    public Object evaluate(Environment env);

    public static class LiteralExpression implements Expression {
        Object literal;

        public LiteralExpression(Object lit) {
            this.literal = lit;
        }

        @Override
        public Object evaluate(Environment env) {
            return literal;
        }

        @Override
        public String toString() {
            if (literal == null)
                return "Lit{<null>}";
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
                if (args.length == 1) { // unary minus
                    return -args[0];
                }
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

            public Float devided_by(float... args) {
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

            public Object prop(String key) {
                return prop(key, null);
            }

            public Object prop(String key, String layer) {
                Cascade c;
                if (layer == null) {
                    c = env.getCascade();
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
                    c = env.getCascade();
                } else {
                    c = env.mc.getCascade(layer);
                }
                return c.containsKey(key);
            }

            public String get_tag_value(String key) {
                return env.osm.get(key);
            }

            public boolean has_tag_key(String key) {
                return env.osm.hasKey(key);
            }

            public Object cond(boolean cond, Object if_, Object else_) {
                return cond ? if_ : else_; // fixme: do not evaluate the other branch
            }

            public boolean not(boolean b) {
                return !b;
            }

            public boolean and(boolean... bs) {
                for (boolean b : bs) {  // fixme: lazy evaluation
                    if (!b)
                        return false;
                }
                return true;
            }

            public boolean or(boolean... bs) {
                for (boolean b : bs) {
                    if (b)
                        return true;
                }
                return false;
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

            public boolean equal(Object a, Object b) {
                // make sure the casts are done in a meaningful way, so
                // the 2 objects really can be considered equal
                for (Class klass : new Class[] {
                        Float.class, Boolean.class, Color.class, float[].class, String.class }) {
                    Object a2 = Cascade.convertTo(a, klass);
                    Object b2 = Cascade.convertTo(a, klass);
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
                if (!m.getName().equals(name))
                    continue;
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
                    if (args.size() != expectedParameterTypes.length)
                        continue;
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
