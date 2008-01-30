// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm.visitor;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;

/**
 * Calculates the total bounding rectangle of a serie of OsmPrimitives, using the 
 * EastNorth values as reference.
 * @author imi
 */
public class BoundingXYVisitor implements Visitor {

	public EastNorth min, max;

	public void visit(Node n) {
		visit(n.eastNorth);
	}

	public void visit(Way w) {
		w.visitNodes(this);
	}

	public void visit(Relation e) {
		// relations have no bounding box.
	}

	public void visit(EastNorth eastNorth) {
		if (eastNorth != null) {
			if (min == null)
				min = eastNorth;
			else if (eastNorth.east() < min.east() || eastNorth.north() < min.north())
				min = new EastNorth(Math.min(min.east(), eastNorth.east()), Math.min(min.north(), eastNorth.north()));
			
			if (max == null)
				max = eastNorth;
			else if (eastNorth.east() > max.east() || eastNorth.north() > max.north())
				max = new EastNorth(Math.max(max.east(), eastNorth.east()), Math.max(max.north(), eastNorth.north()));
		}
	}

	/**
	 * @return The bounding box or <code>null</code> if no coordinates have passed
	 */
	public Bounds getBounds() {
		if (min == null || max == null)
			return null;
		return new Bounds(Main.proj.eastNorth2latlon(min), Main.proj.eastNorth2latlon(max));
	}
}
