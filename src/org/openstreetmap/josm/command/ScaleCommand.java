// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Command, to scale a given set of primitives.
 * The relative distance of the nodes will be increased/decreased.
 */
public class ScaleCommand extends TransformNodesCommand {
    /**
     * Pivot point
     */
    private final EastNorth pivot;

    /**
     * Current scaling factor applied
     */
    private double scalingFactor;

    /**
     * World position of the mouse when the user started the command.
     */
    private final EastNorth startEN;

    /**
     * Creates a ScaleCommand.
     * Assign the initial object set, compute pivot point.
     * Computation of pivot point is done by the same rules that are used in
     * the "align nodes in circle" action.
     * @param objects objects to fetch nodes from
     * @param currentEN cuurent eats/north
     */
    public ScaleCommand(Collection<? extends OsmPrimitive> objects, EastNorth currentEN) {
        super(objects);

        pivot = getNodesCenter();

        // We remember the very first position of the mouse for this action.
        // Note that SelectAction will keep the same ScaleCommand when the user
        // releases the button and presses it again with the same modifiers.
        // The very first point of this operation is stored here.
        startEN = currentEN;

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
        setScalingFactor(Math.cos(startAngle-endAngle) * currentDistance / startDistance);
        transformNodes();
    }

    /**
     * Set the scaling factor
     * @param scalingFactor The scaling factor.
     */
    protected void setScalingFactor(double scalingFactor) {
        this.scalingFactor = scalingFactor;
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
        return Objects.hash(super.hashCode(), pivot, scalingFactor, startEN);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        ScaleCommand that = (ScaleCommand) obj;
        return Double.compare(that.scalingFactor, scalingFactor) == 0 &&
                Objects.equals(pivot, that.pivot) &&
                Objects.equals(startEN, that.startEN);
    }
}
