// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openstreetmap.josm.JOSMFixture;

/**
 * Unit tests of {@link PluginDownloadException} class.
 */
public class PluginDownloadExceptionTest {

    /**
     * Setup test.
     */
    @BeforeClass
    public static void setUp() {
        JOSMFixture.createUnitTestFixture().init();
    }

    /**
     * Unit test of {@link PluginDownloadException#PluginDownloadException}.
     */
    @Test
    public void testPluginDownloadException() {
        PluginDownloadException ex = new PluginDownloadException("foo");
        assertEquals("foo", ex.getMessage());
        NullPointerException npe = new NullPointerException();
        ex = new PluginDownloadException(npe);
        assertEquals(npe, ex.getCause());
        ex = new PluginDownloadException("bar", npe);
        assertEquals("bar", ex.getMessage());
        assertEquals(npe, ex.getCause());
    }
}
