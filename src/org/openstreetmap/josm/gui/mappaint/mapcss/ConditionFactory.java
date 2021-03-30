// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Tagged;
import org.openstreetmap.josm.data.osm.search.SearchCompiler.InDataSourceArea;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Context;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.TagCondition;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.JosmRuntimeException;
import org.openstreetmap.josm.tools.Utils;

/**
 * Factory to generate {@link Condition}s.
 * @since 10837 (Extracted from Condition)
 */
public final class ConditionFactory {

    private ConditionFactory() {
        // Hide default constructor for utils classes
    }

    /**
     * Create a new condition that checks the key and the value of the object.
     * @param k The key.
     * @param v The reference value
     * @param op The operation to use when comparing the value
     * @param context The type of context to use.
     * @param considerValAsKey whether to consider {@code v} as another key and compare the values of key {@code k} and key {@code v}.
     * @return The new condition.
     * @throws MapCSSException if the arguments are incorrect
     */
    public static Condition createKeyValueCondition(String k, String v, Op op, Context context, boolean considerValAsKey) {
        switch (context) {
        case PRIMITIVE:
            if (KeyValueRegexpCondition.SUPPORTED_OPS.contains(op) && !considerValAsKey) {
                try {
                    return new KeyValueRegexpCondition(k, v, op, false);
                } catch (PatternSyntaxException e) {
                    throw new MapCSSException(e);
                }
            }
            if (!considerValAsKey && op == Op.EQ)
                return new SimpleKeyValueCondition(k, v);
            return new KeyValueCondition(k, v, op, considerValAsKey);
        case LINK:
            if (considerValAsKey)
                throw new MapCSSException("''considerValAsKey'' not supported in LINK context");
            if ("role".equalsIgnoreCase(k))
                return new RoleCondition(v, op);
            else if ("index".equalsIgnoreCase(k))
                return new IndexCondition(v, op);
            else
                throw new MapCSSException(
                        MessageFormat.format("Expected key ''role'' or ''index'' in link context. Got ''{0}''.", k));

        default: throw new AssertionError();
        }
    }

    /**
     * Create a condition in which the key and the value need to match a given regexp
     * @param k The key regexp
     * @param v The value regexp
     * @param op The operation to use when comparing the key and the value.
     * @return The new condition.
     */
    public static Condition createRegexpKeyRegexpValueCondition(String k, String v, Op op) {
        return new RegexpKeyValueRegexpCondition(k, v, op);
    }

    /**
     * Creates a condition that checks the given key.
     * @param k The key to test for
     * @param not <code>true</code> to invert the match
     * @param matchType The match type to check for.
     * @param context The context this rule is found in.
     * @return the new condition.
     */
    public static Condition createKeyCondition(String k, boolean not, KeyMatchType matchType, Context context) {
        switch (context) {
        case PRIMITIVE:
            if (KeyMatchType.REGEX == matchType && k.matches("[A-Za-z0-9:_-]+")) {
                // optimization: using String.contains avoids allocating a Matcher
                return new KeyCondition(k, not, KeyMatchType.ANY_CONTAINS);
            } else if (KeyMatchType.REGEX == matchType && k.matches("\\^[A-Za-z0-9:_-]+")) {
                // optimization: using String.startsWith avoids allocating a Matcher
                return new KeyCondition(k.substring(1), not, KeyMatchType.ANY_STARTS_WITH);
            } else if (KeyMatchType.REGEX == matchType && k.matches("[A-Za-z0-9:_-]+\\$")) {
                // optimization: using String.endsWith avoids allocating a Matcher
                return new KeyCondition(k.substring(0, k.length() - 1), not, KeyMatchType.ANY_ENDS_WITH);
            } else if (matchType == KeyMatchType.REGEX) {
                return new KeyRegexpCondition(Pattern.compile(k), not);
            } else {
                return new KeyCondition(k, not, matchType);
            }
        case LINK:
            if (matchType != null)
                throw new MapCSSException("Question mark operator ''?'' and regexp match not supported in LINK context");
            if (not)
                return new RoleCondition(k, Op.NEQ);
            else
                return new RoleCondition(k, Op.EQ);

        default: throw new AssertionError();
        }
    }

    /**
     * Create a new pseudo class condition
     * @param id The id of the pseudo class
     * @param not <code>true</code> to invert the condition
     * @param context The context the class is found in.
     * @return The new condition
     */
    public static PseudoClassCondition createPseudoClassCondition(String id, boolean not, Context context) {
        return PseudoClassCondition.createPseudoClassCondition(id, not, context);
    }

    /**
     * Create a new class condition
     * @param id The id of the class to match
     * @param not <code>true</code> to invert the condition
     * @param context Ignored
     * @return The new condition
     */
    public static ClassCondition createClassCondition(String id, boolean not, Context context) {
        return new ClassCondition(id, not);
    }

    /**
     * Create a new condition that a expression needs to be fulfilled
     * @param e the expression to check
     * @param context Ignored
     * @return The new condition
     */
    public static ExpressionCondition createExpressionCondition(Expression e, Context context) {
        return new ExpressionCondition(e);
    }

    /**
     * This is the operation that {@link KeyValueCondition} uses to match.
     */
    public enum Op {
        /** The value equals the given reference. */
        EQ(Objects::equals),
        /** The value does not equal the reference. */
        NEQ(EQ),
        /** The value is greater than or equal to the given reference value (as float). */
        GREATER_OR_EQUAL(comparisonResult -> comparisonResult >= 0),
        /** The value is greater than the given reference value (as float). */
        GREATER(comparisonResult -> comparisonResult > 0),
        /** The value is less than or equal to the given reference value (as float). */
        LESS_OR_EQUAL(comparisonResult -> comparisonResult <= 0),
        /** The value is less than the given reference value (as float). */
        LESS(comparisonResult -> comparisonResult < 0),
        /** The reference is treated as regular expression and the value needs to match it. */
        REGEX((test, prototype) -> Pattern.compile(prototype).matcher(test).find()),
        /** The reference is treated as regular expression and the value needs to not match it. */
        NREGEX(REGEX),
        /** The reference is treated as a list separated by ';'. Spaces around the ; are ignored.
         *  The value needs to be equal one of the list elements. */
        ONE_OF((test, prototype) -> OsmUtils.splitMultipleValues(test).anyMatch(prototype::equals)),
        /** The value needs to begin with the reference string. */
        BEGINS_WITH(String::startsWith),
        /** The value needs to end with the reference string. */
        ENDS_WITH(String::endsWith),
        /** The value needs to contain the reference string. */
        CONTAINS(String::contains);

        static final Set<Op> NEGATED_OPS = EnumSet.of(NEQ, NREGEX);

        @SuppressWarnings("ImmutableEnumChecker")
        private final BiPredicate<String, String> function;

        private final boolean negated;

        /**
         * Create a new string operation.
         * @param func The function to apply during {@link #eval(String, String)}.
         */
        Op(BiPredicate<String, String> func) {
            this.function = func;
            negated = false;
        }

        /**
         * Create a new float operation that compares two float values
         * @param comparatorResult A function to mapt the result of the comparison
         */
        Op(IntFunction<Boolean> comparatorResult) {
            this.function = (test, prototype) -> {
                float testFloat;
                try {
                    testFloat = Float.parseFloat(test);
                } catch (NumberFormatException e) {
                    return Boolean.FALSE;
                }
                float prototypeFloat = Float.parseFloat(prototype);

                int res = Float.compare(testFloat, prototypeFloat);
                return comparatorResult.apply(res);
            };
            negated = false;
        }

        /**
         * Create a new Op by negating an other op.
         * @param negate inverse operation
         */
        Op(Op negate) {
            this.function = (a, b) -> !negate.function.test(a, b);
            negated = true;
        }

        /**
         * Evaluates a value against a reference string.
         * @param testString The value. May be <code>null</code>
         * @param prototypeString The reference string-
         * @return <code>true</code> if and only if this operation matches for the given value/reference pair.
         */
        public boolean eval(String testString, String prototypeString) {
            if (testString == null)
                return negated;
            else
                return function.test(testString, prototypeString);
        }
    }

    /**
     * Most common case of a KeyValueCondition, this is the basic key=value case.
     *
     * Extra class for performance reasons.
     */
    public static class SimpleKeyValueCondition implements TagCondition {
        /**
         * The key to search for.
         */
        public final String k;
        /**
         * The value to search for.
         */
        public final String v;

        /**
         * Create a new SimpleKeyValueCondition.
         * @param k The key
         * @param v The value.
         */
        public SimpleKeyValueCondition(String k, String v) {
            this.k = k.intern();
            this.v = v.intern();
        }

        @Override
        public boolean applies(Tagged osm) {
            return v.equals(osm.get(k));
        }

        @Override
        public Tag asTag(Tagged primitive) {
            return new Tag(k, v);
        }

        @Override
        public String toString() {
            return '[' + k + '=' + v + ']';
        }

    }

    /**
     * <p>Represents a key/value condition which is either applied to a primitive.</p>
     *
     */
    public static class KeyValueCondition implements TagCondition {
        /**
         * The key to search for.
         */
        public final String k;
        /**
         * The value to search for.
         */
        public final String v;
        /**
         * The key/value match operation.
         */
        public final Op op;
        /**
         * If this flag is set, {@link #v} is treated as a key and the value is the value set for that key.
         */
        public final boolean considerValAsKey;

        /**
         * <p>Creates a key/value-condition.</p>
         *
         * @param k the key
         * @param v the value
         * @param op the operation
         * @param considerValAsKey whether to consider {@code v} as another key and compare the values of key {@code k} and key {@code v}.
         */
        public KeyValueCondition(String k, String v, Op op, boolean considerValAsKey) {
            this.k = k.intern();
            this.v = v.intern();
            this.op = op;
            this.considerValAsKey = considerValAsKey;
        }

        /**
         * Determines if this condition requires an exact key match.
         * @return {@code true} if this condition requires an exact key match.
         * @since 14801
         */
        public boolean requiresExactKeyMatch() {
            return !Op.NEGATED_OPS.contains(op);
        }

        @Override
        public boolean applies(Tagged osm) {
            return op.eval(osm.get(k), considerValAsKey ? osm.get(v) : v);
        }

        @Override
        public Tag asTag(Tagged primitive) {
            return new Tag(k, v);
        }

        @Override
        public String toString() {
            return '[' + k + '\'' + op + '\'' + v + ']';
        }
    }

    /**
     * This condition requires a fixed key to match a given regexp
     */
    public static class KeyValueRegexpCondition extends KeyValueCondition {
        protected static final Set<Op> SUPPORTED_OPS = EnumSet.of(Op.REGEX, Op.NREGEX);

        final Pattern pattern;

        /**
         * Constructs a new {@code KeyValueRegexpCondition}.
         * @param k key
         * @param v value
         * @param op operation
         * @param considerValAsKey must be false
         * @throws PatternSyntaxException if the value syntax is invalid
         */
        public KeyValueRegexpCondition(String k, String v, Op op, boolean considerValAsKey) {
            super(k, v, op, considerValAsKey); /* value is needed in validator messages */
            CheckParameterUtil.ensureThat(!considerValAsKey, "considerValAsKey is not supported");
            CheckParameterUtil.ensureThat(SUPPORTED_OPS.contains(op), "Op must be REGEX or NREGEX");
            this.pattern = Pattern.compile(v);
        }

        protected boolean matches(Tagged osm) {
            final String value = osm.get(k);
            return value != null && pattern.matcher(value).find();
        }

        @Override
        public boolean applies(Tagged osm) {
            if (Op.REGEX == op) {
                return matches(osm);
            } else if (Op.NREGEX == op) {
                return !matches(osm);
            } else {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * A condition that checks that a key with the matching pattern has a value with the matching pattern.
     */
    public static class RegexpKeyValueRegexpCondition extends KeyValueRegexpCondition {

        final Pattern keyPattern;

        /**
         * Create a condition in which the key and the value need to match a given regexp
         * @param k The key regexp
         * @param v The value regexp
         * @param op The operation to use when comparing the key and the value.
         */
        public RegexpKeyValueRegexpCondition(String k, String v, Op op) {
            super(k, v, op, false);
            this.keyPattern = Pattern.compile(k);
        }

        @Override
        public boolean requiresExactKeyMatch() {
            return false;
        }

        @Override
        protected boolean matches(Tagged osm) {
            return osm.getKeys().entrySet().stream()
                    .anyMatch(kv -> keyPattern.matcher(kv.getKey()).find() && pattern.matcher(kv.getValue()).find());
        }
    }

    /**
     * Role condition.
     */
    public static class RoleCondition implements Condition {
        final String role;
        final Op op;

        /**
         * Constructs a new {@code RoleCondition}.
         * @param role role
         * @param op operation
         */
        public RoleCondition(String role, Op op) {
            this.role = role;
            this.op = op;
        }

        @Override
        public boolean applies(Environment env) {
            String testRole = env.getRole();
            if (testRole == null) return false;
            return op.eval(testRole, role);
        }
    }

    /**
     * Index condition.
     */
    public static class IndexCondition implements Condition {
        final String index;
        final Op op;
        final boolean isFirstOrLast;

        /**
         * Constructs a new {@code IndexCondition}.
         * @param index index
         * @param op operation
         */
        public IndexCondition(String index, Op op) {
            this.index = index;
            this.op = op;
            isFirstOrLast = op == Op.EQ && ("1".equals(index) || "-1".equals(index));
        }

        @Override
        public boolean applies(Environment e) {
            if (!(e instanceof Environment.LinkEnvironment)) return false;
            Environment.LinkEnvironment env = (Environment.LinkEnvironment) e;
            if (env.index < 0) return false;
            if (index.startsWith("-")) {
                return env.count >= 0 && op.eval(Integer.toString(env.index - env.count), index);
            } else {
                return op.eval(Integer.toString(env.index + 1), index);
            }
        }
    }

    /**
     * This defines how {@link KeyCondition} matches a given key.
     */
    public enum KeyMatchType {
        /**
         * The key needs to be equal to the given label.
         */
        EQ,
        /**
         * The key needs to have a true value (yes, ...)
         * @see OsmUtils#isTrue(String)
         */
        TRUE,
        /**
         * The key needs to have a false value (no, ...)
         * @see OsmUtils#isFalse(String)
         */
        FALSE,
        /**
         * The key needs to match the given regular expression.
         */
        REGEX,
        /**
         * The key needs to contain the given label as substring.
         */
        ANY_CONTAINS,
        /**
         * The key needs to start with the given label.
         */
        ANY_STARTS_WITH,
        /**
         * The key needs to end with the given label.
         */
        ANY_ENDS_WITH,
    }

    /**
     * <p>KeyCondition represent one of the following conditions in either the link or the
     * primitive context:</p>
     * <pre>
     *     ["a label"]  PRIMITIVE:   the primitive has a tag "a label"
     *                  LINK:        the parent is a relation and it has at least one member with the role
     *                               "a label" referring to the child
     *
     *     [!"a label"]  PRIMITIVE:  the primitive doesn't have a tag "a label"
     *                   LINK:       the parent is a relation but doesn't have a member with the role
     *                               "a label" referring to the child
     *
     *     ["a label"?]  PRIMITIVE:  the primitive has a tag "a label" whose value evaluates to a true-value
     *                   LINK:       not supported
     *
     *     ["a label"?!] PRIMITIVE:  the primitive has a tag "a label" whose value evaluates to a false-value
     *                   LINK:       not supported
     * </pre>
     * @see KeyRegexpCondition
     */
    public static class KeyCondition implements TagCondition {

        /**
         * The key name.
         */
        public final String label;
        /**
         * If we should negate the result of the match.
         */
        public final boolean negateResult;
        /**
         * Describes how to match the label against the key.
         * @see KeyMatchType
         */
        public final KeyMatchType matchType;

        /**
         * Creates a new KeyCondition
         * @param label The key name (or regexp) to use.
         * @param negateResult If we should negate the result.,
         * @param matchType The match type.
         */
        public KeyCondition(String label, boolean negateResult, KeyMatchType matchType) {
            CheckParameterUtil.ensureThat(matchType != KeyMatchType.REGEX, "Use KeyPatternCondition");
            this.label = label;
            this.negateResult = negateResult;
            this.matchType = matchType == null ? KeyMatchType.EQ : matchType;
        }

        @Override
        public boolean applies(Tagged osm) {
            switch (matchType) {
                case TRUE:
                    return osm.isKeyTrue(label) ^ negateResult;
                case FALSE:
                    return osm.isKeyFalse(label) ^ negateResult;
                case ANY_CONTAINS:
                case ANY_STARTS_WITH:
                case ANY_ENDS_WITH:
                    return osm.keys().anyMatch(keyPredicate()) ^ negateResult;
                default:
                    return osm.hasKey(label) ^ negateResult;
            }
        }

        private Predicate<String> keyPredicate() {
            switch (matchType) {
                case ANY_CONTAINS:
                    return key -> key.contains(label);
                case ANY_STARTS_WITH:
                    return key -> key.startsWith(label);
                case ANY_ENDS_WITH:
                    return key -> key.endsWith(label);
                default:
                    return null;
            }
        }

        /**
         * Get the matched key and the corresponding value.
         * <p>
         * WARNING: This ignores {@link #negateResult}.
         * @param p The primitive to get the value from.
         * @return The tag.
         */
        @Override
        public Tag asTag(Tagged p) {
            String key = label;
            Predicate<String> keyPredicate = keyPredicate();
            if (keyPredicate != null) {
                key = p.keys().filter(keyPredicate).findAny().orElse(key);
            }
            return new Tag(key, p.get(key));
        }

        @Override
        public String toString() {
            return '[' + (negateResult ? "!" : "") + label + ']';
        }
    }

    /**
     * KeyPatternCondition represents a conditions matching keys based on a pattern.
     */
    public static class KeyRegexpCondition implements TagCondition {

        /**
         * A predicate used to match a the regexp against the key. Only used if the match type is regexp.
         */
        public final Pattern pattern;
        /**
         * If we should negate the result of the match.
         */
        public final boolean negateResult;

        /**
         * Creates a new KeyPatternCondition
         * @param pattern The regular expression for matching keys.
         * @param negateResult If we should negate the result.
         */
        public KeyRegexpCondition(Pattern pattern, boolean negateResult) {
            this.negateResult = negateResult;
            this.pattern = pattern;
        }

        @Override
        public boolean applies(Tagged osm) {
            return osm.keys().anyMatch(pattern.asPredicate()) ^ negateResult;
        }

        /**
         * Get the matched key and the corresponding value.
         * <p>
         * WARNING: This ignores {@link #negateResult}.
         * <p>
         * WARNING: For regexp, the regular expression is returned instead of a key if the match failed.
         * @param p The primitive to get the value from.
         * @return The tag.
         */
        @Override
        public Tag asTag(Tagged p) {
            String key = p.keys().filter(pattern.asPredicate()).findAny().orElse(pattern.pattern());
            return new Tag(key, p.get(key));
        }

        @Override
        public String toString() {
            return '[' + (negateResult ? "!" : "") + pattern + ']';
        }
    }

    /**
     * Class condition.
     */
    public static class ClassCondition implements Condition {

        /** Class identifier */
        public final String id;
        final boolean not;

        /**
         * Constructs a new {@code ClassCondition}.
         * @param id id
         * @param not negation or not
         */
        public ClassCondition(String id, boolean not) {
            this.id = id;
            this.not = not;
        }

        @Override
        public boolean applies(Environment env) {
            Cascade cascade = env.getCascade(env.layer);
            return cascade != null && (not ^ cascade.containsKey(id));
        }

        @Override
        public String toString() {
            return (not ? "!" : "") + '.' + id;
        }
    }

    /**
     * Like <a href="http://www.w3.org/TR/css3-selectors/#pseudo-classes">CSS pseudo classes</a>, MapCSS pseudo classes
     * are written in lower case with dashes between words.
     */
    public static final class PseudoClasses {

        private PseudoClasses() {
            // Hide default constructor for utilities classes
        }

        /**
         * {@code closed} tests whether the way is closed or the relation is a closed multipolygon
         * @param e MapCSS environment
         * @return {@code true} if the way is closed or the relation is a closed multipolygon
         */
        static boolean closed(Environment e) { // NO_UCD (unused code)
            if (e.osm instanceof IWay<?> && ((IWay<?>) e.osm).isClosed())
                return true;
            return e.osm instanceof IRelation<?> && ((IRelation<?>) e.osm).isMultipolygon();
        }

        /**
         * {@code :modified} tests whether the object has been modified.
         * @param e MapCSS environment
         * @return {@code true} if the object has been modified
         * @see IPrimitive#isModified()
         */
        static boolean modified(Environment e) { // NO_UCD (unused code)
            return e.osm.isModified() || e.osm.isNewOrUndeleted();
        }

        /**
         * {@code ;new} tests whether the object is new.
         * @param e MapCSS environment
         * @return {@code true} if the object is new
         * @see IPrimitive#isNew()
         */
        static boolean _new(Environment e) { // NO_UCD (unused code)
            return e.osm.isNew();
        }

        /**
         * {@code :connection} tests whether the object is a connection node.
         * @param e MapCSS environment
         * @return {@code true} if the object is a connection node
         * @see Node#isConnectionNode()
         */
        static boolean connection(Environment e) { // NO_UCD (unused code)
            return e.osm instanceof INode && e.osm.getDataSet() != null && ((INode) e.osm).isConnectionNode();
        }

        /**
         * {@code :tagged} tests whether the object is tagged.
         * @param e MapCSS environment
         * @return {@code true} if the object is tagged
         * @see IPrimitive#isTagged()
         */
        static boolean tagged(Environment e) { // NO_UCD (unused code)
            return e.osm.isTagged();
        }

        /**
         * {@code :same-tags} tests whether the object has the same tags as its child/parent.
         * @param e MapCSS environment
         * @return {@code true} if the object has the same tags as its child/parent
         * @see IPrimitive#hasSameInterestingTags(IPrimitive)
         */
        static boolean sameTags(Environment e) { // NO_UCD (unused code)
            if (!(e instanceof Environment.LinkEnvironment)) return false;
            Environment.LinkEnvironment env = (Environment.LinkEnvironment) e;
            IPrimitive other = Utils.firstNonNull(env.child, env.parent);
            return e.osm.hasSameInterestingTags(other);
        }

        /**
         * {@code :area-style} tests whether the object has an area style. This is useful for validators.
         * @param e MapCSS environment
         * @return {@code true} if the object has an area style
         * @see ElemStyles#hasAreaElemStyle(IPrimitive, boolean)
         */
        static boolean areaStyle(Environment e) { // NO_UCD (unused code)
            // only for validator
            return ElemStyles.hasAreaElemStyle(e.osm, false);
        }

        /**
         * {@code unconnected}: tests whether the object is a unconnected node.
         * @param e MapCSS environment
         * @return {@code true} if the object is a unconnected node
         */
        static boolean unconnected(Environment e) { // NO_UCD (unused code)
            return e.osm instanceof Node && ((Node) e.osm).getParentWays().isEmpty();
        }

        /**
         * {@code righthandtraffic} checks if there is right-hand traffic at the current location.
         * @param e MapCSS environment
         * @return {@code true} if there is right-hand traffic at the current location
         * @see Functions#is_right_hand_traffic(Environment)
         */
        static boolean righthandtraffic(Environment e) { // NO_UCD (unused code)
            return Functions.is_right_hand_traffic(e);
        }

        /**
         * {@code clockwise} whether the way is closed and oriented clockwise,
         * or non-closed and the 1st, 2nd and last node are in clockwise order.
         * @param e MapCSS environment
         * @return {@code true} if the way clockwise
         * @see Functions#is_clockwise(Environment)
         */
        static boolean clockwise(Environment e) { // NO_UCD (unused code)
            return Functions.is_clockwise(e);
        }

        /**
         * {@code anticlockwise} whether the way is closed and oriented anticlockwise,
         * or non-closed and the 1st, 2nd and last node are in anticlockwise order.
         * @param e MapCSS environment
         * @return {@code true} if the way clockwise
         * @see Functions#is_anticlockwise(Environment)
         */
        static boolean anticlockwise(Environment e) { // NO_UCD (unused code)
            return Functions.is_anticlockwise(e);
        }

        /**
         * {@code unclosed-multipolygon} tests whether the object is an unclosed multipolygon.
         * @param e MapCSS environment
         * @return {@code true} if the object is an unclosed multipolygon
         */
        static boolean unclosed_multipolygon(Environment e) { // NO_UCD (unused code)
            return e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon() &&
                    !e.osm.isIncomplete() && !((Relation) e.osm).hasIncompleteMembers() &&
                    !MultipolygonCache.getInstance().get((Relation) e.osm).getOpenEnds().isEmpty();
        }

        private static final Predicate<OsmPrimitive> IN_DOWNLOADED_AREA = new InDataSourceArea(false);

        /**
         * {@code in-downloaded-area} tests whether the object is within source area ("downloaded area").
         * @param e MapCSS environment
         * @return {@code true} if the object is within source area ("downloaded area")
         * @see InDataSourceArea
         */
        static boolean inDownloadedArea(Environment e) { // NO_UCD (unused code)
            return e.osm instanceof OsmPrimitive && IN_DOWNLOADED_AREA.test((OsmPrimitive) e.osm);
        }

        static boolean completely_downloaded(Environment e) { // NO_UCD (unused code)
            if (e.osm instanceof IRelation<?>) {
                return !((IRelation<?>) e.osm).hasIncompleteMembers();
            } else {
                return true;
            }
        }

        static boolean closed2(Environment e) { // NO_UCD (unused code)
            if (e.osm instanceof IWay<?> && ((IWay<?>) e.osm).isClosed())
                return true;
            if (e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon()) {
                Multipolygon multipolygon = MultipolygonCache.getInstance().get((Relation) e.osm);
                return multipolygon != null && multipolygon.getOpenEnds().isEmpty();
            }
            return false;
        }

        static boolean selected(Environment e) { // NO_UCD (unused code)
            if (e.mc != null) {
                e.mc.getCascade(e.layer).setDefaultSelectedHandling(false);
            }
            return e.osm.isSelected();
        }
    }

    /**
     * Pseudo class condition.
     */
    public static class PseudoClassCondition implements Condition {

        final Method method;
        final boolean not;

        protected PseudoClassCondition(Method method, boolean not) {
            this.method = method;
            this.not = not;
        }

        /**
         * Create a new pseudo class condition
         * @param id The id of the pseudo class
         * @param not <code>true</code> to invert the condition
         * @param context The context the class is found in.
         * @return The new condition
         */
        public static PseudoClassCondition createPseudoClassCondition(String id, boolean not, Context context) {
            CheckParameterUtil.ensureThat(!"sameTags".equals(id) || Context.LINK == context, "sameTags only supported in LINK context");
            if ("open_end".equals(id)) {
                return new OpenEndPseudoClassCondition(not);
            }
            final Method method = getMethod(id);
            if (method != null) {
                return new PseudoClassCondition(method, not);
            }
            throw new MapCSSException("Invalid pseudo class specified: " + id);
        }

        protected static Method getMethod(String id) {
            String cleanId = clean(id);
            return Arrays.stream(PseudoClasses.class.getDeclaredMethods())
                    .filter(method -> clean(method.getName()).equalsIgnoreCase(cleanId))
                    .findFirst().orElse(null);
        }

        private static String clean(String id) {
            // for backwards compatibility, consider :sameTags == :same-tags == :same_tags (#11150)
            return id.replaceAll("[-_]", "");
        }

        @Override
        public boolean applies(Environment e) {
            try {
                return not ^ (Boolean) method.invoke(null, e);
            } catch (ReflectiveOperationException ex) {
                throw new JosmRuntimeException(ex);
            }
        }

        @Override
        public String toString() {
            return (not ? "!" : "") + ':' + method.getName();
        }
    }

    /**
     * Open end pseudo class condition.
     */
    public static class OpenEndPseudoClassCondition extends PseudoClassCondition {
        /**
         * Constructs a new {@code OpenEndPseudoClassCondition}.
         * @param not negation or not
         */
        public OpenEndPseudoClassCondition(boolean not) {
            super(null, not);
        }

        @Override
        public boolean applies(Environment e) {
            return true;
        }
    }

    /**
     * A condition that is fulfilled whenever the expression is evaluated to be true.
     */
    public static class ExpressionCondition implements Condition {

        final Expression e;

        /**
         * Constructs a new {@code ExpressionFactory}
         * @param e expression
         */
        public ExpressionCondition(Expression e) {
            this.e = e;
        }

        /**
         * Returns the expression.
         * @return the expression
         * @since 14484
         */
        public final Expression getExpression() {
            return e;
        }

        @Override
        public boolean applies(Environment env) {
            Boolean b = Cascade.convertTo(e.evaluate(env), Boolean.class);
            return b != null && b;
        }

        @Override
        public String toString() {
            return '[' + e.toString() + ']';
        }
    }
}
