// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
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

    // the joy of single return values only...
    private void nodePairFurthestApart(List<Node> nodes, Node[] resultOut) {
        if(resultOut.length < 2)
            throw new IllegalArgumentException();
        // Find from the selected nodes two that are the furthest apart.
        // Let's call them A and B.
        double distance = 0;

        Node nodea = null;
        Node nodeb = null;

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
        resultOut[0] = nodea;
        resultOut[1] = nodeb;
    }

    private void showWarning() {
        new Notification(
                tr("Please select at least three nodes."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .show();
    }

    private static int indexWrap(int size, int i) {
        i = i % size; // -2 % 5 = -2, -7 % 5 = -2, -5 % 5 = 0
        if (i < 0) {
            i = size + i;
        }
        return i;
    }
    // get the node in w at index i relative to refI
    private static Node getNodeRelative(Way w, int refI, int i) {
        int absI = indexWrap(w.getNodesCount(), refI + i);
        if(w.isClosed() && refI + i < 0) {
            absI--;  // node duplicated in closed ways
        }
        return w.getNode(absI);
    }

    /**
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to align all other selected
     * nodes onto the straight line between these nodes.
     */


    /**
     * Operation depends on the selected objects:
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        Node[] anchors = new Node[2]; // oh, java I love you so much..

        List<Node> selectedNodes = new ArrayList<Node>(getCurrentDataSet().getSelectedNodes());
        Collection<Way> selectedWays = getCurrentDataSet().getSelectedWays();
        List<Node> nodes = new ArrayList<Node>();

        //// Decide what to align based on selection:

        /// Only ways selected -> Align their nodes.
        if ((selectedNodes.isEmpty()) && (selectedWays.size() == 1)) { // TODO: handle multiple ways
            for (Way way : selectedWays) {
                nodes.addAll(way.getNodes());
            }
            // use the nodes furthest apart as anchors
            nodePairFurthestApart(nodes, anchors);
        }
        /// More than 3 nodes selected -> align those nodes
        else if(selectedNodes.size() >= 3) {
            nodes.addAll(selectedNodes);
            // use the nodes furthest apart as anchors
            nodePairFurthestApart(nodes, anchors);
        }
        /// One node selected -> align that node to the relevant neighbors
        else if (selectedNodes.size() == 1) {
            Node n = selectedNodes.iterator().next();

            Way w = null;
            if(selectedWays.size() == 1) {
                w = selectedWays.iterator().next();
                if (!w.containsNode(n))
                    // warning
                    return;
            } else {
                List<Way> refWays = OsmPrimitive.getFilteredList(n.getReferrers(), Way.class);
                if (refWays.size() == 1) { // node used in only one way
                    w = refWays.iterator().next();
                }
            }
            if (w == null || w.getNodesCount() < 3)
                // warning, need at least 3 nodes
                return;

            // Find anchors
            int nodeI = w.getNodes().indexOf(n);
            // End-node in non-circular way selected: align this node with the two neighbors.
            if ((nodeI == 0 || nodeI == w.getNodesCount()-1) && !w.isClosed()) {
                int direction = nodeI == 0 ? 1 : -1;
                anchors[0] = w.getNode(nodeI + direction);
                anchors[1] = w.getNode(nodeI + direction*2);
            } else {
                // o---O---o
                anchors[0] = getNodeRelative(w, nodeI, 1);
                anchors[1] = getNodeRelative(w, nodeI, -1);
            }
            nodes.add(n);
        }

        if (anchors[0] == null || anchors[1] == null) {
            showWarning();
            return;
        }


        Collection<Command> cmds = new ArrayList<Command>(nodes.size());

        createAlignNodesCommands(anchors, nodes, cmds);

        // Do it!
        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Line"), cmds));
        Main.map.repaint();
    }

    private void createAlignNodesCommands(Node[] anchors, Collection<Node> nodes, Collection<Command> cmds) {
        Node nodea = anchors[0];
        Node nodeb = anchors[1];

        // The anchors are aligned per definition
        nodes.remove(nodea);
        nodes.remove(nodeb);

        // Find out co-ords of A and B
        double ax = nodea.getEastNorth().east();
        double ay = nodea.getEastNorth().north();
        double bx = nodeb.getEastNorth().east();
        double by = nodeb.getEastNorth().north();

        // OK, for each node to move, work out where to move it!
        for (Node n : nodes) {
            // Get existing co-ords of node to move
            double nx = n.getEastNorth().east();
            double ny = n.getEastNorth().north();

            if (ax == bx) {
                // Special case if AB is vertical...
                nx = ax;
            } else if (ay == by) {
                // ...or horizontal
                ny = ay;
            } else {
                // Otherwise calculate position by solving y=mx+c
                double m1 = (by - ay) / (bx - ax);
                double c1 = ay - (ax * m1);
                double m2 = (-1) / m1;
                double c2 = n.getEastNorth().north() - (n.getEastNorth().east() * m2);

                nx = (c2 - c1) / (m1 - m2);
                ny = (m1 * nx) + c1;
            }
            double newX = nx - n.getEastNorth().east();
            double newY = ny - n.getEastNorth().north();
            // Add the command to move the node to its new position.
            cmds.add(new MoveCommand(n, newX, newY));
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
