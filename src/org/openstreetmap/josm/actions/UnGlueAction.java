// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

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

/**
 * Dupe a node that is used my multiple ways, so each way has its own node.
 *
 * Resulting nodes are identical, up to their position.
 *
 * This is the opposite of the MergeNodesAction.
 */

public class UnGlueAction extends JosmAction implements SelectionChangedListener {

	private Node selectedNode;
	private Way selectedWay;

	/**
	 * Create a new SplitWayAction.
	 */
	public UnGlueAction() {
		super(tr("UnGlue Ways"), "unglueways", tr("Duplicate the selected node so each way using it has its own copy."), KeyEvent.VK_G, 0, true);
		DataSet.selListeners.add(this);
	}

	/**
	 * Called when the action is executed.
	 *
	 * This method just collects the single node selected and calls the unGlueWay method.
	 */
	public void actionPerformed(ActionEvent e) {

		Collection<OsmPrimitive> selection = Main.ds.getSelected();

		if (!checkSelection(selection)) {
			JOptionPane.showMessageDialog(Main.parent, tr("The current selection cannot be used for unglueing."));
			return;
		}

		int count = 0;
		for (Way w : Main.ds.ways) {
			if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
			if (!w.nodes.contains(selectedNode)) continue;
			count++;
		}
		if (count < 2) {
			JOptionPane.showMessageDialog(Main.parent, tr("You must select a node that is used by at least 2 ways."));
			return;
		}

		// and then do the work.
		unglueWays();
	}

	/**
	 * Checks if the selection consists of something we can work with.
	 * Checks only if the number and type of items selected looks good;
	 * does not check whether the selected items are really a valid
	 * input for splitting (this would be too expensive to be carried
	 * out from the selectionChanged listener).
	 */
	private boolean checkSelection(Collection<? extends OsmPrimitive> selection) {
		
		int size = selection.size();
		if (size < 1 || size > 2)
			return false;
		
		selectedNode = null;
		selectedWay = null;
		
		for (OsmPrimitive p : selection) {
			if (p instanceof Node) {
				selectedNode = (Node) p;
				if (size == 1 || selectedWay != null)
					return size == 1 || selectedWay.nodes.contains(selectedNode);
			} else if (p instanceof Way) {
				selectedWay = (Way) p;
				if (size == 2 && selectedNode != null) 
					return selectedWay.nodes.contains(selectedNode);
			}
		}
		
		return false;
	}

	private boolean modifyWay(boolean firstway, Way w, List<Command> cmds,
			List<Node> newNodes) {
		ArrayList<Node> nn = new ArrayList<Node>();
		for (Node pushNode : w.nodes) {
			if (selectedNode == pushNode) {
				if (firstway) {
					// reuse the old node for the first (==a random) way
					firstway = false;
				} else {
					// clone the node for all other ways
					pushNode = new Node(selectedNode);
					pushNode.id = 0;
					newNodes.add(pushNode);
					cmds.add(new AddCommand(pushNode));
				}
			}
			nn.add(pushNode);
		}
		Way newWay = new Way(w);
		newWay.nodes.clear();
		newWay.nodes.addAll(nn);
		cmds.add(new ChangeCommand(w, newWay));

		return firstway;
	}
	
	/**
	 * see above
	 */
	private void unglueWays() {

		LinkedList<Command> cmds = new LinkedList<Command>();
		List<Node> newNodes = new LinkedList<Node>();

		if (selectedWay == null) {
			
			boolean firstway = true;
			// modify all ways containing the nodes
			for (Way w : Main.ds.ways) {
				if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
				if (!w.nodes.contains(selectedNode)) continue;
	
				firstway = modifyWay(firstway, w, cmds, newNodes);
			}
		} else {
			modifyWay(false, selectedWay, cmds, newNodes);
		}

		// modify all relations containing the node
		Relation newRel = null;
		HashSet<String> rolesToReAdd = null;
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			newRel = null;
			rolesToReAdd = null;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Node) {
					if (rm.member == selectedNode) {
						if (newRel == null) {
							newRel = new Relation(r);
							newRel.members.clear();
							rolesToReAdd = new HashSet<String>();
						}
						rolesToReAdd.add(rm.role);
					}
				}
			}
			if (newRel != null) {
				for (RelationMember rm : r.members) {
					//if (rm.member != selectedNode) {
						newRel.members.add(rm);
					//}
				}
				for (Node n : newNodes) {
					for (String role : rolesToReAdd) {
						newRel.members.add(new RelationMember(role, n));
					}
				}
				cmds.add(new ChangeCommand(r, newRel));
			}
		}

		newNodes.add(selectedNode); // just for the next 2 lines
		Main.main.undoRedo.add(new SequenceCommand(tr("Dupe into {0} nodes", newNodes.size()), cmds));
		Main.ds.setSelected(newNodes);

	}

	/**
	 * Enable the "split way" menu option if the selection looks like we could use it.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(checkSelection(newSelection));
	}
}
