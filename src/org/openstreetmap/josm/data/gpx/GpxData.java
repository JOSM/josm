// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.coor.EastNorth;

/**
 * Objects of this class represent a gpx file with tracks, waypoints and routes.
 * It uses GPX v1.1, see <a href="http://www.topografix.com/GPX/1/1/">the spec</a>
 * for details.
 *
 * @author Raphael Mack &lt;ramack@raphael-mack.de&gt;
 */
public class GpxData extends WithAttributes {

    public File storageFile;
    public boolean fromServer;

    public String creator;

    public final Collection<GpxTrack> tracks = new LinkedList<GpxTrack>();
    public final Collection<GpxRoute> routes = new LinkedList<GpxRoute>();
    public final Collection<WayPoint> waypoints = new LinkedList<WayPoint>();

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

    /**
     * calculates the bounding box of available data and returns it.
     * The bounds are not stored internally, but recalculated every time
     * this function is called.
     *
     * FIXME might perhaps use visitor pattern?
     */
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

     /**
     * Makes a WayPoint at the projection of point P onto the track providing P is less than
     * tolerance away from the track
     *
     * @param P : the point to determine the projection for
     * @param tolerance : must be no further than this from the track
     * @return the closest point on the track to P, which may be the first or last point if off the
     * end of a segment, or may be null if nothing close enough
     */
    public WayPoint nearestPointOnTrack(EastNorth P, double tolerance) {
        /*
         * assume the coordinates of P are xp,yp, and those of a section of track between two
         * trackpoints are R=xr,yr and S=xs,ys. Let N be the projected point.
         *
         * The equation of RS is Ax + By + C = 0 where A = ys - yr B = xr - xs C = - Axr - Byr
         *
         * Also, note that the distance RS^2 is A^2 + B^2
         *
         * If RS^2 == 0.0 ignore the degenerate section of track
         *
         * PN^2 = (Axp + Byp + C)^2 / RS^2 that is the distance from P to the line
         *
         * so if PN^2 is less than PNmin^2 (initialized to tolerance) we can reject the line
         * otherwise... determine if the projected poijnt lies within the bounds of the line: PR^2 -
         * PN^2 <= RS^2 and PS^2 - PN^2 <= RS^2
         *
         * where PR^2 = (xp - xr)^2 + (yp-yr)^2 and PS^2 = (xp - xs)^2 + (yp-ys)^2
         *
         * If so, calculate N as xn = xr + (RN/RS) B yn = y1 + (RN/RS) A
         *
         * where RN = sqrt(PR^2 - PN^2)
         */

        double PNminsq = tolerance * tolerance;
        EastNorth bestEN = null;
        double bestTime = 0.0;
        double px = P.east();
        double py = P.north();
        double rx = 0.0, ry = 0.0, sx, sy, x, y;
        if (tracks == null)
            return null;
        for (GpxTrack track : tracks) {
            for (GpxTrackSegment seg : track.getSegments()) {
                WayPoint R = null;
                for (WayPoint S : seg.getWayPoints()) {
                    EastNorth c = S.getEastNorth();
                    if (R == null) {
                        R = S;
                        rx = c.east();
                        ry = c.north();
                        x = px - rx;
                        y = py - ry;
                        double PRsq = x * x + y * y;
                        if (PRsq < PNminsq) {
                            PNminsq = PRsq;
                            bestEN = c;
                            bestTime = R.time;
                        }
                    } else {
                        sx = c.east();
                        sy = c.north();
                        double A = sy - ry;
                        double B = rx - sx;
                        double C = -A * rx - B * ry;
                        double RSsq = A * A + B * B;
                        if (RSsq == 0.0) {
                            continue;
                        }
                        double PNsq = A * px + B * py + C;
                        PNsq = PNsq * PNsq / RSsq;
                        if (PNsq < PNminsq) {
                            x = px - rx;
                            y = py - ry;
                            double PRsq = x * x + y * y;
                            x = px - sx;
                            y = py - sy;
                            double PSsq = x * x + y * y;
                            if (PRsq - PNsq <= RSsq && PSsq - PNsq <= RSsq) {
                                double RNoverRS = Math.sqrt((PRsq - PNsq) / RSsq);
                                double nx = rx - RNoverRS * B;
                                double ny = ry + RNoverRS * A;
                                bestEN = new EastNorth(nx, ny);
                                bestTime = R.time + RNoverRS * (S.time - R.time);
                                PNminsq = PNsq;
                            }
                        }
                        R = S;
                        rx = sx;
                        ry = sy;
                    }
                }
                if (R != null) {
                    EastNorth c = R.getEastNorth();
                    /* if there is only one point in the seg, it will do this twice, but no matter */
                    rx = c.east();
                    ry = c.north();
                    x = px - rx;
                    y = py - ry;
                    double PRsq = x * x + y * y;
                    if (PRsq < PNminsq) {
                        PNminsq = PRsq;
                        bestEN = c;
                        bestTime = R.time;
                    }
                }
            }
        }
        if (bestEN == null)
            return null;
        WayPoint best = new WayPoint(Main.getProjection().eastNorth2latlon(bestEN));
        best.time = bestTime;
        return best;
    }

    /**
     * Iterate over all track segments and over all routes.
     *
     * @param trackVisibility An array indicating which tracks should be
     * included in the iteration. Can be null, then all tracks are included.
     * @return an Iterable object, which iterates over all track segments and
     * over all routes
     */
    public Iterable<Collection<WayPoint>> getLinesIterable(final boolean[] trackVisibility) {
        return new Iterable<Collection<WayPoint>>() {
            @Override
            public Iterator<Collection<WayPoint>> iterator() {
                return new LinesIterator(GpxData.this, trackVisibility);
            }
        };
    }

    /**
     * Iterates over all track segments and then over all routes.
     */
    public static class LinesIterator implements Iterator<Collection<WayPoint>> {

        private Iterator<GpxTrack> itTracks;
        private int idxTracks;
        private Iterator<GpxTrackSegment> itTrackSegments;
        private Iterator<GpxRoute> itRoutes;

        private Collection<WayPoint> next;
        private boolean[] trackVisibility;

        public LinesIterator(GpxData data, boolean[] trackVisibility) {
            itTracks = data.tracks.iterator();
            idxTracks = -1;
            itRoutes = data.routes.iterator();
            this.trackVisibility = trackVisibility;
            next = getNext();
        }

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public Collection<WayPoint> next() {
            Collection<WayPoint> current = next;
            next = getNext();
            return current;
        }

        private Collection<WayPoint> getNext() {
            if (itTracks != null) {
                if (itTrackSegments != null && itTrackSegments.hasNext()) {
                    return itTrackSegments.next().getWayPoints();
                } else {
                    while (itTracks.hasNext()) {
                        GpxTrack nxtTrack = itTracks.next();
                        idxTracks++;
                        if (trackVisibility != null && !trackVisibility[idxTracks])
                            continue;
                        itTrackSegments = nxtTrack.getSegments().iterator();
                        if (itTrackSegments.hasNext()) {
                            return itTrackSegments.next().getWayPoints();
                        }
                    }
                    // if we get here, all the Tracks are finished; Continue with
                    // Routes
                    itTracks = null;
                }
            }
            if (itRoutes.hasNext()) {
                return itRoutes.next().routePoints;
            }
            return null;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

}
