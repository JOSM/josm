// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link WMTSLayer} class.
 */
class WMTSLayerTest {

    /**
     * Setup tests
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences().timeout(20000);

    /**
     * Unit test of {@link WMTSLayer#WMTSLayer}.
     */
    @Test
    void testWMTSLayer() {
        WMTSLayer wmts = new WMTSLayer(new ImageryInfo("test wmts", "http://localhost", "wmts", null, null));
        assertEquals(ImageryType.WMTS, wmts.getInfo().getImageryType());
    }
}
