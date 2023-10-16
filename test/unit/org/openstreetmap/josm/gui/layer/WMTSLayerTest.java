// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

/**
 * Unit tests of {@link WMTSLayer} class.
 */
@BasicPreferences
@Timeout(20)
class WMTSLayerTest {
    /**
     * Unit test of {@link WMTSLayer#WMTSLayer}.
     */
    @Test
    void testWMTSLayer() {
        WMTSLayer wmts = new WMTSLayer(new ImageryInfo("test wmts", "http://localhost", "wmts", null, null));
        assertEquals(ImageryType.WMTS, wmts.getInfo().getImageryType());
    }
}
