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
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.ShortCut;

/**
 * Duplicate nodes that are used by multiple ways.
 *
 * Resulting nodes are identical, up to their position.
 *
 * This is the opposite of the MergeNodesAction.
 */

public class UnGlueAction extends JosmAction { //implements SelectionChangedListener {

	private Node selectedNode;
	private Way selectedWay;
	private ArrayList<Node> selectedNodes;

	/**
	 * Create a new UnGlueAction.
	 */
	public UnGlueAction() {
		super(tr("UnGlue Ways"), "unglueways", tr("Duplicate nodes that are used by multiple ways."),
		ShortCut.registerShortCut("tools:unglue", tr("Tool: Unglue"), KeyEvent.VK_G, ShortCut.GROUP_EDIT), true);
		//DataSet.selListeners.add(this);
	}

	/**
	 * Called when the action is executed.
	 *
	 * This method does some checking on the selection and calls the matching unGlueWay method.
	 */
	public void actionPerformed(ActionEvent e) {

		Collection<OsmPrimitive> selection = Main.ds.getSelected();

		if (checkSelection(selection)) {
			int count = 0;
			for (Way w : Main.ds.ways) {
				if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
				if (!w.nodes.contains(selectedNode)) continue;
				count++;
			}
			if (count < 2) {
				JOptionPane.showMessageDialog(Main.parent, tr("This node is not glued to anything else."));
			} else {
				// and then do the work.
				unglueWays();
			}
		} else if (checkSelection2(selection)) {
			ArrayList<Node> tmpNodes = new ArrayList<Node>();
			for (Node n : selectedNodes) {
				int count = 0;
				for (Way w : Main.ds.ways) {
					if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
					if (!w.nodes.contains(n)) continue;
					count++;
				}
				if (count >= 2) {
					tmpNodes.add(n);
				}
			}
			if (tmpNodes.size() < 1) {
				if (selection.size() > 1) {
					JOptionPane.showMessageDialog(Main.parent, tr("None of these nodes is glued to anything else."));
				} else {
					JOptionPane.showMessageDialog(Main.parent, tr("None of this way's nodes is glued to anything else."));
				}
			} else {
				// and then do the work.
				selectedNodes = tmpNodes;
				unglueWays2();
			}
		} else {
			JOptionPane.showMessageDialog(Main.parent,
				tr("The current selection cannot be used for unglueing.")+"\n"+
				"\n"+
				tr("Select either:")+"\n"+
				tr("* One node that is used by more than one way, or")+"\n"+
				tr("* One node that is used by more than one way and one of those ways, or")+"\n"+
				tr("* One way that has one or more nodes that are used by more than one way, or")+"\n"+
				tr("* One way and one or more of its nodes that are used by more than one way.")+"\n"+
				"\n"+
				tr("Note: If a way is selected, this way will get fresh copies of the unglued\n"+
				   "nodes and the new nodes will be selected. Otherwise, all ways will get their\n"+
				   "own copy and all nodes will be selected.")
			);
		}
		selectedNode = null;
		selectedWay = null;
		selectedNodes = null;
	}

	/**
	 * Checks if the selection consists of something we can work with.
	 * Checks only if the number and type of items selected looks good;
	 * does not check whether the selected items are really a valid
	 * input for splitting (this would be too expensive to be carried
	 * out from the selectionChanged listener).
	 *
	 * If this method returns "true", selectedNode and selectedWay will
	 * be set.
	 *
	 * Returns true if either one node is selected or one node and one
	 * way are selected and the node is part of the way.
	 *
	 * The way will be put into the object variable "selectedWay", the
	 * node into "selectedNode".
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

	/**
	 * Checks if the selection consists of something we can work with.
	 * Checks only if the number and type of items selected looks good;
	 * does not check whether the selected items are really a valid
	 * input for splitting (this would be too expensive to be carried
	 * out from the selectionChanged listener).
	 *
	 * Returns true if one way and any number of nodes that are part of
	 * that way are selected. Note: "any" can be none, then all nodes of
	 * the way are used.
	 *
	 * The way will be put into the object variable "selectedWay", the
	 * nodes into "selectedNodes".
	 */
	private boolean checkSelection2(Collection<? extends OsmPrimitive> selection) {
		if (selection.size() < 1)
			return false;

		selectedWay = null;
		for (OsmPrimitive p : selection) {
			if (p instanceof Way) {
				if (selectedWay != null) {
					return false;
				}
				selectedWay = (Way) p;
			}
		}
		if (selectedWay == null) {
			return false;
		}

		selectedNodes = new ArrayList<Node>();
		for (OsmPrimitive p : selection) {
			if (p instanceof Node) {
				Node n = (Node) p;
				if (!selectedWay.nodes.contains(n)) {
					return false;
				}
				selectedNodes.add(n);
			}
		}

		if (selectedNodes.size() < 1) {
			selectedNodes.addAll(selectedWay.nodes);
		}

		return true;
	}

	/**
	 * dupe the given node of the given way
	 *
	 * -> the new node will be put into the parameter newNodes.
	 * -> the add-node command will be put into the parameter cmds.
	 * -> the changed way will be returned and must be put into cmds by the caller!
	 */
	private Way modifyWay(Node originalNode, Way w, List<Command> cmds, List<Node> newNodes) {
		ArrayList<Node> nn = new ArrayList<Node>();
		for (Node pushNode : w.nodes) {
			if (originalNode == pushNode) {
				// clone the node for all other ways
				pushNode = new Node(pushNode);
				pushNode.id = 0;
				newNodes.add(pushNode);
				cmds.add(new AddCommand(pushNode));
			}
			nn.add(pushNode);
		}
		Way newWay = new Way(w);
		newWay.nodes.clear();
		newWay.nodes.addAll(nn);

		return newWay;
	}

	/**
	 * put all newNodes into the same relation(s) that originalNode is in
	 */
	private void fixRelations(Node originalNode, List<Command> cmds, List<Node> newNodes) {
		// modify all relations containing the node
		Relation newRel = null;
		HashSet<String> rolesToReAdd = null;
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			newRel = null;
			rolesToReAdd = null;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Node) {
					if (rm.member == originalNode) {
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
	}


	/**
	 * dupe a single node into as many nodes as there are ways using it, OR
	 *
	 * dupe a single node once, and put the copy on the selected way
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
				if (!firstway) cmds.add(new ChangeCommand(w, modifyWay(selectedNode, w, cmds, newNodes)));
				firstway = false;
			}
		} else {
			cmds.add(new ChangeCommand(selectedWay, modifyWay(selectedNode, selectedWay, cmds, newNodes)));
		}

		fixRelations(selectedNode, cmds, newNodes);

		Main.main.undoRedo.add(new SequenceCommand(tr("Dupe into {0} nodes", newNodes.size()+1), cmds));
		if (selectedWay == null) { // if a node has been selected, new selection is ALL nodes
			newNodes.add(selectedNode);
		} // if a node and a way has been selected, new selection is only the new node that was added to the selected way
		Main.ds.setSelected(newNodes);
	}

	/**
	 * dupe all nodes that are selected, and put the copies on the selected way
	 *
	 */
	private void unglueWays2() {
		LinkedList<Command> cmds = new LinkedList<Command>();
		List<Node> allNewNodes = new LinkedList<Node>();
		Way tmpWay = selectedWay;

		for (Node n : selectedNodes) {
			List<Node> newNodes = new LinkedList<Node>();
			tmpWay = modifyWay(n, tmpWay, cmds, newNodes);
			fixRelations(n, cmds, newNodes);
			allNewNodes.addAll(newNodes);
		}
		cmds.add(new ChangeCommand(selectedWay, tmpWay)); // only one changeCommand for a way, else garbage will happen

		Main.main.undoRedo.add(new SequenceCommand(tr("Dupe {0} nodes into {1} nodes", selectedNodes.size(), selectedNodes.size()+allNewNodes.size()), cmds));
		Main.ds.setSelected(allNewNodes);
	}

// Disabled because we have such a nice help text that would not be shown otherwise.
//
//	/**
//	 * Enable the menu option if the selection looks like we could use it.
//	 */
//	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
//		setEnabled(checkSelection(newSelection) || checkSelection2(newSelection));
//		selectedNode = null;
//		selectedWay = null;
//		selectedNodes = null;
//	}
}
