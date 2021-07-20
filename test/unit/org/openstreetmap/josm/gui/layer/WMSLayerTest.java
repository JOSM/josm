// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.testutils.JOSMTestRules;
import org.openstreetmap.josm.testutils.annotations.BasicPreferences;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link WMSLayer} class.
 */
@BasicPreferences
class WMSLayerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().main().projection();

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
        assertThrows(IllegalArgumentException.class, () -> new WMSLayer(new ImageryInfo("TMS", "http://203.159.29.217/try2/{z}/{x}/{y}.png")));
    }
}
