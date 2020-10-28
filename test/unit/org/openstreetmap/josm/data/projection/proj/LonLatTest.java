// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.data.projection.proj;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Tests for {@link LonLat}.
 */
class LonLatTest {
    /**
     * Setup rule.
     */
    @RegisterExtension
    @SuppressFBWarnings(value = "URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
    public JOSMTestRules test = new JOSMTestRules();

    /**
     * Test {@link LonLat#lonIsLinearToEast}
     */
    @Test
    void testLonIsLinearToEast() {
        assertFalse(new LonLat().lonIsLinearToEast());
    }
}
