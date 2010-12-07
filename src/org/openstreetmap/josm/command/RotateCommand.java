// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * RotateCommand rotates a number of objects around their centre.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class RotateCommand extends TransformNodesCommand {

    /**
     * Pivot point
     */
    private EastNorth pivot;

    /**
     * World position of the mouse when the user started the command.
     *
     */
    EastNorth startEN = null;

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

        // We remember the very first position of the mouse for this action.
        // Note that SelectAction will keep the same ScaleCommand when the user
        // releases the button and presses it again with the same modifiers.
        // The very first point of this operation is stored here.
        startEN   = currentEN;

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
    public JLabel getDescription() {
        return new JLabel(trn("Rotate {0} node", "Rotate {0} nodes", nodes.size(), nodes.size()), ImageProvider.get("data", "node"), JLabel.HORIZONTAL);
    }
}
