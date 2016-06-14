// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.EastNorth;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MapViewState.MapViewPoint;

/**
 * Test {@link MapViewState}
 * @author Michael Zangl
 */
public class MapViewStateTest {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private MapViewState state;

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Create the default state.
     */
    @Before
    public void setUp() {
        state = MapViewState.createDefaultState(WIDTH, HEIGHT);
    }

    /**
     * Test {@link MapViewState#getCenter()} returns map view center.
     */
    @Test
    public void testGetCenter() {
        MapViewPoint center = state.getCenter();
        assertHasViewCoords(WIDTH / 2, HEIGHT / 2, center);

        MapViewState newState = state.movedTo(center, new EastNorth(3, 4));

        // state should not change, but new state should.
        center = state.getCenter();
        assertHasViewCoords(WIDTH / 2, HEIGHT / 2, center);

        center = newState.getCenter();
        assertEquals("east", 3, center.getEastNorth().east(), 0.01);
        assertEquals("north", 4, center.getEastNorth().north(), 0.01);
    }

    private void assertHasViewCoords(double x, double y, MapViewPoint center) {
        assertEquals("x", x, center.getInViewX(), 0.01);
        assertEquals("y", y, center.getInViewY(), 0.01);
        assertEquals("x", x, center.getInView().getX(), 0.01);
        assertEquals("y", y, center.getInView().getY(), 0.01);
    }

    /**
     * Test {@link MapViewState#getForView(double, double)}
     */
    @Test
    public void testGetForView() {
        MapViewPoint corner = state.getForView(0, 0);
        assertHasViewCoords(0, 0, corner);

        MapViewPoint middle = state.getForView(120, 130);
        assertHasViewCoords(120, 130, middle);

        MapViewPoint fraction = state.getForView(0.12, 0.7);
        assertHasViewCoords(0.12, 0.7, fraction);

        MapViewPoint negative = state.getForView(-17, -30);
        assertHasViewCoords(-17, -30, negative);
    }

    /**
     * Test {@link MapViewState#getViewWidth()} and {@link MapViewState#getViewHeight()}
     */
    @Test
    public void testGetViewSize() {
        assertEquals(WIDTH, state.getViewWidth(), 0.01);
        assertEquals(HEIGHT, state.getViewHeight(), 0.01);
    }

    /**
     * Tests that all coordinate conversions for the point work.
     */
    @Test
    public void testPointConversions() {
        MapViewPoint p = state.getForView(WIDTH / 2, HEIGHT / 2);
        assertHasViewCoords(WIDTH / 2, HEIGHT / 2, p);


        EastNorth eastnorth = p.getEastNorth();
        LatLon shouldLatLon = Main.getProjection().getWorldBoundsLatLon().getCenter();
        EastNorth shouldEastNorth = Main.getProjection().latlon2eastNorth(shouldLatLon);
        assertEquals("east", shouldEastNorth.east(), eastnorth.east(), 0.01);
        assertEquals("north", shouldEastNorth.north(), eastnorth.north(), 0.01);
        MapViewPoint reversed = state.getPointFor(shouldEastNorth);
        assertHasViewCoords(WIDTH / 2, HEIGHT / 2, reversed);

        LatLon latlon = p.getLatLon();
        assertEquals("lat", shouldLatLon.lat(), latlon.lat(), 0.01);
        assertEquals("lon", shouldLatLon.lon(), latlon.lon(), 0.01);

        MapViewPoint p2 = state.getPointFor(new EastNorth(2, 3));
        assertEquals("east", 2, p2.getEastNorth().east(), 0.01);
        assertEquals("north", 3, p2.getEastNorth().north(), 0.01);
    }
}
