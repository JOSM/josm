// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeEvent;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ListenerList;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;

/**
 * Unit tests for class {@link GpxData}.
 */
public class GpxDataTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    private GpxData data;

    /**
     * Set up empty test data
     */
    @Before
    public void setUp() {
        data = new GpxData();
    }


    /**
     * Test method for {@link GpxData#mergeFrom(GpxData)}.
     */
    @Test
    public void testMergeFrom() {
        ImmutableGpxTrack track = singleWaypointGpxTrack();
        GpxRoute route = singleWaypointRoute();
        WayPoint newWP = new WayPoint(LatLon.NORTH_POLE);
        WayPoint existingWP = new WayPoint(LatLon.SOUTH_POLE);

        GpxData dataToMerge = new GpxData();
        dataToMerge.addTrack(track);
        dataToMerge.addRoute(route);
        dataToMerge.addWaypoint(newWP);

        data.addWaypoint(existingWP);
        data.mergeFrom(dataToMerge);

        assertEquals(1, data.getTracks().size());
        assertEquals(1, data.getRoutes().size());
        assertEquals(2, data.getWaypoints().size());

        assertTrue(data.getTracks().contains(track));
        assertTrue(data.getRoutes().contains(route));
        assertTrue(data.getWaypoints().contains(newWP));
        assertTrue(data.getWaypoints().contains(existingWP));
    }

    /**
     * Test method for {@link GpxData#getTracks()},  {@link GpxData#addTrack(GpxTrack)},  {@link GpxData#removeTrack(GpxTrack)}.
     */
    @Test
    public void testTracks() {
        assertEquals(0, data.getTracks().size());

        ImmutableGpxTrack track1 = emptyGpxTrack();
        ImmutableGpxTrack track2 = singleWaypointGpxTrack();
        data.addTrack(track1);
        assertEquals(1, data.getTracks().size());
        data.addTrack(track2);
        assertEquals(2, data.getTracks().size());
        assertTrue(data.getTracks().contains(track1));
        assertTrue(data.getTracks().contains(track2));

        data.removeTrack(track1);
        assertEquals(1, data.getTracks().size());
        assertFalse(data.getTracks().contains(track1));
        assertTrue(data.getTracks().contains(track2));
    }

    /**
     * Test method for {@link GpxData#addTrack(GpxTrack)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddTrackFails() {
        ImmutableGpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        data.addTrack(track1);
    }

    /**
     * Test method for {@link GpxData#removeTrack(GpxTrack)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveTrackFails() {
        ImmutableGpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        data.removeTrack(track1);
        data.removeTrack(track1);
    }

    /**
     * Test method for {@link GpxData#getRoutes()}, {@link GpxData#addRoute(GpxRoute)}, {@link GpxData#removeRoute(GpxRoute)}.
     */
    @Test
    public void testRoutes() {
        assertEquals(0, data.getTracks().size());

        GpxRoute route1 = new GpxRoute();
        GpxRoute route2 = new GpxRoute();
        route2.routePoints.add(new WayPoint(LatLon.NORTH_POLE));
        data.addRoute(route1);
        assertEquals(1, data.getRoutes().size());
        data.addRoute(route2);
        assertEquals(2, data.getRoutes().size());
        assertTrue(data.getRoutes().contains(route1));
        assertTrue(data.getRoutes().contains(route2));

        data.removeRoute(route1);
        assertEquals(1, data.getRoutes().size());
        assertFalse(data.getRoutes().contains(route1));
        assertTrue(data.getRoutes().contains(route2));
    }

    /**
     * Test method for {@link GpxData#addRoute(GpxRoute)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddRouteFails() {
        GpxRoute route1 = new GpxRoute();
        data.addRoute(route1);
        data.addRoute(route1);
    }

    /**
     * Test method for {@link GpxData#removeRoute(GpxRoute)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveRouteFails() {
        GpxRoute route1 = new GpxRoute();
        data.addRoute(route1);
        data.removeRoute(route1);
        data.removeRoute(route1);
    }

    /**
     * Test method for {@link GpxData#getWaypoints()}, {@link GpxData#addWaypoint(WayPoint)}, {@link GpxData#removeWaypoint(WayPoint)}.
     */
    @Test
    public void testWaypoints() {
        assertEquals(0, data.getTracks().size());

        WayPoint waypoint1 = new WayPoint(LatLon.ZERO);
        WayPoint waypoint2 = new WayPoint(LatLon.NORTH_POLE);
        data.addWaypoint(waypoint1);
        assertEquals(1, data.getWaypoints().size());
        data.addWaypoint(waypoint2);
        assertEquals(2, data.getWaypoints().size());
        assertTrue(data.getWaypoints().contains(waypoint1));
        assertTrue(data.getWaypoints().contains(waypoint2));

        data.removeWaypoint(waypoint1);
        assertEquals(1, data.getWaypoints().size());
        assertFalse(data.getWaypoints().contains(waypoint1));
        assertTrue(data.getWaypoints().contains(waypoint2));
    }

    /**
     * Test method for {@link GpxData#addWaypoint(WayPoint)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testAddWaypointFails() {
        WayPoint waypoint1 = new WayPoint(LatLon.ZERO);
        data.addWaypoint(waypoint1);
        data.addWaypoint(waypoint1);
    }

    /**
     * Test method for {@link GpxData#removeWaypoint(WayPoint)}.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testRemoveWaypointFails() {
        WayPoint waypoint1 = new WayPoint(LatLon.ZERO);
        data.addWaypoint(waypoint1);
        data.removeWaypoint(waypoint1);
        data.removeWaypoint(waypoint1);
    }

    /**
     * Test method for {@link GpxData#hasTrackPoints()}.
     */
    @Test
    public void testHasTrackPoints() {
        assertFalse(data.hasTrackPoints());
        ImmutableGpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        assertFalse(data.hasTrackPoints());
        ImmutableGpxTrack track2 = singleWaypointGpxTrack();
        data.addTrack(track2);
        assertTrue(data.hasTrackPoints());
    }

    /**
     * Test method for {@link GpxData#getTrackPoints()}.
     */
    @Test
    public void testGetTrackPoints() {
        assertEquals(0, data.getTrackPoints().count());
        ImmutableGpxTrack track1 = singleWaypointGpxTrack();
        data.addTrack(track1);
        assertEquals(1, data.getTrackPoints().count());
        ImmutableGpxTrack track2 = singleWaypointGpxTrack();
        data.addTrack(track2);
        assertEquals(2, data.getTrackPoints().count());
    }

    /**
     * Test method for {@link GpxData#hasRoutePoints()}.
     */
    @Test
    public void testHasRoutePoints() {

    }

    /**
     * Test method for {@link GpxData#isEmpty()}.
     */
    @Test
    public void testIsEmpty() {
        ImmutableGpxTrack track1 = singleWaypointGpxTrack();
        WayPoint waypoint = new WayPoint(LatLon.ZERO);
        GpxRoute route = singleWaypointRoute();

        assertTrue(data.isEmpty());

        data.addTrack(track1);
        assertFalse(data.isEmpty());
        data.removeTrack(track1);
        assertTrue(data.isEmpty());

        data.addWaypoint(waypoint);
        assertFalse(data.isEmpty());
        data.removeWaypoint(waypoint);
        assertTrue(data.isEmpty());

        data.addRoute(route);
        assertFalse(data.isEmpty());
        data.removeRoute(route);
        assertTrue(data.isEmpty());
    }

    /**
     * Test method for {@link GpxData#length()}.
     */
    @Test
    public void testLength() {
        ImmutableGpxTrack track1 = waypointGpxTrack(
                new WayPoint(new LatLon(0, 0)),
                new WayPoint(new LatLon(1, 1)),
                new WayPoint(new LatLon(0, 2)));
        ImmutableGpxTrack track2 = waypointGpxTrack(
                new WayPoint(new LatLon(0, 0)),
                new WayPoint(new LatLon(-1, 1)));
        data.addTrack(track1);
        data.addTrack(track2);
        assertEquals(3 * new LatLon(0, 0).greatCircleDistance(new LatLon(1, 1)), data.length(), 1);

    }

    /**
     * Test method for {@link GpxData#getMinMaxTimeForAllTracks()}.
     */
    @Test
    public void testGetMinMaxTimeForAllTracks() {
        assertEquals(0, data.getMinMaxTimeForAllTracks().length);

        WayPoint p1 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p2 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p3 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p4 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p5 = new WayPoint(LatLon.NORTH_POLE);
        p1.setTime(new Date(200020));
        p2.setTime(new Date(100020));
        p4.setTime(new Date(500020));
        data.addTrack(new ImmutableGpxTrack(Arrays.asList(Arrays.asList(p1, p2)), Collections.emptyMap()));
        data.addTrack(new ImmutableGpxTrack(Arrays.asList(Arrays.asList(p3, p4, p5)), Collections.emptyMap()));

        Date[] times = data.getMinMaxTimeForAllTracks();
        assertEquals(times.length, 2);
        assertEquals(new Date(100020), times[0]);
        assertEquals(new Date(500020), times[1]);
    }

    /**
     * Test method for {@link GpxData#nearestPointOnTrack(org.openstreetmap.josm.data.coor.EastNorth, double)}.
     */
    @Test
    public void testNearestPointOnTrack() {
        List<WayPoint> points = Stream
                .of(new EastNorth(10, 10), new EastNorth(10, 0), new EastNorth(-1, 0))
                .map(ProjectionRegistry.getProjection()::eastNorth2latlon)
                .map(WayPoint::new)
                .collect(Collectors.toList());
        data.addTrack(new ImmutableGpxTrack(Arrays.asList(points), Collections.emptyMap()));

        WayPoint closeToMiddle = data.nearestPointOnTrack(new EastNorth(10, 0), 10);
        assertEquals(points.get(1), closeToMiddle);

        WayPoint close = data.nearestPointOnTrack(new EastNorth(5, 5), 10);
        assertEquals(10, close.getEastNorth(ProjectionRegistry.getProjection()).east(), .01);
        assertEquals(5, close.getEastNorth(ProjectionRegistry.getProjection()).north(), .01);

        close = data.nearestPointOnTrack(new EastNorth(15, 5), 10);
        assertEquals(10, close.getEastNorth(ProjectionRegistry.getProjection()).east(), .01);
        assertEquals(5, close.getEastNorth(ProjectionRegistry.getProjection()).north(), .01);

        assertNull(data.nearestPointOnTrack(new EastNorth(5, 5), 1));
    }

    /**
     * Test method for {@link GpxData#getDataSources()}.
     */
    @Test
    public void testGetDataSources() {
        DataSource ds = new DataSource(new Bounds(0, 0, 1, 1), "test");
        data.dataSources.add(ds);
        assertEquals(new ArrayList<>(Arrays.asList(ds)), new ArrayList<>(data.getDataSources()));
    }

    /**
     * Test method for {@link GpxData#getDataSourceArea()}.
     */
    @Test
    public void testGetDataSourceArea() {
        DataSource ds = new DataSource(new Bounds(0, 0, 1, 1), "test");
        data.dataSources.add(ds);
        assertNotNull(data.getDataSourceArea());
        assertTrue(data.getDataSourceArea().contains(0.5, 0.5));
        assertFalse(data.getDataSourceArea().contains(0.5, 1.5));
    }

    /**
     * Test method for {@link GpxData#getDataSourceBounds()}.
     */
    @Test
    public void testGetDataSourceBounds() {
        Bounds bounds = new Bounds(0, 0, 1, 1);
        DataSource ds = new DataSource(bounds, "test");
        data.dataSources.add(ds);
        assertEquals(Arrays.asList(bounds), data.getDataSourceBounds());
    }

    /**
     * Test method for {@link GpxData#addChangeListener(GpxData.GpxDataChangeListener)},
     * {@link GpxData#addWeakChangeListener(GpxData.GpxDataChangeListener)},
     * {@link GpxData#removeChangeListener(GpxData.GpxDataChangeListener)}.
     */
    @Test
    public void testChangeListener() {
        TestChangeListener cl1 = new TestChangeListener();
        TestChangeListener cl2 = new TestChangeListener();

        data.addChangeListener(cl1);
        data.addWeakChangeListener(cl2);
        assertNull(cl1.lastEvent);
        assertNull(cl2.lastEvent);

        data.addTrack(singleWaypointGpxTrack());
        assertEquals(data, cl1.lastEvent.getSource());
        assertEquals(data, cl2.lastEvent.getSource());
        cl1.lastEvent = null;
        cl2.lastEvent = null;

        data.addRoute(singleWaypointRoute());
        assertEquals(data, cl1.lastEvent.getSource());
        assertEquals(data, cl2.lastEvent.getSource());
        cl1.lastEvent = null;
        cl2.lastEvent = null;

        data.removeChangeListener(cl1);
        data.removeChangeListener(cl2);
        data.addTrack(singleWaypointGpxTrack());
        assertNull(cl1.lastEvent);
        assertNull(cl2.lastEvent);
    }

    private static class TestChangeListener implements GpxDataChangeListener {

        private GpxDataChangeEvent lastEvent;

        @Override
        public void gpxDataChanged(GpxDataChangeEvent e) {
            lastEvent = e;
        }

    }

    private static ImmutableGpxTrack emptyGpxTrack() {
        return new ImmutableGpxTrack(Collections.<Collection<WayPoint>>emptyList(), Collections.emptyMap());
    }

    private static ImmutableGpxTrack singleWaypointGpxTrack() {
        return new ImmutableGpxTrack(Collections.singleton(Collections.singleton(new WayPoint(LatLon.ZERO))), Collections.emptyMap());
    }

    private static ImmutableGpxTrack waypointGpxTrack(WayPoint... wps) {
        return new ImmutableGpxTrack(Collections.singleton(Arrays.asList(wps)), Collections.emptyMap());
    }

    private static GpxRoute singleWaypointRoute() {
        GpxRoute route = new GpxRoute();
        route.routePoints.add(new WayPoint(LatLon.ZERO));
        return route;
    }

    /**
     * Unit test of methods {@link GpxData#equals} and {@link GpxData#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        EqualsVerifier.forClass(GpxData.class).usingGetClass()
            .withIgnoredFields("attr", "creator", "fromServer", "storageFile", "listeners", "tracks", "routes", "waypoints", "proxy")
            .withPrefabValues(WayPoint.class, new WayPoint(LatLon.NORTH_POLE), new WayPoint(LatLon.SOUTH_POLE))
            .withPrefabValues(ListenerList.class, ListenerList.create(), ListenerList.create())
            .verify();
    }
}
