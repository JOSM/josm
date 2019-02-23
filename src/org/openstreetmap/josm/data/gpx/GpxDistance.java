// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.tools.Geometry;

/**
 * A class to find the distance between an {@link OsmPrimitive} and a GPX point.
 *
 * @author Taylor Smock
 * @since 14802
 */
public final class GpxDistance {
    private GpxDistance() {
        // This class should not be instantiated
    }

    /**
     * Find the distance between a point and a dataset of surveyed points
     * @param p OsmPrimitive from which to get the lowest distance to a GPX point
     * @param gpxData Data from which to get the GPX points
     * @return The shortest distance
     */
    public static double getLowestDistance(OsmPrimitive p, GpxData gpxData) {
        return gpxData.getTrackPoints()
                .mapToDouble(tp -> getDistance(p, tp))
                .filter(x -> x >= 0)
                .min().orElse(Double.MAX_VALUE);
    }

    /**
     * Get the distance between an object and a waypoint
     * @param p OsmPrimitive to get the distance to the WayPoint
     * @param waypoint WayPoint to get the distance from
     * @return The shortest distance between p and waypoint
     */
    public static double getDistance(OsmPrimitive p, WayPoint waypoint) {
        if (p instanceof Node) {
            return getDistanceNode((Node) p, waypoint);
        } else if (p instanceof Way) {
            return getDistanceWay((Way) p, waypoint);
        } else if (p instanceof Relation) {
            return getDistanceRelation((Relation) p, waypoint);
        }
        return Double.MAX_VALUE;
    }

    /**
     * Get the shortest distance between a relation and a waypoint
     * @param relation Relation to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the relation and the waypoint
     */
    public static double getDistanceRelation(Relation relation, WayPoint waypoint) {
        double shortestDistance = Double.MAX_VALUE;
        List<Node> nodes = new ArrayList<>(relation.getMemberPrimitives(Node.class));
        List<Way> ways = new ArrayList<>(relation.getMemberPrimitives(Way.class));
        List<Relation> relations = new ArrayList<>(relation.getMemberPrimitives(Relation.class));
        if (nodes.isEmpty() && ways.isEmpty() && relations.isEmpty()) return Double.MAX_VALUE;
        for (Relation nrelation : relations) {
            double distance = getDistanceRelation(nrelation, waypoint);
            if (distance < shortestDistance) shortestDistance = distance;
        }
        for (Way way : ways) {
            double distance = getDistanceWay(way, waypoint);
            if (distance < shortestDistance) shortestDistance = distance;
        }
        for (Node node : nodes) {
            double distance = getDistanceNode(node, waypoint);
            if (distance < shortestDistance) shortestDistance = distance;
        }
        return shortestDistance;
    }

    /**
     * Get the shortest distance between a way and a waypoint
     * @param way Way to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the way and the waypoint
     */
    public static double getDistanceWay(Way way, WayPoint waypoint) {
        double shortestDistance = Double.MAX_VALUE;
        if (way == null || waypoint == null) return shortestDistance;
        LatLon llwaypoint = waypoint.getCoor();
        EastNorth enwaypoint = new EastNorth(llwaypoint.getY(), llwaypoint.getX());
        for (int i = 0; i < way.getNodesCount() - 1; i++) {
            double distance = Double.MAX_VALUE;
            LatLon llfirst = way.getNode(i).getCoor();
            LatLon llsecond = way.getNode(i + 1).getCoor();
            EastNorth first = new EastNorth(llfirst.getY(), llfirst.getX());
            EastNorth second = new EastNorth(llsecond.getY(), llsecond.getX());
            if (first.isValid() && second.isValid()) {
                EastNorth closestPoint = Geometry.closestPointToSegment(first, second, enwaypoint);
                distance = llwaypoint.greatCircleDistance(new LatLon(closestPoint.getX(), closestPoint.getY()));
            } else if (first.isValid() && !second.isValid()) {
                distance = getDistanceEastNorth(first, waypoint);
            } else if (!first.isValid() && second.isValid()) {
                distance = getDistanceEastNorth(second, waypoint);
            } else if (!first.isValid() && !second.isValid()) {
                distance = Double.MAX_VALUE;
            }
            if (distance < shortestDistance) shortestDistance = distance;

        }
        return shortestDistance;
    }

    /**
     * Get the distance between a node and a waypoint
     * @param node Node to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     */
    public static double getDistanceNode(Node node, WayPoint waypoint) {
        if (node == null) return Double.MAX_VALUE;
        return getDistanceLatLon(node.getCoor(), waypoint);
    }

    /**
     * Get the distance between coordinates (provided by EastNorth) and a waypoint
     * @param en The EastNorth to get the distance to
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     */
    public static double getDistanceEastNorth(EastNorth en, WayPoint waypoint) {
        if (en == null || !en.isValid()) return Double.MAX_VALUE;
        return getDistanceLatLon(new LatLon(en.getY(), en.getX()), waypoint);
    }

    /**
     * Get the distance between coordinates (latitude longitude) and a waypoint
     * @param latlon LatLon to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     */
    public static double getDistanceLatLon(LatLon latlon, WayPoint waypoint) {
        if (latlon == null || waypoint == null || waypoint.getCoor() == null) return Double.MAX_VALUE;
        return waypoint.getCoor().greatCircleDistance(latlon);
    }
}
