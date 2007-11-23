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
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;

/**
 * Aligns all selected nodes within a circle. (Usefull for roundabouts)
 * 
 * @author Matthew Newton
 */
public final class AlignInCircleAction extends JosmAction {

	public AlignInCircleAction() {
		super(tr("Align Nodes in Circle"), "aligncircle", tr("Move the selected nodes into a circle."), KeyEvent.VK_O, 0, true);
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Collection<Node> nodes = new LinkedList<Node>();
		for (OsmPrimitive osm : sel)
			if (osm instanceof Node)
				nodes.add((Node)osm);
		if (nodes.size() < 4) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least four nodes."));
			return;
		}

		// Get average position of all nodes
		Node avn = new Node(new LatLon(0,0));
		for (Node n : nodes) {
			avn.eastNorth = new EastNorth(avn.eastNorth.east()+n.eastNorth.east(), avn.eastNorth.north()+n.eastNorth.north());
			avn.coor = Main.proj.eastNorth2latlon(avn.eastNorth);
		}
		avn.eastNorth = new EastNorth(avn.eastNorth.east()/nodes.size(), avn.eastNorth.north()/nodes.size());
		avn.coor = Main.proj.eastNorth2latlon(avn.eastNorth);
		// Node "avn" now is central to all selected nodes.

		// Now calculate the average distance to each node from the
		// centre.
		double avdist = 0;
		for (Node n : nodes)
			avdist += Math.sqrt(avn.eastNorth.distance(n.eastNorth));
		avdist = avdist / nodes.size();

		Collection<Command> cmds = new LinkedList<Command>();
		// Move each node to that distance from the centre.
		for (Node n : nodes) {
			double dx = n.eastNorth.east() - avn.eastNorth.east();
			double dy = n.eastNorth.north() - avn.eastNorth.north();
			double dist = Math.sqrt(avn.eastNorth.distance(n.eastNorth));
			cmds.add(new MoveCommand(n, (dx * (avdist / dist)) - dx, (dy * (avdist / dist)) - dy));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
		Main.map.repaint();
	}
}
