// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.PolarCoor;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Aligns all selected nodes into a straight line (useful for roads that should be straight, but have side roads and
 * therefore need multiple nodes)
 *
 * <pre>
 * Case 1: 1 or 2 ways selected and no nodes selected: align nodes of ways taking care of intersection.
 * Case 2: Single node selected and no ways selected: align this node relative to all referrer ways (2 at most).
 * Case 3: Single node and ways selected: align this node relative to selected ways.
 * Case 4.1: Only nodes selected, part of a non-closed way: align these nodes on the line passing through the
 *   extremity nodes (most distant in the way sequence). See https://josm.openstreetmap.de/ticket/9605#comment:3
 * Case 4.2: Only nodes selected, part of a closed way: align these nodes on the line passing through the most distant nodes.
 * Case 4.3: Only nodes selected, part of multiple ways: align these nodes on the line passing through the most distant nodes.
 * </pre>
 *
 * @author Matthew Newton
 */
public final class AlignInLineAction extends JosmAction {

    /**
     * Constructs a new {@code AlignInLineAction}.
     */
    public AlignInLineAction() {
        super(tr("Align Nodes in Line"), "alignline", tr("Move the selected nodes in to a line."),
                Shortcut.registerShortcut("tools:alignline", tr("Tool: {0}", tr("Align Nodes in Line")), KeyEvent.VK_L, Shortcut.DIRECT), true);
        putValue("help", ht("/Action/AlignInLine"));
    }

    /**
     * InvalidSelection exception has to be raised when action can't be performed
     */
    static class InvalidSelection extends Exception {

        /**
         * Create an InvalidSelection exception with default message
         */
        InvalidSelection() {
            super(tr("Please select at least three nodes."));
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
     * Return 2 nodes making up the line along which provided nodes must be aligned.
     *
     * @param nodes Nodes to be aligned.
     * @return A array of two nodes.
     * @throws IllegalArgumentException if nodes is empty
     */
    private static Node[] nodePairFurthestApart(List<Node> nodes) {
        // Detect if selected nodes are on the same way.

        // Get ways passing though all selected nodes.
        Set<Way> waysRef = null;
        for (Node n: nodes) {
            Collection<Way> ref = n.getParentWays();
            if (waysRef == null)
                waysRef = new HashSet<>(ref);
            else
                waysRef.retainAll(ref);
        }

        if (waysRef == null) {
            throw new IllegalArgumentException();
        }

        // Nodes belongs to multiple ways, return most distant nodes.
        if (waysRef.size() != 1)
            return nodeFurthestAppart(nodes);

        // All nodes are part of the same way. See #9605.
        Way way = waysRef.iterator().next();

        if (way.isClosed()) {
            // Align these nodes on the line passing through the most distant nodes.
            return nodeFurthestAppart(nodes);
        }

        Node nodea = null;
        Node nodeb = null;

        // The way is open, align nodes on the line passing through the extremity nodes (most distant in the way
        // sequence). See #9605#comment:3.
        Set<Node> remainNodes = new HashSet<>(nodes);
        for (Node n : way.getNodes()) {
            if (!remainNodes.contains(n))
                continue;
            if (nodea == null)
                nodea = n;
            if (remainNodes.size() == 1) {
                nodeb = remainNodes.iterator().next();
                break;
            }
            remainNodes.remove(n);
        }

        return new Node[] {nodea, nodeb};
    }

    /**
     * Return the two nodes the most distant from the provided list.
     *
     * @param nodes List of nodes to analyze.
     * @return An array containing the two most distant nodes.
     */
    private static Node[] nodeFurthestAppart(List<Node> nodes) {
        Node node1 = null, node2 = null;
        double minSqDistance = 0;
        int nb;

        nb = nodes.size();
        for (int i = 0; i < nb - 1; i++) {
            Node n = nodes.get(i);
            for (int j = i + 1; j < nb; j++) {
                Node m = nodes.get(j);
                double sqDist = n.getEastNorth().distanceSq(m.getEastNorth());
                if (sqDist > minSqDistance) {
                    node1 = n;
                    node2 = m;
                    minSqDistance = sqDist;
                }
            }
        }

        return new Node[] {node1, node2};
    }

    /**
     * Operation depends on the selected objects:
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        try {
            MainApplication.undoRedo.add(buildCommand());
        } catch (InvalidSelection except) {
            Logging.debug(except);
            new Notification(except.getMessage())
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
        }
    }

    /**
     * Builds "align in line" command depending on the selected objects.
     * @return the resulting command to execute to perform action
     * @throws InvalidSelection if a polygon is selected, or if a node is used by 3 or more ways
     * @since 12562
     */
    public Command buildCommand() throws InvalidSelection {
        DataSet ds = getLayerManager().getEditDataSet();
        List<Node> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
        List<Way> selectedWays = new ArrayList<>(ds.getSelectedWays());
        selectedWays.removeIf(OsmPrimitive::isIncomplete);

        // Decide what to align based on selection:
        if (selectedNodes.isEmpty() && !selectedWays.isEmpty()) {
            // Only ways selected -> For each way align their nodes taking care of intersection
            return alignMultiWay(selectedWays);
        } else if (selectedNodes.size() == 1) {
            // Only 1 node selected -> align this node relative to referers way
            Node selectedNode = selectedNodes.get(0);
            List<Way> involvedWays;
            if (selectedWays.isEmpty())
                // No selected way, all way containing this node are used
                involvedWays = selectedNode.getParentWays();
            else
                // Selected way, use only these ways
                involvedWays = selectedWays;
            List<Line> lines = getInvolvedLines(selectedNode, involvedWays);
            if (lines.size() > 2 || lines.isEmpty())
                throw new InvalidSelection();
            return alignSingleNode(selectedNodes.get(0), lines);
        } else if (selectedNodes.size() >= 3) {
            // More than 3 nodes and way(s) selected -> align selected nodes. Don't care of way(s).
            return alignOnlyNodes(selectedNodes);
        } else {
            // All others cases are invalid
            throw new InvalidSelection();
        }
    }

    /**
     * Align nodes in case 3 or more nodes are selected.
     *
     * @param nodes Nodes to be aligned.
     * @return Command that perform action.
     * @throws InvalidSelection If the nodes have same coordinates.
     */
    private static Command alignOnlyNodes(List<Node> nodes) throws InvalidSelection {
        // Choose nodes used as anchor points for projection.
        Node[] anchors = nodePairFurthestApart(nodes);
        Collection<Command> cmds = new ArrayList<>(nodes.size());
        Line line = new Line(anchors[0], anchors[1]);
        for (Node node: nodes) {
            if (node != anchors[0] && node != anchors[1])
                cmds.add(line.projectionCommand(node));
        }
        return new SequenceCommand(tr("Align Nodes in Line"), cmds);
    }

    /**
     * Align way in case of multiple way #6819
     * @param ways Collection of way to align
     * @return Command that perform action
     * @throws InvalidSelection if a polygon is selected, or if a node is used by 3 or more ways
     */
    private static Command alignMultiWay(Collection<Way> ways) throws InvalidSelection {
        // Collect all nodes and compute line equation
        Set<Node> nodes = new HashSet<>();
        Map<Way, Line> lines = new HashMap<>();
        for (Way w: ways) {
            if (w.isClosed())
                throw new InvalidSelection(tr("Can not align a polygon. Abort."));
            nodes.addAll(w.getNodes());
            lines.put(w, new Line(w));
        }
        if (nodes.isEmpty()) {
            throw new InvalidSelection(tr("Intersection of three or more ways can not be solved. Abort."));
        }
        Collection<Command> cmds = new ArrayList<>(nodes.size());
        List<Way> referers = new ArrayList<>(ways.size());
        for (Node n: nodes) {
            referers.clear();
            for (OsmPrimitive o: n.getReferrers()) {
                if (ways.contains(o))
                    referers.add((Way) o);
            }
            if (referers.size() == 1) {
                Way way = referers.get(0);
                if (way.isFirstLastNode(n)) continue;
                cmds.add(lines.get(way).projectionCommand(n));
            } else if (referers.size() == 2) {
                cmds.add(lines.get(referers.get(0)).intersectionCommand(n, lines.get(referers.get(1))));
            } else
                throw new InvalidSelection(tr("Intersection of three or more ways can not be solved. Abort."));
        }
        return new SequenceCommand(tr("Align Nodes in Line"), cmds);
    }

    /**
     * Get lines useful to do alignment of a single node
     * @param node Node to be aligned
     * @param refWays Ways where useful lines will be searched
     * @return List of useful lines
     * @throws InvalidSelection if a node got more than 4 neighbours (self-crossing way)
     */
    private static List<Line> getInvolvedLines(Node node, List<Way> refWays) throws InvalidSelection {
        List<Line> lines = new ArrayList<>();
        List<Node> neighbors = new ArrayList<>();
        for (Way way: refWays) {
            List<Node> nodes = way.getNodes();
            neighbors.clear();
            for (int i = 1; i < nodes.size()-1; i++) {
                if (nodes.get(i) == node) {
                    neighbors.add(nodes.get(i-1));
                    neighbors.add(nodes.get(i+1));
                }
            }
            if (neighbors.isEmpty())
                continue;
            else if (neighbors.size() == 2)
                // Non self crossing
                lines.add(new Line(neighbors.get(0), neighbors.get(1)));
            else if (neighbors.size() == 4) {
                // Self crossing, have to make 2 lines with 4 neighbors
                // see #9081 comment 6
                EastNorth c = node.getEastNorth();
                double[] angle = new double[4];
                for (int i = 0; i < 4; i++) {
                    angle[i] = PolarCoor.computeAngle(neighbors.get(i).getEastNorth(), c);
                }
                double[] deltaAngle = new double[3];
                for (int i = 0; i < 3; i++) {
                    deltaAngle[i] = angle[i+1] - angle[0];
                    if (deltaAngle[i] < 0)
                        deltaAngle[i] += 2*Math.PI;
                }
                int nb = 0;
                if (deltaAngle[1] < deltaAngle[0]) nb++;
                if (deltaAngle[2] < deltaAngle[0]) nb++;
                if (nb == 1) {
                    // Align along [neighbors[0], neighbors[1]] and [neighbors[0], neighbors[2]]
                    lines.add(new Line(neighbors.get(0), neighbors.get(1)));
                    lines.add(new Line(neighbors.get(2), neighbors.get(3)));
                } else {
                    // Align along [neighbors[0], neighbors[2]] and [neighbors[1], neighbors[3]]
                    lines.add(new Line(neighbors.get(0), neighbors.get(2)));
                    lines.add(new Line(neighbors.get(1), neighbors.get(3)));
                }
            } else
                throw new InvalidSelection("cannot treat more than 4 neighbours, got "+neighbors.size());
        }
        return lines;
    }

    /**
     * Align a single node relative to a set of lines #9081
     * @param node Node to be aligned
     * @param lines Lines to align node on
     * @return Command that perform action
     * @throws InvalidSelection if more than 2 lines
     */
    private static Command alignSingleNode(Node node, List<Line> lines) throws InvalidSelection {
        if (lines.size() == 1)
            return lines.get(0).projectionCommand(node);
        else if (lines.size() == 2)
            return lines.get(0).intersectionCommand(node, lines.get(1));
        throw new InvalidSelection();
    }

    /**
     * Class that represent a line
     */
    static class Line {

        /**
         * Line equation ax + by + c = 0
         * Such as a^2 + b^2 = 1, ie (-b, a) is a unit vector of line
         */
        private double a, b, c;
        /**
         * (xM, yM) are coordinates of a point of the line
         */
        private double xM, yM;

        /**
         * Init a line by 2 nodes.
         * @param first One point of the line
         * @param last Other point of the line
         * @throws InvalidSelection if nodes have same coordinates
         */
        Line(Node first, Node last) throws InvalidSelection {
            xM = first.getEastNorth().getX();
            yM = first.getEastNorth().getY();
            double xB = last.getEastNorth().getX();
            double yB = last.getEastNorth().getY();
            a = yB - yM;
            b = xM - xB;
            double norm = Math.sqrt(a*a + b*b);
            if (norm == 0)
                throw new InvalidSelection("Nodes have same coordinates!");
            a /= norm;
            b /= norm;
            c = -(a*xM + b*yM);
        }

        /**
         * Init a line equation from a way.
         * @param way Use extremity of this way to compute line equation
         * @throws InvalidSelection if nodes have same coordinates
         */
        Line(Way way) throws InvalidSelection {
            this(way.firstNode(), way.lastNode());
        }

        /**
         * Orthogonal projection of a node N along this line.
         * @param n Node to be projected
         * @return The command that do the projection of this node
         */
        public Command projectionCommand(Node n) {
            double s = (xM - n.getEastNorth().getX()) * a + (yM - n.getEastNorth().getY()) * b;
            return new MoveCommand(n, a*s, b*s);
        }

        /**
         * Intersection of two line.
         * @param n Node to move to the intersection
         * @param other Second line for intersection
         * @return The command that move the node
         * @throws InvalidSelection if two parallels ways found
         */
        public Command intersectionCommand(Node n, Line other) throws InvalidSelection {
            double d = this.a * other.b - other.a * this.b;
            if (Math.abs(d) < 10e-6)
                // parallels lines
                throw new InvalidSelection(tr("Two parallels ways found. Abort."));
            double x = (this.b * other.c - other.b * this.c) / d;
            double y = (other.a * this.c - this.a * other.c) / d;
            return new MoveCommand(n, x - n.getEastNorth().getX(), y - n.getEastNorth().getY());
        }
    }

    @Override
    protected void updateEnabledState() {
        DataSet ds = getLayerManager().getEditDataSet();
        setEnabled(ds != null && !ds.selectionEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
