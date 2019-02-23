// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link GpxDistance}.
 */
public class GpxDistanceTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    /**
     * Unit test of method {@link GpxDistance#getDistanceWay}.
     */
    @Test
    public void testGetDistanceWay() {
        Node node1 = new Node();
        Node node2 = new Node();
        Way way = new Way();
        node1.setCoor(new LatLon(0, 0));
        node2.setCoor(new LatLon(0, 1));
        way.addNode(node1);
        way.addNode(node2);

        WayPoint waypoint = new WayPoint(new LatLon(1, 0));

        double distance = GpxDistance.getDistanceWay(null, waypoint);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        distance = GpxDistance.getDistanceWay(way, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        distance = GpxDistance.getDistanceWay(null, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        distance = GpxDistance.getDistanceWay(way, waypoint);
        /* 111319.49077 uses the WGS84/NAD38/GRS80 model for
         * the distance between (0, 0) and (0, 1) */
        assertEquals(111319.49077, distance, 0.1);
    }

    /**
     * Unit test of method {@link GpxDistance#getDistanceNode}.
     */
    @Test
    public void testGetDistanceNode() {
        double distance = GpxDistance.getDistanceNode(null, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        Node node = new Node();
        node.setCoor(new LatLon(0, 0));
        distance = GpxDistance.getDistanceNode(node, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        WayPoint waypoint = new WayPoint(new LatLon(0, 0));
        distance = GpxDistance.getDistanceNode(node, waypoint);
        assertEquals(0.0, distance, 0.0001); // should be zero delta

        distance = GpxDistance.getDistanceNode(null, waypoint);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        node.setCoor(new LatLon(1, 0));
        distance = GpxDistance.getDistanceNode(node, waypoint);
        /* 111319.49077 uses the WGS84/NAD38/GRS80 model for
         * the distance between (0, 0) and (0, 1) */
        assertEquals(111319.49077, distance, 0.1);
    }

    /**
     * Unit test of method {@link GpxDistance#getDistanceEastNorth}.
     */
    @Test
    public void testGetDistanceEastNorth() {
        double distance = GpxDistance.getDistanceEastNorth(null, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        EastNorth en = new EastNorth(0, 0);
        distance = GpxDistance.getDistanceEastNorth(en, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        WayPoint waypoint = new WayPoint(new LatLon(0, 0));
        distance = GpxDistance.getDistanceEastNorth(en, waypoint);
        assertEquals(0.0, distance, 0.0001); // should be zero delta

        distance = GpxDistance.getDistanceEastNorth(null, waypoint);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        en = new EastNorth(0, 1);
        distance = GpxDistance.getDistanceEastNorth(en, waypoint);
        /* 111319.49077 uses the WGS84/NAD38/GRS80 model for
         * the distance between (0, 0) and (0, 1) */
        assertEquals(111319.49077, distance, 0.1);
    }


    /**
     * Unit test of method {@link GpxDistance#getDistanceLatLon}.
     */
    @Test
    public void testGetDistanceLatLon() {
        double distance = GpxDistance.getDistanceLatLon(null, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        LatLon ll = new LatLon(0, 0);
        distance = GpxDistance.getDistanceLatLon(ll, null);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        WayPoint waypoint = new WayPoint(ll);
        distance = GpxDistance.getDistanceLatLon(ll, waypoint);
        assertEquals(0.0, distance, 0.0001); // should be zero delta

        distance = GpxDistance.getDistanceLatLon(null, waypoint);
        assertEquals(Double.MAX_VALUE, distance, 0.1);

        ll = new LatLon(0, 1);
        distance = GpxDistance.getDistanceLatLon(ll, waypoint);
        /* 111319.49077 uses the WGS84/NAD38/GRS80 model for
         * the distance between (0, 0) and (0, 1) */
        assertEquals(111319.49077, distance, 0.1);
    }
}
