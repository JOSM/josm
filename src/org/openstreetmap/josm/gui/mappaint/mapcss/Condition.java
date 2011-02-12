// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import static org.openstreetmap.josm.tools.Utils.equal;

import java.util.EnumSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmUtils;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.mapcss.Condition.Op;

abstract public class Condition {

    abstract public boolean applies(Environment e);

    public static enum Op { EQ, NEQ, GREATER_OR_EQUAL, GREATER, LESS_OR_EQUAL, LESS,
        REGEX, ONE_OF, BEGINS_WITH, ENDS_WITH, CONTAINS }

    public final static EnumSet<Op> COMPARISON_OPERATERS =
            EnumSet.of(Op.GREATER_OR_EQUAL, Op.GREATER, Op.LESS_OR_EQUAL, Op.LESS);

    public static class KeyValueCondition extends Condition {

        public String k;
        public String v;
        public Op op;
        private float v_float;

        public KeyValueCondition(String k, String v, Op op) {
            this.k = k;
            this.v = v;
            this.op = op;
            if (COMPARISON_OPERATERS.contains(op)) {
                v_float = Float.parseFloat(v);
            }
        }

        @Override
        public boolean applies(Environment env) {
            String val = env.osm.get(k);
            if (val == null && op != Op.NEQ)
                return false;
            switch (op) {
                case EQ:
                    return equal(val, v);
                case NEQ:
                    return !equal(val, v);
                case REGEX:
                    Pattern p = Pattern.compile(v);
                    Matcher m = p.matcher(val);
                    return m.find();
                case ONE_OF:
                    String[] parts = val.split(";");
                    for (String part : parts) {
                        if (equal(v, part.trim()))
                            return true;
                    }
                    return false;
                case BEGINS_WITH:
                    return val.startsWith(v);
                case ENDS_WITH:
                    return val.endsWith(v);
                case CONTAINS:
                    return val.contains(v);
            }
            float val_float;
            try {
                val_float = Float.parseFloat(val);
            } catch (NumberFormatException e) {
                return false;
            }
            switch (op) {
                case GREATER_OR_EQUAL:
                    return val_float >= v_float;
                case GREATER:
                    return val_float > v_float;
                case LESS_OR_EQUAL:
                    return val_float <= v_float;
                case LESS:
                    return val_float < v_float;
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return "[" + k + "'" + op + "'" + v + "]";
        }
    }

    public static class KeyCondition extends Condition {

        private String k;
        private boolean not;
        private boolean yes;

        public KeyCondition(String k, boolean not, boolean yes) {
            this.k = k;
            this.not = not;
            this.yes = yes;
        }

        @Override
        public boolean applies(Environment e) {
            if (yes)
                return OsmUtils.isTrue(e.osm.get(k)) ^ not;
            else
                return e.osm.hasKey(k) ^ not;
        }

        @Override
        public String toString() {
            return "[" + (not ? "!" : "") + k + "]";
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
            } else if (equal(id, "modified")) {
                return e.osm.isModified() || e.osm.isNewOrUndeleted();
            } else if (equal(id, "new")) {
                return e.osm.isNew();
            } else if (equal(id, "connection") && (e.osm instanceof Node)) {
                return ((Node) e.osm).isConnectionNode();
            }
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
            Object o = e.evaluate(env);
            if (o instanceof Boolean)
                return (Boolean) o;
            return false;
        }

        @Override
        public String toString() {
            return "[" + e + "]";
        }
    }

}
