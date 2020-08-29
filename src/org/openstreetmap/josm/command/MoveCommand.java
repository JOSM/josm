// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

import javax.swing.Icon;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
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
    private Collection<Node> nodes = new LinkedList<>();
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
    private final List<OldNodeState> oldState = new LinkedList<>();

    /**
     * Constructs a new {@code MoveCommand} to move a primitive.
     * @param osm The primitive to move
     * @param x X difference movement. Coordinates are in northern/eastern
     * @param y Y difference movement. Coordinates are in northern/eastern
     */
    public MoveCommand(OsmPrimitive osm, double x, double y) {
        this(Collections.singleton(osm), x, y);
    }

    /**
     * Constructs a new {@code MoveCommand} to move a node.
     * @param node The node to move
     * @param position The new location (lat/lon)
     */
    public MoveCommand(Node node, LatLon position) {
        this(Collections.singleton((OsmPrimitive) node),
                ProjectionRegistry.getProjection().latlon2eastNorth(position).subtract(node.getEastNorth()));
    }

    /**
     * Constructs a new {@code MoveCommand} to move a collection of primitives.
     * @param objects The primitives to move
     * @param offset The movement vector
     */
    public MoveCommand(Collection<OsmPrimitive> objects, EastNorth offset) {
        this(objects, offset.getX(), offset.getY());
    }

    /**
     * Constructs a new {@code MoveCommand} and assign the initial object set and movement vector.
     * @param objects The primitives to move. Must neither be null nor empty. Objects must belong to a data set
     * @param x X difference movement. Coordinates are in northern/eastern
     * @param y Y difference movement. Coordinates are in northern/eastern
     * @throws NullPointerException if objects is null or contain null item
     * @throws NoSuchElementException if objects is empty
     */
    public MoveCommand(Collection<OsmPrimitive> objects, double x, double y) {
        this(objects.iterator().next().getDataSet(), objects, x, y);
    }

    /**
     * Constructs a new {@code MoveCommand} and assign the initial object set and movement vector.
     * @param ds the dataset context for moving these primitives. Must not be null.
     * @param objects The primitives to move. Must neither be null.
     * @param x X difference movement. Coordinates are in northern/eastern
     * @param y Y difference movement. Coordinates are in northern/eastern
     * @throws NullPointerException if objects is null or contain null item
     * @throws NoSuchElementException if objects is empty
     * @since 12759
     */
    public MoveCommand(DataSet ds, Collection<OsmPrimitive> objects, double x, double y) {
        super(ds);
        startEN = null;
        saveCheckpoint(); // (0,0) displacement will be saved
        this.x = x;
        this.y = y;
        Objects.requireNonNull(objects, "objects");
        this.nodes = AllNodesVisitor.getAllNodes(objects);
        for (Node n : this.nodes) {
            oldState.add(new OldNodeState(n));
        }
    }

    /**
     * Constructs a new {@code MoveCommand} to move a collection of primitives.
     * @param ds the dataset context for moving these primitives. Must not be null.
     * @param objects The primitives to move
     * @param start The starting position (northern/eastern)
     * @param end The ending position (northern/eastern)
     * @since 12759
     */
    public MoveCommand(DataSet ds, Collection<OsmPrimitive> objects, EastNorth start, EastNorth end) {
        this(Objects.requireNonNull(ds, "ds"),
             Objects.requireNonNull(objects, "objects"),
             Objects.requireNonNull(end, "end").getX() - Objects.requireNonNull(start, "start").getX(),
             Objects.requireNonNull(end, "end").getY() - Objects.requireNonNull(start, "start").getY());
        startEN = start;
    }

    /**
     * Constructs a new {@code MoveCommand} to move a collection of primitives.
     * @param objects The primitives to move
     * @param start The starting position (northern/eastern)
     * @param end The ending position (northern/eastern)
     */
    public MoveCommand(Collection<OsmPrimitive> objects, EastNorth start, EastNorth end) {
        this(Objects.requireNonNull(objects, "objects").iterator().next().getDataSet(), objects, start, end);
    }

    /**
     * Constructs a new {@code MoveCommand} to move a primitive.
     * @param ds the dataset context for moving these primitives. Must not be null.
     * @param p The primitive to move
     * @param start The starting position (northern/eastern)
     * @param end The ending position (northern/eastern)
     * @since 12759
     */
    public MoveCommand(DataSet ds, OsmPrimitive p, EastNorth start, EastNorth end) {
        this(ds, Collections.singleton(Objects.requireNonNull(p, "p")), start, end);
    }

    /**
     * Constructs a new {@code MoveCommand} to move a primitive.
     * @param p The primitive to move
     * @param start The starting position (northern/eastern)
     * @param end The ending position (northern/eastern)
     */
    public MoveCommand(OsmPrimitive p, EastNorth start, EastNorth end) {
        this(Collections.singleton(Objects.requireNonNull(p, "p")), start, end);
    }

    /**
     * Move the same set of objects again by the specified vector. The vectors
     * are added together and so the resulting will be moved to the previous
     * vector plus this one.
     *
     * The move is immediately executed and any undo will undo both vectors to
     * the original position the objects had before first moving.
     *
     * @param x X difference movement. Coordinates are in northern/eastern
     * @param y Y difference movement. Coordinates are in northern/eastern
     */
    public void moveAgain(double x, double y) {
        for (Node n : nodes) {
            EastNorth eastNorth = n.getEastNorth();
            if (eastNorth != null) {
                n.setEastNorth(eastNorth.add(x, y));
            }
        }
        this.x += x;
        this.y += y;
    }

    /**
     * Move again to the specified coordinates.
     * @param x X coordinate
     * @param y Y coordinate
     * @see #moveAgain
     */
    public void moveAgainTo(double x, double y) {
        moveAgain(x - this.x, y - this.y);
    }

    /**
     * Change the displacement vector to have endpoint {@code currentEN}.
     * starting point is startEN
     * @param currentEN the new endpoint
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
     * Save current displacement to restore in case of some problems
     */
    public final void saveCheckpoint() {
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
            if (os.getEastNorth() != null) {
                n.setEastNorth(os.getEastNorth().add(x, y));
            }
        }
    }

    @Override
    public boolean executeCommand() {
        ensurePrimitivesAreInDataset();

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

    @Override
    public void undoCommand() {
        ensurePrimitivesAreInDataset();

        Iterator<OldNodeState> it = oldState.iterator();
        for (Node n : nodes) {
            OldNodeState os = it.next();
            n.setCoor(os.getLatLon());
            n.setModified(os.isModified());
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
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

    /**
     * Gets the current move offset.
     * @return The current move offset.
     */
    protected EastNorth getOffset() {
        return new EastNorth(x, y);
    }

    /**
     * Computes the move distance for one node matching the specified predicate
     * @param predicate predicate to match
     * @return distance in metres
     */
    public double getDistance(Predicate<Node> predicate) {
        return nodes.stream()
                .filter(predicate)
                .filter(node -> node.getCoor() != null && node.getEastNorth() != null)
                .findFirst()
                .map(node -> {
                    final Node old = new Node(node);
                    old.setEastNorth(old.getEastNorth().add(-x, -y));
                    return node.getCoor().greatCircleDistance(old.getCoor());
                }).orElse(Double.NaN);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), nodes, startEN, x, y, backupX, backupY, oldState);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        if (!super.equals(obj)) return false;
        MoveCommand that = (MoveCommand) obj;
        return Double.compare(that.x, x) == 0 &&
                Double.compare(that.y, y) == 0 &&
                Double.compare(that.backupX, backupX) == 0 &&
                Double.compare(that.backupY, backupY) == 0 &&
                Objects.equals(nodes, that.nodes) &&
                Objects.equals(startEN, that.startEN) &&
                Objects.equals(oldState, that.oldState);
    }
}
