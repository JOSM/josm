// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MapGridPaintable;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.GridType;
import org.openstreetmap.josm.tools.Logging;

/**
 * Moves the origin of the grid drawn over the map to the current selection, so that grid lines pass through it,
 * and enables the grid. The origin is the position of the selected node, or the centroid (arithmetic mean
 * position) of all nodes reachable from the current selection (nodes of selected ways, node members of selected
 * relations) if more than one node is involved.
 * @see MapGridPaintable
 * @see AlignGridRotationAction
 * @since 19640
 */
public class SetGridOriginAction extends JosmAction {

    /**
     * Constructs a new {@code SetGridOriginAction}.
     */
    public SetGridOriginAction() {
        super(tr("Set origin to selection"), "gridorigin",
                tr("Move the grid so that a grid line passes through the selected node, "
                        + "or through the centroid of the current selection, and show the grid."),
                null, false);
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getCentroid(getLayerManager().getEditDataSet()) != null);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        EastNorth centroid = getCentroid(getLayerManager().getEditDataSet());
        if (centroid == null) {
            return;
        }
        setOrigin(centroid);
        if (MainApplication.isDisplayingMapView()) {
            MainApplication.getMap().mapView.repaint();
        }
    }

    /**
     * Computes the centroid of the current selection: the (arithmetic mean) position of every node reachable from
     * the selected primitives (the selected nodes themselves, the nodes of selected ways, and the node members of
     * selected relations). For a single selected node this is simply that node's position.
     * @param ds the data set, may be {@code null}
     * @return the centroid, or {@code null} if the selection contains no node with known coordinates
     */
    static EastNorth getCentroid(DataSet ds) {
        if (ds == null) {
            return null;
        }
        double sumEast = 0;
        double sumNorth = 0;
        int count = 0;
        for (Node n : AllNodesVisitor.getAllNodes(ds.getSelected())) {
            if (n.isLatLonKnown()) {
                EastNorth en = n.getEastNorth();
                sumEast += en.east();
                sumNorth += en.north();
                count++;
            }
        }
        return count == 0 ? null : new EastNorth(sumEast / count, sumNorth / count);
    }

    /**
     * Sets the grid origin (in the coordinates of the current grid type) and enables the grid.
     * @param position the new grid origin, in projected coordinates
     */
    public static void setOrigin(EastNorth position) {
        if (MapGridPaintable.TYPE.get() == GridType.PROJECTED) {
            MapGridPaintable.ORIGIN_X.put(position.east());
            MapGridPaintable.ORIGIN_Y.put(position.north());
        } else {
            final LatLon ll;
            try {
                ll = ProjectionRegistry.getProjection().eastNorth2latlon(position);
            } catch (IllegalArgumentException e) {
                // the position is outside the domain of the projection, leave the grid as it is
                Logging.warn("Cannot use {0} as grid origin: {1}", position, e.getMessage());
                Logging.trace(e);
                return;
            }
            MapGridPaintable.ORIGIN_X.put(ll.lon());
            MapGridPaintable.ORIGIN_Y.put(ll.lat());
        }
        MapGridPaintable.ENABLED.put(true);
    }
}
