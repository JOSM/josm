// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.text.MessageFormat;
import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Cascade;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

abstract public class Condition {

    abstract public boolean applies(Environment e);

    public static Condition create(String k, String v, Op op, Context context) {
        switch (context) {
        case PRIMITIVE:
            return new KeyValueCondition(k, v, op);
        case LINK:
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

    public static Condition create(String k, boolean not, boolean yes, Context context) {
        switch (context) {
        case PRIMITIVE:
            return new KeyCondition(k, not, yes);
        case LINK:
            if (yes)
                throw new MapCSSException("Question mark operator ''?'' not supported in LINK context");
            if (not)
                return new RoleCondition(k, Op.NEQ);
            else
                return new RoleCondition(k, Op.EQ);

        default: throw new AssertionError();
        }
    }

    public static Condition create(String id, boolean not, Context context) {
        return new PseudoClassCondition(id, not);
    }

    public static Condition create(Expression e, Context context) {
        return new ExpressionCondition(e);
    }

    public static enum Op {
        EQ, NEQ, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL, LESS,
        REGEX, ONE_OF, BEGINS_WITH, ENDS_WITH, CONTAINS;

        public boolean eval(String testString, String prototypeString) {
            if (testString == null && this != NEQ)
                return false;
            switch (this) {
            case EQ:
                return equal(testString, prototypeString);
            case NEQ:
                return !equal(testString, prototypeString);
            case REGEX:
                Pattern p = Pattern.compile(prototypeString);
                Matcher m = p.matcher(testString);
                return m.find();
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

        public String k;
        public String v;
        public Op op;

        /**
         * <p>Creates a key/value-condition.</p>
         * 
         * @param k the key
         * @param v the value
         * @param op the operation
         */
        public KeyValueCondition(String k, String v, Op op) {
            this.k = k;
            this.v = v;
            this.op = op;
        }

        @Override
        public boolean applies(Environment env) {
            return op.eval(env.osm.get(k), v);
        }

        @Override
        public String toString() {
            return "[" + k + "'" + op + "'" + v + "]";
        }
    }

    public static class RoleCondition extends Condition {
        public String role;
        public Op op;

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
        public String index;
        public Op op;

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
     * </pre>
     */
    public static class KeyCondition extends Condition {

        private String label;
        private boolean exclamationMarkPresent;
        private boolean questionMarkPresent;

        /**
         * 
         * @param label
         * @param exclamationMarkPresent
         * @param questionMarkPresent
         */
        public KeyCondition(String label, boolean exclamationMarkPresent, boolean questionMarkPresent){
            this.label = label;
            this.exclamationMarkPresent = exclamationMarkPresent;
            this.questionMarkPresent = questionMarkPresent;
        }

        @Override
        public boolean applies(Environment e) {
            switch(e.getContext()) {
            case PRIMITIVE:
                if (questionMarkPresent)
                    return OsmUtils.isTrue(e.osm.get(label)) ^ exclamationMarkPresent;
                else
                    return e.osm.hasKey(label) ^ exclamationMarkPresent;
            case LINK:
                Utils.ensure(false, "Illegal state: KeyCondition not supported in LINK context");
                return false;
            default: throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return "[" + (exclamationMarkPresent ? "!" : "") + label + "]";
        }
    }

    public static class PseudoClassCondition extends Condition {

        String id;
        boolean not;

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
