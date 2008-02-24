// License: GPL. Copyright 2007 by Immanuel Scholz and others
// Author: David Earl
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import java.util.Collection;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

public final class CopyAction extends JosmAction implements SelectionChangedListener {

	private LinkedList<JosmAction> listeners;
	
	public CopyAction() {
		super(tr("Copy"), "copy",
				tr("Copy selected objects to paste buffer."),
				KeyEvent.VK_C, KeyEvent.CTRL_MASK, true);
		setEnabled(false);
		DataSet.selListeners.add(this);
		listeners = new LinkedList<JosmAction>();
	}

	@Override public void addListener(JosmAction a) {
		listeners.add(a);
	}
	
	public void actionPerformed(ActionEvent e) {
		Collection<OsmPrimitive> sel = Main.ds.getSelected();
		if (sel.isEmpty()) { 
			JOptionPane.showMessageDialog(Main.parent,
					tr("Please select something to copy."));	
			return;
		}

		/* New pasteBuffer - will be assigned to the global one at the end */
		final DataSet pasteBuffer = new DataSet();
		final HashMap<OsmPrimitive,OsmPrimitive> map = new HashMap<OsmPrimitive,OsmPrimitive>();
		/* temporarily maps old nodes to new so we can do a true deep copy */

		/* scan the selected objects, mapping them to copies; when copying a way or relation, 
		 * the copy references the copies of their child objects */
		new Visitor(){
			public void visit(Node n) {
				/* check if already in pasteBuffer - e.g. two ways are selected which share a node; 
				 * or a way and a node in that way is selected, we'll see it twice, once via the 
				 * way and once directly; and so on. */
				if (map.containsKey(n)) { return; }
				Node nnew = new Node(n);
				map.put(n, nnew);
				pasteBuffer.addPrimitive(nnew);
			}
			public void visit(Way w) {
				/* check if already in pasteBuffer - could have come from a relation, and directly etc. */
				if (map.containsKey(w)) { return; }
				Way wnew = new Way();
				wnew.cloneFrom(w);
				wnew.nodes.clear();
				List<Node> nodes = new ArrayList<Node>();
				for (Node n : w.nodes) {
					if (! map.containsKey(n)) {
						n.visit(this);
					}
					nodes.add((Node)map.get(n));
				}
				wnew.nodes.clear();
				wnew.nodes.addAll(nodes);
				pasteBuffer.addPrimitive(wnew);
			}
			public void visit(Relation e) {
				if (map.containsKey(e)) { return; }
				Relation enew = new Relation(e);
				List<RelationMember> members = new ArrayList<RelationMember>();
				for (RelationMember m : e.members) {
					if (! map.containsKey(m.member)) {
						m.member.visit(this);
					}
					RelationMember mnew = new RelationMember(m);
					mnew.member = map.get(m.member);
					members.add(mnew);
				}
				enew.members.addAll(members);
				pasteBuffer.addPrimitive(enew);
			}
			public void visitAll() {
				for (OsmPrimitive osm : Main.ds.getSelected())
					osm.visit(this);
			}
		}.visitAll();

		Main.pasteBuffer = pasteBuffer;
		Main.main.menu.paste.setEnabled(true); /* now we have a paste buffer we can make paste available */
		
		for(JosmAction a : listeners) {
			a.pasteBufferChanged(Main.pasteBuffer);
		}
	}

	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(! newSelection.isEmpty());
	}
}
