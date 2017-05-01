// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Rule;
import org.junit.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Unit tests for class {@link DataIntegrityProblemException}.
 */
public class DataIntegrityProblemExceptionTest {

    /**
     * Setup test.
     */
    @Rule
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Unit test of {@link DataIntegrityProblemException} constructor.
     */
    @Test
    public void testDataIntegrityException() {
        DataIntegrityProblemException e1 = new DataIntegrityProblemException("foo");
        assertEquals("foo", e1.getMessage());
        assertNull(e1.getHtmlMessage());
        DataIntegrityProblemException e2 = new DataIntegrityProblemException("foo", "<html>bar</html>");
        assertEquals("foo", e2.getMessage());
        assertEquals("<html>bar</html>", e2.getHtmlMessage());
    }
}
