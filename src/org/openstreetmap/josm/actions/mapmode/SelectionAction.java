// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.actions.GroupAction;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.SelectionManager;
import org.openstreetmap.josm.gui.SelectionManager.SelectionEnded;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * This MapMode enables the user to easy make a selection of different objects.
 *
 * The selected objects are drawn in a different style.
 *
 * Holding and dragging the left mouse button draws an selection rectangle.
 * When releasing the left mouse button, all objects within the rectangle get
 * selected.
 *
 * When releasing the left mouse button while the right mouse button pressed,
 * nothing happens (the selection rectangle will be cleared, however).
 *
 * When releasing the mouse button and one of the following keys was hold:
 *
 * If Alt key was hold, select all objects that are touched by the
 * selection rectangle. If the Alt key was not hold, select only those objects
 * completly within (e.g. for ways mean: only if all nodes of the way are
 * within).
 *
 * If Shift key was hold, the objects are added to the current selection. If
 * Shift key wasn't hold, the current selection get replaced.
 *
 * If Ctrl key was hold, remove all objects under the current rectangle from
 * the active selection (if there were any). Nothing is added to the current
 * selection.
 *
 * Alt can be combined with Ctrl or Shift. Ctrl and Shift cannot be combined.
 * If both are pressed, nothing happens when releasing the mouse button.
 *
 * The user can also only click on the map. All total movements of 2 or less
 * pixel are considered "only click". If that happens, the nearest Node will
 * be selected if there is any within 10 pixel range. If there is no Node within
 * 10 pixel, the nearest Way within 10 pixel range is selected. If there is no
 * Way within 10 pixel and the user clicked in or 10 pixel away from an area,
 * this area is selected.  If there is even no area, nothing is selected.
 * Shift and Ctrl key applies to this as usual. For more,
 * @see MapView#getNearest(Point)
 *
 * @author imi
 */
public class SelectionAction extends MapMode implements SelectionEnded {

	public static class Group extends GroupAction {
		public Group(MapFrame mf) {
			super(KeyEvent.VK_S,0);
			putValue("help", "Action/Selection");
			actions.add(new SelectionAction(mf, tr("Selection"), tr("Select objects by dragging or clicking.")));
			setCurrent(0);
		}
	}


	/**
	 * The SelectionManager that manages the selection rectangle.
	 */
	private SelectionManager selectionManager;

	/**
	 * Create a new SelectionAction in the given frame.
	 * @param mapFrame The frame this action belongs to
	 */
	public SelectionAction(MapFrame mapFrame, String name, String desc) {
		super(name, "selection/select", desc, mapFrame, ImageProvider.getCursor("normal", "selection"));
		putValue("help", "Action/Selection");
		this.selectionManager = new SelectionManager(this, false, mapFrame.mapView);
	}

	@Override public void enterMode() {
		super.enterMode();
			selectionManager.register(Main.map.mapView);
	}

	@Override public void exitMode() {
		super.exitMode();
			selectionManager.unregister(Main.map.mapView);
	}


	/**
	 * Check the state of the keys and buttons and set the selection accordingly.
	 */
	public void selectionEnded(Rectangle r, boolean alt, boolean shift, boolean ctrl) {
		selectEverythingInRectangle(selectionManager, r, alt, shift, ctrl);
	}

	public static void selectEverythingInRectangle(SelectionManager selectionManager, Rectangle r, boolean alt, boolean shift, boolean ctrl) {
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
