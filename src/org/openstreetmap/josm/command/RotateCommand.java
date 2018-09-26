// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Objects;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * RotateCommand rotates a number of objects around their centre.
 *
 * @author Frederik Ramm
 */
public class RotateCommand extends TransformNodesCommand {

    /**
     * Pivot point
     */
    private final EastNorth pivot;

    /**
     * angle of rotation starting click to pivot
     */
    private final double startAngle;

    /**
     * computed rotation angle between starting click and current mouse pos
     */
    private double rotationAngle;

    /**
     * Creates a RotateCommand.
     * Assign the initial object set, compute pivot point and initial rotation angle.
     * @param objects objects to fetch nodes from
     * @param currentEN cuurent eats/north
     */
    public RotateCommand(Collection<? extends OsmPrimitive> objects, EastNorth currentEN) {
        super(objects);

        pivot = getNodesCenter();
        startAngle = getAngle(currentEN);
        rotationAngle = 0.0;

        handleEvent(currentEN);
    }

    /**
     * Get angle between the horizontal axis and the line formed by the pivot and given point.
     * @param currentEN cuurent eats/north
     * @return angle between the horizontal axis and the line formed by the pivot and given point
     **/
    protected final double getAngle(EastNorth currentEN) {
        if (pivot == null)
            return 0.0; // should never happen by contract
        return Math.atan2(currentEN.east()-pivot.east(), currentEN.north()-pivot.north());
    }

    /**
     * Compute new rotation angle and transform nodes accordingly.
     */
    @Override
    public final void handleEvent(EastNorth currentEN) {
        double currentAngle = getAngle(currentEN);
        rotationAngle = currentAngle - startAngle;
        transformNodes();
    }

    /**
     * Set the rotation angle.
     * @param rotationAngle The rotate angle
     */
    protected void setRotationAngle(double rotationAngle) {
        this.rotationAngle = rotationAngle;
    }

    /**
     * Rotate nodes.
     */
    @Override
    protected void transformNodes() {
        double cosPhi = Math.cos(rotationAngle);
        double sinPhi = Math.sin(rotationAngle);
        for (Node n : nodes) {
            EastNorth oldEastNorth = oldStates.get(n).getEastNorth();
            double x = oldEastNorth.east() - pivot.east();
            double y = oldEastNorth.north() - pivot.north();
            // CHECKSTYLE.OFF: SingleSpaceSeparator
            double nx =  cosPhi * x + sinPhi * y + pivot.east();
            double ny = -sinPhi * x + cosPhi * y + pivot.north();
            // CHECKSTYLE.ON: SingleSpaceSeparator
            n.setEastNorth(new EastNorth(nx, ny));
        }
    }

    @Override
    public String getDescriptionText() {
        return trn("Rotate {0} node", "Rotate {0} nodes", nodes.size(), nodes.size());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), pivot, startAngle, rotationAngle);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        RotateCommand that = (RotateCommand) obj;
        return Double.compare(that.startAngle, startAngle) == 0 &&
                Double.compare(that.rotationAngle, rotationAngle) == 0 &&
                Objects.equals(pivot, that.pivot);
    }
}
