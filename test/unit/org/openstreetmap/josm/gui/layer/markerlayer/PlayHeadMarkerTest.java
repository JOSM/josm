// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer.markerlayer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.gpx.WayPoint;

/**
 * Unit tests of {@link PlayHeadMarker} class.
 */
class PlayHeadMarkerTest {
    /**
     * Unit test of {@link PlayHeadMarker#PlayHeadMarker}.
     */
    @Test
    void testPlayHeadMarker() {
        PlayHeadMarker marker = PlayHeadMarker.create();
        assertNotNull(marker);
        marker.actionPerformed(null);
        assertEquals("", marker.getText());
        WayPoint wpt = marker.convertToWayPoint();
        assertEquals(LatLon.ZERO, wpt.getCoor());
    }
}
