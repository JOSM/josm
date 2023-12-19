// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.MapFrame;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link MarkerLayer} class.
 */
@BasicPreferences
@Main
@Projection
class MarkerLayerTest {
    /**
     * Setup tests
     */
    @BeforeEach
    public void setUp() {
        Config.getPref().putBoolean("marker.traceaudio", true);
    }

    /**
     * Unit test of {@link MarkerLayer#MarkerLayer}.
     */
    @Test
    void testMarkerLayer() {
        MarkerLayer layer = new MarkerLayer(new GpxData(), "foo", null, null);
        MainApplication.getLayerManager().addLayer(layer);

        assertEquals("foo", layer.getName());
        assertNull(layer.getColor());
        assertNotNull(layer.getIcon());
        assertEquals("0 markers", layer.getToolTipText());
        assertEquals("<html>foo consists of 0 markers</html>", layer.getInfoComponent());
        assertTrue(layer.getMenuEntries().length > 10);

        GpxData gpx = new GpxData();
        WayPoint wpt = new WayPoint(LatLon.ZERO);
        wpt.attr.put(GpxConstants.META_LINKS, Collections.singletonList(new GpxLink("https://josm.openstreetmap.de")));
        wpt.getExtensions().add("josm", "offset", "1.0");
        gpx.waypoints.add(wpt);
        wpt = new WayPoint(LatLon.ZERO);
        wpt.getExtensions().add("josm", "offset", "NaN");
        gpx.waypoints.add(wpt);
        layer = new MarkerLayer(gpx, "bar", null, null);

        assertEquals("bar", layer.getName());
        assertNull(layer.getColor());
        assertNotNull(layer.getIcon());
        assertEquals("3 markers", layer.getToolTipText());
        assertEquals("<html>bar consists of 3 markers</html>", layer.getInfoComponent());
        assertTrue(layer.getMenuEntries().length > 10);
    }

    /**
     * Unit test of {@code Main.map.mapView.playHeadMarker}.
     */
    @Test
    void testPlayHeadMarker() {
        try {
            MainApplication.getLayerManager().addLayer(new OsmDataLayer(new DataSet(), "", null));
            MapFrame map = MainApplication.getMap();
            MarkerLayer layer = new MarkerLayer(new GpxData(), null, null, null);
            assertNull(map.mapView.playHeadMarker);
            MainApplication.getLayerManager().addLayer(layer);
            assertNotNull(map.mapView.playHeadMarker);
            MainApplication.getLayerManager().removeLayer(layer);
        } finally {
            if (MainApplication.isDisplayingMapView()) {
                MainApplication.getMap().mapView.playHeadMarker = null;
            }
        }
    }
}
