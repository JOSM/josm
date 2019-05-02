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
                .mapToDouble(tp -> Geometry.getDistance(p, new Node(tp.getCoor())))
                .filter(x -> x >= 0)
                .min().orElse(Double.MAX_VALUE);
    }

    /**
     * Get the distance between an object and a waypoint
     * @param p OsmPrimitive to get the distance to the WayPoint
     * @param waypoint WayPoint to get the distance from
     * @return The shortest distance between p and waypoint
     * @deprecated Use {@code Geometry.getDistance(p, new Node(waypoint.getCoor()))}
     * instead
     */
    @Deprecated
    public static double getDistance(OsmPrimitive p, WayPoint waypoint) {
        return Geometry.getDistance(p, new Node(waypoint.getCoor()));
    }

    /**
     * Get the shortest distance between a relation and a waypoint
     * @param relation Relation to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the relation and the waypoint
     * @deprecated Use {@code Geometry.getDistance(relation, new Node(waypoint.getCoor()))}
     * instead
     */
    @Deprecated
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
     * @deprecated Use {@code Geometry.getDistanceWayNode(way, new Node(waypoint.getCoor()))} instead
     */
    @Deprecated
    public static double getDistanceWay(Way way, WayPoint waypoint) {
        if (way == null || waypoint == null) return Double.MAX_VALUE;
        return Geometry.getDistanceWayNode(way, new Node(waypoint.getCoor()));
    }

    /**
     * Get the distance between a node and a waypoint
     * @param node Node to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     * @deprecated Use {@code Geometry.getDistance(node, new Node(waypoint.getCoor()))}
     * instead
     */
    @Deprecated
    public static double getDistanceNode(Node node, WayPoint waypoint) {
        if (node == null || waypoint == null) return Double.MAX_VALUE;
        return Geometry.getDistance(node, new Node(waypoint.getCoor()));
    }

    /**
     * Get the distance between coordinates (provided by EastNorth) and a waypoint
     * @param en The EastNorth to get the distance to
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     * @deprecated Use {@code Geometry.getDistance(new Node(en), new Node(waypoint.getCoor()))} instead
     */
    @Deprecated
    public static double getDistanceEastNorth(EastNorth en, WayPoint waypoint) {
        if (en == null || waypoint == null) return Double.MAX_VALUE;
        return Geometry.getDistance(new Node(en), new Node(waypoint.getCoor()));
    }

    /**
     * Get the distance between coordinates (latitude longitude) and a waypoint
     * @param latlon LatLon to get the distance from
     * @param waypoint WayPoint to get the distance to
     * @return The distance between the two points
     * @deprecated Use {@code Geometry.getDistance(new Node(latlon), new Node(waypoint.getCoor()))} instead
     */
    @Deprecated
    public static double getDistanceLatLon(LatLon latlon, WayPoint waypoint) {
        if (latlon == null || waypoint == null || waypoint.getCoor() == null) return Double.MAX_VALUE;
        return Geometry.getDistance(new Node(latlon), new Node(waypoint.getCoor()));
    }
}
