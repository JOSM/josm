// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

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
    private EastNorth pivot;

    /**
     * angle of rotation starting click to pivot
     */
    private double startAngle;

    /**
     * computed rotation angle between starting click and current mouse pos
     */
    private double rotationAngle;

    /**
     * Creates a RotateCommand.
     * Assign the initial object set, compute pivot point and inital rotation angle.
     */
    public RotateCommand(Collection<OsmPrimitive> objects, EastNorth currentEN) {
        super(objects);

        pivot = getNodesCenter();
        startAngle = getAngle(currentEN);
        rotationAngle = 0.0;

        handleEvent(currentEN);
    }

    /**
     * Get angle between the horizontal axis and the line formed by the pivot and give points.
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
     * Rotate nodes.
     */
    @Override
    protected void transformNodes() {
        for (Node n : nodes) {
            double cosPhi = Math.cos(rotationAngle);
            double sinPhi = Math.sin(rotationAngle);
            EastNorth oldEastNorth = oldStates.get(n).getEastNorth();
            double x = oldEastNorth.east() - pivot.east();
            double y = oldEastNorth.north() - pivot.north();
            double nx =  cosPhi * x + sinPhi * y + pivot.east();
            double ny = -sinPhi * x + cosPhi * y + pivot.north();
            n.setEastNorth(new EastNorth(nx, ny));
        }
    }

    @Override
    public String getDescriptionText() {
        return trn("Rotate {0} node", "Rotate {0} nodes", nodes.size(), nodes.size());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((pivot == null) ? 0 : pivot.hashCode());
        long temp;
        temp = Double.doubleToLongBits(rotationAngle);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(startAngle);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        RotateCommand other = (RotateCommand) obj;
        if (pivot == null) {
            if (other.pivot != null)
                return false;
        } else if (!pivot.equals(other.pivot))
            return false;
        if (Double.doubleToLongBits(rotationAngle) != Double.doubleToLongBits(other.rotationAngle))
            return false;
        if (Double.doubleToLongBits(startAngle) != Double.doubleToLongBits(other.startAngle))
            return false;
        return true;
    }
}
