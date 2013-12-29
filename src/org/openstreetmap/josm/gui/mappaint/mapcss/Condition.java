// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Tag;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Predicates;
import org.openstreetmap.josm.tools.Utils;

abstract public class Condition {

    abstract public boolean applies(Environment e);

    public static Condition createKeyValueCondition(String k, String v, Op op, Context context, boolean considerValAsKey) {
        switch (context) {
        case PRIMITIVE:
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

    public static Condition createPseudoClassCondition(String id, boolean not, Context context) {
        return new PseudoClassCondition(id, not);
    }

    public static Condition createClassCondition(String id, boolean not, Context context) {
        return new ClassCondition(id, not);
    }

    public static Condition createExpressionCondition(Expression e, Context context) {
        return new ExpressionCondition(e);
    }

    public static enum Op {
        EQ, NEQ, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL, LESS,
        REGEX, NREGEX, ONE_OF, BEGINS_WITH, ENDS_WITH, CONTAINS;

        public boolean eval(String testString, String prototypeString) {
            if (testString == null && this != NEQ)
                return false;
            switch (this) {
            case EQ:
                return equal(testString, prototypeString);
            case NEQ:
                return !equal(testString, prototypeString);
            case REGEX:
            case NREGEX:
                final boolean contains = Pattern.compile(prototypeString).matcher(testString).find();
                return REGEX.equals(this) ? contains : !contains;
            case ONE_OF:
                String[] parts = testString.split(";");
                for (String part : parts) {
                    if (equal(prototypeString, part.trim()))
                        return true;
                }
                return false;
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
     * context, where the condition applies
     */
    public static enum Context {
        /**
         * normal primitive selector, e.g. way[highway=residential]
         */
        PRIMITIVE,

        /**
         * link between primitives, e.g. relation >[role=outer] way
         */
        LINK
    }

    public final static EnumSet<Op> COMPARISON_OPERATERS =
        EnumSet.of(Op.GREATER_OR_EQUAL, Op.GREATER, Op.LESS_OR_EQUAL, Op.LESS);

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
            return op.eval(Integer.toString(env.index + 1), index);
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

        public KeyCondition(String label, boolean negateResult, KeyMatchType matchType){
            this.label = label;
            this.negateResult = negateResult;
            this.matchType = matchType;
        }

        @Override
        public boolean applies(Environment e) {
            switch(e.getContext()) {
            case PRIMITIVE:
                if (KeyMatchType.TRUE.equals(matchType))
                    return OsmUtils.isTrue(e.osm.get(label)) ^ negateResult;
                else if (KeyMatchType.FALSE.equals(matchType))
                    return OsmUtils.isFalse(e.osm.get(label)) ^ negateResult;
                else if (KeyMatchType.REGEX.equals(matchType))
                    return Utils.exists(e.osm.keySet(), Predicates.stringContainsPattern(Pattern.compile(label))) ^ negateResult;
                else
                    return e.osm.hasKey(label) ^ negateResult;
            case LINK:
                Utils.ensure(false, "Illegal state: KeyCondition not supported in LINK context");
                return false;
            default: throw new AssertionError();
            }
        }

        public Tag asTag() {
            return new Tag(label);
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
            return not ^ env.mc.getCascade(env.layer).containsKey(id);
        }

        @Override
        public String toString() {
            return (not ? "!" : "") + "." + id;
        }
    }

    public static class PseudoClassCondition extends Condition {

        public final String id;
        public final boolean not;

        public PseudoClassCondition(String id, boolean not) {
            this.id = id;
            this.not = not;
        }

        @Override
        public boolean applies(Environment e) {
            return not ^ appliesImpl(e);
        }

        public boolean appliesImpl(Environment e) {
            if (equal(id, "closed")) {
                if (e.osm instanceof Way && ((Way) e.osm).isClosed())
                    return true;
                if (e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon())
                    return true;
                return false;
            } else if (equal(id, "modified"))
                return e.osm.isModified() || e.osm.isNewOrUndeleted();
            else if (equal(id, "new"))
                return e.osm.isNew();
            else if (equal(id, "connection") && (e.osm instanceof Node))
                return ((Node) e.osm).isConnectionNode();
            else if (equal(id, "tagged"))
                return e.osm.isTagged();
            return true;
        }

        @Override
        public String toString() {
            return ":" + (not ? "!" : "") + id;
        }
    }

    public static class ExpressionCondition extends Condition {

        private Expression e;

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
