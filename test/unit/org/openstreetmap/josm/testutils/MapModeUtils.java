// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.testutils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.event.MouseEvent;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.awaitility.Durations;
import org.openstreetmap.josm.data.coor.ILatLon;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.util.GuiHelper;

/**
 * Utils for doing stuff in the {@link org.openstreetmap.josm.actions.mapmode.MapMode}
 */
public final class MapModeUtils {
    private MapModeUtils() {
        // Hide the constructor
    }

    /**
     * Click at a specified lat/lon
     * Note that we use {@link org.openstreetmap.josm.actions.mapmode.MapMode} from {@link org.openstreetmap.josm.gui.MapFrame}
     * from {@link MainApplication#getMap()}.
     * @param coordinates The coordinates to click at (lat, lon, lat, lon, ...)
     */
    public static void clickAt(double... coordinates) {
        assertEquals(0, coordinates.length % 2, "coordinates must be a multiple of 2");
        for (int i = 0; i < coordinates.length; i += 2) {
            clickAt(new LatLon(coordinates[i], coordinates[i + 1]));
        }
    }

    /**
     * Click at a specified lat/lon
     * Note that we use {@link org.openstreetmap.josm.actions.mapmode.MapMode} from {@link org.openstreetmap.josm.gui.MapFrame}
     * from {@link MainApplication#getMap()}.
     * @param coordinates The coordinates to click at
     */
    public static void clickAt(ILatLon... coordinates) {
        assertEquals(0, coordinates.length % 2, "coordinates must be a multiple of 2");
        for (ILatLon coordinate : coordinates) {
            clickAt(coordinate);
        }
    }

    /**
     * Click at a specified lat/lon
     * Note that we use {@link org.openstreetmap.josm.actions.mapmode.MapMode} from {@link org.openstreetmap.josm.gui.MapFrame}
     * from {@link MainApplication#getMap()}.
     * @param location The location to click at
     */
    public static void clickAt(ILatLon location) {
        clickAt(1, location);
    }

    /**
     * Click at a specified lat/lon
     * Note that we use {@link org.openstreetmap.josm.actions.mapmode.MapMode} from {@link org.openstreetmap.josm.gui.MapFrame}
     * from {@link MainApplication#getMap()}.
     * @param location The location to click at
     * @param times The number of times to click
     */
    public static void clickAt(int times, ILatLon location) {
        for (int i = 0; i < times; i++) {
            final var click = mouseClickAt(location);
            MainApplication.getMap().mapMode.mousePressed(click);
            MainApplication.getMap().mapMode.mouseReleased(click);
            GuiHelper.runInEDTAndWait(() -> { /* Sync UI thread */ });
        }
    }

    /**
     * Perform a click-n-drag operation
     * @param from The originating point
     * @param to The end point
     */
    public static void dragFromTo(ILatLon from, ILatLon to) {
        MainApplication.getMap().mapMode.mousePressed(mouseClickAt(from));
        // Some actions wait a period of time to avoid accidental dragging.
        Awaitility.await().pollDelay(Durations.FIVE_HUNDRED_MILLISECONDS).atLeast(490, TimeUnit.MILLISECONDS).until(() -> true);
        MainApplication.getMap().mapMode.mouseDragged(mouseClickAt(from));
        MainApplication.getMap().mapMode.mouseDragged(mouseClickAt(to));
        MainApplication.getMap().mapMode.mouseReleased(mouseClickAt(to));
        GuiHelper.runInEDTAndWait(() -> { /* Sync UI thread */ });
    }

    /**
     * Create the click event
     * @param location The location for the click event
     * @return The click event
     */
    public static MouseEvent mouseClickAt(ILatLon location) {
        final var mapView = MainApplication.getMap().mapView;
        mapView.zoomTo(mapView.getCenter(), 0.005);
        mapView.zoomTo(location);
        final var point = mapView.getPoint(location);
        return new MouseEvent(MainApplication.getMap(), Long.hashCode(System.currentTimeMillis()),
                System.currentTimeMillis(), 0, point.x, point.y, 1, false, MouseEvent.BUTTON1);
    }
}
