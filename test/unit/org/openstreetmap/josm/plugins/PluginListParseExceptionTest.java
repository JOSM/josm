// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link PluginListParseException} class.
 */
public class PluginListParseExceptionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PluginListParseException#PluginListParseException}.
     */
    @Test
    public void testPluginListParseException() {
        NullPointerException npe = new NullPointerException();
        PluginListParseException ex = new PluginListParseException(npe);
        assertEquals(npe, ex.getCause());
        ex = new PluginListParseException("bar", npe);
        assertEquals("bar", ex.getMessage());
        assertEquals(npe, ex.getCause());
    }
}
