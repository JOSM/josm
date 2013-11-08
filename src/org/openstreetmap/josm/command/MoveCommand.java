// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.projection.Projections;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * MoveCommand moves a set of OsmPrimitives along the map. It can be moved again
 * to collect several MoveCommands into one command.
 *
 * @author imi
 */
public class MoveCommand extends Command {
    /**
     * The objects that should be moved.
     */
    private Collection<Node> nodes = new LinkedList<Node>();
    /**
     * Starting position, base command point, current (mouse-drag) position = startEN + (x,y) =
     */
    private EastNorth startEN;

    /**
     * x difference movement. Coordinates are in northern/eastern
     */
    private double x;
    /**
     * y difference movement. Coordinates are in northern/eastern
     */
    private double y;

    private double backupX;
    private double backupY;

    /**
     * List of all old states of the objects.
     */
    private List<OldNodeState> oldState = new LinkedList<OldNodeState>();

    public MoveCommand(OsmPrimitive osm, double x, double y) {
        this(Collections.singleton(osm), x, y);
    }

    public MoveCommand(Node node, LatLon position) {
        this(Collections.singleton((OsmPrimitive) node), node.getEastNorth().sub(Projections.project(position)));
    }

    public MoveCommand(Collection<OsmPrimitive> objects, EastNorth offset) {
        this(objects, offset.getX(), offset.getY());
    }

    /**
     * Create a MoveCommand and assign the initial object set and movement vector.
     */
    public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
        super();
        startEN = null;
        saveCheckpoint(); // (0,0) displacement will be saved
        this.x = x;
        this.y = y;
        this.nodes = AllNodesVisitor.getAllNodes(objects);
        for (Node n : this.nodes) {
            oldState.add(new OldNodeState(n));
        }
    }

    public MoveCommand(Collection<OsmPrimitive> objects, EastNorth start, EastNorth end) {
        this(objects, end.getX()-start.getX(), end.getY()-start.getY());
        startEN =  start;
    }

    public MoveCommand(OsmPrimitive p, EastNorth start, EastNorth end) {
        this(Collections.singleton(p), end.getX()-start.getX(), end.getY()-start.getY());
        startEN =  start;
    }

    /**
     * Move the same set of objects again by the specified vector. The vectors
     * are added together and so the resulting will be moved to the previous
     * vector plus this one.
     *
     * The move is immediately executed and any undo will undo both vectors to
     * the original position the objects had before first moving.
     */
    public void moveAgain(double x, double y) {
        for (Node n : nodes) {
            n.setEastNorth(n.getEastNorth().add(x, y));
        }
        this.x += x;
        this.y += y;
    }

    public void moveAgainTo(double x, double y) {
        moveAgain(x - this.x, y - this.y);
    }

    /**
     * Change the displacement vector to have endpoint @param currentEN
     * starting point is  startEN
     */
    public void applyVectorTo(EastNorth currentEN) {
        if (startEN == null)
            return;
        x = currentEN.getX() - startEN.getX();
        y = currentEN.getY() - startEN.getY();
        updateCoordinates();
    }

    /**
     * Changes base point of movement
     * @param newDraggedStartPoint - new starting point after movement (where user clicks to start new drag)
     */
    public void changeStartPoint(EastNorth newDraggedStartPoint) {
        startEN = new EastNorth(newDraggedStartPoint.getX()-x, newDraggedStartPoint.getY()-y);
    }

    /**
     * Save curent displacement to restore in case of some problems
     */
    public void saveCheckpoint() {
        backupX = x;
        backupY = y;
    }

    /**
     * Restore old displacement in case of some problems
     */
    public void resetToCheckpoint() {
        x = backupX;
        y = backupY;
        updateCoordinates();
    }

    private void updateCoordinates() {
        Iterator<OldNodeState> it = oldState.iterator();
        for (Node n : nodes) {
            OldNodeState os = it.next();
            if (os.eastNorth != null) {
                n.setEastNorth(os.eastNorth.add(x, y));
            }
        }
    }

    @Override public boolean executeCommand() {
        for (Node n : nodes) {
            // in case #3892 happens again
            if (n == null)
                throw new AssertionError("null detected in node list");
            EastNorth en = n.getEastNorth();
            if (en != null) {
                n.setEastNorth(en.add(x, y));
                n.setModified(true);
            }
        }
        return true;
    }

    @Override public void undoCommand() {
        Iterator<OldNodeState> it = oldState.iterator();
        for (Node n : nodes) {
            OldNodeState os = it.next();
            n.setCoor(os.latlon);
            n.setModified(os.modified);
        }
    }

    @Override public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
        for (OsmPrimitive osm : nodes) {
            modified.add(osm);
        }
    }

    @Override
    public String getDescriptionText() {
        return trn("Move {0} node", "Move {0} nodes", nodes.size(), nodes.size());
    }

    @Override
    public Icon getDescriptionIcon() {
        return ImageProvider.get("data", "node");
    }

    @Override
    public Collection<Node> getParticipatingPrimitives() {
        return nodes;
    }
}
