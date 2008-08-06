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
import org.openstreetmap.josm.data.osm.Way;

/**
 * Aligns all selected nodes within a circle. (Useful for roundabouts)
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
		
		// special case if no single nodes are selected and exactly one way is: 
		// then use the way's nodes
		if ((nodes.size() == 0) && (sel.size() == 1))
			for (OsmPrimitive osm : sel)
				if (osm instanceof Way)
					nodes.addAll(((Way)osm).nodes);
		
		if (nodes.size() < 4) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least four nodes."));
			return;
		}

		// Get average position of all nodes
		Node avn = new Node(new LatLon(0,0));
		avn.eastNorth = new EastNorth(0,0);
		for (Node n : nodes) {
			avn.eastNorth = new EastNorth(avn.eastNorth.east()+n.eastNorth.east(), avn.eastNorth.north()+n.eastNorth.north());
		}
		avn.eastNorth = new EastNorth(avn.eastNorth.east()/nodes.size(), avn.eastNorth.north()/nodes.size());
		avn.coor = Main.proj.eastNorth2latlon(avn.eastNorth);
		// Node "avn" now is central to all selected nodes.

		// Now calculate the average distance to each node from the
		// centre.  This method is ok as long as distances are short 
		// relative to the distance from the N or S poles.
		double distances[] = new double[nodes.size()];
		double avdist = 0, latd, lond;
		double lonscale = Math.cos(avn.coor.lat() * Math.PI/180.0);
		lonscale = lonscale * lonscale;
		int i = 0;
		for (Node n : nodes) {
			latd = n.coor.lat() - avn.coor.lat();
			lond = n.coor.lon() - avn.coor.lon();
			distances[i] = Math.sqrt(latd * latd + lonscale * lond * lond);
			avdist += distances[i++];
		}
		avdist = avdist / nodes.size();

		Collection<Command> cmds = new LinkedList<Command>();
		// Move each node to that distance from the centre.
		i = 0;
		for (Node n : nodes) {
			double dx = n.eastNorth.east() - avn.eastNorth.east();
			double dy = n.eastNorth.north() - avn.eastNorth.north();
			double dist = distances[i++];
			cmds.add(new MoveCommand(n, (dx * (avdist / dist)) - dx, (dy * (avdist / dist)) - dy));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Circle"), cmds));
		Main.map.repaint();
	}
}
