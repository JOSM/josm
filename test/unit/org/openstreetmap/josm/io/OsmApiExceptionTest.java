// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.io;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests of {@link OsmApiException} class.
 */
public class OsmApiExceptionTest {

    /**
     * Setup tests
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules().preferences();

    /**
     * Non-regression test for <a href="https://josm.openstreetmap.de/ticket/17328">Bug #17328</a>.
     */
    @Test
    public void testTicket17328() {
        assertFalse(new OsmApiException(503, "foo", "bar").isHtml());
        assertTrue(new OsmApiException(503, null, "<h2>This website is under heavy load (queue full)</h2><p>Sorry...</p>").isHtml());
    }
}
