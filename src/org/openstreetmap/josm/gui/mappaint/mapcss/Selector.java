// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.mappaint.mapcss;

import java.util.List;

import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.OsmPrimitiveType;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.mappaint.Environment;
import org.openstreetmap.josm.gui.mappaint.Range;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.Utils;

public class Selector {
    public String base;
    public Range range;
    protected List<Condition> conds;
    public String subpart;

    public Selector(String base, Pair<Integer, Integer> zoom, List<Condition> conds, String subpart) {
        this.base = base;
        if (zoom != null) {
            int a = zoom.a == null ? 0 : zoom.a;
            int b = zoom.b == null ? Integer.MAX_VALUE : zoom.b;
            if (a <= b) {
                range = fromLevel(a, b);
            }
        }
        if (range == null) {
            range = new Range();
        }
        this.conds = conds;
        this.subpart = subpart;
    }

    public boolean applies(Environment e) {
        if (!baseApplies(e.osm))
            return false;
        for (Condition c : conds) {
            if (!c.applies(e))
                return false;
        }
        return true;
    }

    private boolean baseApplies(OsmPrimitive osm) {
        if (base.equals("*"))
            return true;
        if (base.equals("area")) {
            if (osm instanceof Way && ((Way)osm).isClosed())
                return true;
            if (osm instanceof Relation && ((Relation) osm).isMultipolygon())
                return true;
        }
        if (base.equals(OsmPrimitiveType.from(osm).getAPIName()))
            return true;
        return false;
    }

    public static Range fromLevel(int a, int b) {
        if (a > b)
            throw new AssertionError();
        double lower = 0;
        double upper = Double.POSITIVE_INFINITY;
        if (b != Integer.MAX_VALUE) {
            lower = level2scale(b + 1);
        }
        if (a != 0) {
            upper = level2scale(a);
        }
        return new Range(lower, upper);
    }

    final static double R = 6378135;

    public static double level2scale(int lvl) {
        if (lvl < 0)
            throw new IllegalArgumentException();
        // preliminary formula - map such that mapnik imagery tiles of the same
        // or similar level are displayed at the given scale
        return 2.0 * Math.PI * R / Math.pow(2.0, lvl) / 2.56;
    }

    @Override
    public String toString() {
        return base + (range == null ? "" : range) + Utils.join("", conds) + (subpart != null ? ("::" + subpart) : "");
    }

}
