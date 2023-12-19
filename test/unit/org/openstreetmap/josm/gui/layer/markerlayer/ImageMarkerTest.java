// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;

/**
 * Unit tests of {@link ImageMarker} class.
 */
@BasicPreferences
@Main
class ImageMarkerTest {
    @AfterEach
    void tearDown() {
        if (ImageViewerDialog.hasInstance()) {
            ImageViewerDialog.getInstance().destroy();
        }
    }

    /**
     * Unit test of {@link ImageMarker#ImageMarker}.
     * @throws MalformedURLException never
     */
    @Test
    void testImageMarker() throws MalformedURLException {
        ImageMarker marker = new ImageMarker(
                LatLon.ZERO,
                new File(TestUtils.getRegressionDataFile(12255, "G0016941.JPG")).toURI().toURL(),
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        marker.actionPerformed(null);
        assertEquals("", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
    }

    /**
     * Non-regression test for #22638: NoSuchFileException causes a crash
     */
    @Test
    void testTicket22638() throws MalformedURLException {
        ImageMarker marker = new ImageMarker(
                LatLon.ZERO,
                new File(TestUtils.getRegressionDataFile(12255, "no_such.jpg")).toURI().toURL(),
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        assertDoesNotThrow(() -> marker.actionPerformed(null));
    }
}
