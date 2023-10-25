// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.Geometry.nodePairFurthestApart;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
                Shortcut.registerShortcut("tools:alignline", tr("Tools: {0}", tr("Align Nodes in Line")), KeyEvent.VK_L, Shortcut.DIRECT), true);
        setHelpId(ht("/Action/AlignInLine"));
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
     * Operation depends on the selected objects:
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        try {
            Command cmd = buildCommand(getLayerManager().getEditDataSet());
            if (cmd != null) {
                UndoRedoHandler.getInstance().add(cmd);
            }
        } catch (InvalidSelection except) {
            Logging.debug(except);
            new Notification(except.getMessage())
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
        }
    }

    /**
     * Builds "align in line" command depending on the selected objects.
     * @param ds data set in which the command operates
     * @return the resulting command to execute to perform action
     * @throws InvalidSelection if a polygon is selected, or if a node is used by three or more ways
     * @since 13108
     */
    public Command buildCommand(DataSet ds) throws InvalidSelection {
        List<Node> selectedNodes = new ArrayList<>(ds.getSelectedNodes());
        List<Way> selectedWays = new ArrayList<>(ds.getSelectedWays());
        selectedWays.removeIf(w -> w.isIncomplete() || w.isEmpty());

        // Decide what to align based on selection:
        if (selectedNodes.isEmpty() && !selectedWays.isEmpty()) {
            // Only ways selected -> For each way align their nodes taking care of intersection
            return alignMultiWay(selectedWays);
        } else if (selectedNodes.size() == 1) {
            // Only 1 node selected -> align this node relative to referrers way
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
     * Align nodes in case three or more nodes are selected.
     *
     * @param nodes Nodes to be aligned.
     * @return Command that perform action.
     * @throws InvalidSelection If the nodes have same coordinates.
     */
    private static Command alignOnlyNodes(List<Node> nodes) throws InvalidSelection {
        // Choose nodes used as anchor points for projection.
        Node[] anchors = nodePairFurthestApart(nodes);
        Line line = new Line(anchors[0], anchors[1]);
        Collection<Command> cmds = nodes.stream()
                .filter(node -> node != anchors[0] && node != anchors[1])
                .map(line::projectionCommand)
                .collect(Collectors.toList());
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
            if (!w.isEmpty()) {
                nodes.addAll(w.getNodes());
                lines.put(w, new Line(w));
            }
        }
        if (nodes.isEmpty()) {
            throw new InvalidSelection(tr("Intersection of three or more ways can not be solved. Abort."));
        }
        Collection<Command> cmds = new ArrayList<>(nodes.size());
        List<Way> referrers = new ArrayList<>(ways.size());
        for (Node n: nodes) {
            referrers.clear();
            for (OsmPrimitive o: n.getReferrers()) {
                if (ways.contains(o))
                    referrers.add((Way) o);
            }
            if (referrers.size() == 1) {
                Way way = referrers.get(0);
                if (way.isFirstLastNode(n)) continue;
                cmds.add(lines.get(way).projectionCommand(n));
            } else if (referrers.size() == 2) {
                cmds.add(lines.get(referrers.get(0)).intersectionCommand(n, lines.get(referrers.get(1))));
            } else
                throw new InvalidSelection(tr("Intersection of three or more ways can not be solved. Abort."));
        }
        return cmds.isEmpty() ? null : new SequenceCommand(tr("Align Nodes in Line"), cmds);
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
                double[] angle = IntStream.range(0, 4)
                        .mapToDouble(i -> PolarCoor.computeAngle(neighbors.get(i).getEastNorth(), c)).toArray();
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
        private double a;
        private double b;
        private final double c;
        /**
         * (xM, yM) are coordinates of a point of the line
         */
        private final double xM;
        private final double yM;

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
        updateEnabledStateOnModifiableSelection(selection);
    }
}
