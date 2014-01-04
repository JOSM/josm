// License: GPL. For details, see LICENSE file.
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

    @Override
    public void visit(Node n) {
        visit(n.getEastNorth());
    }

    @Override
    public void visit(Way w) {
        if (w.isIncomplete()) return;
        for (Node n : w.getNodes()) {
            visit(n);
        }
    }

    @Override
    public void visit(Relation e) {
        // only use direct members
        for (RelationMember m : e.getMembers()) {
            if (!m.isRelation()) {
                m.getMember().accept(this);
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
            visit(b.getMin());
            visit(b.getMax());
        }
    }

    public void visit(LatLon latlon) {
        if(latlon != null)
        {
            if(latlon instanceof CachedLatLon) {
                visit(((CachedLatLon)latlon).getEastNorth());
            } else {
                visit(Main.getProjection().latlon2eastNorth(latlon));
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
        return bounds != null && !bounds.getMin().equals(bounds.getMax());
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
        LatLon minLatlon = Main.getProjection().eastNorth2latlon(bounds.getMin());
        LatLon maxLatlon = Main.getProjection().eastNorth2latlon(bounds.getMax());
        bounds = new ProjectionBounds(
                Main.getProjection().latlon2eastNorth(new LatLon(minLatlon.lat() - enlargeDegree, minLatlon.lon() - enlargeDegree)),
                Main.getProjection().latlon2eastNorth(new LatLon(maxLatlon.lat() + enlargeDegree, maxLatlon.lon() + enlargeDegree)));
    }

    /**
     * Enlarges the bounding box up to <code>maxEnlargePercent</code>, depending on
     * its size. If the bounding box is small, it will be enlarged more in relation
     * to its beginning size. The larger the bounding box, the smaller the change,
     * down to the minimum of 1% enlargement.
     * 
     * Warning: if the bounding box only contains a single node, no expansion takes
     * place because a node has no width/height. Use <code>enlargeToMinDegrees</code>
     * instead.
     * 
     * Example: You specify enlargement to be up to 100%.
     * 
     *          Bounding box is a small house: enlargement will be 95–100%, i.e.
     *          making enough space so that the house fits twice on the screen in
     *          each direction.
     * 
     *          Bounding box is a large landuse, like a forest: Enlargement will
     *          be 1–10%, i.e. just add a little border around the landuse.
     * 
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     * 
     * @param maxEnlargePercent
     */
    public void enlargeBoundingBoxLogarithmically(double maxEnlargePercent) {
        if (bounds == null)
            return;

        double diffEast = bounds.getMax().east() - bounds.getMin().east();
        double diffNorth = bounds.getMax().north() - bounds.getMin().north();

        double enlargeEast = Math.min(maxEnlargePercent - 10*Math.log(diffEast), 1)/100;
        double enlargeNorth = Math.min(maxEnlargePercent - 10*Math.log(diffNorth), 1)/100;

        visit(bounds.getMin().add(-enlargeEast/2, -enlargeNorth/2));
        visit(bounds.getMax().add(+enlargeEast/2, +enlargeNorth/2));
    }

    /**
     * Specify a degree larger than 0 in order to make the bounding box at least
     * the specified amount of degrees high and wide. The value is ignored if the
     * bounding box is already larger than the specified amount.
     * 
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     * 
     * If the bounding box contains objects and is to be enlarged, the objects
     * will be centered within the new bounding box.
     * 
     * @param minDegrees
     */
    public void enlargeToMinDegrees(double minDegrees) {
        if (bounds == null)
            return;

        EastNorth minEnlarge = Main.getProjection().latlon2eastNorth(new LatLon(0, minDegrees));

        visit(bounds.getMin().add(-minEnlarge.east()/2, -minEnlarge.north()/2));
        visit(bounds.getMax().add(+minEnlarge.east()/2, +minEnlarge.north()/2));
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
            p.accept(this);
        }
    }
}
