// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.command;

import static org.openstreetmap.josm.tools.I18n.trn;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JLabel;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * Abstract class with common services for nodes rotation and scaling commands.
 *
 * @author Olivier Croquette <ocroquette@free.fr>
 */
public abstract class TransformNodesCommand extends Command {

    /**
     * The nodes to transform.
     */
    protected Collection<Node> nodes = new LinkedList<Node>();

    /**
     * Small helper for holding the interesting part of the old data state of the
     * nodes.
     */
    public static class OldState {
        LatLon latlon;
        EastNorth eastNorth;
        boolean modified;
    }

    /**
     * List of all old states of the nodes.
     */
    protected Map<Node, OldState> oldStates = new HashMap<Node, OldState>();

    /**
     * Stores the state of the nodes before the command.
     */
    protected void storeOldState() {
        for (Node n : this.nodes) {
            OldState os = new OldState();
            os.latlon = new LatLon(n.getCoor());
            os.eastNorth = n.getEastNorth();
            os.modified = n.isModified();
            oldStates.put(n, os);
        }
    }

    /**
     * Creates a TransformNodesObject.
     * Find out the impacted nodes and store their initial state.
     */
    public TransformNodesCommand(Collection<OsmPrimitive> objects) {
        this.nodes = AllNodesVisitor.getAllNodes(objects);
        storeOldState();
    }

    /**
     * Handling of a mouse event (e.g. dragging event).
     * @param currentEN the current world position of the mouse
     */
    public abstract void handleEvent(EastNorth currentEN);

    /**
     * Implementation for the nodes transformation.
     * No parameters are given here, you should handle the user input in handleEvent()
     * and store it internally.
     */
    protected abstract void transformNodes();

    /**
     * Finally apply the transformation of the nodes.
     * This is called when the user is happy with the current state of the command
     * and its effects.
     */
    @Override
    public boolean executeCommand() {
        transformNodes();
        flagNodesAsModified();
        return true;
    }

    /**
     * Flag all nodes as modified.
     */
    public void flagNodesAsModified() {
        for (Node n : nodes) {
            n.setModified(true);
        }
    }

    /**
     * Restore the state of the nodes from the backup.
     */
    @Override
    public void undoCommand() {
        for (Node n : nodes) {
            OldState os = oldStates.get(n);
            n.setCoor(os.latlon);
            n.setModified(os.modified);
        }
    }

    @Override
    public void fillModifiedData(Collection<OsmPrimitive> modified, Collection<OsmPrimitive> deleted, Collection<OsmPrimitive> added) {
    }

    @Override
    public Collection<? extends OsmPrimitive> getParticipatingPrimitives() {
        return nodes;
    }

    @Override
    public JLabel getDescription() {
        return new JLabel(trn("Transform {0} node", "Transform {0} nodes", nodes.size(), nodes.size()), ImageProvider.get("data", "node"), JLabel.HORIZONTAL);
    }

    /**
     * Get the nodes with the current transformation applied.
     */
    public Collection<Node> getTransformedNodes() {
        return nodes;
    }

    /**
     * Get the center of the nodes under modification.
     * It's just the barycenter.
     */
    public EastNorth getNodesCenter() {
        EastNorth sum = new EastNorth(0,0);

        for (Node n : nodes ) {
            EastNorth en = n.getEastNorth();
            sum = sum.add(en.east(), en.north());
        }
        return new EastNorth(sum.east()/this.nodes.size(), sum.north()/this.nodes.size());

    }
}
