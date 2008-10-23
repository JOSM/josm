//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.DeleteCommand;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.SelectionChangedListener;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.TigerUtils;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.CollectBackReferencesVisitor;
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Pair;
import org.openstreetmap.josm.tools.ShortCut;


/**
 * Merge two or more nodes into one node.
 * (based on Combine ways)
 *
 * @author Matthew Newton
 *
 */
public class MergeNodesAction extends JosmAction implements SelectionChangedListener {

	public MergeNodesAction() {
		super(tr("Merge Nodes"), "mergenodes", tr("Merge nodes into the oldest one."),
		ShortCut.registerShortCut("tools:mergenodes", tr("Tool: {0}", tr("Merge Nodes")), KeyEvent.VK_M, ShortCut.GROUP_EDIT), true);
		DataSet.selListeners.add(this);
	}

	public void actionPerformed(ActionEvent event) {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		LinkedList<Node> selectedNodes = new LinkedList<Node>();

		// the selection check should stop this procedure starting if
		// nothing but node are selected - otherwise we don't care
		// anyway as long as we have at least two nodes
		for (OsmPrimitive osm : selection)
			if (osm instanceof Node)
				selectedNodes.add((Node)osm);

		if (selectedNodes.size() < 2) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least two nodes to merge."));
			return;
		}

		// Find which node to merge into (i.e. which one will be left)
		// - this should be combined from two things:
		//   1. It will be the first node in the list that has a
		//      positive ID number, OR the first node.
		//   2. It will be at the position of the first node in the
		//      list.
		//
		// *However* - there is the problem that the selection list is
		// _not_ in the order that the nodes were clicked on, meaning
		// that the user doesn't know which node will be chosen (so
		// (2) is not implemented yet.)  :-(
		Node useNode = null;
		for (Node n: selectedNodes) {
			if (n.id > 0) {
				useNode = n;
				break;
			}
		}
		if (useNode == null)
			useNode = selectedNodes.iterator().next();

		mergeNodes(selectedNodes, useNode);
	}

	/**
	 * really do the merging - returns the node that is left
	 */
	public static Node mergeNodes(LinkedList<Node> allNodes, Node dest) {
		Node newNode = new Node(dest);

		// Check whether all ways have identical relationship membership. More
		// specifically: If one of the selected ways is a member of relation X
		// in role Y, then all selected ways must be members of X in role Y.

		// FIXME: In a later revision, we should display some sort of conflict
		// dialog like we do for tags, to let the user choose which relations
		// should be kept.

		// Step 1, iterate over all relations and figure out which of our
		// selected ways are members of a relation.
		HashMap<Pair<Relation,String>, HashSet<Node>> backlinks =
			new HashMap<Pair<Relation,String>, HashSet<Node>>();
		HashSet<Relation> relationsUsingNodes = new HashSet<Relation>();
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Node) {
					for (Node n : allNodes) {
						if (rm.member == n) {
							Pair<Relation,String> pair = new Pair<Relation,String>(r, rm.role);
							HashSet<Node> nodelinks = new HashSet<Node>();
							if (backlinks.containsKey(pair)) {
								nodelinks = backlinks.get(pair);
							} else {
								nodelinks = new HashSet<Node>();
								backlinks.put(pair, nodelinks);
							}
							nodelinks.add(n);

							// this is just a cache for later use
							relationsUsingNodes.add(r);
						}
					}
				}
			}
		}

		// Complain to the user if the ways don't have equal memberships.
		for (HashSet<Node> nodelinks : backlinks.values()) {
			if (!nodelinks.containsAll(allNodes)) {
				int option = JOptionPane.showConfirmDialog(Main.parent,
					tr("The selected nodes have differing relation memberships.  "
						+ "Do you still want to merge them?"),
					tr("Merge nodes with different memberships?"),
					JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION)
					break;
				return null;
			}
		}

		// collect properties for later conflict resolving
		Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
		for (Node n : allNodes) {
			for (Entry<String,String> e : n.entrySet()) {
				if (!props.containsKey(e.getKey()))
					props.put(e.getKey(), new TreeSet<String>());
				props.get(e.getKey()).add(e.getValue());
			}
		}

		// display conflict dialog
		Map<String, JComboBox> components = new HashMap<String, JComboBox>();
		JPanel p = new JPanel(new GridBagLayout());
		for (Entry<String, Set<String>> e : props.entrySet()) {
			if (TigerUtils.isTigerTag(e.getKey())) {
				String combined = TigerUtils.combineTags(e.getKey(), e.getValue());
				newNode.put(e.getKey(), combined);
			} else if (e.getValue().size() > 1) {
				if("created_by".equals(e.getKey()))
				{
					newNode.put("created_by", "JOSM");
				}
				else
				{
					JComboBox c = new JComboBox(e.getValue().toArray());
					c.setEditable(true);
					p.add(new JLabel(e.getKey()), GBC.std());
					p.add(Box.createHorizontalStrut(10), GBC.std());
					p.add(c, GBC.eol());
					components.put(e.getKey(), c);
				}
			} else
				newNode.put(e.getKey(), e.getValue().iterator().next());
		}

		if (!components.isEmpty()) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Enter values for all conflicts."), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return null;
			for (Entry<String, JComboBox> e : components.entrySet())
				newNode.put(e.getKey(), e.getValue().getEditor().getItem().toString());
		}

		LinkedList<Command> cmds = new LinkedList<Command>();
		cmds.add(new ChangeCommand(dest, newNode));

		Collection<OsmPrimitive> del = new HashSet<OsmPrimitive>();

		for (Way w : Main.ds.ways) {
			if (w.deleted || w.incomplete || w.nodes.size() < 1) continue;
			boolean modify = false;
			for (Node sn : allNodes) {
				if (sn == dest) continue;
				if (w.nodes.contains(sn)) {
					modify = true;
				}
			}
			if (!modify) continue;
			// OK - this way contains one or more nodes to change
			ArrayList<Node> nn = new ArrayList<Node>();
			Node lastNode = null;
			for (int i = 0; i < w.nodes.size(); i++) {
				Node pushNode = w.nodes.get(i);
				if (allNodes.contains(pushNode)) {
					pushNode = dest;
				}
				if (pushNode != lastNode) {
					nn.add(pushNode);
				}
				lastNode = pushNode;
			}
			if (nn.size() < 2) {
				CollectBackReferencesVisitor backRefs =
					new CollectBackReferencesVisitor(Main.ds, false);
				w.visit(backRefs);
				if (!backRefs.data.isEmpty()) {
					JOptionPane.showMessageDialog(Main.parent,
						tr("Cannot merge nodes: " +
							"Would have to delete a way that is still used."));
					return null;
				}
				del.add(w);
			} else {
				Way newWay = new Way(w);
				newWay.nodes.clear();
				newWay.nodes.addAll(nn);
				cmds.add(new ChangeCommand(w, newWay));
			}
		}

		// delete any merged nodes
		del.addAll(allNodes);
		del.remove(dest);
		if (!del.isEmpty()) cmds.add(new DeleteCommand(del));

		// modify all relations containing the now-deleted nodes
		for (Relation r : relationsUsingNodes) {
			Relation newRel = new Relation(r);
			newRel.members.clear();
			HashSet<String> rolesToReAdd = new HashSet<String>();
			for (RelationMember rm : r.members) {
				// Don't copy the member if it points to one of our nodes,
				// just keep a note to re-add it later on.
				if (allNodes.contains(rm.member)) {
					rolesToReAdd.add(rm.role);
				} else {
					newRel.members.add(rm);
				}
			}
			for (String role : rolesToReAdd) {
				newRel.members.add(new RelationMember(role, dest));
			}
			cmds.add(new ChangeCommand(r, newRel));
		}

		Main.main.undoRedo.add(new SequenceCommand(tr("Merge {0} nodes", allNodes.size()), cmds));
		Main.ds.setSelected(dest);

		return dest;
	}


	/**
	 * Enable the "Merge Nodes" menu option if more then one node is selected
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		boolean ok = true;
		if (newSelection.size() < 2) {
			setEnabled(false);
			return;
		}
		for (OsmPrimitive osm : newSelection) {
			if (!(osm instanceof Node)) {
				ok = false;
				break;
			}
		}
		setEnabled(ok);
	}
}
