// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeEvent;
import org.openstreetmap.josm.data.gpx.GpxData.GpxDataChangeListener;
import org.openstreetmap.josm.data.projection.ProjectionRegistry;
import org.openstreetmap.josm.io.GpxReaderTest;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.ListenerList;
import org.openstreetmap.josm.tools.date.Interval;
import org.xml.sax.SAXException;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

/**
 * Unit tests for class {@link GpxData}.
 */
class GpxDataTest {

    /**
     * Setup test.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().projection();

    private GpxData data;

    /**
     * Set up empty test data
     */
    @BeforeEach
    public void setUp() {
        data = new GpxData();
    }

    /**
     * Test method for {@link GpxData#mergeFrom(GpxData)}.
     */
    @Test
    void testMergeFrom() {
        GpxTrack track = singleWaypointGpxTrack();
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
     * Test method for {@link GpxData#mergeFrom(GpxData, boolean, boolean)} including cutting/connecting tracks using actual files.
     * @throws Exception if the track cannot be parsed
     */
    @Test
    void testMergeFromFiles() throws Exception {
        testMerge(false, false, "Merged-all"); // regular merging
        testMerge(true, false, "Merged-cut"); // cut overlapping tracks, but do not connect them
        testMerge(true, true, "Merged-cut-connect"); // cut overlapping tracks and connect them
    }

    private static void testMerge(boolean cut, boolean connect, String exp) throws IOException, SAXException {
        final GpxData own = getGpx("Layer1");
        final GpxData other = getGpx("Layer2");
        final GpxData expected = getGpx(exp);
        own.mergeFrom(other, cut, connect);
        own.put(GpxConstants.META_BOUNDS, null);
        expected.put(GpxConstants.META_BOUNDS, null); //they are only updated by GpxWriter
        assertEquals(expected, own, exp + " didn't match!");
    }

    private static GpxData getGpx(String file) throws IOException, SAXException {
        return GpxReaderTest.parseGpxData(TestUtils.getTestDataRoot() + "mergelayers/" + file + ".gpx");
    }

    /**
     * Test method for {@link GpxData#getTracks()},  {@link GpxData#addTrack(IGpxTrack)},  {@link GpxData#removeTrack(IGpxTrack)}.
     */
    @Test
    void testTracks() {
        assertEquals(0, data.getTracks().size());

        GpxTrack track1 = emptyGpxTrack();
        GpxTrack track2 = singleWaypointGpxTrack();
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
     * Test method for {@link GpxData#addTrack(IGpxTrack)}.
     */
    @Test
    void testAddTrackFails() {
        GpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        assertThrows(IllegalArgumentException.class, () -> data.addTrack(track1));
    }

    /**
     * Test method for {@link GpxData#removeTrack(IGpxTrack)}.
     */
    @Test
    void testRemoveTrackFails() {
        GpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        data.removeTrack(track1);
        assertThrows(IllegalArgumentException.class, () -> data.removeTrack(track1));
    }

    /**
     * Test method for {@link GpxData#getRoutes()}, {@link GpxData#addRoute(GpxRoute)}, {@link GpxData#removeRoute(GpxRoute)}.
     */
    @Test
    void testRoutes() {
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
    @Test
    void testAddRouteFails() {
        GpxRoute route1 = new GpxRoute();
        data.addRoute(route1);
        assertThrows(IllegalArgumentException.class, () -> data.addRoute(route1));
    }

    /**
     * Test method for {@link GpxData#removeRoute(GpxRoute)}.
     */
    @Test
    void testRemoveRouteFails() {
        GpxRoute route1 = new GpxRoute();
        data.addRoute(route1);
        data.removeRoute(route1);
        assertThrows(IllegalArgumentException.class, () -> data.removeRoute(route1));
    }

    /**
     * Test method for {@link GpxData#getWaypoints()}, {@link GpxData#addWaypoint(WayPoint)}, {@link GpxData#removeWaypoint(WayPoint)}.
     */
    @Test
    void testWaypoints() {
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
    @Test
    void testAddWaypointFails() {
        WayPoint waypoint1 = new WayPoint(LatLon.ZERO);
        data.addWaypoint(waypoint1);
        assertThrows(IllegalArgumentException.class, () -> data.addWaypoint(waypoint1));
    }

    /**
     * Test method for {@link GpxData#removeWaypoint(WayPoint)}.
     */
    @Test
    void testRemoveWaypointFails() {
        WayPoint waypoint1 = new WayPoint(LatLon.ZERO);
        data.addWaypoint(waypoint1);
        data.removeWaypoint(waypoint1);
        assertThrows(IllegalArgumentException.class, () -> data.removeWaypoint(waypoint1));
    }

    /**
     * Test method for {@link GpxData#hasTrackPoints()}.
     */
    @Test
    void testHasTrackPoints() {
        assertFalse(data.hasTrackPoints());
        GpxTrack track1 = emptyGpxTrack();
        data.addTrack(track1);
        assertFalse(data.hasTrackPoints());
        GpxTrack track2 = singleWaypointGpxTrack();
        data.addTrack(track2);
        assertTrue(data.hasTrackPoints());
    }

    /**
     * Test method for {@link GpxData#getTrackPoints()}.
     */
    @Test
    void testGetTrackPoints() {
        assertEquals(0, data.getTrackPoints().count());
        GpxTrack track1 = singleWaypointGpxTrack();
        data.addTrack(track1);
        assertEquals(1, data.getTrackPoints().count());
        GpxTrack track2 = singleWaypointGpxTrack();
        data.addTrack(track2);
        assertEquals(2, data.getTrackPoints().count());
    }

    /**
     * Test method for {@link GpxData#hasRoutePoints()}.
     */
    @Test
    void testHasRoutePoints() {

    }

    /**
     * Test method for {@link GpxData#isEmpty()}.
     */
    @Test
    void testIsEmpty() {
        GpxTrack track1 = singleWaypointGpxTrack();
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
    void testLength() {
        GpxTrack track1 = waypointGpxTrack(
                new WayPoint(new LatLon(0, 0)),
                new WayPoint(new LatLon(1, 1)),
                new WayPoint(new LatLon(0, 2)));
        GpxTrack track2 = waypointGpxTrack(
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
    void testGetMinMaxTimeForAllTracks() {
        assertFalse(data.getMinMaxTimeForAllTracks().isPresent());

        WayPoint p1 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p2 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p3 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p4 = new WayPoint(LatLon.NORTH_POLE);
        WayPoint p5 = new WayPoint(LatLon.NORTH_POLE);
        p1.setInstant(Instant.ofEpochMilli(200020));
        p2.setInstant(Instant.ofEpochMilli(100020));
        p4.setInstant(Instant.ofEpochMilli(500020));
        data.addTrack(new GpxTrack(Arrays.asList(Arrays.asList(p1, p2)), Collections.emptyMap()));
        data.addTrack(new GpxTrack(Arrays.asList(Arrays.asList(p3, p4, p5)), Collections.emptyMap()));

        Interval times = data.getMinMaxTimeForAllTracks().orElse(null);
        assertEquals("1970-01-01T00:01:40.020Z/1970-01-01T00:08:20.020Z", times.toString());
        assertEquals(Instant.ofEpochMilli(100020), times.getStart());
        assertEquals(Instant.ofEpochMilli(500020), times.getEnd());
    }

    /**
     * Test method for {@link GpxData#nearestPointOnTrack(org.openstreetmap.josm.data.coor.EastNorth, double)}.
     */
    @Test
    void testNearestPointOnTrack() {
        List<WayPoint> points = Stream
                .of(new EastNorth(10, 10), new EastNorth(10, 0), new EastNorth(-1, 0))
                .map(ProjectionRegistry.getProjection()::eastNorth2latlon)
                .map(WayPoint::new)
                .collect(Collectors.toList());
        data.addTrack(new GpxTrack(Arrays.asList(points), Collections.emptyMap()));

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
    void testGetDataSources() {
        DataSource ds = new DataSource(new Bounds(0, 0, 1, 1), "test");
        data.dataSources.add(ds);
        assertEquals(new ArrayList<>(Arrays.asList(ds)), new ArrayList<>(data.getDataSources()));
    }

    /**
     * Test method for {@link GpxData#getDataSourceArea()}.
     */
    @Test
    void testGetDataSourceArea() {
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
    void testGetDataSourceBounds() {
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
    void testChangeListener() {
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

    private static GpxTrack emptyGpxTrack() {
        return new GpxTrack(Collections.<Collection<WayPoint>>emptyList(), Collections.emptyMap());
    }

    private static GpxTrack singleWaypointGpxTrack() {
        return new GpxTrack(Collections.singleton(Collections.singleton(new WayPoint(LatLon.ZERO))), Collections.emptyMap());
    }

    private static GpxTrack waypointGpxTrack(WayPoint... wps) {
        return new GpxTrack(Collections.singleton(Arrays.asList(wps)), Collections.emptyMap());
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
    void testEqualsContract() {
        TestUtils.assumeWorkingEqualsVerifier();
        GpxExtensionCollection col = new GpxExtensionCollection();
        col.add("josm", "from-server", "true");
        EqualsVerifier.forClass(GpxData.class).usingGetClass()
            .suppress(Warning.NONFINAL_FIELDS)
            .withIgnoredFields("creator", "fromServer", "storageFile", "initializing", "updating",
                    "suppressedInvalidate", "listeners", "tracks", "routes", "waypoints", "proxy", "segSpans", "modified")
            .withPrefabValues(WayPoint.class, new WayPoint(LatLon.NORTH_POLE), new WayPoint(LatLon.SOUTH_POLE))
            .withPrefabValues(ListenerList.class, ListenerList.create(), ListenerList.create())
            .withPrefabValues(GpxExtensionCollection.class, new GpxExtensionCollection(), col)
            .verify();
    }
}
