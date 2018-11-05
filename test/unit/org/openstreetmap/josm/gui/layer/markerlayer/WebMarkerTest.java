// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.PlatformHook;


import mockit.Expectations;
import mockit.Injectable;

/**
 * Unit tests of {@link WebMarker} class.
 */
public class WebMarkerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link WebMarker#WebMarker}.
     * @param mockPlatformHook platform hook mock
     * @throws Exception  in case of error
     */
    @Test
    public void testWebMarker(@Injectable final PlatformHook mockPlatformHook) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations(PlatformManager.class) {{
            PlatformManager.getPlatform(); result = mockPlatformHook;
        }};
        new Expectations() {{
            mockPlatformHook.openUrl("http://example.com"); result = null; times = 1;
        }};

        WebMarker marker = new WebMarker(
                LatLon.ZERO,
                new URL("http://example.com"),
                new MarkerLayer(new GpxData(), null, null, null),
                1d, 2d);
        marker.actionPerformed(null);
        assertEquals("", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
    }
}
