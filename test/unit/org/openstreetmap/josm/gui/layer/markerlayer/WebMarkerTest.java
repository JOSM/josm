// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.GpxData;
import org.openstreetmap.josm.data.gpx.WayPoint;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.tools.PlatformManager;
import org.openstreetmap.josm.tools.PlatformHook;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;

/**
 * Unit tests of {@link WebMarker} class.
 */
public class WebMarkerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().https();

    /**
     * Unit test of {@link WebMarker#WebMarker}.
     * @param mockPlatformHook platform hook mock
     * @param platformManager {@link PlatformManager} mock
     * @throws Exception  in case of error
     */
    @Test
    public void testWebMarker(@Injectable final PlatformHook mockPlatformHook,
                              @Mocked final PlatformManager platformManager) throws Exception {
        TestUtils.assumeWorkingJMockit();
        new Expectations() {{
            PlatformManager.getPlatform(); result = mockPlatformHook;
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
