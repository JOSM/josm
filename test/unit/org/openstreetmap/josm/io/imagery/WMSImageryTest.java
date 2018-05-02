// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.TestUtils;
import org.openstreetmap.josm.io.imagery.WMSImagery.WMSGetCapabilitiesException;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link WMSImagery} class.
 */
public class WMSImageryTest {

    /**
     * Setup test
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@code WMSImagery.WMSGetCapabilitiesException} class
     */
    @Test
    public void testWMSGetCapabilitiesException() {
        Exception cause = new Exception("test");
        WMSGetCapabilitiesException exc = new WMSGetCapabilitiesException(cause, "bar");
        assertEquals(cause, exc.getCause());
        assertEquals("bar", exc.getIncomingData());
        exc = new WMSGetCapabilitiesException("foo", "bar");
        assertEquals("foo", exc.getMessage());
        assertEquals("bar", exc.getIncomingData());
    }

    /**
     * Non-regression test for bug #15730.
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException never
     */
    @Test
    public void testTicket15730() throws IOException, WMSGetCapabilitiesException {
        try (InputStream is = TestUtils.getRegressionDataStream(15730, "capabilities.xml")) {
            WMSImagery wms = new WMSImagery();
            wms.parseCapabilities(null, is);
            assertEquals(1, wms.getLayers().size());
            assertTrue(wms.getLayers().get(0).abstr.startsWith("South Carolina  NAIP Imagery 2017    Resolution: 100CM "));
        }
    }

    /**
     * Non-regression test for bug #16248.
     * @throws IOException if any I/O error occurs
     * @throws WMSGetCapabilitiesException never
     */
    @Test
    public void testTicket16248() throws IOException, WMSGetCapabilitiesException {
        try (InputStream is = TestUtils.getRegressionDataStream(16248, "capabilities.xml")) {
            WMSImagery wms = new WMSImagery();
            wms.parseCapabilities(null, is);
            assertEquals("http://wms.hgis.cartomatic.pl/topo/3857/m25k", wms.getServiceUrl().toExternalForm());
        }
    }
}
