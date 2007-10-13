// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GroupAction;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RotateCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.tools.ImageProvider;
/**
 * Move is an action that can move all kind of OsmPrimitives (except Keys for now).
 *
 * If an selected object is under the mouse when dragging, move all selected objects.
 * If an unselected object is under the mouse when dragging, it becomes selected
 * and will be moved.
 * If no object is under the mouse, move all selected objects (if any)
 * 
 * @author imi
 */
public class SelectAction extends MapMode implements SelectionEnded {

	enum Mode { move, rotate, select }
	private Mode mode = null;

	/**
	 * The old cursor before the user pressed the mouse button.
	 */
	private Cursor oldCursor;
	/**
	 * The position of the mouse before the user moves a node.
	 */
	private Point mousePos;
	private SelectionManager selectionManager;

	/**
	 * Create a new MoveAction
	 * @param mapFrame The MapFrame, this action belongs to.
	 */
	public SelectAction(MapFrame mapFrame) {
		super(tr("Select"), "move/move", tr("Select, move and rotate objects"),
			KeyEvent.VK_S, mapFrame,
			getCursor("normal", "selection", Cursor.DEFAULT_CURSOR));
		putValue("help", "Action/Move/Move");
		selectionManager = new SelectionManager(this, false, mapFrame.mapView);
	}

	private static Cursor getCursor(String name, String mod, int def) {
		try {
	        return ImageProvider.getCursor(name, mod);
        } catch (Exception e) {
        }
	    return Cursor.getPredefinedCursor(def);
    }

	private static Cursor getCursor(String name, int def) {
		return getCursor(name, null, def);
	}

	private void setCursor(Cursor c) {
		if (oldCursor == null) {
			oldCursor = Main.map.mapView.getCursor();
			Main.map.mapView.setCursor(c);
		}
	}

	private void restoreCursor() {
		if (oldCursor != null) {
			Main.map.mapView.setCursor(oldCursor);
			oldCursor = null;
		}
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
	}

	/**
	 * If the left mouse button is pressed, move all currently selected
	 * objects (if one of them is under the mouse) or the current one under the
	 * mouse (which will become selected).
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if (mode == Mode.select) return;

		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		if (mode == Mode.move) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		if (mousePos == null) {
			mousePos = e.getPoint();
		}
		
		EastNorth mouseEN = Main.map.mapView.getEastNorth(e.getX(), e.getY());
		EastNorth mouseStartEN = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
		double dx = mouseEN.east() - mouseStartEN.east();
		double dy = mouseEN.north() - mouseStartEN.north();
		if (dx == 0 && dy == 0)
			return;

		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
		
		// when rotating, having only one node makes no sense - quit silently
		if (mode == Mode.rotate && affectedNodes.size() < 2) 
			return;
		

		// check if any coordinate would be outside the world
		for (OsmPrimitive osm : affectedNodes) {
			if (osm instanceof Node && ((Node)osm).coor.isOutSideWorld()) {
				JOptionPane.showMessageDialog(Main.parent,
					tr("Cannot move objects outside of the world."));
				return;
			}
		}
		Command c = !Main.main.undoRedo.commands.isEmpty()
			? Main.main.undoRedo.commands.getLast() : null;

		if (mode == Mode.move) {
			if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).objects))
				((MoveCommand)c).moveAgain(dx,dy);
			else
				Main.main.undoRedo.add(new MoveCommand(selection, dx, dy));
		} else if (mode == Mode.rotate) {
			if (c instanceof RotateCommand && affectedNodes.equals(((RotateCommand)c).objects))
				((RotateCommand)c).rotateAgain(mouseStartEN, mouseEN);
			else
				Main.main.undoRedo.add(new RotateCommand(selection, mouseStartEN, mouseEN));
		}

		Main.map.mapView.repaint();
		mousePos = e.getPoint();
	}

	/**
	 * Look, whether any object is selected. If not, select the nearest node.
	 * If there are no nodes in the dataset, do nothing.
	 * 
	 * If the user did not press the left mouse button, do nothing.
	 * 
	 * Also remember the starting position of the movement and change the mouse 
	 * cursor to movement.
	 */
	@Override public void mousePressed(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
		boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;

		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		OsmPrimitive osm = Main.map.mapView.getNearest(e.getPoint());
		Collection<OsmPrimitive> osmColl;
		if (osm == null) {
			osmColl = Collections.emptySet();
		} else {
			osmColl = Collections.singleton(osm);
		}

		if (ctrl && shift) {
			selectPrims(osmColl, true, false);
			mode = Mode.rotate;
			setCursor(ImageProvider.getCursor("rotate", null));
		} else if (osm != null) {
			selectPrims(osmColl, shift, ctrl);
			mode = Mode.move;
		} else {
			mode = Mode.select;
			oldCursor = Main.map.mapView.getCursor();
			selectionManager.register(Main.map.mapView);
			selectionManager.mousePressed(e);
		}

		updateStatusLine();
		Main.map.mapView.repaint();

		mousePos = e.getPoint();
	}

	/**
	 * Restore the old mouse cursor.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		if (mode == Mode.select) {
			selectionManager.unregister(Main.map.mapView);
		}
		restoreCursor();
		updateStatusLine();
		mode = null;
		updateStatusLine();
	}

	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		selectPrims(selectionManager.getObjectsInRectangle(r, alt), shift, ctrl);
	}

	public void selectPrims(Collection<OsmPrimitive> selectionList, boolean shift, boolean ctrl) {
	    if (shift && ctrl)
			return; // not allowed together

		Collection<OsmPrimitive> curSel;
		if (!ctrl && !shift)
			curSel = new LinkedList<OsmPrimitive>(); // new selection will replace the old.
		else
			curSel = Main.ds.getSelected();

		for (OsmPrimitive osm : selectionList)
			if (ctrl)
				curSel.remove(osm);
			else
				curSel.add(osm);
		Main.ds.setSelected(curSel);
		Main.map.mapView.repaint();
    }
	
	@Override public String getModeHelpText() {
		if (mode == Mode.select) {
			return "Release the mouse button to select the objects in the rectangle.";
		} else if (mode == Mode.move) {
			return "Release the mouse button to stop moving.";
		} else if (mode == Mode.rotate) {
			return "Release the mouse button to stop rotating.";
		} else {
			return "Move objects by dragging; Shift to add to selection; Shift-Ctrl to rotate selected; or change selection";
		}
	}
}
