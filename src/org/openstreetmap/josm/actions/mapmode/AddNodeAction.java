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
public class AddNodeAction extends MapMode {

	enum Mode {node, nodeway, autonode}
	private final Mode mode;

	public static class AddNodeGroup extends GroupAction {
		public AddNodeGroup(MapFrame mf) {
			super(KeyEvent.VK_N,0);
			putValue("help", "Action/AddNode");
			actions.add(new AddNodeAction(mf,tr("Add node"), Mode.node, tr("Add a new node to the map")));
			actions.add(new AddNodeAction(mf, tr("Add node into way"), Mode.nodeway,tr( "Add a node into an existing way")));
			actions.add(new AddNodeAction(mf, tr("Add node and connect"), Mode.autonode,tr( "Add a node and connect it to the selected node (with CTRL: add node into way; with SHIFT: re-use existing node)")));
			setCurrent(0);
		}
	}

	public AddNodeAction(MapFrame mapFrame, String name, Mode mode, String desc) {
		super(name, "node/"+mode, desc, mapFrame, getCursor());
		this.mode = mode;
		putValue("help", "Action/AddNode/"+Character.toUpperCase(mode.toString().charAt(0))+mode.toString().substring(1));
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

		Node n = new Node(Main.map.mapView.getLatLon(e.getX(), e.getY()));
		if (n.coor.isOutSideWorld()) {
			JOptionPane.showMessageDialog(Main.parent,tr("Cannot add a node outside of the world."));
			return;
		}

		Command c = new AddCommand(n);
		if (mode == Mode.nodeway) {
			WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
			if (ws == null)
				return;
			
			// see if another segment is also near
			WaySegment other = Main.map.mapView.getNearestWaySegment(e.getPoint(),
				Collections.singleton(ws));

			Node n1 = ws.way.nodes.get(ws.lowerIndex),
				n2 = ws.way.nodes.get(ws.lowerIndex + 1);

			if (other == null && (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) == 0) {
				// moving the new point to the perpendicular point
				// FIXME: when two way segments are split, should move the new point to the
				// intersection point!
				EastNorth A = n1.eastNorth;
				EastNorth B = n2.eastNorth;
				double ab = A.distance(B);
				double nb = n.eastNorth.distance(B);
				double na = n.eastNorth.distance(A);
				double q = (nb-na+ab)/ab/2;
				n.eastNorth = new EastNorth(B.east() + q*(A.east()-B.east()), B.north() + q*(A.north()-B.north()));
				n.coor = Main.proj.eastNorth2latlon(n.eastNorth);
			}

			Collection<Command> cmds = new LinkedList<Command>();
			cmds.add(c);
			
			// split the first segment
			splitWaySegmentAtNode(ws, n, cmds);
			
			// if a second segment was found, split that as well
			if (other != null) splitWaySegmentAtNode(other, n, cmds);

			c = new SequenceCommand(tr((other == null) ? 
				"Add node into way" : "Add common node into two ways"), cmds);
		}

		// Add a node and connecting segment.
		if (mode == Mode.autonode) {

			WaySegment insertInto = null;
			Node reuseNode = null;
			
			// If CTRL is held, insert the node into a potentially existing way segment
			if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
				insertInto = Main.map.mapView.getNearestWaySegment(e.getPoint());
				if (insertInto == null) System.err.println("Couldn't find nearby way segment");
				if (insertInto == null)
					return;
			} 
			// If SHIFT is held, instead of creating a new node, re-use an existing
			// node (making this action identical to AddSegmentAction with the
			// small difference that the node used will then be selected to allow
			// continuation of the "add node and connect" stuff)
			else if ((e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
				OsmPrimitive clicked = Main.map.mapView.getNearest(e.getPoint());
				if (clicked == null || !(clicked instanceof Node))
					return;
				reuseNode = (Node) clicked;
			}
			
			Collection<OsmPrimitive> selection = Main.ds.getSelected();
			if (selection.size() == 1 && selection.iterator().next() instanceof Node) {
				Node n1 = (Node)selection.iterator().next();

				Collection<Command> cmds = new LinkedList<Command>();
				
				if (reuseNode != null) {
					// in re-use node mode, n1 must not be identical to clicked node
					if (n1 == reuseNode) System.err.println("n1 == reuseNode");
					if (n1 == reuseNode) return;
					// replace newly created node with existing node
					n = reuseNode;
				} else {
					// only add the node creation command if we're not re-using
					cmds.add(c);
				}
				
				/* Keep track of the way we change, it might be the same into
				 * which we insert the node.
				 */
				Way newInsertInto = null;
				if (insertInto != null)
					newInsertInto = splitWaySegmentAtNode(insertInto, n, cmds);

				Way way = getWayForNode(n1);
				if (way == null) {
					way = new Way();
					way.nodes.add(n1);
					cmds.add(new AddCommand(way));
				} else {
					if (insertInto != null) {
						if (way == insertInto.way) {
							way = newInsertInto;
						}
					} else {
						Way wnew = new Way(way);
						cmds.add(new ChangeCommand(way, wnew));
						way = wnew;
					}
				}

				if (way.nodes.get(way.nodes.size() - 1) == n1) {
					way.nodes.add(n);
				} else {
					way.nodes.add(0, n);
				}

				c = new SequenceCommand(tr((insertInto == null) ? "Add node and connect" : "Add node into way and connect"), cmds);
			}	
		}		
	
		Main.main.undoRedo.add(c);
		Main.ds.setSelected(n);
		Main.map.mapView.repaint();
	}
	
	/**
	 * @return If the node is the end of exactly one way, return this. 
	 * 	<code>null</code> otherwise.
	 */
	private Way getWayForNode(Node n) {
		Way way = null;
		for (Way w : Main.ds.ways) {
			if (w.nodes.size() < 1) continue;
			int i = w.nodes.indexOf(n);
			if (w.nodes.get(0) == n || w.nodes.get(w.nodes.size() - 1) == n) {
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
