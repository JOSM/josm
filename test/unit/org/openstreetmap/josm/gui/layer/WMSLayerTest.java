// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;
import org.openstreetmap.josm.testutils.annotations.LayerEnvironment;

/**
 * Unit tests of {@link WMSLayer} class.
 */
@BasicPreferences
@LayerEnvironment
class WMSLayerTest {
    /**
     * Unit test of {@link WMSLayer#WMSLayer}.
     */
    @Test
    void testWMSLayer() {
        WMSLayer wms = new WMSLayer(new ImageryInfo("test wms", "http://localhost"));
        MainApplication.getLayerManager().addLayer(wms);
        assertEquals(ImageryType.WMS, wms.getInfo().getImageryType());
    }

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/13828">Bug #13828</a>.
     */
    @Test
    void testTicket13828() {
        assertThrows(IllegalArgumentException.class, () -> new WMSLayer(new ImageryInfo("TMS", "http://203.159.29.217/try2/{z}/{x}/{y}.png")));
    }
}
