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

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Aligns all selected nodes into a straight line (useful for
 * roads that should be straight, but have side roads and
 * therefore need multiple nodes)
 *
 * Case 1: Only ways selected, align each ways taking care of intersection.
 * Case 2: Single node selected, align this node relative to the surrounding nodes.
 * Case 3: Single node and ways selected, align this node relative to the surrounding nodes only parts of selected ways.
 * Case 4: Only nodes selected, align these nodes respect to the line passing through the most distant nodes.
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
     * InvalidSelection exception has to be raised when action can't be perform
     */
    private class InvalidSelection extends Exception {

        /**
         * Create an InvalidSelection exception with default message
         */
        public InvalidSelection() {
            super(tr("Please select at least three nodes."));
        }

        /**
         * Create an InvalidSelection exception with specific message
         * @param msg Message that will be display to the user
         */
        public InvalidSelection(String msg) {
            super(msg);
        }
    }

    /**
     * Compute 2 anchor points to align a set of nodes.
     * If all nodes are part of a same way anchor points are choose farthest relative to this way,
     * else choose farthest nodes.
     * @param nodes Nodes to be aligned
     * @param resultOut Array of size >= 2
     */
    private void nodePairFurthestApart(List<Node> nodes, Node[] resultOut) {
        if(resultOut.length < 2)
            throw new IllegalArgumentException();

        Node nodea = null;
        Node nodeb = null;

        // Intersection of all ways referred by each node
        HashSet<Way> waysRef = null;
        for(Node n: nodes) {
            Collection<Way> ref = OsmPrimitive.getFilteredList(n.getReferrers(), Way.class);
            if(waysRef == null)
                waysRef = new HashSet<Way>(ref);
            else
                waysRef.retainAll(ref);
        }
        if(waysRef.size() == 1) {
            // All nodes are part of the same way. See #9605
            HashSet<Node> remainNodes = new HashSet<Node>(nodes);
            Way way = waysRef.iterator().next();
            for(Node n: way.getNodes()) {
                if(!remainNodes.contains(n)) continue;
                if(nodea == null) nodea = n;
                if(remainNodes.size() == 1) {
                    nodeb = remainNodes.iterator().next();
                    break;
                }
                remainNodes.remove(n);
            }
        } else {
            // Find from the selected nodes two that are the furthest apart.
            // Let's call them A and B.
            double distance = 0;
            for (int i = 0; i < nodes.size()-1; i++) {
                Node n = nodes.get(i);
                for (int j = i+1; j < nodes.size(); j++) {
                    Node m = nodes.get(j);
                    double dist = Math.sqrt(n.getEastNorth().distance(m.getEastNorth()));
                    if (dist > distance) {
                        nodea = n;
                        nodeb = m;
                        distance = dist;
                    }
                }
            }
        }
        resultOut[0] = nodea;
        resultOut[1] = nodeb;
    }

    /**
     * Operation depends on the selected objects:
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        List<Node> selectedNodes = new ArrayList<Node>(getCurrentDataSet().getSelectedNodes());
        List<Way> selectedWays = new ArrayList<Way>(getCurrentDataSet().getSelectedWays());

        try {
            Command cmd = null;
            //// Decide what to align based on selection:

            /// Only ways selected -> For each way align their nodes taking care of intersection
            if(selectedNodes.isEmpty() && !selectedWays.isEmpty()) {
                cmd = alignMultiWay(selectedWays);
            }
            /// Only 1 node selected -> align this node relative to referers way
            else if(selectedNodes.size() == 1) {
                Node selectedNode = selectedNodes.get(0);
                List<Way> involvedWays = null;
                if(selectedWays.isEmpty())
                    /// No selected way, all way containing this node are used
                    involvedWays = OsmPrimitive.getFilteredList(selectedNode.getReferrers(), Way.class);
                else
                    /// Selected way, use only these ways
                    involvedWays = selectedWays;
                List<Line> lines = getInvolvedLines(selectedNode, involvedWays);
                if(lines.size() > 2 || lines.isEmpty())
                    throw new InvalidSelection();
                cmd = alignSingleNode(selectedNodes.get(0), lines);
            }
            /// More than 3 nodes selected -> align those nodes
            else if(selectedNodes.size() >= 3) {
                cmd = alignOnlyNodes(selectedNodes);
            }
            /// All others cases are invalid
            else {
                throw new InvalidSelection();
            }

            // Do it!
            Main.main.undoRedo.add(cmd);
            Main.map.repaint();

        } catch (InvalidSelection except) {
            new Notification(except.getMessage())
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
        }
    }

    /**
     * Align nodes in case that only nodes are selected
     *
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to align all other selected
     * nodes onto the straight line between these nodes.

     * @param nodes Nodes to be aligned
     * @return Command that perform action
     * @throws InvalidSelection
     */
    private Command alignOnlyNodes(List<Node> nodes) throws InvalidSelection {
        Node[] anchors = new Node[2]; // oh, java I love you so much..
        // use the nodes furthest apart as anchors
        nodePairFurthestApart(nodes, anchors);
        Collection<Command> cmds = new ArrayList<Command>(nodes.size());
        Line line = new Line(anchors[0], anchors[1]);
        for(Node node: nodes)
            if(node != anchors[0] && node != anchors[1])
                cmds.add(line.projectionCommand(node));
        return new SequenceCommand(tr("Align Nodes in Line"), cmds);
    }

    /**
     * Align way in case of multiple way #6819
     * @param ways Collection of way to align
     * @return Command that perform action
     * @throws InvalidSelection
     */
    private Command alignMultiWay(Collection<Way> ways) throws InvalidSelection {
        // Collect all nodes and compute line equation
        HashSet<Node> nodes = new HashSet<Node>();
        HashMap<Way, Line> lines = new HashMap<Way, Line>();
        for(Way w: ways) {
            if(w.firstNode() == w.lastNode())
                throw new InvalidSelection(tr("Can not align a polygon. Abort."));
            nodes.addAll(w.getNodes());
            lines.put(w, new Line(w));
        }
        Collection<Command> cmds = new ArrayList<Command>(nodes.size());
        List<Way> referers = new ArrayList<Way>(ways.size());
        for(Node n: nodes) {
            referers.clear();
            for(OsmPrimitive o: n.getReferrers())
                if(ways.contains(o))
                    referers.add((Way) o);
            if(referers.size() == 1) {
                Way way = referers.get(0);
                if(n == way.firstNode() || n == way.lastNode()) continue;
                cmds.add(lines.get(way).projectionCommand(n));
            }
            else if(referers.size() == 2) {
                Command cmd = lines.get(referers.get(0)).intersectionCommand(n, lines.get(referers.get(1)));
                cmds.add(cmd);
            }
            else
                throw new InvalidSelection(tr("Intersection of three or more ways can not be solved. Abort."));
        }
        return new SequenceCommand(tr("Align Nodes in Line"), cmds);
    }

    /**
     * Get lines useful to do alignment of a single node
     * @param node Node to be aligned
     * @param refWays Ways where useful lines will be searched
     * @return List of useful lines
     * @throws InvalidSelection
     */
    private List<Line> getInvolvedLines(Node node, List<Way> refWays) throws InvalidSelection {
        ArrayList<Line> lines = new ArrayList<Line>();
        ArrayList<Node> neighbors = new ArrayList<Node>();
        for(Way way: refWays) {
            List<Node> nodes = way.getNodes();
            neighbors.clear();
            for(int i = 1; i < nodes.size()-1; i++)
                if(nodes.get(i) == node) {
                    neighbors.add(nodes.get(i-1));
                    neighbors.add(nodes.get(i+1));
                }
            if(neighbors.size() == 0)
                continue;
            else if(neighbors.size() == 2)
                // Non self crossing
                lines.add(new Line(neighbors.get(0), neighbors.get(1)));
            else if(neighbors.size() == 4) {
                // Self crossing, have to make 2 lines with 4 neighbors
                // see #9081 comment 6
                EastNorth c = node.getEastNorth();
                double[] angle = new double[4];
                for(int i = 0; i < 4; i++) {
                    EastNorth p = neighbors.get(i).getEastNorth();
                    angle[i] = Math.atan2(p.north() - c.north(), p.east() - c.east());
                }
                double[] deltaAngle = new double[3];
                for(int i = 0; i < 3; i++) {
                    deltaAngle[i] = angle[i+1] - angle[0];
                    if(deltaAngle[i] < 0)
                        deltaAngle[i] += 2*Math.PI;
                }
                int nb = 0;
                if(deltaAngle[1] < deltaAngle[0]) nb++;
                if(deltaAngle[2] < deltaAngle[0]) nb++;
                if(nb == 1) {
                    // Align along [neighbors[0], neighbors[1]] and [neighbors[0], neighbors[2]]
                    lines.add(new Line(neighbors.get(0), neighbors.get(1)));
                    lines.add(new Line(neighbors.get(2), neighbors.get(3)));
                } else {
                    // Align along [neighbors[0], neighbors[2]] and [neighbors[1], neighbors[3]]
                    lines.add(new Line(neighbors.get(0), neighbors.get(2)));
                    lines.add(new Line(neighbors.get(1), neighbors.get(3)));
                }
            } else
                throw new InvalidSelection();
        }
        return lines;
    }

    /**
     * Align a single node relative to a set of lines #9081
     * @param node Node to be aligned
     * @param lines Lines to align node on
     * @return Command that perform action
     * @throws InvalidSelection
     */
    private Command alignSingleNode(Node node, List<Line> lines) throws InvalidSelection {
        if(lines.size() == 1)
            return lines.get(0).projectionCommand(node);
        else if(lines.size() == 2)
            return lines.get(0).intersectionCommand(node,  lines.get(1));
        throw new InvalidSelection();
    }

    /**
     * Class that represent a line
     */
    private class Line {

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
         * @param first On point of the line
         * @param last Other point of the line
         * @throws InvalidSelection
         */
        public Line(Node first, Node last) throws InvalidSelection {
            xM = first.getEastNorth().getX();
            yM = first.getEastNorth().getY();
            double xB = last.getEastNorth().getX();
            double yB = last.getEastNorth().getY();
            a = yB - yM;
            b = xM - xB;
            double norm = Math.sqrt(a*a + b*b);
            if (norm == 0)
                // Nodes have same coordinates !
                throw new InvalidSelection();
            a /= norm;
            b /= norm;
            c = -(a*xM + b*yM);
        }

        /**
         * Init a line equation from a way.
         * @param way Use extremity of this way to compute line equation
         * @throws InvalidSelection
         */
        public Line(Way way) throws InvalidSelection {
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
         * @throws InvalidSelection
         */
        public Command intersectionCommand(Node n, Line other) throws InvalidSelection {
            double d = this.a * other.b - other.a * this.b;
            if(Math.abs(d) < 10e-6)
                // parallels lines
                throw new InvalidSelection(tr("Two parallels ways found. Abort."));
            double x = (this.b * other.c - other.b * this.c) / d;
            double y = (other.a * this.c - this.a * other.c) / d;
            return new MoveCommand(n, x - n.getEastNorth().getX(), y - n.getEastNorth().getY());
        }
    }

    @Override
    protected void updateEnabledState() {
        setEnabled(getCurrentDataSet() != null && !getCurrentDataSet().getSelected().isEmpty());
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
