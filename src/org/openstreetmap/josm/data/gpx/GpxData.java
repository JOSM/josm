//License: GPLv2 or later. Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.HashMap;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.LatLon;
import java.lang.Math;
import java.io.File;

/**
 * objects of this class represent a gpx file with tracks, waypoints and routes
 * it uses GPX1.1 see http://www.topografix.com/GPX/1/1/ for details
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
}
