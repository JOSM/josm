// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.awt.Color;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import org.openstreetmap.josm.data.osm.PrimitiveId;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
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

    @FunctionalInterface
    public interface TriFunction<T, U, V, R> {
        R apply(T t, U u, V v);
    }

    @FunctionalInterface
    public interface QuadFunction<T, U, V, W, R> {
        R apply(T t, U u, V v, W w);
    }

    @FunctionalInterface
    interface Factory {
        Expression createExpression(List<Expression> args);

        static Factory of(DoubleUnaryOperator operator) {
            return of(Double.class, operator::applyAsDouble);
        }

        static Factory ofNumberVarArgs(double identity, DoubleUnaryOperator unaryOperator, DoubleBinaryOperator operator) {
            return args -> env -> {
                if (args.isEmpty()) {
                    return identity;
                } else if (args.size() == 1) {
                    Double arg = Cascade.convertTo(args.get(0).evaluate(env), Double.class);
                    return arg == null ? null : unaryOperator.applyAsDouble(arg);
                } else {
                    return args.stream()
                            .map(arg -> Cascade.convertTo(arg.evaluate(env), Double.class))
                            .filter(Objects::nonNull)
                            .reduce(operator::applyAsDouble).orElse(null);
                }
            };
        }

        static Factory ofStringVarargs(BiFunction<Environment, String[], ?> function) {
            return args -> env -> function.apply(env, args.stream()
                    .map(arg -> Cascade.convertTo(arg.evaluate(env), String.class))
                    .toArray(String[]::new));
        }

        static Factory ofObjectVarargs(BiFunction<Environment, Object[], ?> function) {
            return args -> env -> function.apply(env, args.stream()
                    .map(arg -> arg.evaluate(env))
                    .toArray(Object[]::new));
        }

        static <T> Factory of(Class<T> type, Function<T, ?> function) {
            return args -> env -> {
                T v = Cascade.convertTo(args.get(0).evaluate(env), type);
                return v == null ? null : function.apply(v);
            };
        }

        static <T, U> Factory of(Class<T> type1, Class<U> type2, BiFunction<T, U, ?> function) {
            return args -> env -> {
                T v1 = Cascade.convertTo(args.get(0).evaluate(env), type1);
                U v2 = Cascade.convertTo(args.get(1).evaluate(env), type2);
                return v1 == null || v2 == null ? null : function.apply(v1, v2);
            };
        }

        static <T, U, V> Factory of(Class<T> type1, Class<U> type2, Class<V> type3,
                                    BiFunction<T, U, ?> biFunction, TriFunction<T, U, V, ?> triFunction) {
            return args -> env -> {
                T v1 = !args.isEmpty() ? Cascade.convertTo(args.get(0).evaluate(env), type1) : null;
                U v2 = args.size() >= 2 ? Cascade.convertTo(args.get(1).evaluate(env), type2) : null;
                V v3 = args.size() >= 3 ? Cascade.convertTo(args.get(2).evaluate(env), type3) : null;
                return v1 == null || v2 == null ? null : v3 == null ? biFunction.apply(v1, v2) : triFunction.apply(v1, v2, v3);
            };
        }

        static <T, U, V, W> Factory of(Class<T> type1, Class<U> type2, Class<V> type3, Class<W> type4,
                                       QuadFunction<T, U, V, W, ?> function) {
            return args -> env -> {
                T v1 = !args.isEmpty() ? Cascade.convertTo(args.get(0).evaluate(env), type1) : null;
                U v2 = args.size() >= 2 ? Cascade.convertTo(args.get(1).evaluate(env), type2) : null;
                V v3 = args.size() >= 3 ? Cascade.convertTo(args.get(2).evaluate(env), type3) : null;
                W v4 = args.size() >= 4 ? Cascade.convertTo(args.get(3).evaluate(env), type4) : null;
                return v1 == null || v2 == null || v3 == null || v4 == null ? null : function.apply(v1, v2, v3, v4);
            };
        }

        /**
         * Create a factory that accepts an iterable (array <i>or</i> generic iterable)
         * @param type The expected type class
         * @param function The function to apply the arguments to
         * @param <T> The iterable type
         * @return The result of the function call
         */
        @SuppressWarnings("unchecked")
        static <T> Factory ofIterable(Class<T> type, Function<Iterable<T>, ?> function) {
            return args -> env -> {
                Object arg0 = args.get(0).evaluate(env);
                if (args.size() == 1 && arg0 instanceof Iterable) {
                    return function.apply((Iterable<T>) arg0);
                } else {
                    return function.apply(Arrays.asList(args.stream().map(arg -> Cascade.convertTo(arg, type))
                            .toArray(length -> (T[]) Array.newInstance(type, length))));
                }
            };
        }

        /**
         * Create a {@link Factory} for a function
         * @param function The function to use
         * @return The result of the function
         */
        static Factory ofEnv(Function<Environment, ?> function) {
            return args -> function::apply;
        }

        /**
         * Create a {@link Factory} for a function that takes a parameter
         * @param type The parameter type class
         * @param function The function to use when one argument is available
         * @param <T> the type of the input to the function
         * @return The result of the function
         */
        static <T> Factory ofEnv(Class<T> type, BiFunction<Environment, T, ?> function) {
            return args -> env -> {
                T v = Cascade.convertTo(args.get(0).evaluate(env), type);
                return v == null ? null : function.apply(env, v);
            };
        }

        /**
         * Create a {@link Factory} for an overloaded function
         * @param type1 The first parameter type class
         * @param type2 The second parameter type class
         * @param biFunction The function to use when one argument is available
         * @param triFunction The function to use when two arguments are available
         * @param <T> the type of the input to the function
         * @param <U> the type of the input to the function
         * @return The result of one of the functions
         */
        static <T, U> Factory ofEnv(Class<T> type1, Class<U> type2,
                                    BiFunction<Environment, T, ?> biFunction, TriFunction<Environment, T, U, ?> triFunction) {
            return args -> env -> {
                T v1 = !args.isEmpty() ? Cascade.convertTo(args.get(0).evaluate(env), type1) : null;
                U v2 = args.size() >= 2 ? Cascade.convertTo(args.get(1).evaluate(env), type2) : null;
                return v1 == null ? null : v2 == null ? biFunction.apply(env, v1) : triFunction.apply(env, v1, v2);
            };
        }

        /**
         * Create a {@link Factory} for an overloaded function
         * @param type1 The first parameter type class
         * @param type2 The second parameter type class
         * @param function The function to use when no args are available
         * @param biFunction The function to use when one argument is available
         * @param triFunction The function to use when two arguments are available
         * @param <T> the type of the input to the function
         * @param <U> the type of the input to the function
         * @return The result of one of the functions
         */
        static <T, U> Factory ofEnv(Class<T> type1, Class<U> type2, Function<Environment, ?> function,
                                    BiFunction<Environment, T, ?> biFunction, TriFunction<Environment, T, U, ?> triFunction) {
            return args -> env -> {
                T v1 = !args.isEmpty() ? Cascade.convertTo(args.get(0).evaluate(env), type1) : null;
                U v2 = args.size() >= 2 ? Cascade.convertTo(args.get(1).evaluate(env), type2) : null;
                return v1 == null ? function.apply(env) : v2 == null ? biFunction.apply(env, v1) : triFunction.apply(env, v1, v2);
            };
        }
    }

    static final Map<String, Factory> FACTORY_MAP = new HashMap<>();

    static {
        initFactories();
    }

    @SuppressWarnings("unchecked")
    private static void initFactories() {
        FACTORY_MAP.put("CRC32_checksum", Factory.of(String.class, Functions::CRC32_checksum));
        FACTORY_MAP.put("JOSM_pref", Factory.ofEnv(String.class, String.class, null, Functions::JOSM_pref));
        FACTORY_MAP.put("JOSM_search", Factory.ofEnv(String.class, Functions::JOSM_search));
        FACTORY_MAP.put("URL_decode", Factory.of(String.class, Functions::URL_decode));
        FACTORY_MAP.put("URL_encode", Factory.of(String.class, Functions::URL_encode));
        FACTORY_MAP.put("XML_encode", Factory.of(String.class, Functions::XML_encode));
        FACTORY_MAP.put("abs", Factory.of(Math::acos));
        FACTORY_MAP.put("acos", Factory.of(Math::acos));
        FACTORY_MAP.put("alpha", Factory.of(Color.class, Functions::alpha));
        FACTORY_MAP.put("any", Factory.ofObjectVarargs(Functions::any));
        FACTORY_MAP.put("areasize", Factory.ofEnv(Functions::areasize));
        FACTORY_MAP.put("asin", Factory.of(Math::asin));
        FACTORY_MAP.put("at", Factory.ofEnv(double.class, double.class, null, Functions::at));
        FACTORY_MAP.put("atan", Factory.of(Math::atan));
        FACTORY_MAP.put("atan2", Factory.of(Double.class, Double.class, Math::atan2));
        FACTORY_MAP.put("blue", Factory.of(Color.class, Functions::blue));
        FACTORY_MAP.put("cardinal_to_radians", Factory.of(String.class, Functions::cardinal_to_radians));
        FACTORY_MAP.put("ceil", Factory.of(Math::ceil));
        FACTORY_MAP.put("center", Factory.ofEnv(Functions::center));
        FACTORY_MAP.put("child_tag", Factory.ofEnv(String.class, Functions::child_tag));
        FACTORY_MAP.put("color2html", Factory.of(Color.class, Functions::color2html));
        FACTORY_MAP.put("concat", Factory.ofObjectVarargs(Functions::concat));
        FACTORY_MAP.put("convert_primitive_to_string", Factory.of(PrimitiveId.class, Functions::convert_primitive_to_string));
        FACTORY_MAP.put("convert_primitives_to_string", Factory.ofIterable(PrimitiveId.class, Functions::convert_primitives_to_string));
        FACTORY_MAP.put("cos", Factory.of(Math::cos));
        FACTORY_MAP.put("cosh", Factory.of(Math::cosh));
        FACTORY_MAP.put("count", Factory.of(List.class, Functions::count));
        FACTORY_MAP.put("count_roles", Factory.ofStringVarargs(Functions::count_roles));
        FACTORY_MAP.put("degree_to_radians", Factory.of(Functions::degree_to_radians));
        FACTORY_MAP.put("divided_by", Factory.ofNumberVarArgs(1.0, DoubleUnaryOperator.identity(), Functions::divided_by));
        FACTORY_MAP.put("equal", Factory.of(Object.class, Object.class, Functions::equal));
        FACTORY_MAP.put("eval", Factory.of(Object.class, Functions::eval));
        FACTORY_MAP.put("exp", Factory.of(Math::exp));
        FACTORY_MAP.put("floor", Factory.of(Math::floor));
        FACTORY_MAP.put("get", Factory.of(List.class, float.class, Functions::get));
        FACTORY_MAP.put("gpx_distance", Factory.ofEnv(Functions::gpx_distance));
        FACTORY_MAP.put("greater", Factory.of(float.class, float.class, Functions::greater));
        FACTORY_MAP.put("greater_equal", Factory.of(float.class, float.class, Functions::greater_equal));
        FACTORY_MAP.put("green", Factory.of(Color.class, Functions::green));
        FACTORY_MAP.put("has_tag_key", Factory.ofEnv(String.class, Functions::has_tag_key));
        FACTORY_MAP.put("hsb_color", Factory.of(float.class, float.class, float.class, null, Functions::hsb_color));
        FACTORY_MAP.put("html2color", Factory.of(String.class, Functions::html2color));
        FACTORY_MAP.put("index", Factory.ofEnv(Functions::index));
        FACTORY_MAP.put("inside", Factory.ofEnv(String.class, Functions::inside));
        FACTORY_MAP.put("is_anticlockwise", Factory.ofEnv(Functions::is_anticlockwise));
        FACTORY_MAP.put("is_clockwise", Factory.ofEnv(Functions::is_clockwise));
        FACTORY_MAP.put("is_prop_set", Factory.ofEnv(String.class, String.class, Functions::is_prop_set, Functions::is_prop_set));
        FACTORY_MAP.put("is_right_hand_traffic", Factory.ofEnv(Functions::is_right_hand_traffic));
        FACTORY_MAP.put("is_similar", Factory.of(String.class, String.class, Functions::is_similar));
        FACTORY_MAP.put("join", Factory.ofStringVarargs(Functions::join));
        FACTORY_MAP.put("join_list", Factory.of(String.class, List.class, Functions::join_list));
        FACTORY_MAP.put("less", Factory.of(float.class, float.class, Functions::less));
        FACTORY_MAP.put("less_equal", Factory.of(float.class, float.class, Functions::less_equal));
        FACTORY_MAP.put("list", Factory.ofObjectVarargs(Functions::list));
        FACTORY_MAP.put("log", Factory.of(Math::log));
        FACTORY_MAP.put("lower", Factory.of(String.class, Functions::lower));
        FACTORY_MAP.put("minus", Factory.ofNumberVarArgs(0.0, v -> -v, Functions::minus));
        FACTORY_MAP.put("mod", Factory.of(float.class, float.class, Functions::mod));
        FACTORY_MAP.put("not", Factory.of(boolean.class, Functions::not));
        FACTORY_MAP.put("not_equal", Factory.of(Object.class, Object.class, Functions::not_equal));
        FACTORY_MAP.put("number_of_tags", Factory.ofEnv(Functions::number_of_tags));
        FACTORY_MAP.put("osm_changeset_id", Factory.ofEnv(Functions::osm_changeset_id));
        FACTORY_MAP.put("osm_id", Factory.ofEnv(Functions::osm_id));
        FACTORY_MAP.put("osm_timestamp", Factory.ofEnv(Functions::osm_timestamp));
        FACTORY_MAP.put("osm_user_id", Factory.ofEnv(Functions::osm_user_id));
        FACTORY_MAP.put("osm_user_name", Factory.ofEnv(Functions::osm_user_name));
        FACTORY_MAP.put("osm_version", Factory.ofEnv(Functions::osm_version));
        FACTORY_MAP.put("outside", Factory.ofEnv(String.class, Functions::outside));
        FACTORY_MAP.put("parent_osm_id", Factory.ofEnv(Functions::parent_osm_id));
        FACTORY_MAP.put("parent_osm_primitives", Factory.ofEnv(String.class, String.class,
                Functions::parent_osm_primitives, Functions::parent_osm_primitives, Functions::parent_osm_primitives));
        FACTORY_MAP.put("parent_tag", Factory.ofEnv(String.class, Functions::parent_tag));
        FACTORY_MAP.put("parent_tags", Factory.ofEnv(String.class, Functions::parent_tags));
        FACTORY_MAP.put("parent_way_angle", Factory.ofEnv(Functions::parent_way_angle));
        FACTORY_MAP.put("plus", Factory.ofNumberVarArgs(0.0, DoubleUnaryOperator.identity(), Functions::plus));
        FACTORY_MAP.put("print", Factory.of(Object.class, Functions::print));
        FACTORY_MAP.put("println", Factory.of(Object.class, Functions::println));
        FACTORY_MAP.put("prop", Factory.ofEnv(String.class, String.class, Functions::prop, Functions::prop));
        FACTORY_MAP.put("red", Factory.of(Color.class, Functions::red));
        FACTORY_MAP.put("regexp_match", Factory.of(String.class, String.class, String.class, Functions::regexp_match, Functions::regexp_match));
        FACTORY_MAP.put("regexp_test", Factory.of(String.class, String.class, String.class, Functions::regexp_test, Functions::regexp_test));
        FACTORY_MAP.put("replace", Factory.of(String.class, String.class, String.class, null, Functions::replace));
        FACTORY_MAP.put("rgb", Factory.of(float.class, float.class, float.class, null, Functions::rgb));
        FACTORY_MAP.put("rgba", Factory.of(float.class, float.class, float.class, float.class, Functions::rgba));
        FACTORY_MAP.put("role", Factory.ofEnv(Functions::role));
        FACTORY_MAP.put("round", Factory.of(Math::round));
        FACTORY_MAP.put("setting", Factory.ofEnv(String.class, Functions::setting));
        FACTORY_MAP.put("signum", Factory.of(Math::signum));
        FACTORY_MAP.put("sin", Factory.of(Math::sin));
        FACTORY_MAP.put("sinh", Factory.of(Math::sinh));
        FACTORY_MAP.put("sort", Factory.ofStringVarargs(Functions::sort));
        FACTORY_MAP.put("sort_list", Factory.of(List.class, Functions::sort_list));
        FACTORY_MAP.put("split", Factory.of(String.class, String.class, Functions::split));
        FACTORY_MAP.put("sqrt", Factory.of(Math::sqrt));
        FACTORY_MAP.put("substring", Factory.of(String.class, float.class, float.class, Functions::substring, Functions::substring));
        FACTORY_MAP.put("tag", Factory.ofEnv(String.class, Functions::tag));
        FACTORY_MAP.put("tag_regex", Factory.ofEnv(String.class, String.class, Functions::tag_regex, Functions::tag_regex));
        FACTORY_MAP.put("tan", Factory.of(Math::tan));
        FACTORY_MAP.put("tanh", Factory.of(Math::tanh));
        FACTORY_MAP.put("times", Factory.ofNumberVarArgs(1.0, DoubleUnaryOperator.identity(), Functions::times));
        FACTORY_MAP.put("title", Factory.of(String.class, Functions::title));
        FACTORY_MAP.put("to_boolean", Factory.of(String.class, Functions::to_boolean));
        FACTORY_MAP.put("to_byte", Factory.of(String.class, Functions::to_byte));
        FACTORY_MAP.put("to_double", Factory.of(String.class, Functions::to_double));
        FACTORY_MAP.put("to_float", Factory.of(String.class, Functions::to_float));
        FACTORY_MAP.put("to_int", Factory.of(String.class, Functions::to_int));
        FACTORY_MAP.put("to_long", Factory.of(String.class, Functions::to_long));
        FACTORY_MAP.put("to_short", Factory.of(String.class, Functions::to_short));
        FACTORY_MAP.put("tr", Factory.ofStringVarargs(Functions::tr));
        FACTORY_MAP.put("trim", Factory.of(String.class, Functions::trim));
        FACTORY_MAP.put("trim_list", Factory.of(List.class, Functions::trim_list));
        FACTORY_MAP.put("uniq", Factory.ofStringVarargs(Functions::uniq));
        FACTORY_MAP.put("uniq_list", Factory.of(List.class, Functions::uniq_list));
        FACTORY_MAP.put("upper", Factory.of(String.class, Functions::upper));
        FACTORY_MAP.put("waylength", Factory.ofEnv(Functions::waylength));
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
        else if ("inside".equals(name) && args.size() == 1)
            return new IsInsideFunction(args.get(0));
        else if ("random".equals(name))
            return env -> Math.random();

        Factory factory = FACTORY_MAP.get(name);
        if (factory != null) {
            return factory.createExpression(args);
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
     * <p>
     * Separate implementation to support overloading for different argument types.
     * <p>
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
     * {@code Functions#inside} implementation for use in {@link org.openstreetmap.josm.data.validation.tests.MapCSSTagChecker}
     *
     * @see Functions#inside
     */
    public static class IsInsideFunction implements Expression {
        private final Expression arg;

        /**
         * Constructs a new {@code IsInsideFunction}.
         * @param arg argument
         */
        public IsInsideFunction(Expression arg) {
            this.arg = arg;
        }

        /**
         * Returns the argument
         * @return the argument
         */
        public Expression getArg() {
            return arg;
        }

        @Override
        public Object evaluate(Environment env) {
            String codes = Cascade.convertTo(arg.evaluate(env), String.class);
            return Functions.inside(env, codes);
        }
    }
}
