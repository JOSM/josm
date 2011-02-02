// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Utils;

abstract public class Condition {

    abstract public boolean applies(OsmPrimitive osm);

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
        public boolean applies(OsmPrimitive osm) {
            switch (op) {
                case EQ:
                    return Utils.equal(osm.get(k), v);
                case NEQ:
                    return !Utils.equal(osm.get(k), v);
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
        public boolean applies(OsmPrimitive osm) {
            return osm.hasKey(k) ^ not;
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
        public boolean applies(OsmPrimitive osm) {
            if ("closed".equals(id)) {
                if (osm instanceof Way && ((Way) osm).isClosed())
                    return true;
                if (osm instanceof Relation && ((Relation) osm).isMultipolygon())
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

}
