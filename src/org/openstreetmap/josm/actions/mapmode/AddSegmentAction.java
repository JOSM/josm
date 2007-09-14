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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
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
		super(tr("Add segment"), 
				"addsegment", 
				tr("Add a segment between two nodes."), 
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

		OsmPrimitive clicked = Main.map.mapView.getNearest(e.getPoint(), true);
		if (clicked == null || !(clicked instanceof Node))
			return;

		drawHint(false);
		first = second = (Node)clicked;
	}

	/**
	 * Draw a hint which nodes will get connected if the user release
	 * the mouse button now.
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		OsmPrimitive clicked = Main.map.mapView.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (clicked == null || clicked == second || !(clicked instanceof Node))
			return;

		drawHint(false);

		second = (Node)clicked;
		drawHint(true);
	}

	/**
	 * If left button was released, try to create the segment.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			makeSegment();
			first = null; // release segment drawing
		}
	}

	/**
	 * Create the segment if first and second are different and there is
	 * not already a segment.
	 */
	private void makeSegment() {
		if (first == null || second == null) {
			first = null;
			second = null;
			return;
		}

		drawHint(false);
		
		Node start = first;
		Node end = second;
		first = second;
		second = null;
		
		if (start != end) {
			// try to find a segment
			for (Segment ls : Main.ds.segments)
				if (!ls.deleted && ((start == ls.from && end == ls.to) || (end == ls.from && start == ls.to)))
					return; // already a segment here - be happy, do nothing.

			Segment ls = new Segment(start, end);
			Main.main.undoRedo.add(new AddCommand(ls));
			Collection<OsmPrimitive> sel = Main.ds.getSelected();
			sel.add(ls);
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
