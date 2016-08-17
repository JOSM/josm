// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.Main;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.GpxLink;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Unit tests of {@link MarkerLayer} class.
 */
public class MarkerLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init(true);
        Main.pref.put("marker.traceaudio", true);
    }

    /**
     * Unit test of {@link MarkerLayer#MarkerLayer}.
     */
    @Test
    public void testMarkerLayer() {
        assertEquals(Color.magenta, MarkerLayer.getGenericColor());
        MarkerLayer layer = new MarkerLayer(new GpxData(), "foo", null, null);

        assertEquals("foo", layer.getName());
        assertEquals(Color.magenta, layer.getColorProperty().get());
        assertNotNull(layer.getIcon());
        assertEquals("0 markers", layer.getToolTipText());
        assertEquals("<html>foo consists of 0 markers</html>", layer.getInfoComponent());
        assertTrue(layer.getMenuEntries().length > 10);

        GpxData gpx = new GpxData();
        WayPoint wpt = new WayPoint(LatLon.ZERO);
        wpt.attr.put(GpxConstants.META_LINKS, Arrays.asList(new GpxLink("https://josm.openstreetmap.de")));
        wpt.addExtension("offset", "1.0");
        gpx.waypoints.add(wpt);
        wpt = new WayPoint(LatLon.ZERO);
        wpt.addExtension("offset", "NaN");
        gpx.waypoints.add(wpt);
        layer = new MarkerLayer(gpx, "bar", null, null);

        assertEquals("bar", layer.getName());
        assertEquals(Color.magenta, layer.getColorProperty().get());
        assertNotNull(layer.getIcon());
        assertEquals("3 markers", layer.getToolTipText());
        assertEquals("<html>bar consists of 3 markers</html>", layer.getInfoComponent());
        assertTrue(layer.getMenuEntries().length > 10);
    }
}
