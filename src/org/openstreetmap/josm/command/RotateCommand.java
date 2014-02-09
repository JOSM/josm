// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

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
    private double startAngle = 0.0;

    /**
     * computed rotation angle between starting click and current mouse pos
     */
    private double rotationAngle = 0.0;

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
    protected double getAngle(EastNorth currentEN) {
        if ( pivot == null )
            return 0.0; // should never happen by contract
        return Math.atan2(currentEN.east()-pivot.east(), currentEN.north()-pivot.north());
    }

    /**
     * Compute new rotation angle and transform nodes accordingly.
     */
    @Override
    public void handleEvent(EastNorth currentEN) {
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
            EastNorth oldEastNorth = oldStates.get(n).eastNorth;
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
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "node");
    }
}
