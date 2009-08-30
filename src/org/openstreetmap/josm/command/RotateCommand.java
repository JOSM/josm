package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * RotateCommand rotates a number of objects around their centre.
 *
 * @author Frederik Ramm <frederik@remote.org>
 */
public class RotateCommand extends Command {

    /**
     * The objects to rotate.
     */
    private Collection<Node> nodes = new LinkedList<Node>();

    /**
     * pivot point
     */
    private EastNorth pivot;

    /**
     * Small helper for holding the interesting part of the old data state of the
     * objects.
     */
    public static class OldState {
        LatLon latlon;
        EastNorth eastNorth;
        boolean modified;
    }

    /**
     * angle of rotation starting click to pivot
     */
    private double startAngle;

    /**
     * computed rotation angle between starting click and current mouse pos
     */
    private double rotationAngle;

    /**
     * List of all old states of the objects.
     */
    private Map<Node, OldState> oldState = new HashMap<Node, OldState>();

    /**
     * Creates a RotateCommand.
     * Assign the initial object set, compute pivot point and rotation angle.
     * Computation of pivot point is done by the same rules that are used in
     * the "align nodes in circle" action.
     */
    public RotateCommand(Collection<OsmPrimitive> objects, EastNorth start, EastNorth end) {

        this.nodes = AllNodesVisitor.getAllNodes(objects);
        pivot = new EastNorth(0,0);

        for (Node n : this.nodes) {
            OldState os = new OldState();
            os.latlon = new LatLon(n.getCoor());
            os.eastNorth = n.getEastNorth();
            os.modified = n.modified;
            oldState.put(n, os);
            pivot = pivot.add(os.eastNorth.east(), os.eastNorth.north());
        }
        pivot = new EastNorth(pivot.east()/this.nodes.size(), pivot.north()/this.nodes.size());

        rotationAngle = Math.PI/2;
        rotateAgain(start, end);
    }

    /**
     * Rotate the same set of objects again, by the angle between given
     * start and end nodes. Internally this is added to the existing
     * rotation so a later undo will undo the whole rotation.
     */
    public void rotateAgain(EastNorth start, EastNorth end) {
        // compute angle
        startAngle = Math.atan2(start.east()-pivot.east(), start.north()-pivot.north());
        double endAngle = Math.atan2(end.east()-pivot.east(), end.north()-pivot.north());
        rotationAngle += startAngle - endAngle;
        rotateNodes(false);
    }

    /**
     * Helper for actually rotationg the nodes.
     * @param setModified - true if rotated nodes should be flagged "modified"
     */
    private void rotateNodes(boolean setModified) {
        for (Node n : nodes) {
            double cosPhi = Math.cos(rotationAngle);
            double sinPhi = Math.sin(rotationAngle);
            EastNorth oldEastNorth = oldState.get(n).eastNorth;
            double x = oldEastNorth.east() - pivot.east();
            double y = oldEastNorth.north() - pivot.north();
            double nx =  sinPhi * x + cosPhi * y + pivot.east();
            double ny = -cosPhi * x + sinPhi * y + pivot.north();
            n.setEastNorth(new EastNorth(nx, ny));
            if (setModified) {
                n.modified = true;
            }
        }
    }

    @Override public boolean executeCommand() {
        rotateNodes(true);
        return true;
    }

    @Override public void undoCommand() {
        for (Node n : nodes) {
            OldState os = oldState.get(n);
            n.setCoor(os.latlon);
            n.modified = os.modified;
        }
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (OsmPrimitive osm : nodes) {
            modified.add(osm);
        }
    }

    @Override public MutableTreeNode description() {
        return new DefaultMutableTreeNode(new JLabel(trn("Rotate {0} node", "Rotate {0} nodes", nodes.size(), nodes.size()), ImageProvider.get("data", "node"), JLabel.HORIZONTAL));
    }

    public Collection<Node> getRotatedNodes() {
        return nodes;
    }
}
