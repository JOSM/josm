//License: GPLv2 or later. Copyright 2007 by Raphael Mack and others

package org.openstreetmap.josm.data.gpx;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.data.Bounds;

/**
 * Objects of this class represent a gpx file with tracks, waypoints and routes.
 * It uses GPX v1.1, see {@link <a href="http://www.topografix.com/GPX/1/1/">the spec</a>}
 * for details.
 *
 * @author Raphael Mack <ramack@raphael-mack.de>
 */
public class GpxData extends WithAttributes {

    public static final String META_PREFIX = "meta.";
    public static final String META_AUTHOR_NAME = META_PREFIX + "author.name";
    public static final String META_AUTHOR_EMAIL = META_PREFIX + "author.email";
    public static final String META_AUTHOR_LINK = META_PREFIX + "author.link";
    public static final String META_COPYRIGHT_AUTHOR = META_PREFIX + "copyright.author";
    public static final String META_COPYRIGHT_LICENSE = META_PREFIX + "copyright.license";
    public static final String META_COPYRIGHT_YEAR = META_PREFIX + "copyright.year";
    public static final String META_DESC = META_PREFIX + "desc";
    public static final String META_KEYWORDS = META_PREFIX + "keywords";
    public static final String META_LINKS = META_PREFIX + "links";
    public static final String META_NAME = META_PREFIX + "name";
    public static final String META_TIME = META_PREFIX + "time";

    public File storageFile;
    public boolean fromServer;

    public Collection<GpxTrack> tracks = new LinkedList<GpxTrack>();
    public Collection<GpxRoute> routes = new LinkedList<GpxRoute>();
    public Collection<WayPoint> waypoints = new LinkedList<WayPoint>();

    @SuppressWarnings("unchecked")
    public void mergeFrom(GpxData other) {
        if (storageFile == null && other.storageFile != null) {
            storageFile = other.storageFile;
        }
        fromServer = fromServer && other.fromServer;

        for (Map.Entry<String, Object> ent : other.attr.entrySet()) {
            // TODO: Detect conflicts.
            String k = ent.getKey();
            if (k.equals(META_LINKS) && attr.containsKey(META_LINKS)) {
                ((Collection<GpxLink>) attr.get(META_LINKS)).addAll(
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
            for (GpxTrackSegment trkseg : trk.getSegments()) {
                if (!trkseg.getWayPoints().isEmpty())
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

    public boolean isEmpty() {
        return !hasRoutePoints() && !hasTrackPoints() && waypoints.isEmpty();
    }

    // FIXME might perhaps use visitor pattern?
    public Bounds recalculateBounds() {
        Bounds bounds = null;
        for (WayPoint wpt : waypoints) {
            if (bounds == null) {
                bounds = new Bounds(wpt.getCoor());
            } else {
                bounds.extend(wpt.getCoor());
            }
        }
        for (GpxRoute rte : routes) {
            for (WayPoint wpt : rte.routePoints) {
                if (bounds == null) {
                    bounds = new Bounds(wpt.getCoor());
                } else {
                    bounds.extend(wpt.getCoor());
                }
            }
        }
        for (GpxTrack trk : tracks) {
            Bounds trkBounds = trk.getBounds();
            if (trkBounds != null) {
                if (bounds == null) {
                    bounds = new Bounds(trkBounds);
                } else {
                    bounds.extend(trkBounds);
                }
            }
        }
        return bounds;
    }

    /**
     * calculates the sum of the lengths of all track segments
     */
    public double length(){
        double result = 0.0; // in meters

        for (GpxTrack trk : tracks) {
            result += trk.length();
        }

        return result;
    }
}
