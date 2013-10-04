// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

public class ScaleCommand extends TransformNodesCommand {
    /**
     * Pivot point
     */
    private EastNorth pivot;

    /**
     * Current scaling factor applied
     */
    private double scalingFactor;

    /**
     * World position of the mouse when the user started the command.
     *
     */
    EastNorth startEN = null;

    /**
     * Creates a ScaleCommand.
     * Assign the initial object set, compute pivot point.
     * Computation of pivot point is done by the same rules that are used in
     * the "align nodes in circle" action.
     */
    public ScaleCommand(Collection<OsmPrimitive> objects, EastNorth currentEN) {
        super(objects);

        pivot = getNodesCenter();

        // We remember the very first position of the mouse for this action.
        // Note that SelectAction will keep the same ScaleCommand when the user
        // releases the button and presses it again with the same modifiers.
        // The very first point of this operation is stored here.
        startEN   = currentEN;

        handleEvent(currentEN);
    }

    /**
     * Compute new scaling factor and transform nodes accordingly.
     */
    @Override
    public void handleEvent(EastNorth currentEN) {
        double startAngle = Math.atan2(startEN.east()-pivot.east(), startEN.north()-pivot.north());
        double endAngle = Math.atan2(currentEN.east()-pivot.east(), currentEN.north()-pivot.north());
        double startDistance = pivot.distance(startEN);
        double currentDistance = pivot.distance(currentEN);
        scalingFactor = Math.cos(startAngle-endAngle) * currentDistance / startDistance;
        transformNodes();
    }


    /**
     * Scale nodes.
     */
    @Override
    protected void transformNodes() {
        for (Node n : nodes) {
            EastNorth oldEastNorth = oldStates.get(n).eastNorth;
            double dx = oldEastNorth.east() - pivot.east();
            double dy = oldEastNorth.north() - pivot.north();
            double nx = pivot.east() + scalingFactor * dx;
            double ny = pivot.north() + scalingFactor * dy;
            n.setEastNorth(new EastNorth(nx, ny));
        }
    }

    @Override
    public String getDescriptionText() {
        return trn("Scale {0} node", "Scale {0} nodes", nodes.size(), nodes.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "node");
    }
}
