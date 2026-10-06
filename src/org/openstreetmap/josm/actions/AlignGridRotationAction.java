// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Iterator;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.MapGridPaintable;
import org.openstreetmap.josm.gui.layer.MapGridPaintable.GridType;

/**
 * Rotates the grid drawn over the map so that its lines run parallel to the current selection: either a single way
 * (the direction from its first to its last node) or exactly two nodes. Switches the grid to projected coordinates,
 * since a latitude/longitude grid cannot be rotated, and enables it.
 * @see MapGridPaintable
 * @see SetGridOriginAction
 * @since 19640
 */
public class AlignGridRotationAction extends JosmAction {

    /**
     * Constructs a new {@code AlignGridRotationAction}.
     */
    public AlignGridRotationAction() {
        super(tr("Align rotation to selection"), "gridrotate",
                tr("Rotate the grid so that its lines are parallel to the selected way "
                        + "(first to last node) or to the line between the two selected nodes. Switches to a projected grid."),
                null, false);
    }

    /**
     * Determines the direction given by the selection: a single way with at least two nodes, or exactly two nodes.
     * @param ds the data set, may be null
     * @return start and end point, or {@code null} if the selection does not define a direction
     */
    static EastNorth[] getSelectedDirection(DataSet ds) {
        if (ds == null) {
            return null;
        }
        Collection<Way> ways = ds.getSelectedWays();
        Collection<Node> nodes = ds.getSelectedNodes();
        Node a = null;
        Node b = null;
        if (ways.size() == 1 && nodes.isEmpty()) {
            Way w = ways.iterator().next();
            if (w.getNodesCount() >= 2) {
                a = w.firstNode();
                b = w.isClosed() ? w.getNode(1) : w.lastNode();
            }
        } else if (nodes.size() == 2 && ways.isEmpty()) {
            Iterator<Node> it = nodes.iterator();
            a = it.next();
            b = it.next();
        }
        if (a == null || b == null || !a.isLatLonKnown() || !b.isLatLonKnown()) {
            return null;
        }
        EastNorth[] result = {a.getEastNorth(), b.getEastNorth()};
        return result[0].equalsEpsilon(result[1], 1e-9) ? null : result;
    }

    /**
     * Computes the grid rotation (counter clockwise, in degrees, in the range [0, 90)) for which a grid line is
     * parallel to the given direction.
     * @param from start point
     * @param to end point
     * @return the rotation in degrees
     */
    public static double rotationOf(EastNorth from, EastNorth to) {
        double angle = Math.toDegrees(Math.atan2(to.north() - from.north(), to.east() - from.east()));
        angle %= 90;
        if (angle < 0) {
            angle += 90;
        }
        return angle >= 90 - 1e-9 ? 0 : angle;
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getSelectedDirection(getLayerManager().getEditDataSet()) != null);
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledState();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        EastNorth[] direction = getSelectedDirection(getLayerManager().getEditDataSet());
        if (direction == null) {
            return;
        }
        MapGridPaintable.ROTATION.put(rotationOf(direction[0], direction[1]));
        MapGridPaintable.TYPE.put(GridType.PROJECTED);
        MapGridPaintable.ENABLED.put(true);
        if (MainApplication.isDisplayingMapView()) {
            MainApplication.getMap().mapView.repaint();
        }
    }
}
