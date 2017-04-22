// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io.imagery;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
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
}
