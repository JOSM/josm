// License: GPL. Copyright 2009 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Distributes the selected nodes to equal distances along a line.
 *
 * @author Teemu Koskinen
 */
public final class DistributeAction extends JosmAction {

    public DistributeAction() {
        super(tr("Distribute Nodes"), "distribute", tr("Distribute the selected nodes to equal distances along a line."),
                Shortcut.registerShortcut("tools:distribute", tr("Tool: {0}", tr("Distribute Nodes")), KeyEvent.VK_B, Shortcut.GROUP_EDIT), true);
    }

    /**
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to distribute all other selected
     * nodes along the straight line between these nodes.
     */
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
        if ((nodes.size() == 0) && (sel.size() == 1)) {
            for (OsmPrimitive osm : sel)
                if (osm instanceof Way) {
                    nodes.addAll(((Way)osm).getNodes());
                    itnodes.addAll(((Way)osm).getNodes());
                }
        }

        if (nodes.size() < 3) {
            JOptionPane.showMessageDialog(
                    Main.parent,
                    tr("Please select at least three nodes."),
                    tr("Information"),
                    JOptionPane.INFORMATION_MESSAGE
            );
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
        while (nodes.size() > 0) {
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
