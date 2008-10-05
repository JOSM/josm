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
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * Create a new circle from three selected nodes--or a way with 3 nodes. (Useful for roundabouts)
 *
 * Note: If a way is selected, it is changed. If nodes are selected a new way is created.
 *       So if you've got a way with 3 nodes it makes a difference between running this on the way or the nodes!
 *
 * BTW: Someone might want to implement projection corrections for this...
 *
 * @author Henry Loenwind, based on much copy&Paste from other Actions.
 */
public final class CreateCircleAction extends JosmAction {

	public CreateCircleAction() {
		super(tr("Create Circle"), "createcircle", tr("Create a circle from three selected nodes."),
		ShortCut.registerShortCut("tools:createcircle", tr("Tool: Create circle"), KeyEvent.VK_O, ShortCut.GROUP_EDIT), true);
	}

	private double calcang(double xc, double yc, double x, double y) {
		// calculate the angle from xc|yc to x|y
		if (xc == x && yc == y) {
			return 0; // actually invalid, but we won't have this case in this context
		}
		double yd = Math.abs(y - yc);
		if (yd == 0 && xc < x) {
			return 0;
		}
		if (yd == 0 && xc > x) {
			return Math.PI;
		}
		double xd = Math.abs(x - xc);
		double a = Math.atan2(xd, yd);
		if (y > yc) {
			a = Math.PI - a;
		}
		if (x < xc) {
			a = -a;
		}
		a = 1.5*Math.PI + a;
		if (a < 0) {
			a += 2*Math.PI;
		}
		if (a >= 2*Math.PI) {
			a -= 2*Math.PI;
		}
		return a;
	}

	public void actionPerformed(ActionEvent e) {
		int numberOfNodesInCircle = Integer.parseInt(Main.pref.get("createcircle.nodecount", "8"));
		if (numberOfNodesInCircle < 1) {
			numberOfNodesInCircle = 1;
		} else if (numberOfNodesInCircle > 100) {
			numberOfNodesInCircle = 100;
		}

		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Collection<Node> nodes = new LinkedList<Node>();
		Way existingWay = null;

		for (OsmPrimitive osm : sel)
			if (osm instanceof Node)
				nodes.add((Node)osm);

		// special case if no single nodes are selected and exactly one way is:
		// then use the way's nodes
		if ((nodes.size() == 0) && (sel.size() == 1))
			for (OsmPrimitive osm : sel)
				if (osm instanceof Way) {
					existingWay = ((Way)osm);
					for (Node n : ((Way)osm).nodes)
					{
						if(!nodes.contains(n))
							nodes.add(n);
					}
				}

		if (nodes.size() != 3) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select exactly three nodes or one way with exactly three nodes."));
			return;
		}

		// let's get some shorter names
		Node   n1 = ((Node)nodes.toArray()[0]);
		double x1 = n1.eastNorth.east();
		double y1 = n1.eastNorth.north();
		Node   n2 = ((Node)nodes.toArray()[1]);
		double x2 = n2.eastNorth.east();
		double y2 = n2.eastNorth.north();
		Node   n3 = ((Node)nodes.toArray()[2]);
		double x3 = n3.eastNorth.east();
		double y3 = n3.eastNorth.north();

		// calculate the center (xc/yc)
		double s = 0.5*((x2 - x3)*(x1 - x3) - (y2 - y3)*(y3 - y1));
		double sUnder = (x1 - x2)*(y3 - y1) - (y2 - y1)*(x1 - x3);

		if (sUnder == 0) {
			JOptionPane.showMessageDialog(Main.parent, tr("Those nodes are not in a circle."));
			return;
		}

		s /= sUnder;

		double xc = 0.5*(x1 + x2) + s*(y2 - y1);
		double yc = 0.5*(y1 + y2) + s*(x1 - x2);

		// calculate the radius (r)
		double r = Math.sqrt(Math.pow(xc-x1,2) + Math.pow(yc-y1,2));

		// find where to put the existing nodes
		double a1 = calcang(xc, yc, x1, y1);
		double a2 = calcang(xc, yc, x2, y2);
		double a3 = calcang(xc, yc, x3, y3);
		if (a1 < a2) { double at = a1; Node nt = n1; a1 = a2; n1 = n2; a2 = at; n2 = nt; }
		if (a2 < a3) { double at = a2; Node nt = n2; a2 = a3; n2 = n3; a3 = at; n3 = nt; }
		if (a1 < a2) { double at = a1; Node nt = n1; a1 = a2; n1 = n2; a2 = at; n2 = nt; }

		// now we can start doing thigs to OSM data
		Collection<Command> cmds = new LinkedList<Command>();

		// build a way for the circle
		Way wayToAdd;
		if (existingWay == null) {
			wayToAdd = new Way();
		} else {
			// re-use existing way if it was selected
			wayToAdd = new Way(existingWay);
			wayToAdd.nodes.clear();
		}
		for (int i = 1; i <= numberOfNodesInCircle; i++) {
			double a = 2*Math.PI*(1.0 - i/(double)numberOfNodesInCircle); // "1-" to get it clock-wise
			// insert existing nodes if they fit before this new node (999 means "already added this node")
			if (a1 < 999 && a1 > a) {
				wayToAdd.nodes.add(n1);
				a1 = 999;
			}
			if (a2 < 999 && a2 > a) {
				wayToAdd.nodes.add(n2);
				a2 = 999;
			}
			if (a3 < 999 && a3 > a) {
				wayToAdd.nodes.add(n3);
				a3 = 999;
			}
			// get the position of the new node and insert it
			double x = xc + r*Math.cos(a);
			double y = yc + r*Math.sin(a);
			Node n = new Node(Main.proj.eastNorth2latlon(new EastNorth(x,y)));
			wayToAdd.nodes.add(n);
			cmds.add(new AddCommand(n));
		}
		wayToAdd.nodes.add(wayToAdd.nodes.get(0)); // close the circle
		if (existingWay == null) {
			cmds.add(new AddCommand(wayToAdd));
		} else {
			cmds.add(new ChangeCommand(existingWay, wayToAdd));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Create Circle"), cmds));
		Main.map.repaint();
	}
}
