// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.function.DoubleUnaryOperator;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.ProjectionBounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.INode;
import org.openstreetmap.josm.data.osm.IPrimitive;
import org.openstreetmap.josm.data.osm.IRelation;
import org.openstreetmap.josm.data.osm.IRelationMember;
import org.openstreetmap.josm.data.osm.IWay;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Calculates the total bounding rectangle of a series of {@link OsmPrimitive} objects, using the
 * EastNorth values as reference.
 * @author imi
 */
public class BoundingXYVisitor implements OsmPrimitiveVisitor, PrimitiveVisitor {
    /** default value for setting "edit.zoom-enlarge-bbox" */
    private static final double ENLARGE_DEFAULT = 0.0002;

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
            ProjectionRegistry.getProjection().visitOutline(b, this::visit);
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
            visit(latlon.getEastNorth(ProjectionRegistry.getProjection()));
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
     * Enlarges the calculated bounding box by 0.0002 degrees or user value
     * given in edit.zoom-enlarge-bbox.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     */
    public void enlargeBoundingBox() {
        final double enlarge = Config.getPref().getDouble("edit.zoom-enlarge-bbox", ENLARGE_DEFAULT);
        enlargeBoundingBox(enlarge, enlarge);
    }

    /**
     * Enlarges the calculated bounding box by the specified number of degrees.
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * @param enlargeDegreeX number of degrees to enlarge on each side along X
     * @param enlargeDegreeY number of degrees to enlarge on each side along Y
     */
    public void enlargeBoundingBox(double enlargeDegreeX, double enlargeDegreeY) {
        if (bounds == null)
            return;
        LatLon minLatlon = ProjectionRegistry.getProjection().eastNorth2latlon(bounds.getMin());
        LatLon maxLatlon = ProjectionRegistry.getProjection().eastNorth2latlon(bounds.getMax());
        bounds = new ProjectionBounds(new LatLon(
                        Math.max(-90, minLatlon.lat() - enlargeDegreeY),
                        Math.max(-180, minLatlon.lon() - enlargeDegreeX)).getEastNorth(ProjectionRegistry.getProjection()),
                new LatLon(
                        Math.min(90, maxLatlon.lat() + enlargeDegreeY),
                        Math.min(180, maxLatlon.lon() + enlargeDegreeX)).getEastNorth(ProjectionRegistry.getProjection()));
    }

    /**
     * Enlarges the bounding box up to 0.0002 degrees, depending on its size and user
     * settings in edit.zoom-enlarge-bbox. If the bounding box is small, it will be enlarged more in relation
     * to its beginning size. The larger the bounding box, the smaller the change,
     * down to 0.0 degrees.
     *
     * If the bounding box has not been set (<code>min</code> or <code>max</code>
     * equal <code>null</code>) this method does not do anything.
     *
     * @since 14628
     */
    public void enlargeBoundingBoxLogarithmically() {
        if (bounds == null)
            return;
        final LatLon min = ProjectionRegistry.getProjection().eastNorth2latlon(bounds.getMin());
        final LatLon max = ProjectionRegistry.getProjection().eastNorth2latlon(bounds.getMax());
        final double deltaLat = max.lat() - min.lat();
        final double deltaLon = max.lon() - min.lon();
        final double enlarge = Config.getPref().getDouble("edit.zoom-enlarge-bbox", ENLARGE_DEFAULT);

        final DoubleUnaryOperator enlargement = deltaDegress -> {
            if (deltaDegress < enlarge) {
                // delta is very small, use configured minimum value
                return enlarge;
            }
            if (deltaDegress < 0.1) {
                return enlarge - deltaDegress / 100;
            }
            return 0.0;
        };
        enlargeBoundingBox(enlargement.applyAsDouble(deltaLon), enlargement.applyAsDouble(deltaLat));
    }

    @Override
    public String toString() {
        return "BoundingXYVisitor["+bounds+']';
    }

    /**
     * Compute the bounding box of a collection of primitives.
     * @param primitives the collection of primitives
     */
    public void computeBoundingBox(Collection<? extends IPrimitive> primitives) {
        if (primitives == null) return;
        for (IPrimitive p: primitives) {
            if (p == null) {
                continue;
            }
            p.accept(this);
        }
    }
}
