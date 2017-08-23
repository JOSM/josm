// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.BBox;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.projection.Projection;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.tools.Geometry;

/**
 * This allows to select a polygon/multipolygon by an internal point.
 * @since 7144
 */
public final class SelectByInternalPointAction {

    private SelectByInternalPointAction() {
        // Hide public constructor for utility class
    }

    /**
     * Returns the surrounding polygons/multipolygons ordered by their area size (from small to large)
     * which contain the internal point.
     *
     * @param internalPoint the internal point.
     * @return the surrounding polygons/multipolygons
     */
    public static Collection<OsmPrimitive> getSurroundingObjects(EastNorth internalPoint) {
        return getSurroundingObjects(Main.getLayerManager().getEditDataSet(), internalPoint, false);
    }

    /**
     * Returns the surrounding polygons/multipolygons ordered by their area size (from small to large)
     * which contain the internal point.
     *
     * @param ds the data set
     * @param internalPoint the internal point.
     * @param includeMultipolygonWays whether to include multipolygon ways in the result (false by default)
     * @return the surrounding polygons/multipolygons
     * @since 11247
     */
    public static Collection<OsmPrimitive> getSurroundingObjects(DataSet ds, EastNorth internalPoint, boolean includeMultipolygonWays) {
        if (ds == null) {
            return Collections.emptySet();
        }
        final Node n = new Node(internalPoint);
        final Map<Double, OsmPrimitive> found = new TreeMap<>();
        for (Way w : ds.getWays()) {
            if (w.isUsable() && w.isClosed() && w.isSelectable() && Geometry.nodeInsidePolygon(n, w.getNodes())) {
                found.put(Geometry.closedWayArea(w), w);
            }
        }
        Projection projection = MainApplication.getMap().mapView.getProjection();
        for (Relation r : ds.getRelations()) {
            if (r.isUsable() && r.isMultipolygon() && r.isSelectable() && Geometry.isNodeInsideMultiPolygon(n, r, null)) {
                if (!includeMultipolygonWays) {
                    for (RelationMember m : r.getMembers()) {
                        if (m.isWay() && m.getWay().isClosed()) {
                            found.values().remove(m.getWay());
                        }
                    }
                }
                // estimate multipolygon size by its bounding box area
                BBox bBox = r.getBBox();
                EastNorth en1 = projection.latlon2eastNorth(bBox.getTopLeft());
                EastNorth en2 = projection.latlon2eastNorth(bBox.getBottomRight());
                double s = Math.abs((en1.east() - en2.east()) * (en1.north() - en2.north()));
                found.put(s <= 0 ? 1e8 : s, r);
            }
        }
        return found.values();
    }

    /**
     * Returns the smallest surrounding polygon/multipolygon which contains the internal point.
     *
     * @param internalPoint the internal point.
     * @return the smallest surrounding polygon/multipolygon
     */
    public static OsmPrimitive getSmallestSurroundingObject(EastNorth internalPoint) {
        final Collection<OsmPrimitive> surroundingObjects = getSurroundingObjects(internalPoint);
        return surroundingObjects.isEmpty() ? null : surroundingObjects.iterator().next();
    }

    /**
     * Select a polygon or multipolygon by an internal point.
     *
     * @param internalPoint the internal point.
     * @param doAdd         whether to add selected polygon to the current selection.
     * @param doRemove      whether to remove the selected polygon from the current selection.
     */
    public static void performSelection(EastNorth internalPoint, boolean doAdd, boolean doRemove) {
        final Collection<OsmPrimitive> surroundingObjects = getSurroundingObjects(internalPoint);
        final DataSet ds = Main.getLayerManager().getEditDataSet();
        if (surroundingObjects.isEmpty()) {
            return;
        } else if (doRemove) {
            final Collection<OsmPrimitive> newSelection = new ArrayList<>(ds.getSelected());
            newSelection.removeAll(surroundingObjects);
            ds.setSelected(newSelection);
        } else if (doAdd) {
            final Collection<OsmPrimitive> newSelection = new ArrayList<>(ds.getSelected());
            newSelection.add(surroundingObjects.iterator().next());
            ds.setSelected(newSelection);
        } else {
            ds.setSelected(surroundingObjects.iterator().next());
        }
    }
}
