//License: GPLv2 or later. Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;

/**
 * Objects of this class represent a gpx file with tracks, waypoints and routes.
 * It uses GPX v1.1, see {@link <a href="http://www.topografix.com/GPX/1/1/">the spec</a>}
 * for details.
 * 
 * @author Raphael Mack <ramack@raphael-mack.de>
 */
public class GpxData extends WithAttributes {
	public File storageFile;
	public boolean fromServer;

	public Collection<GpxTrack> tracks = new LinkedList<GpxTrack>();
	public Collection<GpxRoute> routes = new LinkedList<GpxRoute>();
	public Collection<WayPoint> waypoints = new LinkedList<WayPoint>();

	public Bounds bounds;

	public void mergeFrom(GpxData other) {
		if (storageFile == null && other.storageFile != null) {
			storageFile = other.storageFile;
		}
		fromServer = fromServer && other.fromServer;

		for (Map.Entry<String, Object> ent : other.attr.entrySet()) {
			// TODO: Detect conflicts.
			String k = ent.getKey();
			if (k.equals("link") && attr.containsKey("link")) {
				((Collection<GpxLink>) attr.get("link")).addAll(
					(Collection<GpxLink>) ent.getValue());
			} else {
				attr.put(k, ent.getValue());
			}
		}
		tracks.addAll(other.tracks);
		routes.addAll(other.routes);
		waypoints.addAll(other.waypoints);
	}

	public boolean hasTrackPoints() {
		for (GpxTrack trk : tracks) {
			for (Collection<WayPoint> trkseg : trk.trackSegs) {
				if (!trkseg.isEmpty())
					return true;
			}
		}
		return false;
	}

	public boolean hasRoutePoints() {
		for (GpxRoute rte : routes) {
			if (!rte.routePoints.isEmpty())
				return true;
		}
		return false;
	}

	// FIXME might perhaps use visitor pattern?
	public void recalculateBounds() {
		bounds = null;
		for (WayPoint wpt : waypoints) {
			if (bounds == null) {
				bounds = new Bounds(wpt.latlon, wpt.latlon);
			} else {
				bounds.extend(wpt.latlon);
			}
		}
		for (GpxRoute rte : routes) {
			for (WayPoint wpt : rte.routePoints) {
				if (bounds == null) {
					bounds = new Bounds(wpt.latlon, wpt.latlon);
				} else {
					bounds.extend(wpt.latlon);
				}
			}
		}
		for (GpxTrack trk : tracks) {
			for (Collection<WayPoint> trkseg : trk.trackSegs) {
				for (WayPoint wpt : trkseg) {
					if (bounds == null) {
						bounds = new Bounds(wpt.latlon, wpt.latlon);
					} else {
						bounds.extend(wpt.latlon);
					}
				}
			}
		}
		if (bounds == null) {
			bounds = new Bounds();
		}
	}
    
    /**
     * calculates the sum of the lengths of all track segments
     */
    public double length(){
        double result = 0.0; // in meters
        WayPoint last = null;
		
        for (GpxTrack trk : tracks) {
            for (Collection<WayPoint> trkseg : trk.trackSegs) {
                for (WayPoint tpt : trkseg) {
                    if(last != null){
                        result += calcDistance(last.latlon, tpt.latlon);
                    }
                    last = tpt;
                }
                last = null; // restart for each track segment
            }
        }
        return result;
    }

    /**
     * returns the distance in meters between two LatLons
     */
    public static double calcDistance(LatLon p1, LatLon p2){
        double lat1, lon1, lat2, lon2;
        double dlon, dlat;
	    
        lat1 = p1.lat() * Math.PI / 180.0;
        lon1 = p1.lon() * Math.PI / 180.0;
        lat2 = p2.lat() * Math.PI / 180.0;
        lon2 = p2.lon() * Math.PI / 180.0;

        dlon = lon2 - lon1;
        dlat = lat2 - lat1;

        double a = (Math.pow(Math.sin(dlat/2), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow(Math.sin(dlon/2), 2));
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return 6367000 * c;
    }

}
