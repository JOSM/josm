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

/**
 * Aligns all selected nodes into a straight line (useful for
 * roads that should be straight, but have side roads and
 * therefore need multiple nodes)
 * 
 * @author Matthew Newton
 */
public final class AlignInLineAction extends JosmAction {

	public AlignInLineAction() {
		super(tr("Align Nodes in Line"), "alignline", tr("Move the selected nodes onto a line."), KeyEvent.VK_L, 0, true);
	}

	/**
	 * The general algorithm here is to find the two selected nodes
	 * that are furthest apart, and then to align all other selected
	 * nodes onto the straight line between these nodes.
	 */
	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Collection<Node> nodes = new LinkedList<Node>();
		Collection<Node> itnodes = new LinkedList<Node>();
		for (OsmPrimitive osm : sel)
			if (osm instanceof Node) {
				nodes.add((Node)osm);
				itnodes.add((Node)osm);
			}
		if (nodes.size() < 3) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least three nodes."));
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
				double dist = Math.sqrt(n.eastNorth.distance(m.eastNorth));
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
		double ax = nodea.eastNorth.east();
		double ay = nodea.eastNorth.north();
		double bx = nodeb.eastNorth.east();
		double by = nodeb.eastNorth.north();

		// A list of commands to do
		Collection<Command> cmds = new LinkedList<Command>();

		// OK, for each node to move, work out where to move it!
		for (Node n : nodes) {
			// Get existing co-ords of node to move
			double nx = n.eastNorth.east();
			double ny = n.eastNorth.north();

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
				double c2 = n.eastNorth.north() - (n.eastNorth.east() * m2);

				nx = (c2 - c1) / (m1 - m2);
				ny = (m1 * nx) + c1;
			}

			// Add the command to move the node to its new position.
			cmds.add(new MoveCommand(n, nx - n.eastNorth.east(), ny - n.eastNorth.north() ));
		}

		// Do it!
		Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Line"), cmds));
		Main.map.repaint();
	}
}
