// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Segment;
import org.openstreetmap.josm.data.osm.Way;

/**
 * A visitor that get a data set at construction time and merge every visited object
 * into it.
 * 
 * @author imi
 */
public class MergeVisitor implements Visitor {

	/**
	 * Map from primitives in the database to visited primitives. (Attention: The other way 
	 * round than mergedNodes and mergedSegments)
	 */
	public Map<OsmPrimitive, OsmPrimitive> conflicts = new HashMap<OsmPrimitive, OsmPrimitive>();

	private final DataSet ds;
	private final DataSet mergeds;

	/**
	 * A list of all nodes that got replaced with other nodes.
	 * Key is the node in the other's dataset and the value is the one that is now
	 * in ds.nodes instead.
	 */
	private final Map<Node, Node> mergedNodes = new HashMap<Node, Node>();
	/**
	 * A list of all segments that got replaced with others.
	 * Key is the segment in the other's dataset and the value is the one that is now
	 * in ds.segments.
	 */
	private final Map<Segment, Segment> mergedSegments = new HashMap<Segment, Segment>();

	public MergeVisitor(DataSet ds, DataSet mergeds) {
		this.ds = ds;
		this.mergeds = mergeds;
	}

	/**
	 * Merge the node if the id matches with any of the internal set or if
	 * either id is zero, merge if lat/lon matches.
	 */
	public void visit(Node other) {
		if (mergeAfterId(mergedNodes, ds.nodes, other))
			return;

		Node my = null;
		for (Node n : ds.nodes) {
			if (match(n, other) && ((mergeds == null) || (!mergeds.nodes.contains(n)))) {
				my = n;
				break;
			}
		}
		if (my == null)
			ds.nodes.add(other);
		else {
			mergedNodes.put(other, my);
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			if (!my.coor.equalsEpsilon(other.coor)) {
				my.coor = other.coor;
				my.eastNorth = other.eastNorth;
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Merge the segment if id matches or if both nodes are the same (and the
	 * id is zero of either segment). Nodes are the "same" when they @see match
	 */
	public void visit(Segment other) {
		if (mergeAfterId(mergedSegments, ds.segments, other))
			return;

		Segment my = null;
		for (Segment ls : ds.segments) {
			if (match(other, ls) && ((mergeds == null) || (!mergeds.segments.contains(ls)))) {
				my = ls;
				break;
			}
		}
		
		if (my == null)
			ds.segments.add(other);
		else if (my.incomplete && !other.incomplete) {
			mergedSegments.put(other, my);
			my.cloneFrom(other);
		} else if (!other.incomplete) {
			mergedSegments.put(other, my);
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			if (!match(my.from, other.from)) {
				my.from = other.from;
				my.modified = other.modified;
			}
			if (!match(my.to, other.to)) {
				my.to = other.to;
				my.modified = other.modified;
			}
		}
	}

	private <T extends OsmPrimitive> void cloneFromExceptIncomplete(T myOsm, T otherOsm) {
		if (!(myOsm instanceof Way))
			myOsm.cloneFrom(otherOsm);
		else {
			Way my = (Way)myOsm;
			Way other = (Way)otherOsm;
			HashMap<Long, Segment> copy = new HashMap<Long, Segment>();
			for (Segment s : my.segments)
				copy.put(s.id, s);
			my.cloneFrom(other);
			my.segments.clear();
			for (Segment s : other.segments) {
				Segment myS = copy.get(s.id);
				if (s.incomplete && myS != null && !myS.incomplete) {
					mergedSegments.put(s, myS);
					my.segments.add(myS);
				} else
					my.segments.add(s);
			}
		}
    }

	/**
	 * Merge the way if id matches or if all segments matches and the
	 * id is zero of either way.
	 */
	public void visit(Way other) {
		if (mergeAfterId(null, ds.ways, other))
			return;

		Way my = null;
		for (Way w : ds.ways) {
			if (match(other, w) && ((mergeds == null) || (!mergeds.ways.contains(w)))) {
				my = w;
				break;
			}
		}
		if (my == null) {
			// Add the way and replace any incomplete segments that we already have
			ds.ways.add(other);
			for (Segment s : other.segments) {
				if (s.incomplete) {
					for (Segment ourSegment : ds.segments) {
						if (ourSegment.id == s.id) {
							mergedSegments.put(s, ourSegment);
							break;
						}
					}
				}
			}
		} else {
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			boolean same = true;
			Iterator<Segment> it = other.segments.iterator();
			for (Segment ls : my.segments) {
				if (!match(ls, it.next()))
					same = false;
			}
			if (!same) {
				HashMap<Long, Segment> copy = new HashMap<Long, Segment>();
				for (Segment s : my.segments)
					copy.put(s.id, s);
				my.segments.clear();
				for (Segment s : other.segments) {
					Segment myS = copy.get(s.id);
					if (s.incomplete && myS != null && !myS.incomplete) {
						mergedSegments.put(s, myS);
						my.segments.add(myS);
					} else
						my.segments.add(s);
				}
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Postprocess the dataset and fix all merged references to point to the actual
	 * data.
	 */
	public void fixReferences() {
		for (Segment s : ds.segments)
			fixSegment(s);
		for (OsmPrimitive osm : conflicts.values())
			if (osm instanceof Segment)
				fixSegment((Segment)osm);
		for (Way w : ds.ways)
			fixWay(w);
		for (OsmPrimitive osm : conflicts.values())
			if (osm instanceof Way)
				fixWay((Way)osm);
	}

	private void fixWay(Way w) {
	    boolean replacedSomething = false;
	    LinkedList<Segment> newSegments = new LinkedList<Segment>();
	    for (Segment ls : w.segments) {
	    	Segment otherLs = mergedSegments.get(ls);
	    	newSegments.add(otherLs == null ? ls : otherLs);
	    	if (otherLs != null)
	    		replacedSomething = true;
	    }
	    if (replacedSomething) {
	    	w.segments.clear();
	    	w.segments.addAll(newSegments);
	    }
	    for (Segment ls : w.segments)
	    	fixSegment(ls);
    }

	private void fixSegment(Segment ls) {
		
	    if (mergedNodes.containsKey(ls.from)) 
	    	ls.from = mergedNodes.get(ls.from);
	    
	    if (mergedNodes.containsKey(ls.to))  
	    	ls.to = mergedNodes.get(ls.to);
	   
    }

	/**
	 * @return Whether the nodes matches (in sense of "be mergable").
	 */
	private boolean match(Node n1, Node n2) {
		if (n1.id == 0 || n2.id == 0)
			return n1.coor.equalsEpsilon(n2.coor);
		return n1.id == n2.id;
	}

	/**
	 * @return Whether the segments matches (in sense of "be mergable").
	 */
	private boolean match(Segment ls1, Segment ls2) {
		if (ls1.id == ls2.id && ls1.id != 0)
			return true;
		//if (ls1.id != 0 && ls2.id != 0)
		//	return false;
		if (ls1.incomplete || ls2.incomplete)
			return false;
		return match(ls1.from, ls2.from) && match(ls1.to, ls2.to);
	}

	/**
	 * @return Whether the ways matches (in sense of "be mergable").
	 */
	private boolean match(Way w1, Way w2) {
		if (w1.id == 0 || w2.id == 0) {
			if (w1.segments.size() != w2.segments.size())
				return false;
			Iterator<Segment> it = w1.segments.iterator();
			for (Segment ls : w2.segments)
				if (!match(ls, it.next()))
					return false;
			return true;
		}
		return w1.id == w2.id;
	}

	/**
	 * Merge the common parts of an osm primitive.
	 * @param my The object, the information gets merged into
	 * @param other The object, the information gets merged from
	 */
	private void mergeCommon(OsmPrimitive my, OsmPrimitive other) {
		if (other.deleted)
			my.delete(true);
		if (my.id == 0 || !my.modified || other.modified) {
			if (my.id == 0 && other.id != 0) {
				my.id = other.id;
				my.modified = other.modified; // match a new node
			} else if (my.id != 0 && other.id != 0 && other.modified)
				my.modified = true;
		}
		if (other.keys == null)
			return;
		if (my.keySet().containsAll(other.keys.keySet()))
			return;
		if (my.keys == null)
			my.keys = other.keys;
		else
			my.keys.putAll(other.keys);
		
		my.modified = true;
	}

	/**
	 * @return <code>true</code>, if no merge is needed or merge is performed already.
	 */
	private <P extends OsmPrimitive> boolean mergeAfterId(Map<P,P> merged, Collection<P> primitives, P other) {
		for (P my : primitives) {
			Date d1 = my.timestamp == null ? new Date(0) : my.timestamp;
			Date d2 = other.timestamp == null ? new Date(0) : other.timestamp;
			if (my.realEqual(other, false)) {
				if (merged != null)
					merged.put(other, my);
				return true; // no merge needed.
			}
			if (my.realEqual(other, true)) {
				// they differ in modified/timestamp combination only. Auto-resolve it.
				if (merged != null)
					merged.put(other, my);
				if (d1.before(d2)) {
					my.modified = other.modified;
					my.timestamp = other.timestamp;
				}
				return true; // merge done.
			}
			if (my.id == other.id && my.id != 0) {
				if (my instanceof Segment && ((Segment)my).incomplete)
					return false; // merge always over an incomplete
				if (my.modified && other.modified) {
					conflicts.put(my, other);
					if (merged != null)
						merged.put(other, my);
				} else if (!my.modified && !other.modified) {
					if (d1.before(d2)) {
						cloneFromExceptIncomplete(my, other);
						if (merged != null)
							merged.put(other, my);
					}
				} else if (other.modified) {
					if (d1.after(d2)) {
						conflicts.put(my, other);
						if (merged != null)
							merged.put(other, my);
					} else {
						cloneFromExceptIncomplete(my, other);
						if (merged != null)
							merged.put(other, my);
					}
				} else if (my.modified) {
					if (d2.after(d1)) {
						conflicts.put(my, other);
						if (merged != null)
							merged.put(other, my);
					}
				}
				return true;
			}
		}
		return false;
	}
}
