// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.gpx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.testutils.JOSMTestRules;

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
    public JOSMTestRules test = new JOSMTestRules();

    private GpxData data;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        data = new GpxData();
    }


    /**
     * Test method for {@link GpxData#mergeFrom(GpxData)}.
     */
    @Test
    public void testMergeFrom() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getTracks()},  {@link GpxData#addTrack(GpxTrack)},  {@link GpxData#removeTrack(GpxTrack)}.
     */
    @Test
    public void testTracks() {
        assertEquals(0, data.getTracks().size());

        ImmutableGpxTrack track1 = emptyGpxTrack();
        ImmutableGpxTrack track2 = emptyGpxTrack();
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
        WayPoint waypoint2 = new WayPoint(LatLon.ZERO);
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
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#isEmpty()}.
     */
    @Test
    public void testIsEmpty() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getMetaBounds()}.
     */
    @Test
    public void testGetMetaBounds() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#recalculateBounds()}.
     */
    @Test
    public void testRecalculateBounds() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#length()}.
     */
    @Test
    public void testLength() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getMinMaxTimeForTrack(GpxTrack)}.
     */
    @Test
    public void testGetMinMaxTimeForTrack() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getMinMaxTimeForAllTracks()}.
     */
    @Test
    public void testGetMinMaxTimeForAllTracks() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#nearestPointOnTrack(org.openstreetmap.josm.data.coor.EastNorth, double)}.
     */
    @Test
    public void testNearestPointOnTrack() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getLinesIterable(boolean[])}.
     */
    @Test
    public void testGetLinesIterable() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#resetEastNorthCache()}.
     */
    @Test
    public void testResetEastNorthCache() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getDataSources()}.
     */
    @Test
    public void testGetDataSources() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getDataSourceArea()}.
     */
    @Test
    public void testGetDataSourceArea() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#getDataSourceBounds()}.
     */
    @Test
    public void testGetDataSourceBounds() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#addChangeListener(GpxData.GpxDataChangeListener)}.
     */
    @Test
    public void testAddChangeListener() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#addWeakChangeListener(GpxData.GpxDataChangeListener)}.
     */
    @Test
    public void testAddWeakChangeListener() {
        fail("Not yet implemented");
    }

    /**
     * Test method for {@link GpxData#removeChangeListener(GpxData.GpxDataChangeListener)}.
     */
    @Test
    public void testRemoveChangeListener() {
        fail("Not yet implemented");
    }

    private static ImmutableGpxTrack emptyGpxTrack() {
        return new ImmutableGpxTrack(Collections.emptyList(), Collections.emptyMap());
    }

    private static ImmutableGpxTrack singleWaypointGpxTrack() {
        return new ImmutableGpxTrack(Collections.singleton(Collections.singleton(new WayPoint(LatLon.ZERO))), Collections.emptyMap());
    }

    /**
     * Unit test of methods {@link GpxData#equals} and {@link GpxData#hashCode}.
     */
    @Test
    public void testEqualsContract() {
        EqualsVerifier.forClass(GpxData.class).usingGetClass()
            .withIgnoredFields("attr", "creator", "fromServer", "storageFile", "listeners")
            .withPrefabValues(WayPoint.class, new WayPoint(LatLon.NORTH_POLE), new WayPoint(LatLon.SOUTH_POLE))
            .verify();
    }
}
