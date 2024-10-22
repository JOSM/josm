// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.gui.layer.geoimage.ImageViewerDialog;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.tools.PlatformManager;

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

    /**
     * Windows does not like {@code :} to appear multiple times in a path.
     * @throws MalformedURLException if the URI fails to create and convert to a URL.
     */
    @Test
    void testNonRegression23978() throws MalformedURLException {
        final URL testURL;
        if (PlatformManager.isPlatformWindows()) {
            // This throws the InvalidPathException (subclass of IllegalArgumentException), and is what the initial problem was.
            testURL = URI.create("file:/c:/foo/c:/bar/image.jpg").toURL();
        } else {
            // This throws an IllegalArgumentException.
            testURL = new URL("file:/foobar/image.jpg#hashtagForIAE");
        }
        ImageMarker imageMarker = new ImageMarker(LatLon.ZERO, testURL,
                new MarkerLayer(new GpxData(), null, null, null), 0, 0);
        assertDoesNotThrow(() -> imageMarker.actionPerformed(null));
    }
}
