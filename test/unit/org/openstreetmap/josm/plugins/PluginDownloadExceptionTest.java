// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PluginDownloadException} class.
 */
public class PluginDownloadExceptionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
