// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.osm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for class {@link DataIntegrityProblemException}.
 */
class DataIntegrityProblemExceptionTest {
    /**
     * Unit test of {@link DataIntegrityProblemException} constructor.
     */
    @Test
    void testDataIntegrityException() {
        DataIntegrityProblemException e1 = new DataIntegrityProblemException("foo");
        assertEquals("foo", e1.getMessage());
        assertNull(e1.getHtmlMessage());
        DataIntegrityProblemException e2 = new DataIntegrityProblemException("foo", "<html>bar</html>");
        assertEquals("foo", e2.getMessage());
        assertEquals("<html>bar</html>", e2.getHtmlMessage());
    }
}
