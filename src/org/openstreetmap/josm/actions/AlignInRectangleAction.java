// License: GPL. See LICENSE file for details.
//
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
 * Aligns all selected nodes within a rectangle. 
 * 
 * There are many ways this could be done, for example:
 * - find smallest rectangle to contain all points (rectangular hull) OR
 * - find largest rectangle to fit inside OR
 * - find both and compute the average
 * 
 * Also, it would be possible to let the user specify more input, e.g.
 * two nodes that should remain where they are.
 * 
 * This method uses the following algorithm:
 * 1. compute "heading" of all four edges
 * 2. select the edge that is oriented closest to the average of all headings
 * @author Frederik Ramm <frederik@remote.org>
 */
public final class AlignInRectangleAction extends JosmAction {

	public AlignInRectangleAction() {
		super(tr("Align Nodes in Rectangle"), "alignrect", tr("Move the selected nodes into a rectangle."), KeyEvent.VK_Q, 0, true);
	}

	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		Way myWay = null;
		if (sel.size() == 1) 
			for (OsmPrimitive osm : sel)
				if (osm instanceof Way)
					myWay = (Way) osm;
		
		if ((myWay == null) || (myWay.nodes.size() != 5) || (!myWay.nodes.get(0).equals(myWay.nodes.get(4)))) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select one circular way of exactly four nodes."));
			return;
		}

		// find orientation of all four edges, compute average
		double avg_angle = 0;
		double angle[] = new double[4];
		EastNorth en[] = new EastNorth[5];
		for (int i=0; i<5; i++) en[i] = new EastNorth(myWay.nodes.get(i).eastNorth.east(), myWay.nodes.get(i).eastNorth.north());
		for (int i=0; i<4; i++) {
			angle[i] = Math.asin((en[i+1].north()-en[i].north())/en[i+1].distance(en[i])) + 2 * Math.PI;
		    while(angle[i] > Math.PI/4) angle[i] -= Math.PI/2;
			avg_angle += angle[i];
		}
		avg_angle /= 4;
		
		// select edge that is closest to average, and use it as the base for the following
		double best_dist = 0; 
		int base = 0;
		for (int i=0; i<4; i++)
		{
			if ((i==0)||(Math.abs(angle[i]-avg_angle))<best_dist)
			{
				best_dist = Math.abs(angle[i]-avg_angle);
				base = i;
			}
		}


		// nice symbolic names for the nodes we're working with
		EastNorth begin = en[base]; // first node of base segment
		EastNorth end = en[(base+1)%4]; // second node of base segment
		EastNorth next = en[(base+2)%4]; // node following the second node of the base seg
		EastNorth prev= en[(base+3)%4];  // node before the first node of the base seg
		
		// find a parallel to the base segment
		double base_slope = (end.north() - begin.north()) / (end.east() - begin.east());
		// base intercept of parallels that go through "next" and "prev" points
		double b1 = next.north() - base_slope * next.east();
		double b2 = prev.north() - base_slope * prev.east();
		// average of both
		double opposite_b = (b1+b2)/2;

		// find the point on the base segment from which distance to "next" is shortest
		double u = ((next.east()-begin.east())*(end.east()-begin.east()) + (next.north()-begin.north())*(end.north()-begin.north()))/end.distanceSq(begin);
		EastNorth end2 = new EastNorth(begin.east()+u*(end.east()-begin.east()), begin.north()+u*(end.north()-begin.north()));

		// same for "prev"
		u = ((prev.east()-begin.east())*(end.east()-begin.east()) + (prev.north()-begin.north())*(end.north()-begin.north()))/end.distanceSq(begin);
		EastNorth begin2 = new EastNorth(begin.east()+u*(end.east()-begin.east()), begin.north()+u*(end.north()-begin.north()));
		
		// new "begin" and "end" points are halfway between their old position and 
		// the base points found above
		end = new EastNorth((end2.east()+end.east())/2, (end2.north()+end.north())/2);
		begin = new EastNorth((begin2.east()+begin.east())/2, (begin2.north()+begin.north())/2);
		
		double other_slope = -1 / base_slope;
		double next_b = end.north() - other_slope * end.east();
		double prev_b = begin.north() - other_slope * begin.east();
		
		double x = (opposite_b-next_b)/(other_slope-base_slope);
		double y = opposite_b + base_slope * x;
		next = new EastNorth(x, y);
		
		x = (opposite_b-prev_b)/(other_slope-base_slope);
		y = opposite_b + base_slope * x;
		prev = new EastNorth(x, y);
		
		Collection<Command> cmds = new LinkedList<Command>();
		for (int i=0; i<4; i++) {
			Node n = myWay.nodes.get(i);
			EastNorth ref = (i == base) ? begin : (i == (base+1)%4) ? end : (i==(base+2)%4) ? next : prev;
			double dx = ref.east()-n.eastNorth.east();
			double dy = ref.north()-n.eastNorth.north();
			cmds.add(new MoveCommand(n, dx, dy));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Align Nodes in Rectangle"), cmds));
		Main.map.repaint();
	}
}
