//License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.actions;

import static org.openstreetmap.josm.tools.I18n.tr;
import static org.openstreetmap.josm.tools.I18n.trn;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.HashMap;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.command.ChangeCommand;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.command.SequenceCommand;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.visitor.NameVisitor;

public class ReorderAction extends JosmAction {

	public ReorderAction() {
		super(tr("Reorder Segments"), "reorder", tr("Try to reorder segments of a way so that they are in a line. May try to flip segments around to match a line."), KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK | KeyEvent.ALT_DOWN_MASK, true);
	}

	/**
	 * This method first sorts all the segments in a way, then makes sure that all 
	 * the segments are facing the same direction as the first one.
	 */
	public void actionPerformed(ActionEvent e) {
		Collection<Way> ways = new LinkedList<Way>();
		for (OsmPrimitive osm : Main.ds.getSelected())
			if (osm instanceof Way)
				ways.add((Way)osm);

		if (ways.size() < 1) {
			JOptionPane.showMessageDialog(Main.parent, tr("Please select at least one way."));
			return;
		}

		if (ways.size() > 1) {
			int answer = JOptionPane.showConfirmDialog(Main.parent, 
					trn(null, "You selected more than one way. Reorder the segments of {0} ways?", ways.size(), ways.size()), 
					tr("Reorder segments"), JOptionPane.OK_CANCEL_OPTION);
			if (answer != JOptionPane.OK_OPTION)
				return;
		}
		boolean doneSomething = false;
		for (Way way : ways) {
			if (!way.isIncomplete() && way.segments.size() > 1)
			{			
				doneSomething = true;
				Command c = reorderWay(way);

				if( c != null )
					Main.main.undoRedo.add(c);
			}
		}
		if (!doneSomething) {
			JOptionPane.showMessageDialog(Main.parent, 
					trn("The selected way is incomplete or has only one segment.",
							"None of the selected ways are complete and have more than one segment.",
							ways.size()));
		}
		Main.map.repaint();
	}

	/**
	 * This method first sorts all the segments in a way, then makes sure that all 
	 * the segments are facing the same direction as the first one.
	 * @param way The way to reorder
	 * @return The command needed to reorder the way
	 */
	public static Command reorderWay(Way way) {
		final LinkedList<Segment> sel = new LinkedList<Segment>(sortSegments(new LinkedList<Segment>(way.segments), false));   	

		Collection<Command> c = new LinkedList<Command>();

		boolean direction = false;
		// work out the "average" direction of the way, we use this to direct the rest of the segments
		int dirCounter = 0;
		for(int i = 0; i < sel.size() - 1; i++)
		{
			Segment firstSegment = sel.get(i);
			Segment secondSegment = sel.get(i+1);
			if ( firstSegment.to == secondSegment.from || firstSegment.to == secondSegment.to ) // direction = true when 'from' is the first node in the Way
				dirCounter++;
			else
				dirCounter--;
		}
		if ( dirCounter <= 0 )
			direction = false;
		else
			direction = true;

		Node lastNode = null;

		// we need to calculate what the first node in the way is, we work from there
		Segment firstSegment = sel.getFirst();
		Segment secondSegment = sel.get(1);
		if (firstSegment.to == secondSegment.from || firstSegment.to == secondSegment.to)
			lastNode = firstSegment.from;
		else
			lastNode = firstSegment.to;

		// go through each segment and flip them if required
		for (Segment s : sel) {
			Segment snew = new Segment(s);
			boolean segDirection = s.from == lastNode;
			// segDirection = true when the 'from' node occurs before the 'to' node in the Way 
			if (direction != segDirection)
			{    			
				// reverse the segment's direction
				Node n = snew.from;
				snew.from = snew.to;
				snew.to = n;
				c.add(new ChangeCommand(s, snew));
			}	

			if (direction) // if its facing forwards,
				lastNode = snew.to; // our next node is the 'to' one
			else
				lastNode = snew.from; // otherwise its the 'from' one
		}

		LinkedList<Segment> segments = new LinkedList<Segment>();

		// Now we recreate the segment list, in the correct order of the direction
		for (Segment s : sel) 
			if (!direction) 
				segments.addFirst(s);
			else
				segments.addLast(s);

		// Check if the new segment list is actually different from the old one
		// before we go and add a change command for it
		for(int i = 0; i < segments.size(); i++)
			if (way.segments.get(i) != segments.get(i))
			{
				Way newWay = new Way(way);
				newWay.segments.clear();
				newWay.segments.addAll(segments);
				c.add(new ChangeCommand(way, newWay));
				break;
			}

		// Check we've got some change commands before we add a sequence command
		if (c.size() != 0) {
			NameVisitor v = new NameVisitor();
			way.visit(v);
			return new SequenceCommand(tr("Reorder segments for way {0}",v.name), c);
		}
		return null;
	}

	/**
	 * This sort is based on the sort in the old ReorderAction, but it can work 
	 * irresepective of the direction of the segments. This produces a sort 
	 * that can be useful even if the segments are facing the wrong direction.
	 * 
	 * @param segments list of segments to be sorted
	 * @param strict true if segment direction should be observed, false if not
	 */
	public static LinkedList<Segment> sortSegments(LinkedList<Segment> segments, boolean strict) {

		LinkedList<Segment> sortedSegments = new LinkedList<Segment>();

		while (!segments.isEmpty()) {
			LinkedList<Segment> pivotList = new LinkedList<Segment>();
			pivotList.add(firstSegment(segments));
			segments.remove(pivotList.getLast());
			boolean found;
			do {
				found = false;
				//try working forwards first
				for (Iterator<Segment> it = segments.iterator(); it.hasNext();) {
					Segment ls = it.next();
					if (ls.incomplete)
						continue; // incomplete segments are never added to a new way
					if (ls.from == pivotList.getLast().to) {
						pivotList.addLast(ls);
						it.remove();
						found = true;
					}
				}
				if(!found){
					for (Iterator<Segment> it = segments.iterator(); it.hasNext();) {
						Segment ls = it.next();
						if (ls.incomplete)
							continue; // incomplete segments are never added to a new way
						if (ls.from == pivotList.getLast().to || (!strict && (ls.to == pivotList.getLast().to || ls.from == pivotList.getLast().from || ls.to == pivotList.getLast().from))) {
							pivotList.addLast(ls);
							it.remove();
							found = true;
						} else if (ls.to == pivotList.getFirst().from || (!strict && (ls.from == pivotList.getFirst().from || ls.to == pivotList.getFirst().to || ls.from == pivotList.getFirst().to))) {
							pivotList.addFirst(ls);
							it.remove(); 
							found = true;
						}
					}
				}
			} while (found);
			sortedSegments.addAll(pivotList);
		}
		return sortedSegments;
	}

	/**
	 * This method searches for a good segment to start a reorder from.
	 * In most cases this will be a segment with a start node that occurs only
	 * once in the way. In cases with loops, this could be any odd number. If no nodes
	 * are referenced an odd number of times, then any segment is a good start point.
	 */
	public static Segment firstSegment(Collection<Segment> segments) {
		HashMap<Node, Integer> refCount = new HashMap<Node, Integer>(segments.size()*2);
		//loop through all segments and count up how many times each node is referenced
		for (Segment seg : segments) {
			if (!refCount.containsKey(seg.from))
				refCount.put(seg.from, 0);
			refCount.put(seg.from,refCount.get(seg.from)+1);

			if (!refCount.containsKey(seg.to))
				refCount.put(seg.to, 0);
			refCount.put(seg.to,refCount.get(seg.to)+1);
		}

		//now look for start nodes that are referenced only once
		for (Segment seg : segments)
			if (refCount.get(seg.from) == 1)
				return seg;
		//now look for start nodes that are referenced only (2n+1)
		for (Segment seg : segments)
			if (refCount.get(seg.from) % 2 == 1)
				return seg;
		//now look for end nodes that are referenced only once
		for (Segment seg : segments)
			if (refCount.get(seg.to) == 1)
				return seg;
		//now look for end nodes that are referenced only (2n+1)
		for (Segment seg : segments)
			if (refCount.get(seg.to) % 2 == 1)
				return seg;

		return segments.iterator().next();
	}    
}
