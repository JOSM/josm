// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * One full way, consisting of a list of way nodes.
 *
 * @author imi
 */
public final class Way extends OsmPrimitive {

	/**
	 * All way nodes in this way
	 */
	public final List<Node> nodes = new ArrayList<Node>();

	@Override public void visit(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Create an identical clone of the argument (including the id)
	 */
	public Way(Way clone) {
		cloneFrom(clone);
	}
	
	/**
	 * Create an empty way without id. Use this only if you set meaningful 
	 * values yourself.
	 */
	public Way() {
	}
	
	/**
	 * Create an incomplete Way.
	 */
	public Way(long id) {
		this.id = id;
		incomplete = true;
	}
	
	@Override public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		nodes.clear();
		nodes.addAll(((Way)osm).nodes);
	}

    @Override public String toString() {
        return "{Way id="+id+" nodes="+Arrays.toString(nodes.toArray())+"}";
    }

	@Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		return osm instanceof Way ? super.realEqual(osm, semanticOnly) && nodes.equals(((Way)osm).nodes) : false;
    }

	public int compareTo(OsmPrimitive o) {
	    return o instanceof Way ? Long.valueOf(id).compareTo(o.id) : -1;
    }

	@Deprecated
	public boolean isIncomplete() {
		return incomplete;
	}
}
