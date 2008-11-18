// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.AddCommand;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;
import org.openstreetmap.josm.tools.Shortcut;

/**
 * Splits a way into multiple ways (all identical except for their node list).
 *
 * Ways are just split at the selected nodes.  The nodes remain in their
 * original order.  Selected nodes at the end of a way are ignored.
 */

public class SplitWayAction extends JosmAction implements SelectionChangedListener {

	private Way selectedWay;
	private List<Node> selectedNodes;

	/**
	 * Create a new SplitWayAction.
	 */
	public SplitWayAction() {
		super(tr("Split Way"), "splitway", tr("Split a way at the selected node."),
		Shortcut.registerShortcut("tools:splitway", tr("Tool: {0}", tr("Split Way")), KeyEvent.VK_P, Shortcut.GROUP_EDIT), true);
		DataSet.selListeners.add(this);
	}

	/**
	 * Called when the action is executed.
	 *
	 * This method performs an expensive check whether the selection clearly defines one
	 * of the split actions outlined above, and if yes, calls the splitWay method.
	 */
	public void actionPerformed(ActionEvent e) {

		Collection<OsmPrimitive> selection = Main.ds.getSelected();

		if (!checkSelection(selection)) {
			JOptionPane.showMessageDialog(Main.parent, tr("The current selection cannot be used for splitting."));
			return;
		}

		selectedWay = null;
		selectedNodes = null;

		Visitor splitVisitor = new Visitor(){
			public void visit(Node n) {
				if (selectedNodes == null)
					selectedNodes = new LinkedList<Node>();
				selectedNodes.add(n);
            }
			public void visit(Way w) {
				selectedWay = w;
            }
			public void visit(Relation e) {
				// enties are not considered
			}
		};

		for (OsmPrimitive p : selection)
			p.visit(splitVisitor);

		// If only nodes are selected, try to guess which way to split. This works if there
		// is exactly one way that all nodes are part of.
		if (selectedWay == null && selectedNodes != null) {
			HashMap<Way, Integer> wayOccurenceCounter = new HashMap<Way, Integer>();
			for (Node n : selectedNodes) {
				for (Way w : Main.ds.ways) {
					if (w.deleted || w.incomplete) continue;
					int last = w.nodes.size()-1;
					if(last <= 0) continue; // zero or one node ways
					Boolean circular = w.nodes.get(0).equals(w.nodes.get(last));
					int i = 0;
					for (Node wn : w.nodes) {
						if ((circular || (i > 0 && i < last)) && n.equals(wn)) {
							Integer old = wayOccurenceCounter.get(w);
							wayOccurenceCounter.put(w, (old == null) ? 1 : old+1);
							break;
						}
						i++;
					}
				}
			}
			if (wayOccurenceCounter.isEmpty()) {
				JOptionPane.showMessageDialog(Main.parent,
						trn("The selected node is no inner part of any way.",
								"The selected nodes are no inner part of any way.", selectedNodes.size()));
				return;
			}

			for (Entry<Way, Integer> entry : wayOccurenceCounter.entrySet()) {
				if (entry.getValue().equals(selectedNodes.size())) {
					if (selectedWay != null) {
						JOptionPane.showMessageDialog(Main.parent, tr("There is more than one way using the node(s) you selected. Please select the way also."));
						return;
					}
					selectedWay = entry.getKey();
				}
			}

			if (selectedWay == null) {
				JOptionPane.showMessageDialog(Main.parent, tr("The selected nodes do not share the same way."));
				return;
			}

		// If a way and nodes are selected, verify that the nodes are part of the way.
		} else if (selectedWay != null && selectedNodes != null) {

			HashSet<Node> nds = new HashSet<Node>(selectedNodes);
			for (Node n : selectedWay.nodes) {
				nds.remove(n);
			}
			if (!nds.isEmpty()) {
				JOptionPane.showMessageDialog(Main.parent,
						trn("The selected way does not contain the selected node.",
								"The selected way does not contain all the selected nodes.", selectedNodes.size()));
				return;
			}
		}

		// and then do the work.
		splitWay();
	}

	/**
	 * Checks if the selection consists of something we can work with.
	 * Checks only if the number and type of items selected looks good;
	 * does not check whether the selected items are really a valid
	 * input for splitting (this would be too expensive to be carried
	 * out from the selectionChanged listener).
	 */
	private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
		boolean way = false;
		boolean node = false;
		for (OsmPrimitive p : selection) {
			if (p instanceof Way && !way) {
				way = true;
			} else if (p instanceof Node) {
				node = true;
			} else {
				return false;
		}
		}
		return node;
	}

	/**
	 * Split a way into two or more parts, starting at a selected node.
	 */
	private void splitWay() {
		// We take our way's list of nodes and copy them to a way chunk (a
		// list of nodes).  Whenever we stumble upon a selected node, we start
		// a new way chunk.

		Set<Node> nodeSet = new HashSet<Node>(selectedNodes);
		List<List<Node>> wayChunks = new LinkedList<List<Node>>();
		List<Node> currentWayChunk = new ArrayList<Node>();
		wayChunks.add(currentWayChunk);

		Iterator<Node> it = selectedWay.nodes.iterator();
		while (it.hasNext()) {
			Node currentNode = it.next();
			boolean atEndOfWay = currentWayChunk.isEmpty() || !it.hasNext();
			currentWayChunk.add(currentNode);
			if (nodeSet.contains(currentNode) && !atEndOfWay) {
				currentWayChunk = new ArrayList<Node>();
				currentWayChunk.add(currentNode);
				wayChunks.add(currentWayChunk);
			}
		}

		// Handle circular ways specially.
		// If you split at a circular way at two nodes, you just want to split
		// it at these points, not also at the former endpoint.
		// So if the last node is the same first node, join the last and the
		// first way chunk.
		List<Node> lastWayChunk = wayChunks.get(wayChunks.size() - 1);
		if (wayChunks.size() >= 2
				&& wayChunks.get(0).get(0) == lastWayChunk.get(lastWayChunk.size() - 1)
				&& !nodeSet.contains(wayChunks.get(0).get(0))) {
			if (wayChunks.size() == 2) {
				JOptionPane.showMessageDialog(Main.parent, tr("You must select two or more nodes to split a circular way."));
				return;
			}
			lastWayChunk.remove(lastWayChunk.size() - 1);
			lastWayChunk.addAll(wayChunks.get(0));
			wayChunks.remove(wayChunks.size() - 1);
			wayChunks.set(0, lastWayChunk);
		}

		if (wayChunks.size() < 2) {
			if(wayChunks.get(0).get(0) == wayChunks.get(0).get(wayChunks.get(0).size()-1))
				JOptionPane.showMessageDialog(Main.parent, tr("You must select two or more nodes to split a circular way."));
			else
				JOptionPane.showMessageDialog(Main.parent, tr("The way cannot be split at the selected nodes. (Hint: Select nodes in the middle of the way.)"));
			return;
		}
		//Main.debug("wayChunks.size(): " + wayChunks.size());
		//Main.debug("way id: " + selectedWay.id);

		// build a list of commands, and also a new selection list
		Collection<Command> commandList = new ArrayList<Command>(wayChunks.size());
		Collection<Way> newSelection = new ArrayList<Way>(wayChunks.size());

		Iterator<List<Node>> chunkIt = wayChunks.iterator();

		// First, change the original way
		Way changedWay = new Way(selectedWay);
		changedWay.nodes.clear();
		changedWay.nodes.addAll(chunkIt.next());
		commandList.add(new ChangeCommand(selectedWay, changedWay));
		newSelection.add(selectedWay);

		Collection<Way> newWays = new ArrayList<Way>();
		// Second, create new ways
		while (chunkIt.hasNext()) {
			Way wayToAdd = new Way();
			if (selectedWay.keys != null) {
				wayToAdd.keys = new HashMap<String, String>(selectedWay.keys);
				wayToAdd.checkTagged();
                                wayToAdd.checkDirectionTagged();
			}
			newWays.add(wayToAdd);
			wayToAdd.nodes.addAll(chunkIt.next());
			commandList.add(new AddCommand(wayToAdd));
			//Main.debug("wayToAdd: " + wayToAdd);
			newSelection.add(wayToAdd);

		}
		Boolean warnme=false;
		// now copy all relations to new way also
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Way) {
					if (rm.member == selectedWay)
					{
						Relation c = new Relation(r);
						for(Way wayToAdd : newWays)
						{
							RelationMember em = new RelationMember();
							em.member = wayToAdd;
							em.role = rm.role;
							if(em.role.length() > 0)
								warnme = true;
							c.members.add(em);
						}
						commandList.add(new ChangeCommand(r, c));
						break;
					}
				}
			}
		}
		if(warnme)
			JOptionPane.showMessageDialog(Main.parent, tr("A role based relation membership was copied to all new ways.\nYou should verify this and correct it when necessary."));

		NameVisitor v = new NameVisitor();
		v.visit(selectedWay);
		Main.main.undoRedo.add(
			new SequenceCommand(tr("Split way {0} into {1} parts",
				v.name, wayChunks.size()),
			commandList));
		Main.ds.setSelected(newSelection);
	}

	/**
	 * Enable the "split way" menu option if the selection looks like we could use it.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(checkSelection(newSelection));
	}
}
