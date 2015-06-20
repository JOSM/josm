// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.search.SearchCompiler;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.MultipolygonCache;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.ElemStyles;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.CheckParameterUtil;
import org.openstreetmap.josm.tools.Predicate;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.Utils;

public abstract class Condition {

    public abstract boolean applies(Environment e);

    public static Condition createKeyValueCondition(String k, String v, Op op, Context context, boolean considerValAsKey) {
        switch (context) {
        case PRIMITIVE:
            if (KeyValueRegexpCondition.SUPPORTED_OPS.contains(op) && !considerValAsKey)
                return new KeyValueRegexpCondition(k, v, op, false);
            if (!considerValAsKey && op.equals(Op.EQ))
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

    public static Condition createKeyCondition(String k, boolean not, KeyMatchType matchType, Context context) {
        switch (context) {
        case PRIMITIVE:
            return new KeyCondition(k, not, matchType);
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

    public static PseudoClassCondition createPseudoClassCondition(String id, boolean not, Context context) {
        return PseudoClassCondition.createPseudoClassCondition(id, not, context);
    }

    public static ClassCondition createClassCondition(String id, boolean not, Context context) {
        return new ClassCondition(id, not);
    }

    public static ExpressionCondition createExpressionCondition(Expression e, Context context) {
        return new ExpressionCondition(e);
    }

    public static enum Op {
        EQ, NEQ, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL, LESS,
        REGEX, NREGEX, ONE_OF, BEGINS_WITH, ENDS_WITH, CONTAINS;

        public static final Set<Op> NEGATED_OPS = EnumSet.of(NEQ, NREGEX);

        public boolean eval(String testString, String prototypeString) {
            if (testString == null && !NEGATED_OPS.contains(this))
                return false;
            switch (this) {
            case EQ:
                return Objects.equals(testString, prototypeString);
            case NEQ:
                return !Objects.equals(testString, prototypeString);
            case REGEX:
            case NREGEX:
                final boolean contains = Pattern.compile(prototypeString).matcher(testString).find();
                return REGEX.equals(this) ? contains : !contains;
            case ONE_OF:
                return Arrays.asList(testString.split("\\s*;\\s*")).contains(prototypeString);
            case BEGINS_WITH:
                return testString.startsWith(prototypeString);
            case ENDS_WITH:
                return testString.endsWith(prototypeString);
            case CONTAINS:
                return testString.contains(prototypeString);
            }

            float test_float;
            try {
                test_float = Float.parseFloat(testString);
            } catch (NumberFormatException e) {
                return false;
            }
            float prototype_float = Float.parseFloat(prototypeString);

            switch (this) {
            case GREATER_OR_EQUAL:
                return test_float >= prototype_float;
            case GREATER:
                return test_float > prototype_float;
            case LESS_OR_EQUAL:
                return test_float <= prototype_float;
            case LESS:
                return test_float < prototype_float;
            default:
                throw new AssertionError();
            }
        }
    }

    /**
     * Context, where the condition applies.
     */
    public static enum Context {
        /**
         * normal primitive selector, e.g. way[highway=residential]
         */
        PRIMITIVE,

        /**
         * link between primitives, e.g. relation &gt;[role=outer] way
         */
        LINK
    }

    /**
     * Most common case of a KeyValueCondition.
     *
     * Extra class for performance reasons.
     */
    public static class SimpleKeyValueCondition extends Condition {
        public final String k;
        public final String v;

        public SimpleKeyValueCondition(String k, String v) {
            this.k = k;
            this.v = v;
        }

        @Override
        public boolean applies(Environment e) {
            return v.equals(e.osm.get(k));
        }

        public Tag asTag() {
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
    public static class KeyValueCondition extends Condition {

        public final String k;
        public final String v;
        public final Op op;
        public boolean considerValAsKey;

        /**
         * <p>Creates a key/value-condition.</p>
         *
         * @param k the key
         * @param v the value
         * @param op the operation
         * @param considerValAsKey whether to consider {@code v} as another key and compare the values of key {@code k} and key {@code v}.
         */
        public KeyValueCondition(String k, String v, Op op, boolean considerValAsKey) {
            this.k = k;
            this.v = v;
            this.op = op;
            this.considerValAsKey = considerValAsKey;
        }

        @Override
        public boolean applies(Environment env) {
            return op.eval(env.osm.get(k), considerValAsKey ? env.osm.get(v) : v);
        }

        public Tag asTag() {
            return new Tag(k, v);
        }

        @Override
        public String toString() {
            return "[" + k + "'" + op + "'" + v + "]";
        }
    }

    public static class KeyValueRegexpCondition extends KeyValueCondition {

        public final Pattern pattern;
        public static final Set<Op> SUPPORTED_OPS = EnumSet.of(Op.REGEX, Op.NREGEX);

        public KeyValueRegexpCondition(String k, String v, Op op, boolean considerValAsKey) {
            super(k, v, op, considerValAsKey);
            CheckParameterUtil.ensureThat(!considerValAsKey, "considerValAsKey is not supported");
            CheckParameterUtil.ensureThat(SUPPORTED_OPS.contains(op), "Op must be REGEX or NREGEX");
            this.pattern = Pattern.compile(v);
        }

        @Override
        public boolean applies(Environment env) {
            final String value = env.osm.get(k);
            if (Op.REGEX.equals(op)) {
                return value != null && pattern.matcher(value).find();
            } else if (Op.NREGEX.equals(op)) {
                return value == null || !pattern.matcher(value).find();
            } else {
                throw new IllegalStateException();
            }
        }
    }

    public static class RoleCondition extends Condition {
        public final String role;
        public final Op op;

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

    public static class IndexCondition extends Condition {
        public final String index;
        public final Op op;

        public IndexCondition(String index, Op op) {
            this.index = index;
            this.op = op;
        }

        @Override
        public boolean applies(Environment env) {
            if (env.index == null) return false;
            if (index.startsWith("-")) {
                return env.count != null && op.eval(Integer.toString(env.index - env.count), index);
            } else {
                return op.eval(Integer.toString(env.index + 1), index);
            }
        }
    }

    public static enum KeyMatchType {
        EQ, TRUE, FALSE, REGEX
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
     */
    public static class KeyCondition extends Condition {

        public final String label;
        public final boolean negateResult;
        public final KeyMatchType matchType;
        public Predicate<String> containsPattern;

        public KeyCondition(String label, boolean negateResult, KeyMatchType matchType) {
            this.label = label;
            this.negateResult = negateResult;
            this.matchType = matchType;
            this.containsPattern = KeyMatchType.REGEX.equals(matchType)
                    ? Predicates.stringContainsPattern(Pattern.compile(label))
                    : null;
        }

        @Override
        public boolean applies(Environment e) {
            switch(e.getContext()) {
            case PRIMITIVE:
                if (KeyMatchType.TRUE.equals(matchType))
                    return e.osm.isKeyTrue(label) ^ negateResult;
                else if (KeyMatchType.FALSE.equals(matchType))
                    return e.osm.isKeyFalse(label) ^ negateResult;
                else if (KeyMatchType.REGEX.equals(matchType)) {
                    return Utils.exists(e.osm.keySet(), containsPattern) ^ negateResult;
                } else {
                    return e.osm.hasKey(label) ^ negateResult;
                }
            case LINK:
                Utils.ensure(false, "Illegal state: KeyCondition not supported in LINK context");
                return false;
            default: throw new AssertionError();
            }
        }

        public Tag asTag(OsmPrimitive p) {
            String key = label;
            if (KeyMatchType.REGEX.equals(matchType)) {
                final Collection<String> matchingKeys = Utils.filter(p.keySet(), containsPattern);
                if (!matchingKeys.isEmpty()) {
                    key = matchingKeys.iterator().next();
                }
            }
            return new Tag(key, p.get(key));
        }

        @Override
        public String toString() {
            return "[" + (negateResult ? "!" : "") + label + "]";
        }
    }

    public static class ClassCondition extends Condition {

        public final String id;
        public final boolean not;

        public ClassCondition(String id, boolean not) {
            this.id = id;
            this.not = not;
        }

        @Override
        public boolean applies(Environment env) {
            return env != null && env.getCascade(env.layer) != null && not ^ env.getCascade(env.layer).containsKey(id);
        }

        @Override
        public String toString() {
            return (not ? "!" : "") + "." + id;
        }
    }

    /**
     * Like <a href="http://www.w3.org/TR/css3-selectors/#pseudo-classes">CSS pseudo classes</a>, MapCSS pseudo classes
     * are written in lower case with dashes between words.
     */
    static class PseudoClasses {

        /**
         * {@code closed} tests whether the way is closed or the relation is a closed multipolygon
         */
        static boolean closed(Environment e) {
            if (e.osm instanceof Way && ((Way) e.osm).isClosed())
                return true;
            if (e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon())
                return true;
            return false;
        }

        /**
         * {@code :modified} tests whether the object has been modified.
         * @see OsmPrimitive#isModified() ()
         */
        static boolean modified(Environment e) {
            return e.osm.isModified() || e.osm.isNewOrUndeleted();
        }

        /**
         * {@code ;new} tests whether the object is new.
         * @see OsmPrimitive#isNew()
         */
        static boolean _new(Environment e) {
            return e.osm.isNew();
        }

        /**
         * {@code :connection} tests whether the object is a connection node.
         * @see Node#isConnectionNode()
         */
        static boolean connection(Environment e) {
            return e.osm instanceof Node && ((Node) e.osm).isConnectionNode();
        }

        /**
         * {@code :tagged} tests whether the object is tagged.
         * @see OsmPrimitive#isTagged()
         */
        static boolean tagged(Environment e) {
            return e.osm.isTagged();
        }

        /**
         * {@code :same-tags} tests whether the object has the same tags as its child/parent.
         * @see OsmPrimitive#hasSameInterestingTags(OsmPrimitive)
         */
        static boolean sameTags(Environment e) {
            return e.osm.hasSameInterestingTags(Utils.firstNonNull(e.child, e.parent));
        }

        /**
         * {@code :area-style} tests whether the object has an area style. This is useful for validators.
         * @see ElemStyles#hasAreaElemStyle(OsmPrimitive, boolean)
         */
        static boolean areaStyle(Environment e) {
            // only for validator
            return ElemStyles.hasAreaElemStyle(e.osm, false);
        }

        /**
         * {@code unconnected}: tests whether the object is a unconnected node.
         */
        static boolean unconnected(Environment e) {
            return e.osm instanceof Node && OsmPrimitive.getFilteredList(e.osm.getReferrers(), Way.class).isEmpty();
        }

        /**
         * {@code righthandtraffic} checks if there is right-hand traffic at the current location.
         * @see ExpressionFactory.Functions#is_right_hand_traffic(Environment)
         */
        static boolean righthandtraffic(Environment e) {
            return ExpressionFactory.Functions.is_right_hand_traffic(e);
        }

        /**
         * {@code unclosed-multipolygon} tests whether the object is an unclosed multipolygon.
         */
        static boolean unclosed_multipolygon(Environment e) {
            return e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon() &&
                    !e.osm.isIncomplete() && !((Relation) e.osm).hasIncompleteMembers() &&
                    !MultipolygonCache.getInstance().get(Main.map.mapView, (Relation) e.osm).getOpenEnds().isEmpty();
        }

        private static final Predicate<OsmPrimitive> IN_DOWNLOADED_AREA = new SearchCompiler.InDataSourceArea(false);

        /**
         * {@code in-downloaded-area} tests whether the object is within source area ("downloaded area").
         * @see SearchCompiler.InDataSourceArea
         */
        static boolean inDownloadedArea(Environment e) {
            return IN_DOWNLOADED_AREA.evaluate(e.osm);
        }

    }

    public static class PseudoClassCondition extends Condition {

        public final Method method;
        public final boolean not;

        private PseudoClassCondition(Method method, boolean not) {
            this.method = method;
            this.not = not;
        }

        public static PseudoClassCondition createPseudoClassCondition(String id, boolean not, Context context) {
            CheckParameterUtil.ensureThat(!"sameTags".equals(id) || Context.LINK.equals(context), "sameTags only supported in LINK context");
            if ("open_end".equals(id)) {
                return new OpenEndPseudoClassCondition(not);
            }
            final Method method = getMethod(id);
            if (method != null) {
                return new PseudoClassCondition(method, not);
            }
            throw new IllegalArgumentException("Invalid pseudo class specified: " + id);

        }

        protected static Method getMethod(String id) {
            id = id.replaceAll("-|_", "");
            for (Method method : PseudoClasses.class.getDeclaredMethods()) {
                // for backwards compatibility, consider :sameTags == :same-tags == :same_tags (#11150)
                final String methodName = method.getName().replaceAll("-|_", "");
                if (methodName.equalsIgnoreCase(id)) {
                    return method;
                }
            }
            return null;
        }

        @Override
        public boolean applies(Environment e) {
            try {
                return not ^ (Boolean) method.invoke(null, e);
            } catch (IllegalAccessException | InvocationTargetException ex) {
                throw new RuntimeException(ex);
            }
        }

        @Override
        public String toString() {
            return (not ? "!" : "") + ":" + method.getName();
        }
    }

    public static class OpenEndPseudoClassCondition extends PseudoClassCondition {
        public OpenEndPseudoClassCondition(boolean not) {
            super(null, not);
        }

        @Override
        public boolean applies(Environment e) {
            return true;
        }
    }

    public static class ExpressionCondition extends Condition {

        private final Expression e;

        public ExpressionCondition(Expression e) {
            this.e = e;
        }

        @Override
        public boolean applies(Environment env) {
            Boolean b = Cascade.convertTo(e.evaluate(env), Boolean.class);
            return b != null && b;
        }

        @Override
        public String toString() {
            return "[" + e + "]";
        }
    }
}
