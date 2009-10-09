// License: GPL. Copyright 2007 by Immanuel Scholz and others
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
 * Aligns all selected nodes into a straight line (useful for
 * roads that should be straight, but have side roads and
 * therefore need multiple nodes)
 *
 * @author Matthew Newton
 */
public final class AlignInLineAction extends JosmAction {

    public AlignInLineAction() {
        super(tr("Align Nodes in Line"), "alignline", tr("Move the selected nodes in to a line."),
                Shortcut.registerShortcut("tools:alignline", tr("Tool: {0}", tr("Align Nodes in Line")), KeyEvent.VK_L, Shortcut.GROUP_EDIT), true);
    }

    /**
     * The general algorithm here is to find the two selected nodes
     * that are furthest apart, and then to align all other selected
     * nodes onto the straight line between these nodes.
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

            // Add the command to move the node to its new position.
            cmds.add(new MoveCommand(n, nx - n.getEastNorth().east(), ny - n.getEastNorth().north() ));
        }

        // Do it!
        Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Line"), cmds));
        Main.map.repaint();
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
