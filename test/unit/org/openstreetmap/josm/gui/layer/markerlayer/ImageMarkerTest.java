// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImageMarker} class.
 */
@BasicPreferences
class ImageMarkerTest {
    /**
     * Unit test of {@link ImageMarker#ImageMarker}.
     * @throws MalformedURLException never
     */
    @Test
    void testImageMarker() throws MalformedURLException {
        ImageMarker marker = new ImageMarker(
                LatLon.ZERO,
                new URL("file://something.jpg"),
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        marker.actionPerformed(null);
        assertEquals("", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
    }
}
