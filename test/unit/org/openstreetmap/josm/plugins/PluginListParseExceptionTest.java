// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link PluginListParseException} class.
 */
public class PluginListParseExceptionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

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
