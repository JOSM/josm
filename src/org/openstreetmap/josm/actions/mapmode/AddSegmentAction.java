// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * The user can add a new segment between two nodes by pressing on the 
 * starting node and dragging to the ending node. 
 * 
 * No segment can be created if there is already a segment containing
 * both nodes.
 * 
 * @author imi
 */
public class AddSegmentAction extends MapMode implements MouseListener {

	/**
	 * The first node the user pressed the button onto.
	 */
	private Node first;
	/**
	 * The second node used if the user releases the button.
	 */
	private Node second;

	/**
	 * Whether the hint is currently drawn on screen.
	 */
	private boolean hintDrawn = false;
	
	/**
	 * Create a new AddSegmentAction.
	 * @param mapFrame The MapFrame this action belongs to.
	 */
	public AddSegmentAction(MapFrame mapFrame) {
		super(tr("Connect two nodes"), 
				"addsegment", 
				tr("Connect two nodes using ways."), 
				KeyEvent.VK_G, 
				mapFrame, 
				ImageProvider.getCursor("normal", "segment"));
	}

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
		Main.map.mapView.addMouseMotionListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
		Main.map.mapView.removeMouseMotionListener(this);
		drawHint(false);
	}

	/**
	 * Called when user hits space bar while dragging.
	 */
	@Override public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		makeSegment();
	}

	/**
	 * If user clicked on a node, from the dragging with that node. 
	 */
	@Override public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;

		Node clicked = Main.map.mapView.getNearestNode(e.getPoint());
		if (clicked == null) return;

		drawHint(false);
		first = second = clicked;
	}

	/**
	 * Draw a hint which nodes will get connected if the user release
	 * the mouse button now.
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		Node hovered = Main.map.mapView.getNearestNode(e.getPoint());
		if (hovered == second) return;

		drawHint(false);
		second = hovered;
		drawHint(true);
	}

	/**
	 * If left button was released, try to create the segment.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			drawHint(false);
			makeSegment();
			first = null;
		}
	}

	/**
	 * Create the segment if first and second are different and there is
	 * not already a segment.
	 */
	private void makeSegment() {
		Node n1 = first;
		Node n2 = second;
		
		// this is to allow continued segment drawing by hitting the space bar
		// at every intermediate node
		first = second;
		second = null;

		if (n1 == null || n2 == null || n1 == n2) return;

		Way w = AddNodeAction.getWayForNode(n1);
		Way wnew;
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		
		if (w == null) {
			// create a new way and add it to the current selection.
			wnew = new Way();
			wnew.nodes.add(n1);
			wnew.nodes.add(n2);
			Main.main.undoRedo.add(new AddCommand(wnew));
			sel.add(wnew);
			Main.ds.setSelected(sel);
		} else {
			// extend an existing way; only add to current selection if
			// it is not already in there.
			wnew = new Way(w);
			if (wnew.nodes.get(wnew.nodes.size() - 1) == n1) {
				wnew.nodes.add(n2);
			} else {
				wnew.nodes.add(0, n2);
			}
			Main.main.undoRedo.add(new ChangeCommand(w, wnew));
			// do not use wnew below; ChangeCommand only uses wnew as a
			// message about changes to be done to w but will not replace w!
			if (!sel.contains(w)) {
				sel.add(w);
			}
			// do not move this into the if block above since it also
			// fires the selection change event which is desired.
			Main.ds.setSelected(sel);
		}

		Main.map.mapView.repaint();
	}

	/**
	 * Draw or remove the hint line, depending on the parameter.
	 */
	private void drawHint(boolean draw) {
		if (draw == hintDrawn)
			return;
		if (first == null || second == null)
			return;
		if (second == first)
			return;

		Graphics g = Main.map.mapView.getGraphics();
		g.setColor(Color.BLACK);
		g.setXORMode(Color.WHITE);
		Point firstDrawn = Main.map.mapView.getPoint(first.eastNorth);
		Point secondDrawn = Main.map.mapView.getPoint(second.eastNorth);
		g.drawLine(firstDrawn.x, firstDrawn.y, secondDrawn.x, secondDrawn.y);
		hintDrawn = !hintDrawn;
	}
}
