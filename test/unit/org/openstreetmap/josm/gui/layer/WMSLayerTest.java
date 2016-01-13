// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.gui.layer;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;
import org.openstreetmap.josm.data.imagery.ImageryInfo;
import org.openstreetmap.josm.data.imagery.ImageryInfo.ImageryType;

/**
 * Unit tests of {@link WMSLayer} class.
 */
public class WMSLayerTest {

    /**
     * Setup tests
     */
    @BeforeClass
    public static void setUpBeforeClass() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link WMSLayer#WMSLayer}.
     */
    @Test
    public void testWMSLayer() {
        WMSLayer wms = new WMSLayer(new ImageryInfo("test wms", "http://localhost"));
        assertEquals(ImageryType.WMS, wms.getInfo().getImageryType());
    }
}
