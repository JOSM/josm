// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openstreetmap.josm.data.osm.visitor.Visitor;

/**
 * One full way, consisting of several way segments chained together.
 *
 * @author imi
 */
public final class Way extends OsmPrimitive {

	/**
	 * All way segments in this way
	 */
	public final List<Segment> segments = new ArrayList<Segment>();

	@Override public void visit(Visitor visitor) {
		visitor.visit(this);
	}

	/**
	 * Create an identical clone of the argument (including the id)
	 */
	public Way(Way clone) {
		cloneFrom(clone);
	}
	
	public Way() {
	}
	
	@Override public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		segments.clear();
		segments.addAll(((Way)osm).segments);
	}

    @Override public String toString() {
        return "{Way id="+id+" segments="+Arrays.toString(segments.toArray())+"}";
    }

	@Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		return osm instanceof Way ? super.realEqual(osm, semanticOnly) && segments.equals(((Way)osm).segments) : false;
    }

	public int compareTo(OsmPrimitive o) {
	    return o instanceof Way ? Long.valueOf(id).compareTo(o.id) : -1;
    }
	
	public boolean isIncomplete() {
		for (Segment s : segments)
			if (s.incomplete)
				return true;
		return false;
	}
}
