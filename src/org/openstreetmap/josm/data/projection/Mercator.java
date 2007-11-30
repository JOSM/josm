// License: GPL. Copyright 2007 by Immanuel Scholz and others
package org.openstreetmap.josm.data.projection;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Implement Mercator Projection code, coded after documentation
 * from wikipedia.
 * 
 * The center of the mercator projection is always the 0 grad 
 * coordinate.
 * 
 * @author imi
 */
public class Mercator implements Projection {

	public EastNorth latlon2eastNorth(LatLon p) {
		return new EastNorth(
			p.lon()*Math.PI/180,
			Math.log(Math.tan(Math.PI/4+p.lat()*Math.PI/360)));
	}

	public LatLon eastNorth2latlon(EastNorth p) {
		return new LatLon(
			Math.atan(Math.sinh(p.north()))*180/Math.PI,
			p.east()*180/Math.PI);
	}

	@Override public String toString() {
		return "Mercator";
	}

    public String getCacheDirectoryName() {
        return "mercator";
    }

	public double scaleFactor() {
	    return 1/Math.PI/2;
    }

	@Override public boolean equals(Object o) {
		return o instanceof Mercator;
	}

	@Override public int hashCode() {
		return Mercator.class.hashCode();
	}
}
