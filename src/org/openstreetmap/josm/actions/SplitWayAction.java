// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;
import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * Splits a way into multiple ways (all identical except the segments
 * belonging to the way).
 * 
 * Various split modes are used depending on what is selected.
 * 
 * 1. One or more NODES (and, optionally, also one way) selected:
 * 
 * (All nodes must be part of the same way. If a way is also selected, that way
 * must contain all selected nodes.)
 * 
 * Way is split AT the node(s) into contiguous ways. If the original contained
 * one or more parts that were not reachable from any of the nodes, they form an
 * extra new way. Examples (numbers are unselected nodes, letters are selected
 * nodes)
 * 
 * 1---A---2  becomes  1---A and A---2
 * 
 * 1---A---2---B---3  becomes  1---A and A---2---B and B---3
 *  
 *     2                    
 *     |                   
 * 1---A---3  becomes  1---A and 2---A and A---3
 *
 * 1---A---2  3---4  becomes  1---A and A---2 and 3---4
 * 
 * If the selected node(s) do not clearly define the way that is to be split,
 * then the way must be selected for disambiguation (e.g. you have two ways,
 * 1---2---3 and 4---2---5, and select node 2, then you must also select the 
 * way you want to split).
 * 
 * This function will result in at least two ways, unless the selected node is
 * at the end of the way AND the way is contiguous, which will lead to an error
 * message.
 * 
 * After executing the operation, the selection will be cleared.
 * 
 * 2. One or more SEGMENTS (and, optionally, also one way) selected:
 * 
 * (All segments must be part of the same way)
 * 
 * The way is split in a fashion that makes a new way from the selected segments,
 * i.e. the selected segments are removed from the way to form a new one.
 * 
 * This function will result in exactly two ways. 
 * 
 * If splitting the segments out of the way makes a non-contiguous part from
 * something that was contiguous before, the action is aborted and an error
 * message is shown.
 * 
 * 3. Exactly one WAY selected
 * 
 * If the way is contiguous, you will get an error message. If the way is not
 * contiguous it is split it into 2...n contiguous ways.
 */

public class SplitWayAction extends JosmAction implements SelectionChangedListener {

	private Way selectedWay;
	private List<Node> selectedNodes;
	private List<Segment> selectedSegments;

	/**
	 * Create a new SplitWayAction.
	 */
	public SplitWayAction() {
		super(tr("Split Way"), "splitway", tr("Split a way at the selected node."), KeyEvent.VK_P, KeyEvent.CTRL_MASK | KeyEvent.SHIFT_MASK, true);
		DataSet.listeners.add(this);
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
		selectedSegments = null;

		Visitor splitVisitor = new Visitor(){
			public void visit(Node n) {
				if (selectedNodes == null)
					selectedNodes = new LinkedList<Node>();
				selectedNodes.add(n);
            }
			public void visit(Segment s) {
				if (selectedSegments == null)
					selectedSegments = new LinkedList<Segment>();
				selectedSegments.add(s);
            }
			public void visit(Way w) {
				selectedWay = w;
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
					for (Segment s : w.segments) {
						if (n.equals(s.from) || n.equals(s.to)) {
							Integer old = wayOccurenceCounter.get(w);
							wayOccurenceCounter.put(w, (old == null) ? 1 : old+1);
							break;
						}
					}
				}
			}
			if (wayOccurenceCounter.isEmpty()) {
				JOptionPane.showMessageDialog(Main.parent, 
						trn("The selected node is not part of any way.",
								"The selected nodes are not part of any way.", selectedNodes.size()));
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
			for (Segment s : selectedWay.segments) {
				nds.remove(s.from);
				nds.remove(s.to);
			}
			if (!nds.isEmpty()) {
				JOptionPane.showMessageDialog(Main.parent, 
						trn("The selected way does not contain the selected node.",
								"The selected way does not contain all the selected nodes.", selectedNodes.size()));
				return;
			}

			// If only segments are selected, guess which way to use.
		} else if (selectedWay == null && selectedSegments != null) {

			HashMap<Way, Integer> wayOccurenceCounter = new HashMap<Way, Integer>();
			for (Segment s : selectedSegments) {
				for (Way w : Main.ds.ways) {
					if (w.segments.contains(s)) {
						Integer old = wayOccurenceCounter.get(w);
						wayOccurenceCounter.put(w, (old == null) ? 1 : old+1);
						break;
					}
				}
			}
			if (wayOccurenceCounter.isEmpty()) {
				JOptionPane.showMessageDialog(Main.parent, 
						trn("The selected segment is not part of any way.",
								"The selected segments are not part of any way.", selectedSegments.size()));
				return;
			}

			for (Entry<Way, Integer> entry : wayOccurenceCounter.entrySet()) {
				if (entry.getValue().equals(selectedSegments.size())) {
					if (selectedWay != null) {
						JOptionPane.showMessageDialog(Main.parent,
								trn("There is more than one way using the segment you selected. Please select the way also.",
										"There is more than one way using the segments you selected. Please select the way also.", selectedSegments.size()));
						return;
					}
					selectedWay = entry.getKey();
				}
			}

			if (selectedWay == null) {
				JOptionPane.showMessageDialog(Main.parent, tr("The selected segments do not share the same way."));
				return;
			}

			// If a way and segments are selected, verify that the segments are part of the way.
		} else if (selectedWay != null && selectedSegments != null) {

			if (!selectedWay.segments.containsAll(selectedSegments)) {
				JOptionPane.showMessageDialog(Main.parent, 
						trn("The selected way does not contain the selected segment.",
								"The selected way does not contain all the selected segments.", selectedSegments.size()));
				return;
			}
		}

		// finally check if the selected way is complete.
		if (selectedWay.isIncomplete()) {
			JOptionPane.showMessageDialog(Main.parent, tr("Warning: This way is incomplete. Try to download it before splitting."));
			return;
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
		boolean segment = false;
		boolean node = false;
		for (OsmPrimitive p : selection) {
			if (p instanceof Way && !way)
				way = true;
			else if (p instanceof Node && !segment)
				node = true;
			else if (p instanceof Segment && !node)
				segment = true;
			else
				return false;
		}
		return way || segment || node;
	}

	/**
	 * Split a way into two or more parts, starting at a selected node.
	 * 
	 * @param way the way to split
	 * @param nodes the node(s) to split the way at; must be part of the way.
	 */
	private void splitWay() {

		// The basic idea is to first divide all segments forming this way into 
		// groups, and later form new ways according to the groups. Initally, 
		// all segments are copied into allSegments, and then gradually removed
		// from there as new groups are built.

		LinkedList<Segment> allSegments = new LinkedList<Segment>();
		allSegments.addAll(selectedWay.segments);
		List<List<Segment>> segmentSets = new ArrayList<List<Segment>>();

		if (selectedNodes != null) {

			// This is the "split at node" mode.

			boolean split = true;
			Segment splitSeg = null;
			while (split) {
				split = false;

				// Look for a "split segment". A split segment is any segment
				// that touches one of the split nodes and has not yet been
				// assigned to one of the segment groups.
				for (Segment s : allSegments) {
					for (Node node : selectedNodes) {
						if (s.from.equals(node) || s.to.equals(node)) {
							split = true;
							splitSeg = s;
							break;
						}
					}
					if (split)
						break;
				}

				// If a split segment was found, move this segment and all segments
				// connected to it into a new segment group, stopping only if we
				// reach another split node. Segment moving is done recursively by
				// the moveSegments method.
				if (split) {
					LinkedList<Segment> subSegments = new LinkedList<Segment>();
					moveSegments(allSegments, subSegments, splitSeg, selectedNodes);
					segmentSets.add(subSegments);
				}

				// The loop continues until no more split segments were found.
				// Nb. not all segments touching a split node are split segments; 
				// e.g. 
				//
				//     2       4
				//     |       |
				// 1---A---3---C---5
				//
				// This way will be split into 5 ways (1---A,2---A,A---3---C,4---C,
				// C---5). Depending on which is processed first, either A---3 becomes
				// a split segment and 3---C is moved as a connecting segment, or vice
				// versa. The result is, of course, the same but this explains why we
				// cannot simply start a new way for each segment connecting to a split
				// node.
			}

		} else if (selectedSegments != null) {

			// This is the "split segments" mode. It is quite easy as the segments to
			// remove are already explicitly selected, but some restrictions have to 
			// be observed to make sure that no non-contiguous parts are created.

			// first create a "scratch" copy of the full segment list and move all
			// segments connected to the first selected segment into a temporary list.
			LinkedList<Segment> copyOfAllSegments = new LinkedList<Segment>(allSegments);
			LinkedList<Segment> partThatContainsSegments = new LinkedList<Segment>();
			moveSegments(copyOfAllSegments, partThatContainsSegments, selectedSegments.get(0), null);

			// this list must now contain ALL selected segments; otherwise, segments
			// from unconnected parts of the way have been selected and this is not allowed
			// as it would create a new non-contiguous way.
			if (!partThatContainsSegments.containsAll(selectedSegments)) {
				JOptionPane.showMessageDialog(Main.parent, tr("The selected segments are not in the same contiguous part of the way."));				
				return;		
			}

			// if the contiguous part that contains the segments becomes non-contiguous
			// after the removal of the segments, that is also an error.
			partThatContainsSegments.removeAll(selectedSegments);
			if (!partThatContainsSegments.isEmpty()) {
				LinkedList<Segment> contiguousSubpart = new LinkedList<Segment>();
				moveSegments(partThatContainsSegments, contiguousSubpart, partThatContainsSegments.get(0), null);
				// if partThatContainsSegments was contiguous before, it will now be empty as all segments
				// connected to the first segment therein have been moved
				if (!partThatContainsSegments.isEmpty()) {
					JOptionPane.showMessageDialog(Main.parent, tr("Removing the selected segments would make a part of the way non-contiguous."));				
					return;				
				}
			}

			ArrayList<Segment> subSegments = new ArrayList<Segment>();
			subSegments.addAll(selectedSegments);
			allSegments.removeAll(selectedSegments);
			segmentSets.add(subSegments);

		} else {

			// This is the "split way into contiguous parts" mode.
			// We use a similar mechanism to splitting at nodes, but we do not 
			// select split segments. Instead, we randomly grab a segment out 
			// of the way and move all connecting segments to a new group. If
			// segments remain in the original way, we repeat the procedure.

			while (!allSegments.isEmpty()) {
				LinkedList<Segment> subSegments = new LinkedList<Segment>();
				moveSegments(allSegments, subSegments, allSegments.get(0), null);
				segmentSets.add(subSegments);
			}			
		}

		// We now have a number of segment groups.

		// If segments remain in allSegments, this means that they were not reachable
		// from any of the split nodes, and they will be made into an extra way.
		if (!allSegments.isEmpty()) {
			segmentSets.add(allSegments);
		}

		// If we do not have at least two parts, then the way was circular or the node(s)
		// were at one end of the way. User error ;-)
		if (segmentSets.size() < 2) {
			if (selectedNodes != null) {
				JOptionPane.showMessageDialog(Main.parent, tr("The way cannot be split at the selected node. (Hint: To split circular ways, select two nodes.)"));
			} else {
				JOptionPane.showMessageDialog(Main.parent, tr("The way cannot be split because it is contiguous. (Hint: To split at a node, select that node.)"));				
			}
			return;
		}

		// sort the list of segment lists according to their number of elements, so that
		// the biggest part of the way comes first. That way, we will "change" the largest
		// part of the way by removing a few segments, and "add" new, smaller ways; looks
		// nicer.
		Collections.sort(segmentSets, new Comparator<Collection<Segment>>() {
			public int compare(Collection<Segment> a, Collection<Segment> b) {
				if (a.size() < b.size())
					return 1;
				if (b.size() < a.size())
					return -1;
				return 0;
			}
		});

		// build a list of commands, and also a list of ways
		Collection<Command> commandList = new ArrayList<Command>(segmentSets.size());
		Collection<Way> newSelection = new ArrayList<Way>(segmentSets.size());
		Iterator<List<Segment>> segsIt = segmentSets.iterator();
		
		// the first is always a change to the existing way;
		Way changedWay = new Way(selectedWay);
		changedWay.segments.clear();
		changedWay.segments.addAll(segsIt.next());
		commandList.add(new ChangeCommand(selectedWay, changedWay));
		newSelection.add(selectedWay);

		// and commands 1...n are additions of new ways.
		while (segsIt.hasNext()) {
			Way wayToAdd = new Way();
			if (selectedWay.keys != null)
				wayToAdd.keys = new HashMap<String, String>(selectedWay.keys);
			wayToAdd.segments.clear();
			wayToAdd.segments.addAll(segsIt.next());
			commandList.add(new AddCommand(wayToAdd));
			newSelection.add(wayToAdd);
		}

		NameVisitor v = new NameVisitor();
		v.visit(selectedWay);
		Main.main.undoRedo.add(new SequenceCommand(tr("Split way {0} into {1} parts",v.name, segmentSets.size()), commandList));
		Main.ds.setSelected(newSelection);
	}

	/**
	 * Enable the "split way" menu option if the selection looks like we could use it.
	 */
	public void selectionChanged(Collection<? extends OsmPrimitive> newSelection) {
		setEnabled(checkSelection(newSelection));
	}

	/**
	 * Move contiguous segments from one collection to another. The given segment is moved first, and
	 * then the procedure is recursively called for all segments that connect to the first segment at
	 * either end.
	 * 
	 * @param source the source collection
	 * @param destination the destination collection
	 * @param start the first segment to be moved
	 * @param stopNodes collection of nodes which should be considered end points for moving (may be null).
	 */
	private void moveSegments(Collection<Segment> source, LinkedList<Segment> destination, Segment start, Collection<Node> stopNodes) {
		source.remove(start);
		if (destination.isEmpty() || destination.iterator().next().from.equals(start.to))
			destination.addFirst(start);
		else
			destination.addLast(start);
		Segment moveSeg = start;
		while(moveSeg != null) {
			moveSeg = null;

			for (Node node : new Node[] { start.from, start.to }) {
				if (stopNodes != null && stopNodes.contains(node))
					continue;
				for (Segment sourceSeg : source) {
					if (sourceSeg.from.equals(node) || sourceSeg.to.equals(node)) {
						moveSeg = sourceSeg;
						break;
					}
				}
				if (moveSeg != null)
					break;
			}
			if (moveSeg != null) {
				moveSegments(source, destination, moveSeg, stopNodes);
			}
		}
	}
}
