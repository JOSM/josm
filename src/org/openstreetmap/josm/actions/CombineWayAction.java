// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
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
import org.openstreetmap.josm.tools.GBC;
import org.openstreetmap.josm.tools.Pair;

/**
 * Combines multiple ways into one.
 * 
 * @author Imi
 */
public class CombineWayAction extends JosmAction implements SelectionChangedListener {

	public CombineWayAction() {
		super(tr("Combine Way"), "combineway", tr("Combine several ways into one."), KeyEvent.VK_C, 0, true);
		DataSet.selListeners.add(this);
	}

	public void actionPerformed(ActionEvent event) {
		Collection<OsmPrimitive> selection = Main.ds.getSelected();
		LinkedList<Way> selectedWays = new LinkedList<Way>();

		for (OsmPrimitive osm : selection)
			if (osm instanceof Way)
				selectedWays.add((Way)osm);

		if (selectedWays.size() < 2) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least two ways to combine."));
			return;
		}

		// Check whether all ways have identical relationship membership. More
		// specifically: If one of the selected ways is a member of relation X
		// in role Y, then all selected ways must be members of X in role Y.

		// FIXME: In a later revision, we should display some sort of conflict
		// dialog like we do for tags, to let the user choose which relations
		// should be kept.

		// Step 1, iterate over all relations and figure out which of our
		// selected ways are members of a relation.
		HashMap<Pair<Relation,String>, HashSet<Way>> backlinks =
			new HashMap<Pair<Relation,String>, HashSet<Way>>();
		HashSet<Relation> relationsUsingWays = new HashSet<Relation>();
		for (Relation r : Main.ds.relations) {
			if (r.deleted || r.incomplete) continue;
			for (RelationMember rm : r.members) {
				if (rm.member instanceof Way) {
					for(Way w : selectedWays) {
						if (rm.member == w) {
							Pair<Relation,String> pair = new Pair<Relation,String>(r, rm.role);
							HashSet<Way> waylinks = new HashSet<Way>();
							if (backlinks.containsKey(pair)) {
								waylinks = backlinks.get(pair);
							} else {
								waylinks = new HashSet<Way>();
								backlinks.put(pair, waylinks);
							}
							waylinks.add(w);

							// this is just a cache for later use
							relationsUsingWays.add(r);
						}
					}
				}
			}
		}

		// Complain to the user if the ways don't have equal memberships.
		for (HashSet<Way> waylinks : backlinks.values()) {
			if (!waylinks.containsAll(selectedWays)) {
				int option = JOptionPane.showConfirmDialog(Main.parent,
					tr("The selected ways have differing relation memberships.  "
						+ "Do you still want to combine them?"),
					tr("Combine ways with different memberships?"),
					JOptionPane.YES_NO_OPTION);
				if (option == JOptionPane.YES_OPTION)
					break;
				return;
			}
		}

		// collect properties for later conflict resolving
		Map<String, Set<String>> props = new TreeMap<String, Set<String>>();
		for (Way w : selectedWays) {
			for (Entry<String,String> e : w.entrySet()) {
				if (!props.containsKey(e.getKey()))
					props.put(e.getKey(), new TreeSet<String>());
				props.get(e.getKey()).add(e.getValue());
			}
		}

		List<Node> nodeList = null;
		Object firstTry = actuallyCombineWays(selectedWays, false);
		if (firstTry instanceof List) {
			nodeList = (List<Node>) firstTry;
		} else {
			Object secondTry = actuallyCombineWays(selectedWays, true);
			if (secondTry instanceof List) {
				int option = JOptionPane.showConfirmDialog(Main.parent,
					tr("The ways can not be combined in their current directions.  "
					+ "Do you want to reverse some of them?"), tr("Change directions?"),
					JOptionPane.YES_NO_OPTION);
				if (option != JOptionPane.YES_OPTION) {
					return;
				}
				nodeList = (List<Node>) secondTry;
			} else {
				JOptionPane.showMessageDialog(Main.parent, secondTry);
				return;
			}
		}

		// Find the most appropriate way to modify.

		// Eventually this might want to be the way with the longest
		// history or the longest selected way but for now just attempt
		// to reuse an existing id.
		Way modifyWay = selectedWays.peek();
		for (Way w : selectedWays) {
			modifyWay = w;
			if (w.id != 0) break;
		}
		Way newWay = new Way(modifyWay);

		newWay.nodes.clear();
		newWay.nodes.addAll(nodeList);

		// display conflict dialog
		Map<String, JComboBox> components = new HashMap<String, JComboBox>();
		JPanel p = new JPanel(new GridBagLayout());
		for (Entry<String, Set<String>> e : props.entrySet()) {
			if (TigerUtils.isTigerTag(e.getKey())) {
				String combined = TigerUtils.combineTags(e.getKey(), e.getValue());
				newWay.put(e.getKey(), combined);
			} else if (e.getValue().size() > 1) {
				JComboBox c = new JComboBox(e.getValue().toArray());
				c.setEditable(true);
				p.add(new JLabel(e.getKey()), GBC.std());
				p.add(Box.createHorizontalStrut(10), GBC.std());
				p.add(c, GBC.eol());
				components.put(e.getKey(), c);
			} else
				newWay.put(e.getKey(), e.getValue().iterator().next());
		}

		if (!components.isEmpty()) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, p, tr("Enter values for all conflicts."), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return;
			for (Entry<String, JComboBox> e : components.entrySet())
				newWay.put(e.getKey(), e.getValue().getEditor().getItem().toString());
		}

		LinkedList<Command> cmds = new LinkedList<Command>();
		LinkedList<Way> deletedWays = new LinkedList<Way>(selectedWays);
		deletedWays.remove(modifyWay);
		cmds.add(new DeleteCommand(deletedWays));
		cmds.add(new ChangeCommand(modifyWay, newWay));

		// modify all relations containing the now-deleted ways
		for (Relation r : relationsUsingWays) {
			Relation newRel = new Relation(r);
			newRel.members.clear();
			HashSet<String> rolesToReAdd = new HashSet<String>();
			for (RelationMember rm : r.members) {
				// Don't copy the member if it to one of our ways, just keep a
				// note to re-add it later on.
				if (selectedWays.contains(rm.member)) {
					rolesToReAdd.add(rm.role);
				} else {
					newRel.members.add(rm);
				}
			}
			for (String role : rolesToReAdd) {
				newRel.members.add(new RelationMember(role, modifyWay));
			}
			cmds.add(new ChangeCommand(r, newRel));
		}
		Main.main.undoRedo.add(new SequenceCommand(tr("Combine {0} ways", selectedWays.size()), cmds));
		Main.ds.setSelected(modifyWay);
	}

	/**
	 * @return a message if combining failed, else a list of nodes.
	 */
	private Object actuallyCombineWays(List<Way> ways, boolean ignoreDirection) {
		// Battle plan:
		//  1. Split the ways into small chunks of 2 nodes and weed out
		//	   duplicates.
		//  2. Take a chunk and see if others could be appended or prepended,
		//	   if so, do it and remove it from the list of remaining chunks.
		//	   Rather, rinse, repeat.
		//  3. If this algorithm does not produce a single way,
		//     complain to the user.
		//  4. Profit!

		HashSet<Pair<Node,Node>> chunkSet = new HashSet<Pair<Node,Node>>();
		for (Way w : ways)
			chunkSet.addAll(w.getNodePairs(ignoreDirection));

		LinkedList<Pair<Node,Node>> chunks = new LinkedList<Pair<Node,Node>>(chunkSet);

		if (chunks.isEmpty()) {
			return tr("All the ways were empty");
		}

		List<Node> nodeList = Pair.toArrayList(chunks.poll());
		while (!chunks.isEmpty()) {
			ListIterator<Pair<Node,Node>> it = chunks.listIterator();
			boolean foundChunk = false;
			while (it.hasNext()) {
				Pair<Node,Node> curChunk = it.next();
				if (curChunk.a == nodeList.get(nodeList.size() - 1)) { // append
					nodeList.add(curChunk.b);
				} else if (curChunk.b == nodeList.get(0)) { // prepend
					nodeList.add(0, curChunk.a);
				} else if (ignoreDirection && curChunk.b == nodeList.get(nodeList.size() - 1)) { // append
					nodeList.add(curChunk.a);
				} else if (ignoreDirection && curChunk.a == nodeList.get(0)) { // prepend
					nodeList.add(0, curChunk.b);
				} else {
					continue;
				}

				foundChunk = true;
				it.remove();
				break;
			}
			if (!foundChunk) break;
		}

		if (!chunks.isEmpty()) {
			return tr("Could not combine ways "
				+ "(They could not be merged into a single string of nodes)");
		}

		return nodeList;
	}

	/**
	 * Enable the "Combine way" menu option if more then one way is selected
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		boolean first = false;
		for (OsmPrimitive osm : newSelection) {
			if (osm instanceof Way) {
				if (first) {
					setEnabled(true);
					return;
				}
				first = true;
			}
		}
		setEnabled(false);
	}
}
