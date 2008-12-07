// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.osm;

import static org.openstreetmap.josm.tools.I18n.tr;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon.CoordinateFormat;
import org.openstreetmap.josm.data.osm.visitor.Visitor;


/**
 * One node data, consisting of one world coordinate waypoint.
 *
 * @author imi
 */
public final class Node extends OsmPrimitive {
	
	public LatLon coor;
	public volatile EastNorth eastNorth;
    
    private static CoordinateFormat mCord;
    
    static {
        try {
            mCord = LatLon.CoordinateFormat.valueOf(Main.pref.get("coordinates"));
        } catch (IllegalArgumentException iae) {
            mCord =LatLon.CoordinateFormat.DECIMAL_DEGREES;
        }
    }

	/**
	 * Create an incomplete Node object
	 */
	public Node(long id) {
		this.id = id;
		incomplete = true;
	}
	
	/**
	 * Create an identical clone of the argument (including the id)
	 */
	public Node(Node clone) {
		cloneFrom(clone);
	}

	public Node(LatLon latlon) {
		this.coor = latlon;
		eastNorth = Main.proj.latlon2eastNorth(latlon);
	}

	@Override public void visit(Visitor visitor) {
		visitor.visit(this);
	}
	
	@Override public void cloneFrom(OsmPrimitive osm) {
		super.cloneFrom(osm);
		coor = ((Node)osm).coor;
		eastNorth = ((Node)osm).eastNorth;
	}

	@Override public String toString() {
		if (coor == null) return "{Node id="+id+"}";
		return "{Node id="+id+",version="+version+",lat="+coor.lat()+",lon="+coor.lon()+"}";
	}

	@Override public boolean realEqual(OsmPrimitive osm, boolean semanticOnly) {
		if (osm instanceof Node) {
            if (super.realEqual(osm, semanticOnly)) {
                if ((coor == null) && ((Node)osm).coor == null)
                    return true;
                if (coor != null)
                    return coor.equals(((Node)osm).coor);
            }
        }
        return false;
    }

	public int compareTo(OsmPrimitive o) {
	    return o instanceof Node ? Long.valueOf(id).compareTo(o.id) : 1;
	}

	public String getName() {
		String name;
		if (incomplete) {
			name = tr("incomplete");
		} else {
			name = get("name");
			if (name == null)
				name = id == 0 ? "" : ""+id;
			name += " (" + coor.latToString(mCord) + ", " + coor.lonToString(mCord) + ")";
		}
		return name;
	}
}
