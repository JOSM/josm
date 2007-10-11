// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
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
	
	enum Mode {move, rotate}
	private final Mode mode;

	public static class SelectGroup extends GroupAction {
		public SelectGroup(MapFrame mf) {
			super(KeyEvent.VK_S,0);
			putValue("help", "Action/Move");
			actions.add(new SelectAction(mf, tr("Select/Move"), Mode.move, tr("Select and move around objects that are under the mouse or selected.")));
			actions.add(new SelectAction(mf, tr("Rotate"), Mode.rotate, tr("Rotate selected nodes around centre")));
			setCurrent(0);
		}
	}
	
	/**
	 * The old cursor before the user pressed the mouse button.
	 */
	private Cursor oldCursor;
	/**
	 * The position of the mouse before the user moves a node.
	 */
	private Point mousePos;
	private SelectionManager selectionManager;
	private boolean selectionMode = false;

	/**
	 * Create a new MoveAction
	 * @param mapFrame The MapFrame, this action belongs to.
	 */
	public SelectAction(MapFrame mapFrame, String name, Mode mode, String desc) {
		super(name, "move/"+mode, desc, mapFrame, getCursor());
		this.mode = mode;
		putValue("help", "Action/Move/"+Character.toUpperCase(mode.toString().charAt(0))+mode.toString().substring(1));
		selectionManager = new SelectionManager(this, false, mapFrame.mapView);
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
		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		if (selectionMode)
			return;

		if (mousePos == null)
			mousePos = e.getPoint();
		
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
				JOptionPane.showMessageDialog(Main.parent,tr("Cannot move objects outside of the world."));
				return;
			}
		}
		Command c = !Main.main.undoRedo.commands.isEmpty() ? Main.main.undoRedo.commands.getLast() : null;

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

		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		OsmPrimitive osm = Main.map.mapView.getNearest(e.getPoint());
		if (osm != null) {
			if (!sel.contains(osm))
				Main.ds.setSelected(osm);
			oldCursor = Main.map.mapView.getCursor();
			
			if (mode == Mode.move) {
				Main.map.mapView.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
			} else {
				Main.map.mapView.setCursor(ImageProvider.getCursor("rotate", null));
			}
		} else {
			selectionMode = true;
			selectionManager.register(Main.map.mapView);
			selectionManager.mousePressed(e);
		}

		Main.map.mapView.repaint();

		mousePos = e.getPoint();
	}

	/**
	 * Restore the old mouse cursor.
	 */
	@Override public void mouseReleased(MouseEvent e) {
		if (selectionMode) {
			selectionManager.unregister(Main.map.mapView);
			selectionMode = false;
		} else
			Main.map.mapView.setCursor(oldCursor);
	}

	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		SelectionAction.selectEverythingInRectangle(selectionManager, r, alt, shift, ctrl);
	}

	public static void selectEverythingInRectangle(
			SelectionManager selectionManager, Rectangle r,
			boolean alt, boolean shift, boolean ctrl) {
	    if (shift && ctrl)
			return; // not allowed together

		Collection<OsmPrimitive> curSel;
		if (!ctrl && !shift)
			curSel = new LinkedList<OsmPrimitive>(); // new selection will replace the old.
		else
			curSel = Main.ds.getSelected();

		Collection<OsmPrimitive> selectionList = selectionManager.getObjectsInRectangle(r,alt);
		for (OsmPrimitive osm : selectionList)
			if (ctrl)
				curSel.remove(osm);
			else
				curSel.add(osm);
		Main.ds.setSelected(curSel);
		Main.map.mapView.repaint();
    }
}
