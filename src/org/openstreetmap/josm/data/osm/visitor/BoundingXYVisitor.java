// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Calculates the total bounding rectangle of a series of {@link OsmPrimitive} objects, using the
 * EastNorth values as reference.
 * @author imi
 */
public class BoundingXYVisitor implements OsmPrimitiveVisitor, PrimitiveVisitor {

    private ProjectionBounds bounds;

    @Override
    public void visit(Node n) {
        visit((ILatLon) n);
    }

    @Override
    public void visit(Way w) {
        visit((IWay<?>) w);
    }

    @Override
    public void visit(Relation r) {
        visit((IRelation<?>) r);
    }

    @Override
    public void visit(INode n) {
        visit((ILatLon) n);
    }

    @Override
    public void visit(IWay<?> w) {
        if (w.isIncomplete()) return;
        for (INode n : w.getNodes()) {
            visit(n);
        }
    }

    @Override
    public void visit(IRelation<?> r) {
        // only use direct members
        for (IRelationMember<?> m : r.getMembers()) {
            if (!m.isRelation()) {
                m.getMember().accept(this);
            }
        }
    }

    /**
     * Visiting call for bounds.
     * @param b bounds
     */
    public void visit(Bounds b) {
        if (b != null) {
            Main.getProjection().visitOutline(b, this::visit);
        }
    }

    /**
     * Visiting call for projection bounds.
     * @param b projection bounds
     */
    public void visit(ProjectionBounds b) {
        if (b != null) {
            visit(b.getMin());
            visit(b.getMax());
        }
    }

    /**
     * Visiting call for lat/lon.
     * @param latlon lat/lon
     * @since 12725 (public for ILatLon parameter)
     */
    public void visit(ILatLon latlon) {
        if (latlon != null) {
            visit(latlon.getEastNorth(Main.getProjection()));
        }
    }

    /**
     * Visiting call for lat/lon.
     * @param latlon lat/lon
     */
    public void visit(LatLon latlon) {
        visit((ILatLon) latlon);
    }

    /**
     * Visiting call for east/north.
     * @param eastNorth east/north
     */
    public void visit(EastNorth eastNorth) {
        if (eastNorth != null) {
            if (bounds == null) {
                bounds = new ProjectionBounds(eastNorth);
            } else {
                bounds.extend(eastNorth);
            }
        }
    }

    /**
     * Determines if the visitor has a non null bounds area.
     * @return {@code true} if the visitor has a non null bounds area
     * @see ProjectionBounds#hasExtend
     */
    public boolean hasExtend() {
        return bounds != null && bounds.hasExtend();
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
        enlargeBoundingBox(Config.getPref().getDouble("edit.zoom-enlarge-bbox", 0.002));
    }

    /**
     * Enlarges the calculated bounding box by the specified number of degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * @param enlargeDegree number of degrees to enlarge on each side
     */
    public void enlargeBoundingBox(double enlargeDegree) {
        if (bounds == null)
            return;
        LatLon minLatlon = Main.getProjection().eastNorth2latlon(bounds.getMin());
        LatLon maxLatlon = Main.getProjection().eastNorth2latlon(bounds.getMax());
        bounds = new ProjectionBounds(new LatLon(
                        Math.max(-90, minLatlon.lat() - enlargeDegree),
                        Math.max(-180, minLatlon.lon() - enlargeDegree)).getEastNorth(Main.getProjection()),
                new LatLon(
                        Math.min(90, maxLatlon.lat() + enlargeDegree),
                        Math.min(180, maxLatlon.lon() + enlargeDegree)).getEastNorth(Main.getProjection()));
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
     * @param maxEnlargePercent maximum enlargement in percentage (100.0 for 100%)
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
     * the specified size in width and height. The value is ignored if the
     * bounding box is already larger than the specified amount.
     *
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * If the bounding box contains objects and is to be enlarged, the objects
     * will be centered within the new bounding box.
     *
     * @param size minimum width and height in meter
     */
    public void enlargeToMinSize(double size) {
        if (bounds == null)
            return;
        // convert size from meters to east/north units
        MapFrame map = MainApplication.getMap();
        double enSize = size * map.mapView.getScale() / map.mapView.getDist100Pixel() * 100;
        visit(bounds.getMin().add(-enSize/2, -enSize/2));
        visit(bounds.getMax().add(+enSize/2, +enSize/2));
    }

    @Override
    public String toString() {
        return "BoundingXYVisitor["+bounds+']';
    }

    /**
     * Compute the bounding box of a collection of primitives.
     * @param primitives the collection of primitives
     */
    public void computeBoundingBox(Collection<? extends OsmPrimitive> primitives) {
        if (primitives == null) return;
        for (OsmPrimitive p: primitives) {
            if (p == null) {
                continue;
            }
            p.accept((PrimitiveVisitor) this);
        }
    }
}
