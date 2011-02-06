// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.tools.Utils;

abstract public class Condition {

    abstract public boolean applies(Environment e);

    public static enum Op {EQ, NEQ}

    public static class KeyValueCondition extends Condition {

        public String k;
        public String v;
        public Op op;

        public KeyValueCondition(String k, String v, Op op) {

            this.k = k;
            this.v = v;
            this.op = op;
        }

        @Override
        public boolean applies(Environment e) {
            switch (op) {
                case EQ:
                    return Utils.equal(e.osm.get(k), v);
                case NEQ:
                    return !Utils.equal(e.osm.get(k), v);
                default:
                    throw new AssertionError();
            }
        }

        @Override
        public String toString() {
            return "[" + k + (op == Op.EQ ? "=" : "!=") + v + "]";
        }
    }

    public static class KeyCondition extends Condition {

        private String k;
        private boolean not;

        public KeyCondition(String k, boolean not) {
            this.k = k;
            this.not = not;
        }

        @Override
        public boolean applies(Environment e) {
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
            if ("closed".equals(id)) {
                if (e.osm instanceof Way && ((Way) e.osm).isClosed())
                    return true;
                if (e.osm instanceof Relation && ((Relation) e.osm).isMultipolygon())
                    return true;
                return false;
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
