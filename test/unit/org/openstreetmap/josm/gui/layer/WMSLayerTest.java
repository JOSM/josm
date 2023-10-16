// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.Main;
import org.openstreetmap.josm.testutils.annotations.Projection;

/**
 * Unit tests of {@link WMSLayer} class.
 */
@Main
@Projection
class WMSLayerTest {
    /**
     * Unit test of {@link WMSLayer#WMSLayer}.
     */
    @Test
    void testWMSLayer() {
        WMSLayer wms = new WMSLayer(new ImageryInfo("test wms", "http://localhost"));
        MainApplication.getLayerManager().addLayer(wms);
        try {
            assertEquals(ImageryType.WMS, wms.getInfo().getImageryType());
        } finally {
            // Ensure we clean the place before leaving, even if test fails.
            MainApplication.getLayerManager().removeLayer(wms);
        }
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13828">Bug #13828</a>.
     */
    @Test
    void testTicket13828() {
        final ImageryInfo info = new ImageryInfo("TMS", "http://203.159.29.217/try2/{z}/{x}/{y}.png");
        assertThrows(IllegalArgumentException.class, () -> new WMSLayer(info));
    }
}
