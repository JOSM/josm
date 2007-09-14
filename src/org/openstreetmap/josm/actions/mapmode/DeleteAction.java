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
import org.openstreetmap.josm.data.osm.Segment;
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
 * Pressing Alt will select the way instead of a segment, as usual.
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
				tr("Delete nodes, streets or segments."), 
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
		
		OsmPrimitive sel = Main.map.mapView.getNearest(e.getPoint(), (e.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0);
		if (sel == null)
			return;

		if ((e.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0)
			deleteWithReferences(Collections.singleton(sel));
		else
			delete(Collections.singleton(sel), true, true);

		Main.map.mapView.repaint();
	}

	/**
	 * Delete the primitives and everything they references.
	 * 
	 * If a node is deleted, the node and all segments, ways and areas
	 * the node is part of are deleted as well.
	 * 
	 * If a segment is deleted, all ways the segment is part of 
	 * are deleted as well. No nodes are deleted.
	 * 
	 * If a way is deleted, only the way and no segments or nodes are 
	 * deleted.
	 * 
	 * If an area is deleted, only the area gets deleted.
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
	 * If deleting a node which is part of exactly two segments, and both segments
	 * have no conflicting keys, join them and remove the node.
	 * If the two segments are part of the same way, remove the deleted segment
	 * from the way.
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
					String reason = deleteNodeAndJoinSegment((Node)osm);
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

	private String deleteNodeAndJoinSegment(Node n) {
		ArrayList<Segment> segs = new ArrayList<Segment>(2);
		for (Segment s : Main.ds.segments) {
			if (!s.deleted && (s.from == n || s.to == n)) {
				if (segs.size() > 1)
					return tr("Used by more than two segments.");
				segs.add(s);
			}
		}
		if (segs.size() != 2)
			return tr("Used by only one segment.");
		Segment seg1 = segs.get(0);
		Segment seg2 = segs.get(1);
		if (seg1.from == seg2.to) {
			Segment s = seg1;
			seg1 = seg2;
			seg2 = s;
		}
		if (seg1.from == seg2.from || seg1.to == seg2.to)
			return tr("Wrong direction of segments.");
		for (Entry<String, String> e : seg1.entrySet())
			if (seg2.keySet().contains(e.getKey()) && !seg2.get(e.getKey()).equals(e.getValue()))
				return tr("Conflicting keys");
		ArrayList<Way> ways = new ArrayList<Way>(2);
		for (Way w : Main.ds.ways) {
			if (w.deleted)
				continue;
			if ((w.segments.contains(seg1) && !w.segments.contains(seg2)) || (w.segments.contains(seg2) && !w.segments.contains(seg1)))
				return tr("Segments are part of different ways.");
			if (w.segments.contains(seg1) && w.segments.contains(seg2))
				ways.add(w);
		}
		Segment s = new Segment(seg1);
		s.to = seg2.to;
		if (s.keys == null)
			s.keys = seg2.keys;
		else if (seg2.keys != null)
			s.keys.putAll(seg2.keys);
		Collection<Command> cmds = new LinkedList<Command>();
		for (Way w : ways) {
			Way copy = new Way(w);
			copy.segments.remove(seg2);
			cmds.add(new ChangeCommand(w, copy));
		}
		cmds.add(new ChangeCommand(seg1, s));
		cmds.add(new DeleteCommand(Arrays.asList(new OsmPrimitive[]{n, seg2})));
		Main.main.undoRedo.add(new SequenceCommand(tr("Delete Node"), cmds));
		return null;
    }
}
