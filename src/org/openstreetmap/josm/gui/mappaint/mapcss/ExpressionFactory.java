// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.SubclassFilteredCollection;
import org.openstreetmap.josm.tools.Utils;

/**
 * Factory to generate {@link Expression}s.
 * <p>
 * See {@link #createFunctionExpression}.
 */
public final class ExpressionFactory {

    /**
     * Marks functions which should be executed also when one or more arguments are null.
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface NullableArguments {}

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
            throw new JosmRuntimeException(ex);
        }
    }

    private ExpressionFactory() {
        // Hide default constructor for utils classes
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

        private final Expression condition, firstOption, secondOption;

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

        private final List<Expression> args;

        /**
         * Constructs a new {@code AndOperator}.
         * @param args arguments
         */
        public AndOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            return args.stream()
                    .map(arg -> Cascade.convertTo(arg.evaluate(env), boolean.class))
                    .allMatch(Boolean.TRUE::equals);
        }
    }

    /**
     * "Or" logical operator.
     */
    public static class OrOperator implements Expression {

        private final List<Expression> args;

        /**
         * Constructs a new {@code OrOperator}.
         * @param args arguments
         */
        public OrOperator(List<Expression> args) {
            this.args = args;
        }

        @Override
        public Object evaluate(Environment env) {
            return args.stream()
                    .map(arg -> Cascade.convertTo(arg.evaluate(env), boolean.class))
                    .anyMatch(Boolean.TRUE::equals);
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

        private final Expression arg;

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

        /**
         * Compute the minimum / maximum over the list
         * @param lst The list
         * @return The minimum or maximum depending on {@link #computeMax}
         */
        public Float aggregateList(List<?> lst) {
            final List<Float> floats = Utils.transform(lst, (Function<Object, Float>) x -> Cascade.convertTo(x, float.class));
            final Collection<Float> nonNullList = SubclassFilteredCollection.filter(floats, Objects::nonNull);
            return nonNullList.isEmpty() ? (Float) Float.NaN : computeMax ? Collections.max(nonNullList) : Collections.min(nonNullList);
        }

        @Override
        public Object evaluate(final Environment env) {
            List<?> l = Cascade.convertTo(args.get(0).evaluate(env), List.class);
            if (args.size() != 1 || l == null)
                l = Utils.transform(args, (Function<Expression, Object>) x -> x.evaluate(env));
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
         * @param m method
         * @param args arguments
         * @param needsEnvironment whether function needs environment
         */
        public ParameterFunction(Method m, List<Expression> args, boolean needsEnvironment) {
            this.m = m;
            this.nullable = m.getAnnotation(NullableArguments.class) != null;
            this.args = args;
            this.expectedParameterTypes = m.getParameterTypes();
            this.needsEnvironment = needsEnvironment;
        }

        /**
         * Returns the method.
         * @return the method
         * @since 14484
         */
        public final Method getMethod() {
            return m;
        }

        /**
         * Returns the arguments.
         * @return the arguments
         * @since 14484
         */
        public final List<Expression> getArgs() {
            return args;
        }

        @Override
        public Object evaluate(Environment env) {
            Object[] convertedArgs;

            int start = 0;
            int offset = 0;
            if (needsEnvironment) {
                start = 1;
                offset = 1;
                convertedArgs = new Object[args.size() + 1];
                convertedArgs[0] = env;
            } else {
                convertedArgs = new Object[args.size()];
            }

            for (int i = start; i < convertedArgs.length; ++i) {
                if (!expectedParameterTypes[i].isArray()) {
                    convertedArgs[i] = Cascade.convertTo(args.get(i - offset).evaluate(env), expectedParameterTypes[i]);
                } else {
                    Class<?> clazz = expectedParameterTypes[i].getComponentType();
                    Object[] varargs = (Object[]) Array.newInstance(clazz, args.size() - i + 1);
                    for (int j = 0; j < args.size() - i + 1; ++j) {
                        varargs[j] = Cascade.convertTo(args.get(j + i - 1).evaluate(env), clazz);
                    }
                    convertedArgs[i] = expectedParameterTypes[i].cast(varargs);
                    break;
                }
                if (convertedArgs[i] == null && !nullable) {
                    return null;
                }
            }

            Object result = null;
            try {
                result = m.invoke(null, convertedArgs);
            } catch (IllegalAccessException | IllegalArgumentException ex) {
                throw new JosmRuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Logging.error(ex);
                return null;
            }
            return result;
        }

        @Override
        public String toString() {
            StringBuilder b = new StringBuilder("ParameterFunction~");
            b.append(m.getName()).append('(');
            for (int i = 0; i < expectedParameterTypes.length; ++i) {
                if (i > 0) b.append(',');
                b.append(expectedParameterTypes[i]);
                if (!needsEnvironment) {
                    b.append(' ').append(args.get(i));
                } else if (i > 0) {
                    b.append(' ').append(args.get(i-1));
                }
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
         * @param m method
         * @param args arguments
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
                throw new JosmRuntimeException(ex);
            } catch (InvocationTargetException ex) {
                Logging.error(ex);
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
