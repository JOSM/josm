// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.gui.mappaint.Cascade;
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

        @Override
        public Object evaluate(Environment env) {
            if (name.equals("eval")) {
                if (args.size() != 1) {
                    return null;
                }
                return args.get(0).evaluate(env);
            }
            
            if (name.equals("prop")) {
                if (!(args.size() == 1 || args.size() == 2))
                    return null;
                Object pr = args.get(0).evaluate(env);
                if (!(pr instanceof String))
                    return null;
                Cascade c;
                if (args.size() == 1) {
                    c = env.getCascade();
                } else {
                    Object layer = args.get(1).evaluate(env);
                    if (!(layer instanceof String))
                        return null;
                    c = env.mc.getCascade((String) layer);
                }
                return c.get((String) pr);
            }
            if (name.equals("+") || name.equals("*")) {
                float result = name.equals("+") ? 0f : 1f;
                for (Expression exp : args) {
                    Float f = getFloat(exp.evaluate(env));
                    if (f == null)
                        return null;
                    if (name.equals("+")) {
                        result += f;
                    } else {
                        result *= f;
                    }
                }
                return result;
            }
            if (name.equals("-")) {
                if (args.size() != 2) {
                    return null;
                }
                Float fst = getFloat(args.get(0).evaluate(env));
                Float snd = getFloat(args.get(1).evaluate(env));
                if (fst == null || snd == null)
                    return null;
                return fst - snd;
            }
            return null;
        }

        @Override
        public String toString() {
            return name + "(" + Utils.join(", ", args) + ")";
        }

        static Float getFloat(Object o) {
            if (o instanceof Float)
                return (Float) o;
            if (o instanceof Integer)
                return new Float((Integer) o);
            return null;
        }
    }
}
