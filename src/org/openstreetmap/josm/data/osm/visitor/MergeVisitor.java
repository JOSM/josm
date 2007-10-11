// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
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
	 * round than mergedNodes)
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
	 * Simply calls cloneFrom() for now.
	 * Might be useful to keep around to facilitate merge with the relations
	 * branch.
	 */
	private <T extends OsmPrimitive> void cloneFromExceptIncomplete(T myOsm, T otherOsm) {
		myOsm.cloneFrom(otherOsm);
    }

	/**
	 * Merge the way if id matches or if all nodes match and the
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
			ds.ways.add(other);
		} else {
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			boolean same = true;
			Iterator<Node> it = other.nodes.iterator();
			for (Node n : my.nodes) {
				if (!match(n, it.next()))
					same = false;
			}
			if (!same) {
				my.nodes.clear();
				my.nodes.addAll(other.nodes);
				my.modified = other.modified;
			}
		}
	}

	/**
	 * Merge the relation if id matches or if all members match and the
	 * id of either relation is zero.
	 */
	public void visit(Relation other) {
		if (mergeAfterId(null, ds.relations, other))
			return;

		Relation my = null;
		for (Relation e : ds.relations) {
			if (match(other, e) && ((mergeds == null) || (!mergeds.relations.contains(e)))) {
				my = e;
				break;
			}
		}
		
		if (my == null) {
			// Add the relation and replace any incomplete segments that we already have
			ds.relations.add(other);
			// FIXME unclear!
			/*
			for (RelationMember em : other.getMembers()) {
				if (em.member.incomplete) {
					for (Segment ourSegment : ds.segments) {
						if (ourSegment.id == s.id) {
							mergedSegments.put(s, ourSegment);
							break;
						}
					}
				}
			}*/
		} else {
			mergeCommon(my, other);
			if (my.modified && !other.modified)
				return;
			boolean same = true;
			if (other.members.size() != my.members.size()) {
					same = false;
			} else {
				for (RelationMember em : my.members) {
					if (!other.members.contains(em)) {
						same = false;
						break;
					}
				}
			}
			// FIXME Unclear
			/*
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
			*/
		}
	}

	/**
	 * Postprocess the dataset and fix all merged references to point to the actual
	 * data.
	 */
	public void fixReferences() {
		for (Way w : ds.ways)
			fixWay(w);
		for (OsmPrimitive osm : conflicts.values())
			if (osm instanceof Way)
				fixWay((Way)osm);
	}

	private void fixWay(Way w) {
	    boolean replacedSomething = false;
	    LinkedList<Node> newNodes = new LinkedList<Node>();
	    for (Node n : w.nodes) {
	    	Node otherN = mergedNodes.get(n);
	    	newNodes.add(otherN == null ? n : otherN);
	    	if (otherN != null)
	    		replacedSomething = true;
	    }
	    if (replacedSomething) {
	    	w.nodes.clear();
	    	w.nodes.addAll(newNodes);
    }
    }

	/**
	 * @return Whether the nodes match (in sense of "be mergable").
	 */
	private boolean match(Node n1, Node n2) {
		if (n1.id == 0 || n2.id == 0)
			return n1.coor.equalsEpsilon(n2.coor);
		return n1.id == n2.id;
	}

	/**
	 * @return Whether the ways match (in sense of "be mergable").
	 */
	private boolean match(Way w1, Way w2) {
		if (w1.id == 0 || w2.id == 0) {
			if (w1.nodes.size() != w2.nodes.size())
			return false;
			Iterator<Node> it = w1.nodes.iterator();
			for (Node n : w2.nodes)
				if (!match(n, it.next()))
					return false;
			return true;
		}
		return w1.id == w2.id;
	}
	/**
	 * @return Whether the relations match (in sense of "be mergable").
	 */
	private boolean match(Relation w1, Relation w2) {
		// FIXME this is not perfect yet...
		if (w1.id == 0 || w2.id == 0) {
			if (w1.members.size() != w2.members.size())
				return false;
			for (RelationMember em : w1.members) {
				if (!w2.members.contains(em)) {
					return false;
				}
			}
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
			if (my.realEqual(other, false)) {
				if (merged != null)
					merged.put(other, my);
				return true; // no merge needed.
			}
			if (my.realEqual(other, true)) {
				// they differ in modified/timestamp combination only. Auto-resolve it.
				if (merged != null)
					merged.put(other, my);
				if (my.getTimestamp().before(other.getTimestamp())) {
					my.modified = other.modified;
					my.timestamp = other.timestamp;
				}
				return true; // merge done.
			}
			if (my.id == other.id && my.id != 0) {
				if (my.modified && other.modified) {
					conflicts.put(my, other);
					if (merged != null)
						merged.put(other, my);
				} else if (!my.modified && !other.modified) {
					if (my.getTimestamp().before(other.getTimestamp())) {
						cloneFromExceptIncomplete(my, other);
						if (merged != null)
							merged.put(other, my);
					}
				} else if (other.modified) {
					if (my.getTimestamp().after(other.getTimestamp())) {
						conflicts.put(my, other);
						if (merged != null)
							merged.put(other, my);
					} else {
						cloneFromExceptIncomplete(my, other);
						if (merged != null)
							merged.put(other, my);
					}
				} else if (my.modified) {
					if (my.getTimestamp().before(other.getTimestamp())) {
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
