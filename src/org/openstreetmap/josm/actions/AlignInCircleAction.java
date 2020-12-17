// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.UndoRedoHandler;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.PolarCoor;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon;
import org.openstreetmap.josm.data.osm.visitor.paint.relations.Multipolygon.JoinedWay;
import org.openstreetmap.josm.data.validation.tests.CrossingWays;
import org.openstreetmap.josm.data.validation.tests.SelfIntersectingWay;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Aligns all selected nodes within a circle. (Useful for roundabouts)
 *
 * @author Matthew Newton
 * @author Petr Dlouhý
 * @author Teemu Koskinen
 * @author Alain Delplanque
 * @author Gerd Petermann
 *
 * @since 146
 */
public final class AlignInCircleAction extends JosmAction {

    /**
     * Constructs a new {@code AlignInCircleAction}.
     */
    public AlignInCircleAction() {
        super(tr("Align Nodes in Circle"), "aligncircle", tr("Move the selected nodes into a circle."),
                Shortcut.registerShortcut("tools:aligncircle", tr("Tools: {0}", tr("Align Nodes in Circle")),
                        KeyEvent.VK_O, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/AlignInCircle"));
    }

    /**
     * InvalidSelection exception has to be raised when action can't be performed
     */
    public static class InvalidSelection extends Exception {

        /**
         * Create an InvalidSelection exception with default message
         */
        InvalidSelection() {
            super(tr("Selection could not be used to align in circle."));
        }

        /**
         * Create an InvalidSelection exception with specific message
         * @param msg Message that will be displayed to the user
         */
        InvalidSelection(String msg) {
            super(msg);
        }
    }

    /**
     * Add a {@link MoveCommand} to move a node to a PolarCoor if there is a significant move.
     * @param n Node to move
     * @param coor polar coordinate where to move the node
     * @param cmds list of commands
     * @since 17386
     */
    public static void addMoveCommandIfNeeded(Node n, PolarCoor coor, List<Command> cmds) {
        EastNorth en = coor.toEastNorth();
        double deltaEast = en.east() - n.getEastNorth().east();
        double deltaNorth = en.north() - n.getEastNorth().north();

        if (Math.abs(deltaEast) > 5e-6 || Math.abs(deltaNorth) > 5e-6) {
            cmds.add(new MoveCommand(n, deltaEast, deltaNorth));
        }
    }

    /**
     * Perform AlignInCircle action.
     *
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        try {
            Command cmd = buildCommand(getLayerManager().getEditDataSet());
            if (cmd != null)
                UndoRedoHandler.getInstance().add(cmd);
            else {
                new Notification(tr("Nothing changed"))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .show();

            }
        } catch (InvalidSelection except) {
            Logging.debug(except);
            new Notification(except.getMessage())
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .show();
        }
    }

    /**
     * Builds "align in circle" command depending on the selected objects.
     * A fixed node is a node for which it is forbidden to change the angle relative to center of the circle.
     * All other nodes are uniformly distributed.
     * <p>
     * Case 1: One unclosed way.
     * --&gt; allow action, and align selected way nodes
     * If nodes contained by this way are selected, there are fix.
     * If nodes outside from the way are selected there are ignored.
     * <p>
     * Case 2: One or more ways are selected and can be joined into a polygon
     * --&gt; allow action, and align selected ways nodes
     * If 1 node outside of way is selected, it became center
     * If 1 node outside and 1 node inside are selected there define center and radius
     * If no outside node and 2 inside nodes are selected those 2 nodes define diameter
     * In all other cases outside nodes are ignored
     * In all cases, selected nodes are fix, nodes with more than one referrers are fix
     * (first referrer is the selected way)
     * <p>
     * Case 3: Only nodes are selected
     * --&gt; Align these nodes, all are fix
     * @param ds data set in which the command operates
     * @return the resulting command to execute to perform action, or null if nothing was changed
     * @throws InvalidSelection if selection cannot be used
     * @since 17386
     *
     */
    public static Command buildCommand(DataSet ds) throws InvalidSelection {
        Collection<OsmPrimitive> sel = ds.getSelected();
        List<Node> selectedNodes = new LinkedList<>();
        // fixNodes: All nodes for which the angle relative to center should not be modified
        Set<Node> fixNodes = new HashSet<>();
        List<Way> ways = new LinkedList<>();
        EastNorth center = null;
        double radius = 0;

        for (OsmPrimitive osm : sel) {
            if (osm instanceof Node) {
                selectedNodes.add((Node) osm);
            } else if (osm instanceof Way) {
                ways.add((Way) osm);
            }
        }

        // nodes on selected ways
        List<Node> onWay = new ArrayList<>();
        if (!ways.isEmpty()) {
            List<Node> potentialCenter = new ArrayList<>();
            for (Node n : selectedNodes) {
                if (ways.stream().anyMatch(w -> w.containsNode(n))) {
                    onWay.add(n);
                } else {
                    potentialCenter.add(n);
                }
            }
            if (potentialCenter.size() == 1) {
                // center is given
                center = potentialCenter.get(0).getEastNorth();
                if (onWay.size() == 1) {
                    radius = center.distance(onWay.get(0).getEastNorth());
                }
            } else if (potentialCenter.size() > 1) {
                throw new InvalidSelection(tr("Please select only one node as center."));
            }

        }

        final List<Node> nodes;
        if (ways.isEmpty()) {
            nodes = sortByAngle(selectedNodes);
            fixNodes.addAll(nodes);
        } else if (ways.size() == 1 && !ways.get(0).isClosed()) {
            // Case 1
            Way w = ways.get(0);
            fixNodes.add(w.firstNode());
            fixNodes.add(w.lastNode());
            fixNodes.addAll(onWay);
            // Temporary closed way used to reorder nodes
            Way closedWay = new Way(w);
            try {
                closedWay.addNode(w.firstNode());
                nodes = collectNodesAnticlockwise(Collections.singletonList(closedWay));
            } finally {
                closedWay.setNodes(null); // see #19885
            }
        } else if (Multipolygon.joinWays(ways).size() == 1) {
            // Case 2:
            if (onWay.size() == 2) {
                // 2 way nodes define diameter
                EastNorth en0 = onWay.get(0).getEastNorth();
                EastNorth en1 = onWay.get(1).getEastNorth();
                radius = en0.distance(en1) / 2;
                if (center == null) {
                    center = en0.getCenter(en1);
                }
            }
            fixNodes.addAll(onWay);
            nodes = collectNodesAnticlockwise(ways);
        } else {
            throw new InvalidSelection();
        }
        fixNodes.addAll(collectNodesWithExternReferrers(ways));

        // Check if one or more nodes are outside of download area
        if (nodes.stream().anyMatch(Node::isOutsideDownloadArea))
            throw new InvalidSelection(tr("One or more nodes involved in this action is outside of the downloaded area."));


        if (center == null) {
            if (nodes.size() < 4) {
                throw new InvalidSelection(tr("Not enough nodes to calculate center."));
            }
            if (validateGeometry(nodes)) {
                // Compute the center of nodes
                center = Geometry.getCenter(nodes);
            }
            if (center == null) {
                throw new InvalidSelection(tr("Cannot determine center of circle for this geometry."));
            }
        }

        // Now calculate the average distance to each node from the
        // center. This method is ok as long as distances are short
        // relative to the distance from the N or S poles.
        if (radius == 0) {
            for (Node n : nodes) {
                radius += center.distance(n.getEastNorth());
            }
            radius = radius / nodes.size();
        }

        List<Command> cmds = new LinkedList<>();

        // Move each node to that distance from the center.
        // Nodes that are not "fix" will be adjust making regular arcs.
        int nodeCount = nodes.size();
        // Search first fixed node
        int startPosition;
        for (startPosition = 0; startPosition < nodeCount; startPosition++) {
            if (fixNodes.contains(nodes.get(startPosition % nodeCount)))
                break;
        }
        int i = startPosition; // Start position for current arc
        int j; // End position for current arc
        while (i < startPosition + nodeCount) {
            for (j = i + 1; j < startPosition + nodeCount; j++) {
                if (fixNodes.contains(nodes.get(j % nodeCount)))
                    break;
            }
            Node first = nodes.get(i % nodeCount);
            PolarCoor pcFirst = new PolarCoor(radius, PolarCoor.computeAngle(first.getEastNorth(), center), center);
            addMoveCommandIfNeeded(first, pcFirst, cmds);
            if (j > i + 1) {
                double delta;
                if (j == i + nodeCount) {
                    delta = 2 * Math.PI / nodeCount;
                } else {
                    PolarCoor pcLast = new PolarCoor(nodes.get(j % nodeCount).getEastNorth(), center);
                    delta = pcLast.angle - pcFirst.angle;
                    if (delta < 0) // Assume each PolarCoor.angle is in range ]-pi; pi]
                        delta += 2*Math.PI;
                    delta /= j - i;
                }
                for (int k = i+1; k < j; k++) {
                    PolarCoor p = new PolarCoor(radius, pcFirst.angle + (k-i)*delta, center);
                    addMoveCommandIfNeeded(nodes.get(k % nodeCount), p, cmds);
                }
            }
            i = j; // Update start point for next iteration
        }
        if (cmds.isEmpty())
            return null;
        return new SequenceCommand(tr("Align Nodes in Circle"), cmds);
    }

    private static List<Node> sortByAngle(final List<Node> nodes) {
        EastNorth sum = new EastNorth(0, 0);
        for (Node n : nodes) {
            EastNorth en = n.getEastNorth();
            sum = sum.add(en.east(), en.north());
        }
        final EastNorth simpleCenter = new EastNorth(sum.east()/nodes.size(), sum.north()/nodes.size());

        SortedMap<Double, List<Node>> orderedMap = new TreeMap<>();
        for (Node n : nodes) {
            double angle = new PolarCoor(n.getEastNorth(), simpleCenter).angle;
            orderedMap.computeIfAbsent(angle, k-> new ArrayList<>()).add(n);
        }
        return orderedMap.values().stream().flatMap(List<Node>::stream).collect(Collectors.toList());
    }

    private static boolean validateGeometry(List<Node> nodes) {
        Way test = new Way();
        test.setNodes(nodes);
        if (!test.isClosed()) {
            test.addNode(test.firstNode());
        }

        try {
            if (CrossingWays.isSelfCrossing(test))
                return false;
            return !SelfIntersectingWay.isSelfIntersecting(test);
        } finally {
            test.setNodes(null); // see #19855
        }
    }

    /**
     * Collect all nodes with more than one referrer.
     * @param ways Ways from witch nodes are selected
     * @return List of nodes with more than one referrer
     */
    private static List<Node> collectNodesWithExternReferrers(List<Way> ways) {
        return ways.stream().flatMap(w -> w.getNodes().stream()).filter(n -> n.getReferrers().size() > 1).collect(Collectors.toList());
    }

    /**
     * Assuming all ways can be joined into polygon, create an ordered list of node.
     * @param ways List of ways to be joined
     * @return Nodes anticlockwise ordered
     * @throws InvalidSelection if selection cannot be used
     */
    private static List<Node> collectNodesAnticlockwise(List<Way> ways) throws InvalidSelection {
        Collection<JoinedWay> rings = Multipolygon.joinWays(ways);
        if (rings.size() != 1)
            throw new InvalidSelection(); // we should never get here
        List<Node> nodes = new ArrayList<>(rings.iterator().next().getNodes());
        if (nodes.get(0) != nodes.get(nodes.size() - 1))
            throw new InvalidSelection();
        if (Geometry.isClockwise(nodes))
            Collections.reverse(nodes);
        nodes.remove(nodes.size() - 1);
        return nodes;
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        setEnabled(ds != null && !ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        updateEnabledStateOnModifiableSelection(selection);
    }
}
