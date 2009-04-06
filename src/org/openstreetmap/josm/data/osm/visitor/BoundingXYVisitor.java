// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Calculates the total bounding rectangle of a series of {@link OsmPrimitive} objects, using the
 * EastNorth values as reference.
 * @author imi
 */
public class BoundingXYVisitor extends AbstractVisitor {

    public EastNorth min, max;

    public void visit(Node n) {
        visit(n.eastNorth);
    }

    public void visit(Way w) {
        w.visitNodes(this);
    }

    public void visit(Relation e) {
        // only use direct members
        for (RelationMember m : e.members) {
            if (!(m.member instanceof Relation)) {
                m.member.visit(this);
            }
        }
    }

    public void visit(EastNorth eastNorth) {
        if (eastNorth != null) {
            if (min == null)
                min = eastNorth;
            else if (eastNorth.east() < min.east() || eastNorth.north() < min.north())
                min = new EastNorth(Math.min(min.east(), eastNorth.east()), Math.min(min.north(), eastNorth.north()));

            if (max == null)
                max = eastNorth;
            else if (eastNorth.east() > max.east() || eastNorth.north() > max.north())
                max = new EastNorth(Math.max(max.east(), eastNorth.east()), Math.max(max.north(), eastNorth.north()));
        }
    }

    /**
     * @return The bounding box or <code>null</code> if no coordinates have passed
     */
    public Bounds getBounds() {
        if (min == null || max == null)
            return null;
        return new Bounds(Main.proj.eastNorth2latlon(min), Main.proj.eastNorth2latlon(max));
    }

    /**
     * Enlarges the calculated bounding box by 0.002 degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     */
    public void enlargeBoundingBox() {
        enlargeBoundingBox(0.002);
    }

    /**
     * Enlarges the calculated bounding box by the specified number of degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * @param enlargeDegree
     */
    public void enlargeBoundingBox(double enlargeDegree) {
        if (min == null || max == null)
            return;
        LatLon minLatlon = Main.proj.eastNorth2latlon(min);
        min = Main.proj.latlon2eastNorth(new LatLon(minLatlon.lat() - enlargeDegree, minLatlon.lon() - enlargeDegree));
        LatLon maxLatlon = Main.proj.eastNorth2latlon(max);
        max = Main.proj.latlon2eastNorth(new LatLon(maxLatlon.lat() + enlargeDegree, maxLatlon.lon() + enlargeDegree));
    }
}
