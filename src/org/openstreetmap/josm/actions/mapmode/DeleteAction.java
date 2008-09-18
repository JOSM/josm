// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Collections;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which gets deleted if possible. When Ctrl is 
 * pressed when releasing the button, the objects and all its references are 
 * deleted. The exact definition of "all its references" are in
 * {@link #deleteWithReferences deleteWithReferences}.
 *
 * If the user did not press Ctrl and the object has any references, the user
 * is informed and nothing is deleted.
 *
 * If the user enters the mapmode and any object is selected, all selected
 * objects that can be deleted will.
 * 
 * @author imi
 */
public class DeleteAction extends MapMode {

	/**
	 * Construct a new DeleteAction. Mnemonic is the delete - key.
	 * @param mapFrame The frame this action belongs to.
	 */
	public DeleteAction(MapFrame mapFrame) {
		super(tr("Delete Mode"),
				"delete", 
				tr("Delete nodes or ways."), 
				KeyEvent.VK_D, 
				mapFrame, 
				ImageProvider.getCursor("normal", "delete"));
	}

	@Override public void enterMode() {
		super.enterMode();
		Main.map.mapView.addMouseListener(this);
	}

	@Override public void exitMode() {
		super.exitMode();
		Main.map.mapView.removeMouseListener(this);
	}

	
	@Override public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		if(!Main.map.mapView.isDrawableLayer())
			return;
		doActionPerformed(e);
	}

	public void doActionPerformed(ActionEvent e) {
		if(!Main.map.mapView.isDrawableLayer())
			return;
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;

		Command c;
		if (ctrl) {
			c = DeleteCommand.deleteWithReferences(Main.ds.getSelected());
		} else {
			c = DeleteCommand.delete(Main.ds.getSelected(), !alt);
		}
		if (c != null) {
			Main.main.undoRedo.add(c);
		}

		Main.map.repaint();
	}

	/**
	 * If user clicked with the left button, delete the nearest object.
	 * position.
	 */
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		if(!Main.map.mapView.isDrawableLayer())
			return;
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
		boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
		
		OsmPrimitive sel = Main.map.mapView.getNearestNode(e.getPoint());
		Command c = null;
		if (sel == null) {
			WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
			if (ws != null) {
				if (shift) {
					c = DeleteCommand.deleteWaySegment(ws); 
				} else if (ctrl) {
					c = DeleteCommand.deleteWithReferences(Collections.singleton((OsmPrimitive)ws.way));
				} else {
					c = DeleteCommand.delete(Collections.singleton((OsmPrimitive)ws.way), !alt);
				}
			}
		} else if (ctrl) {
			c = DeleteCommand.deleteWithReferences(Collections.singleton(sel));
		} else {
			c = DeleteCommand.delete(Collections.singleton(sel), !alt);
		}
		if (c != null) {
			Main.main.undoRedo.add(c);
		}

		Main.map.mapView.repaint();
	}
	
	@Override public String getModeHelpText() {
		return tr("Click to delete. Shift: delete way segment. Alt: don't delete unused nodes when deleting a way. Ctrl: delete referring objects.");
	}
}
