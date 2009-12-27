// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.CachedLatLon;
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

    private ProjectionBounds bounds = null;

    public void visit(Node n) {
        visit(n.getEastNorth());
    }

    public void visit(Way w) {
        if (w.isIncomplete()) return;
        for (Node n : w.getNodes()) {
            visit(n);
        }
    }

    public void visit(Relation e) {
        // only use direct members
        for (RelationMember m : e.getMembers()) {
            if (!m.isRelation()) {
                m.getMember().visit(this);
            }
        }
    }

    public void visit(Bounds b) {
        if(b != null)
        {
            visit(b.getMin());
            visit(b.getMax());
        }
    }

    public void visit(ProjectionBounds b) {
        if(b != null)
        {
            visit(b.min);
            visit(b.max);
        }
    }

    public void visit(LatLon latlon) {
        if(latlon != null)
        {
            if(latlon instanceof CachedLatLon) {
                visit(((CachedLatLon)latlon).getEastNorth());
            } else {
                visit(Main.proj.latlon2eastNorth(latlon));
            }
        }
    }

    public void visit(EastNorth eastNorth) {
        if (eastNorth != null) {
            if (bounds == null) {
                bounds = new ProjectionBounds(eastNorth);
            } else {
                bounds.extend(eastNorth);
            }
        }
    }

    public boolean hasExtend()
    {
        return bounds != null && !bounds.min.equals(bounds.max);
    }

    /**
     * @return The bounding box or <code>null</code> if no coordinates have passed
     */
    public ProjectionBounds getBounds() {
        return bounds;
    }

    /**
     * Enlarges the calculated bounding box by 0.002 degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     */
    public void enlargeBoundingBox() {
        enlargeBoundingBox(Main.pref.getDouble("edit.zoom-enlarge-bbox", 0.002));
    }

    /**
     * Enlarges the calculated bounding box by the specified number of degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * @param enlargeDegree
     */
    public void enlargeBoundingBox(double enlargeDegree) {
        if (bounds == null)
            return;
        LatLon minLatlon = Main.proj.eastNorth2latlon(bounds.min);
        LatLon maxLatlon = Main.proj.eastNorth2latlon(bounds.max);
        bounds = new ProjectionBounds(
                Main.proj.latlon2eastNorth(new LatLon(minLatlon.lat() - enlargeDegree, minLatlon.lon() - enlargeDegree)),
                Main.proj.latlon2eastNorth(new LatLon(maxLatlon.lat() + enlargeDegree, maxLatlon.lon() + enlargeDegree)));
    }

    @Override public String toString() {
        return "BoundingXYVisitor["+bounds+"]";
    }

    public void computeBoundingBox(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        for (OsmPrimitive p: primitives) {
            if (p == null) {
                continue;
            }
            p.visit(this);
        }
    }
}
