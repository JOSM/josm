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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.data.osm.Relation;
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
		boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;

		Command c;
		if (ctrl) {
			c = deleteWithReferences(Main.ds.getSelected());
		} else {
			c = delete(Main.ds.getSelected(), !alt);
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
		boolean ctrl = (e.getModifiers() & ActionEvent.CTRL_MASK) != 0;
		boolean shift = (e.getModifiers() & ActionEvent.SHIFT_MASK) != 0;
		boolean alt = (e.getModifiers() & ActionEvent.ALT_MASK) != 0;
		
		OsmPrimitive sel = Main.map.mapView.getNearestNode(e.getPoint());
		Command c = null;
		if (sel == null) {
			WaySegment ws = Main.map.mapView.getNearestWaySegment(e.getPoint());
			if (ws != null) {
				if (shift) {
					c = deleteWaySegment(ws); 
				} else if (ctrl) {
					c = deleteWithReferences(Collections.singleton((OsmPrimitive)ws.way));
				} else {
					c = delete(Collections.singleton((OsmPrimitive)ws.way), !alt);
				}
			}
		} else if (ctrl) {
			c = deleteWithReferences(Collections.singleton(sel));
		} else {
			c = delete(Collections.singleton(sel), !alt);
		}
		if (c != null) {
			Main.main.undoRedo.add(c);
		}

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
	 * @return command A command to perform the deletions, or null of there is
	 * nothing to delete.
	 */
	private Command deleteWithReferences(Collection<OsmPrimitive> selection) {
		CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds);
		for (OsmPrimitive osm : selection)
			osm.visit(v);
		v.data.addAll(selection);
		if (v.data.isEmpty()) {
			return null;
		} else {
			return new DeleteCommand(v.data);
		}
	}

	/**
	 * Try to delete all given primitives.
	 *
	 * If a node is used by a way, it's removed from that way.  If a node or a
	 * way is used by a relation, inform the user and do not delete.
	 *
	 * If this would cause ways with less than 2 nodes to be created, delete
	 * these ways instead.  If they are part of a relation, inform the user
	 * and do not delete.
	 * 
	 * @param selection The objects to delete.
	 * @param alsoDeleteNodesInWay true if nodes should be deleted as well
	 * @return command A command to perform the deletions, or null of there is
	 * nothing to delete.
	 */
	private Command delete(Collection<OsmPrimitive> selection, boolean alsoDeleteNodesInWay) {
		if (selection.isEmpty()) return null;

		Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>(selection);
		Collection<Way> waysToBeChanged = new HashSet<Way>();

		if (alsoDeleteNodesInWay) {
			// Delete untagged nodes that are to be unreferenced.
			Collection<OsmPrimitive> delNodes = new HashSet<OsmPrimitive>();
			for (OsmPrimitive osm : del) {
				if (osm instanceof Way) {
					for (Node n : ((Way)osm).nodes) {
						if (!n.tagged) {
							CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
							n.visit(v);
							if (v.data.size() == 1) {
								delNodes.add(n);
							}
						}
					}
				}
			}
			del.addAll(delNodes);
		}
		
		for (OsmPrimitive osm : del) {
			CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
			osm.visit(v);
			for (OsmPrimitive ref : v.data) {
				if (del.contains(ref)) continue;
				if (ref instanceof Way) {
					waysToBeChanged.add((Way) ref);
				} else if (ref instanceof Relation) {
					JOptionPane.showMessageDialog(Main.parent,
						tr("Cannot delete: Selection is used by relation"));
					return null;
				} else {
					return null;
				}
			}
		}

		Collection<Command> cmds = new LinkedList<Command>();
		for (Way w : waysToBeChanged) {
			Way wnew = new Way(w);
			wnew.nodes.removeAll(del);
			if (wnew.nodes.size() < 2) {
				del.add(w);

				CollectBackReferencesVisitor v = new CollectBackReferencesVisitor(Main.ds, false);
				w.visit(v);
				for (OsmPrimitive ref : v.data) {
					if (del.contains(ref)) continue;
					if (ref instanceof Relation) {
						JOptionPane.showMessageDialog(Main.parent,
							tr("Cannot delete: Selection is used by relation"));
					} else {
						return null;
					}
				}
			} else {
				cmds.add(new ChangeCommand(w, wnew));
			}
		}

		if (!del.isEmpty()) cmds.add(new DeleteCommand(del));

		return new SequenceCommand(tr("Delete"), cmds);
	}

	private Command deleteWaySegment(WaySegment ws) {
		List<Node> n1 = new ArrayList<Node>(),
			n2 = new ArrayList<Node>();

		n1.addAll(ws.way.nodes.subList(0, ws.lowerIndex + 1));
		n2.addAll(ws.way.nodes.subList(ws.lowerIndex + 1, ws.way.nodes.size()));

		if (n1.size() < 2 && n2.size() < 2) {
			return new DeleteCommand(Collections.singleton(ws.way));
		}
		
		Way wnew = new Way(ws.way);
		wnew.nodes.clear();

		if (n1.size() < 2) {
			wnew.nodes.addAll(n2);
			return new ChangeCommand(ws.way, wnew);
		} else if (n2.size() < 2) {
			wnew.nodes.addAll(n1);
			return new ChangeCommand(ws.way, wnew);
		} else {
			Collection<Command> cmds = new LinkedList<Command>();

			wnew.nodes.addAll(n1);
			cmds.add(new ChangeCommand(ws.way, wnew));

			Way wnew2 = new Way();
			if (wnew.keys != null) {
				wnew2.keys = new HashMap<String, String>(wnew.keys);
				wnew2.checkTagged();
			}
			wnew2.nodes.addAll(n2);
			cmds.add(new AddCommand(wnew2));

			return new SequenceCommand(tr("Split way segment"), cmds);
		}
	}
	
	@Override public String getModeHelpText() {
		return tr("Click to delete. Shift: delete way segment. Alt: don't delete unused nodes when deleting a way. Ctrl: delete referring objects.");
	}
}
