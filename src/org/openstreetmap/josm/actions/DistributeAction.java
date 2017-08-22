// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.gui.help.HelpUtil.ht;
import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.Notification;
import org.openstreetmap.josm.tools.Logging;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Distributes the selected nodes to equal distances along a line.
 *
 * @author Teemu Koskinen
 */
public final class DistributeAction extends JosmAction {

    /**
     * Constructs a new {@code DistributeAction}.
     */
    public DistributeAction() {
        super(tr("Distribute Nodes"), "distribute",
              tr("Distribute the selected nodes to equal distances along a line."),
              Shortcut.registerShortcut("tools:distribute", tr("Tool: {0}", tr("Distribute Nodes")), KeyEvent.VK_B, Shortcut.SHIFT),
              true);
        putValue("help", ht("/Action/DistributeNodes"));
    }

    /**
     * Perform action.
     * Select method according to user selection.
     * Case 1: One Way (no self-crossing) and at most 2 nodes contains by this way:
     *     Distribute nodes keeping order along the way
     * Case 2: Other
     *     Distribute nodes
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;

        // Collect user selected objects
        Collection<OsmPrimitive> selected = getLayerManager().getEditDataSet().getSelected();
        Collection<Way> ways = new LinkedList<>();
        Collection<Node> nodes = new HashSet<>();
        for (OsmPrimitive osm : selected) {
            if (osm instanceof Node) {
                nodes.add((Node) osm);
            } else if (osm instanceof Way) {
                ways.add((Way) osm);
            }
        }

        Set<Node> ignoredNodes = removeNodesWithoutCoordinates(nodes);
        if (!ignoredNodes.isEmpty()) {
            Logging.warn(tr("Ignoring {0} nodes with null coordinates", ignoredNodes.size()));
            ignoredNodes.clear();
        }

        // Switch between algorithms
        Collection<Command> cmds;
        if (checkDistributeWay(ways, nodes)) {
            cmds = distributeWay(ways, nodes);
        } else if (checkDistributeNodes(ways, nodes)) {
            cmds = distributeNodes(nodes);
        } else {
            new Notification(
                             tr("Please select :\n" +
                                "* One no self-crossing way with at most two of its nodes;\n" +
                                "* Three nodes."))
                .setIcon(JOptionPane.INFORMATION_MESSAGE)
                .setDuration(Notification.TIME_SHORT)
                .show();
            return;
        }

        if (cmds.isEmpty()) {
            return;
        }

        // Do it!
        Main.main.undoRedo.add(new SequenceCommand(tr("Distribute Nodes"), cmds));
    }

    /**
     * Test if one way, no self-crossing, is selected with at most two of its nodes.
     * @param ways Selected ways
     * @param nodes Selected nodes
     * @return true in this case
     */
    private static boolean checkDistributeWay(Collection<Way> ways, Collection<Node> nodes) {
        if (ways.size() == 1 && nodes.size() <= 2) {
            Way w = ways.iterator().next();
            Set<Node> unduplicated = new HashSet<>(w.getNodes());
            if (unduplicated.size() != w.getNodesCount()) {
                // No self crossing way
                return false;
            }
            for (Node node: nodes) {
                if (!w.containsNode(node)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Distribute nodes contained by a way, keeping nodes order.
     * If one or two nodes are selected, keep these nodes in place.
     * @param ways Selected ways, must be collection of size 1.
     * @param nodes Selected nodes, at most two nodes.
     * @return Collection of command to be executed.
     */
    private static Collection<Command> distributeWay(Collection<Way> ways,
                                              Collection<Node> nodes) {
        Way w = ways.iterator().next();
        Collection<Command> cmds = new LinkedList<>();

        if (w.getNodesCount() == nodes.size() || w.getNodesCount() <= 2) {
            // Nothing to do
            return cmds;
        }

        double xa, ya; // Start point
        double dx, dy; // Segment increment
        if (nodes.isEmpty()) {
            Node na = w.firstNode();
            nodes.add(na);
            Node nb = w.lastNode();
            nodes.add(nb);
            xa = na.getEastNorth().east();
            ya = na.getEastNorth().north();
            dx = (nb.getEastNorth().east() - xa) / (w.getNodesCount() - 1);
            dy = (nb.getEastNorth().north() - ya) / (w.getNodesCount() - 1);
        } else if (nodes.size() == 1) {
            Node n = nodes.iterator().next();
            int nIdx = w.getNodes().indexOf(n);
            Node na = w.firstNode();
            Node nb = w.lastNode();
            dx = (nb.getEastNorth().east() - na.getEastNorth().east()) /
                (w.getNodesCount() - 1);
            dy = (nb.getEastNorth().north() - na.getEastNorth().north()) /
                (w.getNodesCount() - 1);
            xa = n.getEastNorth().east() - dx * nIdx;
            ya = n.getEastNorth().north() - dy * nIdx;
        } else {
            Iterator<Node> it = nodes.iterator();
            Node na = it.next();
            Node nb = it.next();
            List<Node> wayNodes = w.getNodes();
            int naIdx = wayNodes.indexOf(na);
            int nbIdx = wayNodes.indexOf(nb);
            dx = (nb.getEastNorth().east() - na.getEastNorth().east()) / (nbIdx - naIdx);
            dy = (nb.getEastNorth().north() - na.getEastNorth().north()) / (nbIdx - naIdx);
            xa = na.getEastNorth().east() - dx * naIdx;
            ya = na.getEastNorth().north() - dy * naIdx;
        }

        for (int i = 0; i < w.getNodesCount(); i++) {
            Node n = w.getNode(i);
            if (!n.isLatLonKnown() || nodes.contains(n)) {
                continue;
            }
            double x = xa + i * dx;
            double y = ya + i * dy;
            cmds.add(new MoveCommand(n, x - n.getEastNorth().east(),
                                     y - n.getEastNorth().north()));
        }
        return cmds;
    }

    /**
     * Test if nodes oriented algorithm applies to the selection.
     * @param ways Selected ways
     * @param nodes Selected nodes
     * @return true in this case
     */
    private static Boolean checkDistributeNodes(Collection<Way> ways, Collection<Node> nodes) {
        return ways.isEmpty() && nodes.size() >= 3;
    }

    /**
     * Distribute nodes when only nodes are selected.
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to distribute all other selected
     * nodes along the straight line between these nodes.
     * @param nodes nodes to distribute
     * @return Commands to execute to perform action
     * @throws IllegalArgumentException if nodes is empty
     */
    private static Collection<Command> distributeNodes(Collection<Node> nodes) {
        // Find from the selected nodes two that are the furthest apart.
        // Let's call them A and B.
        double distance = Double.NEGATIVE_INFINITY;

        Node nodea = null;
        Node nodeb = null;

        Collection<Node> itnodes = new LinkedList<>(nodes);
        for (Node n : nodes) {
            itnodes.remove(n);
            for (Node m : itnodes) {
                double dist = Math.sqrt(n.getEastNorth().distance(m.getEastNorth()));
                if (dist > distance) {
                    nodea = n;
                    nodeb = m;
                    distance = dist;
                }
            }
        }

        if (nodea == null || nodeb == null) {
            throw new IllegalArgumentException();
        }

        // Remove the nodes A and B from the list of nodes to move
        nodes.remove(nodea);
        nodes.remove(nodeb);

        // Find out co-ords of A and B
        double ax = nodea.getEastNorth().east();
        double ay = nodea.getEastNorth().north();
        double bx = nodeb.getEastNorth().east();
        double by = nodeb.getEastNorth().north();

        // A list of commands to do
        Collection<Command> cmds = new LinkedList<>();

        // Amount of nodes between A and B plus 1
        int num = nodes.size()+1;

        // Current number of node
        int pos = 0;
        while (!nodes.isEmpty()) {
            pos++;
            Node s = null;

            // Find the node that is furthest from B (i.e. closest to A)
            distance = Double.NEGATIVE_INFINITY;
            for (Node n : nodes) {
                double dist = Math.sqrt(nodeb.getEastNorth().distance(n.getEastNorth()));
                if (dist > distance) {
                    s = n;
                    distance = dist;
                }
            }

            if (s != null) {
                // First move the node to A's position, then move it towards B
                double dx = ax - s.getEastNorth().east() + (bx-ax)*pos/num;
                double dy = ay - s.getEastNorth().north() + (by-ay)*pos/num;

                cmds.add(new MoveCommand(s, dx, dy));

                //remove moved node from the list
                nodes.remove(s);
            }
        }

        return cmds;
    }

    /**
     * Remove nodes without knowned coordinates from a collection.
     * @param col Collection of nodes to check
     * @return Set of nodes without coordinates
     */
    private static Set<Node> removeNodesWithoutCoordinates(Collection<Node> col) {
        Set<Node> result = new HashSet<>();
        for (Iterator<Node> it = col.iterator(); it.hasNext();) {
            Node n = it.next();
            if (!n.isLatLonKnown()) {
                it.remove();
                result.add(n);
            }
        }
        return result;
    }

    @Override
    protected void updateEnabledState() {
        updateEnabledStateOnCurrentSelection();
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
