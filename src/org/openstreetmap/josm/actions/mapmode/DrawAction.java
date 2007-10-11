// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GroupAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This mode adds a new node to the dataset. The user clicks on a place to add
 * and there is it. Nothing more, nothing less.
 *
 * FIXME: "nothing more, nothing less" is a bit out-of-date
 *
 * Newly created nodes are selected. Shift modifier does not cancel the old
 * selection as usual.
 *
 * @author imi
 *
 */
public class DrawAction extends MapMode {

	public DrawAction(MapFrame mapFrame) {
		super(tr("Draw"), "node/autonode", tr("Draw nodes"),
			KeyEvent.VK_N, mapFrame, getCursor());
		//putValue("help", "Action/AddNode/Autnode");
	}

	private static Cursor getCursor() {
		try {
	        return ImageProvider.getCursor("crosshair", null);
        } catch (Exception e) {
        }
	    return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
	}

	/**
	 * If user clicked with the left button, add a node at the current mouse
	 * position.
	 *
	 * If in nodeway mode, insert the node into the way. 
	 */
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		Collection<Command> cmds = new LinkedList<Command>();

		Way reuseWay = null, replacedWay = null;
		boolean newNode = false;
		Node n = Main.map.mapView.getNearestNode(e.getPoint());
		if (n == null) {
			n = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));
			if (n.coor.isOutSideWorld()) {
				JOptionPane.showMessageDialog(Main.parent,
					tr("Cannot add a node outside of the world."));
				return;
			}
			newNode = true;

			cmds.add(new AddCommand(n));

			WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
			if (ws != null) {
				replacedWay = ws.way;
				reuseWay = splitWaySegmentAtNode(ws, n, cmds);
			}
		}

		boolean extendedWay = false;
		if (selection.size() == 1 && selection.iterator().next() instanceof Node) {
			Node n0 = (Node) selection.iterator().next();

			Way way = getWayForNode(n0);
			if (way == null) {
				way = new Way();
				way.nodes.add(n0);
				cmds.add(new AddCommand(way));
			} else {
				if (way == replacedWay) {
					way = reuseWay;
				} else {
					Way wnew = new Way(way);
					cmds.add(new ChangeCommand(way, wnew));
					way = wnew;
				}
			}

			if (way.nodes.get(way.nodes.size() - 1) == n0) {
				way.nodes.add(n);
			} else {
				way.nodes.add(0, n);
			}

			extendedWay = true;
		}

		String title;
		if (!extendedWay && !newNode) {
			return; // We didn't do anything.
		} else if (!extendedWay) {
			if (reuseWay == null) {
				title = tr("Add node");
			} else {
				title = tr("Add node into way");
			}
		} else if (!newNode) {
			title = tr("Connect existing way to node");
		} else if (reuseWay == null) {
			title = tr("Add a new node to an existing way");
		} else {
			title = tr("Add node into way and connect");
		}

		Command c = new SequenceCommand(title, cmds);
	
		Main.main.undoRedo.add(c);
		Main.ds.setSelected(n);
		Main.map.mapView.repaint();
	}
	
	/**
	 * @return If the node is the end of exactly one way, return this. 
	 * 	<code>null</code> otherwise.
	 */
	public static Way getWayForNode(Node n) {
		Way way = null;
		for (Way w : Main.ds.ways) {
			if (w.nodes.size() < 1) continue;
			Node firstNode = w.nodes.get(0);
			Node lastNode = w.nodes.get(w.nodes.size() - 1);
			if ((firstNode == n || lastNode == n) && (firstNode != lastNode)) {
				if (way != null)
					return null;
				way = w;
			}
		}
		return way;
	}
	
	private Way splitWaySegmentAtNode(WaySegment ws, Node n, Collection<Command> cmds) {
		Way wnew = new Way(ws.way);
		wnew.nodes.add(ws.lowerIndex + 1, n);
		cmds.add(new ChangeCommand(ws.way, wnew));
		return wnew;
	}
}
