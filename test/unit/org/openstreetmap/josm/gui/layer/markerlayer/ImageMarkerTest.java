// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link ImageMarker} class.
 */
@BasicPreferences
class ImageMarkerTest {
    @RegisterExtension
    static JOSMTestRules josmTestRules = new JOSMTestRules().main();

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
}
