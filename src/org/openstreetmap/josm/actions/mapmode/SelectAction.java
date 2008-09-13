// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.MergeNodesAction;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.MoveCommand;
import org.openstreetmap.josm.command.RotateCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.visitor.AllNodesVisitor;
import org.openstreetmap.josm.data.osm.visitor.SimplePaintVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.tools.ImageProvider;
/**
 * Move is an action that can move all kind of OsmPrimitives (except keys for now).
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
	private long mouseDownTime = 0;
	private boolean didMove = false;
	Node virtualNode = null;
	WaySegment virtualWay = null;
	SequenceCommand virtualCmds = null;

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
	 * The time which needs to pass between click and release before something
	 * counts as a move, in milliseconds
	 */
	private int initialMoveDelay = 200;

	/**
	 * The screen distance which needs to be travelled before something
	 * counts as a move, in pixels
	 */
	private int initialMoveThreshold = 15;
	private boolean initialMoveThresholdExceeded = false;
	/**
	 * Create a new SelectAction
	 * @param mapFrame The MapFrame this action belongs to.
	 */
	public SelectAction(MapFrame mapFrame) {
		super(tr("Select"), "move/move", tr("Select, move and rotate objects"),
			KeyEvent.VK_S, mapFrame,
			getCursor("normal", "selection", Cursor.DEFAULT_CURSOR));
		putValue("help", "Action/Move/Move");
		selectionManager = new SelectionManager(this, false, mapFrame.mapView);		
		try { initialMoveDelay = Integer.parseInt(Main.pref.get("edit.initial-move-delay","200")); } catch (NumberFormatException x) {}
		try { initialMoveThreshold = Integer.parseInt(Main.pref.get("edit.initial-move-threshold","5")); } catch (NumberFormatException x) {}
		
	}

	private static Cursor getCursor(String name, String mod, int def) {
		try {
	        return ImageProvider.getCursor(name, mod);
        } catch (Exception e) {
        }
	    return Cursor.getPredefinedCursor(def);
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
		Main.map.mapView.enableVirtualNodes(
		Main.pref.getInteger("mappaint.node.virtual-size", 8) != 0);
	}

	@Override public void exitMode() {
		super.exitMode();
		selectionManager.unregister(Main.map.mapView);
		Main.map.mapView.removeMouseListener(this);
		Main.map.mapView.removeMouseMotionListener(this);
		Main.map.mapView.enableVirtualNodes(false);
	}

	/**
	 * If the left mouse button is pressed, move all currently selected
	 * objects (if one of them is under the mouse) or the current one under the
	 * mouse (which will become selected).
	 */
	@Override public void mouseDragged(MouseEvent e) {
		if (mode == Mode.select) return;

		// do not count anything as a move if it lasts less than 100 milliseconds.
		if ((mode == Mode.move) && (System.currentTimeMillis() - mouseDownTime < initialMoveDelay)) return;

		if ((e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) == 0)
			return;

		if (mode == Mode.move) {
			setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
		}

		if (mousePos == null) {
			mousePos = e.getPoint();
			return;
		}
		
		if (!initialMoveThresholdExceeded) {
			int dxp = mousePos.x - e.getX();
			int dyp = mousePos.y - e.getY();
			int dp = (int) Math.sqrt(dxp*dxp+dyp*dyp);
			if (dp < initialMoveThreshold) return;
			initialMoveThresholdExceeded = true;
		}
		
		EastNorth mouseEN = Main.map.mapView.getEastNorth(e.getX(), e.getY());
		EastNorth mouseStartEN = Main.map.mapView.getEastNorth(mousePos.x, mousePos.y);
		double dx = mouseEN.east() - mouseStartEN.east();
		double dy = mouseEN.north() - mouseStartEN.north();
		if (dx == 0 && dy == 0)
			return;

		if (virtualWay != null)	{
			Collection<Command> virtualCmds = new LinkedList<Command>();
			virtualCmds.add(new AddCommand(virtualNode));
			Way w = virtualWay.way;
			Way wnew = new Way(w);
			wnew.nodes.add(virtualWay.lowerIndex+1, virtualNode);
			virtualCmds.add(new ChangeCommand(w, wnew));
			virtualCmds.add(new MoveCommand(virtualNode, dx, dy));
			Main.main.undoRedo.add(new SequenceCommand(tr("Add and move a virtual new node to way"), virtualCmds));
			selectPrims(Collections.singleton((OsmPrimitive)virtualNode), false, false);
			virtualWay = null;
			virtualNode = null;
		} else {
			Collection<OsmPrimitive> selection = Main.ds.getSelected();
			Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
		
			// when rotating, having only one node makes no sense - quit silently
			if (mode == Mode.rotate && affectedNodes.size() < 2) 
				return;

			Command c = !Main.main.undoRedo.commands.isEmpty()
				? Main.main.undoRedo.commands.getLast() : null;
			if (c instanceof SequenceCommand)
				c = ((SequenceCommand)c).getLastCommand();

			if (mode == Mode.move) {
				if (c instanceof MoveCommand && affectedNodes.equals(((MoveCommand)c).objects))
					((MoveCommand)c).moveAgain(dx,dy);
				else
					Main.main.undoRedo.add(
						c = new MoveCommand(selection, dx, dy));

				for (Node n : affectedNodes) {
					if (n.coor.isOutSideWorld()) {
						// Revert move
						((MoveCommand) c).moveAgain(-dx, -dy);

						JOptionPane.showMessageDialog(Main.parent,
							tr("Cannot move objects outside of the world."));
						return;
					}
				}
			} else if (mode == Mode.rotate) {
				if (c instanceof RotateCommand && affectedNodes.equals(((RotateCommand)c).objects))
					((RotateCommand)c).rotateAgain(mouseStartEN, mouseEN);
				else
					Main.main.undoRedo.add(new RotateCommand(selection, mouseStartEN, mouseEN));
			}
		}

		Main.map.mapView.repaint();
		mousePos = e.getPoint();

		didMove = true;
	}

	private Collection<OsmPrimitive> getNearestCollectionVirtual(Point p) {
		MapView c = Main.map.mapView;
		int snapDistance = Main.pref.getInteger("mappaint.node.virtual-snap-distance", 8);
		snapDistance *= snapDistance;
		OsmPrimitive osm = c.getNearestNode(p);
		virtualWay = null;
		virtualNode = null;
		
		if (osm == null)
		{
			WaySegment nearestWaySeg = c.getNearestWaySegment(p);
			if (nearestWaySeg != null)
			{
				osm = nearestWaySeg.way;
				if(Main.pref.getInteger("mappaint.node.virtual-size", 8) > 0)
				{
					Way w = (Way)osm;
					Point p1 = c.getPoint(w.nodes.get(nearestWaySeg.lowerIndex).eastNorth);
					Point p2 = c.getPoint(w.nodes.get(nearestWaySeg.lowerIndex+1).eastNorth);
					if(SimplePaintVisitor.isLargeSegment(p1, p2, Main.pref.getInteger("mappaint.node.virtual-space", 70)))
					{
						Point pc = new Point((p1.x+p2.x)/2, (p1.y+p2.y)/2);
						if (p.distanceSq(pc) < snapDistance)
						{
							virtualWay = nearestWaySeg;
							virtualNode = new Node(Main.map.mapView.getLatLon(pc.x, pc.y));
							osm = w;
						}
					}
				}
			}
		}
		if (osm == null) 
			return Collections.emptySet();
		return Collections.singleton(osm);
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
		if (! (Boolean)this.getValue("active")) return;
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		// boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
		boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
		
		mouseDownTime = System.currentTimeMillis();
		didMove = false;
		initialMoveThresholdExceeded = false;

		Collection<OsmPrimitive> osmColl = getNearestCollectionVirtual(e.getPoint());

		if (ctrl && shift) {
			if (Main.ds.getSelected().isEmpty()) selectPrims(osmColl, true, false);
			mode = Mode.rotate;
			setCursor(ImageProvider.getCursor("rotate", null));
		} else if (!osmColl.isEmpty()) {
			// Don't replace the selection now if the user clicked on a
			// selected object (this would break moving of selected groups).
			// We'll do that later in mouseReleased if the user didn't try to
			// move.
			selectPrims(osmColl,
				shift || Main.ds.getSelected().containsAll(osmColl),
				ctrl);
			mode = Mode.move;
		} else {
			mode = Mode.select;
			oldCursor = Main.map.mapView.getCursor();
			selectionManager.register(Main.map.mapView);
			selectionManager.mousePressed(e);
		}
		if(mode != Mode.move || shift || ctrl)
		{
			virtualNode = null;
			virtualWay = null;
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

		if (mode == Mode.move) {
			boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
			boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
			if (!didMove) {
				selectPrims(
					Main.map.mapView.getNearestCollection(e.getPoint()),
					shift, ctrl);
			} else if (ctrl) {
				Collection<OsmPrimitive> selection = Main.ds.getSelected();
				Collection<Node> affectedNodes = AllNodesVisitor.getAllNodes(selection);
				Collection<Node> nn = Main.map.mapView.getNearestNodes(e.getPoint(), affectedNodes);
				if (nn != null) {
					Node n = nn.iterator().next();
				    LinkedList<Node> selNodes = new LinkedList<Node>();
				    for (OsmPrimitive osm : selection)
						if (osm instanceof Node)
							selNodes.add((Node)osm);
					if (selNodes.size() > 0) {
						selNodes.add(n);
						MergeNodesAction.mergeNodes(selNodes, n);
					}
				}
			}
		}

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
			return tr("Release the mouse button to select the objects in the rectangle.");
		} else if (mode == Mode.move) {
			return tr("Release the mouse button to stop moving. Ctrl to merge with nearest node.");
		} else if (mode == Mode.rotate) {
			return tr("Release the mouse button to stop rotating.");
		} else {
			return tr("Move objects by dragging; Shift to add to selection; Shift-Ctrl to rotate selected; or change selection");
		}
	}
}
