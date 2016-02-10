// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.MalformedURLException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.Extensions;
import org.openstreetmap.josm.data.gpx.GpxConstants;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.AudioPlayer;

/**
 * Unit tests of {@link AudioMarker} class.
 */
public class AudioMarkerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link AudioMarker#AudioMarker}.
     * @throws MalformedURLException never
     */
    @Test
    public void testAudioMarker() throws MalformedURLException {
        AudioMarker marker = new AudioMarker(
                LatLon.ZERO,
                null,
                new File(TestUtils.getRegressionDataFile(6851, "20111003_121226.wav")).toURI().toURL(),
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        try {
            marker.actionPerformed(null);
        } finally {
            AudioPlayer.reset();
        }
        assertEquals("2", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
        Extensions ext = (Extensions) wpt.get(GpxConstants.META_EXTENSIONS);
        assertEquals("2.0", ext.get("offset"));
    }
}
