// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions.mapmode;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.tools.ImageProvider;

/**
 * An action that enables the user to delete nodes and other objects.
 *
 * The user can click on an object, which get deleted if possible. When Ctrl is 
 * pressed when releasing the button, the objects and all its references are 
 * deleted. The exact definition of "all its references" are in 
 * @see #deleteWithReferences(OsmPrimitive)
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
		super(tr("Delete"), 
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
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		if (ctrl)
			deleteWithReferences(Main.ds.getSelected());
		else
			delete(Main.ds.getSelected(), false, false);
		Main.map.repaint();
	}

	/**
	 * If user clicked with the left button, delete the nearest object.
	 * position.
	 */
	@Override public void mouseClicked(MouseEvent e) {
		if (e.getButton() != MouseEvent.BUTTON1)
			return;
		
		OsmPrimitive sel = Main.map.mapView.getNearest(e.getPoint());
		if (sel == null)
			return;

		if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
			deleteWithReferences(Collections.singleton(sel));
		else
			delete(Collections.singleton(sel), true, true);

		Main.map.mapView.repaint();
	}

	/**
	 * Delete the primitives and everything they reference.
	 * 
	 * If a node is deleted, the node and all ways and relations
	 * the node is part of are deleted as well.
	 * 
	 * If a way is deleted, all relations the way is member of are also deleted.
	 * 
	 * If a way is deleted, only the way and no nodes are deleted.
	 * 
	 * @param selection The list of all object to be deleted.
	 */
	private void deleteWithReferences(Collection<OsmPrimitive> selection) {
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
		for (OsmPrimitive osm : selection)
			osm.visit(v);
		v.data.addAll(selection);
		if (!v.data.isEmpty())
			Main.main.undoRedo.add(new DeleteCommand(v.data));
	}

	/**
	 * Try to delete all given primitives. If a primitive is
	 * used somewhere and that "somewhere" is not going to be deleted,
	 * inform the user and do not delete.
	 * 
	 * If a node is to be deleted which is in the middle of exactly one way,
	 * the node is removed from the way's node list and after that removed
	 * itself.
	 * 
	 * @param selection The objects to delete.
	 * @param msgBox Whether a message box for errors should be shown
	 */
	private void delete(Collection<OsmPrimitive> selection, boolean msgBox, boolean joinIfPossible) {
		Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>();
		for (OsmPrimitive osm : selection) {
			CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
			osm.visit(v);
			if (!selection.containsAll(v.data)) {
				if (osm instanceof Node && joinIfPossible) {
					String reason = deleteNodeAndJoinWay((Node)osm);
					if (reason != null && msgBox) {
						JOptionPane.showMessageDialog(Main.parent,tr("Cannot delete node.")+" "+reason);
						return;
					}
				} else if (msgBox) {
					JOptionPane.showMessageDialog(Main.parent, tr("This object is in use."));
					return;
				}
			} else {
				del.addAll(v.data);
				del.add(osm);
			}
		}
		if (!del.isEmpty())
			Main.main.undoRedo.add(new DeleteCommand(del));
	}

	private String deleteNodeAndJoinWay(Node n) {
		ArrayList<Way> ways = new ArrayList<Way>(1);
		for (Way w : Main.ds.ways) {
			if (!w.deleted && w.nodes.contains(n)) {
				ways.add(w);
		}
		}

		if (ways.size() > 1)
			return tr("Used by more than one way.");
		
		if (ways.size() == 1) {
			// node in way
			Way w = ways.get(0);

			int i = w.nodes.indexOf(n);
			if (w.nodes.lastIndexOf(n) != i)
				return tr("Occurs more than once in the same way.");
			if (i == 0 || i == w.nodes.size() - 1)
				return tr("Is at the end of a way");

			Way wnew = new Way(w);
			wnew.nodes.remove(i);

			Collection<Command> cmds = new LinkedList<Command>();
			cmds.add(new ChangeCommand(w, wnew));
			cmds.add(new DeleteCommand(Collections.singleton(n)));
			Main.main.undoRedo.add(new SequenceCommand(tr("Delete Node"), cmds));
		} else {
			// unwayed node
			Main.main.undoRedo.add(new DeleteCommand(Collections.singleton(n)));	
		}
		return null;
    }
}
