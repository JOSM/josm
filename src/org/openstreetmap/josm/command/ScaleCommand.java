// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

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
     */
    private EastNorth startEN;

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
    public final void handleEvent(EastNorth currentEN) {
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
            EastNorth oldEastNorth = oldStates.get(n).getEastNorth();
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
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((pivot == null) ? 0 : pivot.hashCode());
        long temp;
        temp = Double.doubleToLongBits(scalingFactor);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + ((startEN == null) ? 0 : startEN.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        ScaleCommand other = (ScaleCommand) obj;
        if (pivot == null) {
            if (other.pivot != null)
                return false;
        } else if (!pivot.equals(other.pivot))
            return false;
        if (Double.doubleToLongBits(scalingFactor) != Double.doubleToLongBits(other.scalingFactor))
            return false;
        if (startEN == null) {
            if (other.startEN != null)
                return false;
        } else if (!startEN.equals(other.startEN))
            return false;
        return true;
    }
}
