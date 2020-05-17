// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.actions.mapmode;

import java.awt.Point;
import java.util.List;
import java.util.Optional;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.osm.WaySegment;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapView;
import org.openstreetmap.josm.tools.Geometry;
import org.openstreetmap.josm.tools.Pair;

/**
 * This static class contains functions used to find target way, node to move or
 * segment to divide.
 *
 * @author Alexander Kachkaev &lt;alexander@kachkaev.ru&gt;, 2011
 */
final class ImproveWayAccuracyHelper {

    private ImproveWayAccuracyHelper() {
        // Hide default constructor for utils classes
    }

    /**
     * Finds the way to work on. If the mouse is on the node, extracts one of
     * the ways containing it. If the mouse is on the way, simply returns it.
     *
     * @param mv the current map view
     * @param p the cursor position
     * @return {@code Way} or {@code null} in case there is nothing under the cursor.
     */
    public static Way findWay(MapView mv, Point p) {
        if (mv == null || p == null) {
            return null;
        }

        Node node = mv.getNearestNode(p, OsmPrimitive::isSelectable);

        if (node != null) {
            Optional<Way> candidate = node.referrers(Way.class).findFirst();
            if (candidate.isPresent()) {
                return candidate.get();
            }
        }

        return MainApplication.getMap().mapView.getNearestWay(p, OsmPrimitive::isSelectable);
    }

    /**
     * Returns the nearest node to cursor. All nodes that are “behind” segments
     * are neglected. This is to avoid way self-intersection after moving the
     * candidateNode to a new place.
     *
     * @param mv the current map view
     * @param w the way to check
     * @param p the cursor position
     * @return nearest node to cursor
     */
    public static Node findCandidateNode(MapView mv, Way w, Point p) {
        if (mv == null || w == null || p == null) {
            return null;
        }

        EastNorth pEN = mv.getEastNorth(p.x, p.y);

        Double bestDistance = Double.MAX_VALUE;
        Double currentDistance;
        List<Pair<Node, Node>> wpps = w.getNodePairs(false);

        Node result = null;

        mainLoop:
        for (Node n : w.getNodes()) {
            EastNorth nEN = n.getEastNorth();

            if (nEN == null) {
                // Might happen if lat/lon for that point are not known.
                continue;
            }

            currentDistance = pEN.distance(nEN);

            if (currentDistance < bestDistance) {
                // Making sure this candidate is not behind any segment.
                for (Pair<Node, Node> wpp : wpps) {
                    if (!wpp.a.equals(n)
                            && !wpp.b.equals(n)
                            && Geometry.getSegmentSegmentIntersection(
                            wpp.a.getEastNorth(), wpp.b.getEastNorth(),
                            pEN, nEN) != null) {
                        continue mainLoop;
                    }
                }
                result = n;
                bestDistance = currentDistance;
            }
        }

        return result;
    }

    /**
     * Returns the nearest way segment to cursor. The distance to segment ab is
     * the length of altitude from p to ab (say, c) or the minimum distance from
     * p to a or b if c is out of ab.
     *
     * The priority is given to segments where c is in ab. Otherwise, a segment
     * with the largest angle apb is chosen.
     *
     * @param mv the current map view
     * @param w the way to check
     * @param p the cursor position
     * @return nearest way segment to cursor
     */
    public static WaySegment findCandidateSegment(MapView mv, Way w, Point p) {
        if (mv == null || w == null || p == null) {
            return null;
        }

        EastNorth pEN = mv.getEastNorth(p.x, p.y);

        Double currentDistance;
        Double currentAngle;
        Double bestDistance = Double.MAX_VALUE;
        Double bestAngle = 0.0;

        int candidate = -1;

        List<Pair<Node, Node>> wpps = w.getNodePairs(true);

        int i = -1;
        for (Pair<Node, Node> wpp : wpps) {
            ++i;

            EastNorth a = wpp.a.getEastNorth();
            EastNorth b = wpp.b.getEastNorth();

            // Finding intersection of the segment with its altitude from p
            EastNorth altitudeIntersection = Geometry.closestPointToSegment(a, b, pEN);
            currentDistance = pEN.distance(altitudeIntersection);

            if (!altitudeIntersection.equals(a) && !altitudeIntersection.equals(b)) {
                // If the segment intersects with the altitude from p,
                // make an angle too big to let this candidate win any others
                // having the same distance.
                currentAngle = Double.MAX_VALUE;
            } else {
                // Otherwise measure the angle
                currentAngle = Math.abs(Geometry.getCornerAngle(a, pEN, b));
            }

            if (currentDistance < bestDistance
                    || (currentAngle > bestAngle && currentDistance < bestDistance * 1.0001 /*
                     * equality
                     */)) {
                candidate = i;
                bestAngle = currentAngle;
                bestDistance = currentDistance;
            }

        }
        return candidate != -1 ? new WaySegment(w, candidate) : null;
    }
}
