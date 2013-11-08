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
        super(tr("Distribute Nodes"), "distribute", tr("Distribute the selected nodes to equal distances along a line."),
                Shortcut.registerShortcut("tools:distribute", tr("Tool: {0}", tr("Distribute Nodes")), KeyEvent.VK_B,
                Shortcut.SHIFT), true);
        putValue("help", ht("/Action/DistributeNodes"));
    }

    /**
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to distribute all other selected
     * nodes along the straight line between these nodes.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isEnabled())
            return;
        Collection<OsmPrimitive> sel = getCurrentDataSet().getSelected();
        Collection<Node> nodes = new LinkedList<Node>();
        Collection<Node> itnodes = new LinkedList<Node>();
        for (OsmPrimitive osm : sel)
            if (osm instanceof Node) {
                nodes.add((Node)osm);
                itnodes.add((Node)osm);
            }
        // special case if no single nodes are selected and exactly one way is:
        // then use the way's nodes
        if (nodes.isEmpty() && (sel.size() == 1)) {
            for (OsmPrimitive osm : sel)
                if (osm instanceof Way) {
                    nodes.addAll(((Way)osm).getNodes());
                    itnodes.addAll(((Way)osm).getNodes());
                }
        }

        Set<Node> ignoredNodes = removeNodesWithoutCoordinates(nodes);
        ignoredNodes.addAll(removeNodesWithoutCoordinates(itnodes));
        if (!ignoredNodes.isEmpty()) {
            Main.warn(tr("Ignoring {0} nodes with null coordinates", ignoredNodes.size()));
            ignoredNodes.clear();
        }

        if (nodes.size() < 3) {
            new Notification(
                    tr("Please select at least three nodes."))
                    .setIcon(JOptionPane.INFORMATION_MESSAGE)
                    .setDuration(Notification.TIME_SHORT)
                    .show();
            return;
        }

        // Find from the selected nodes two that are the furthest apart.
        // Let's call them A and B.
        double distance = 0;

        Node nodea = null;
        Node nodeb = null;

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

        // Remove the nodes A and B from the list of nodes to move
        nodes.remove(nodea);
        nodes.remove(nodeb);

        // Find out co-ords of A and B
        double ax = nodea.getEastNorth().east();
        double ay = nodea.getEastNorth().north();
        double bx = nodeb.getEastNorth().east();
        double by = nodeb.getEastNorth().north();

        // A list of commands to do
        Collection<Command> cmds = new LinkedList<Command>();

        // Amount of nodes between A and B plus 1
        int num = nodes.size()+1;

        // Current number of node
        int pos = 0;
        while (!nodes.isEmpty()) {
            pos++;
            Node s = null;

            // Find the node that is furthest from B (i.e. closest to A)
            distance = 0.0;
            for (Node n : nodes) {
                double dist = Math.sqrt(nodeb.getEastNorth().distance(n.getEastNorth()));
                if (dist > distance) {
                    s = n;
                    distance = dist;
                }
            }

            // First move the node to A's position, then move it towards B
            double dx = ax - s.getEastNorth().east() + (bx-ax)*pos/num;
            double dy = ay - s.getEastNorth().north() + (by-ay)*pos/num;

            cmds.add(new MoveCommand(s, dx, dy));

            //remove moved node from the list
            nodes.remove(s);
        }

        // Do it!
        Main.main.undoRedo.add(new SequenceCommand(tr("Distribute Nodes"), cmds));
        Main.map.repaint();
    }

    private Set<Node> removeNodesWithoutCoordinates(Collection<Node> col) {
        Set<Node> result = new HashSet<Node>();
        for (Iterator<Node> it = col.iterator(); it.hasNext();) {
            Node n = it.next();
            if (n.getCoor() == null) {
                it.remove();
                result.add(n);
            }
        }
        return result;
    }

    @Override
    protected void updateEnabledState() {
        if (getCurrentDataSet() == null) {
            setEnabled(false);
        } else {
            updateEnabledState(getCurrentDataSet().getSelected());
        }
    }

    @Override
    protected void updateEnabledState(Collection<? extends OsmPrimitive> selection) {
        setEnabled(selection != null && !selection.isEmpty());
    }
}
