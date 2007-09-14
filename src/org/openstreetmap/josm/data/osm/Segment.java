// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One way segment consisting of a pair of nodes (from/to) 
 *
 * @author imi
 */
public final class Segment extends OsmPrimitive {

	/**
	 * The starting node of the segment
	 */
	public Node from;

	/**
	 * The ending node of the segment
	 */
	public Node to;

	/**
	 * If set to true, this object is incomplete, which means only the id
	 * and type is known (type is the objects instance class)
	 */
	public boolean incomplete;

	/**
	 * Create an identical clone of the argument (including the id)
	 */
	public Segment(Segment clone) {
		cloneFrom(clone);
	}

	/**
	 * Create an segment from the given starting and ending node
	 * @param from	Starting node of the segment.
	 * @param to	Ending node of the segment.
	 */
	public Segment(Node from, Node to) {
		this.from = from;
		this.to = to;
		incomplete = false;
	}

	public Segment(long id) {
		this.id = id;
		incomplete = true;
	}

	@Override public void visit(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * @return <code>true</code>, if the <code>ls</code> occupy
	 * exactly the same place as <code>this</code>.
	 */
	public boolean equalPlace(Segment ls) {
		if (equals(ls))
			return true;
		if (incomplete || ls.incomplete)
			return incomplete == ls.incomplete;
		return ((from.coor.equals(ls.from.coor) && to.coor.equals(ls.to.coor)) ||
				(from.coor.equals(ls.to.coor) && to.coor.equals(ls.from.coor)));
	}

	@Override public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		Segment ls = ((Segment)osm);
		from = ls.from;
		to = ls.to;
		incomplete = ls.incomplete;
	}

	@Override public String toString() {
		return "{Segment id="+id+" from="+from+" to="+to+"}";
	}

	@Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		if (!(osm instanceof Segment))
			return super.realEqual(osm, semanticOnly); 
		if (incomplete)
			return super.realEqual(osm, semanticOnly) && ((Segment)osm).incomplete;
		return super.realEqual(osm, semanticOnly) && from.equals(((Segment)osm).from) && to.equals(((Segment)osm).to);
	}

	public int compareTo(OsmPrimitive o) {
		return o instanceof Segment ? Long.valueOf(id).compareTo(o.id) : (o instanceof Node ? -1 : 1);
	}
}
